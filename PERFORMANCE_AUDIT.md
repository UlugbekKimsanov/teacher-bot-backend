# Backend Performance Audit — 100K foydalanuvchi uchun

> 10 parallel agent tahlili (faqat o'qish). Maqsad: platforma 100K user'da qotmasdan, barqaror ishlashi. Jiddiylik bo'yicha tartiblangan + aniq yechimlar.

## Umumiy xulosa
Hozirgi holatda backend **100K user'ni ko'tarmaydi**. Asosiy naqsh — **per-user N+1 reaktiv so'rovlar** (`findByRole(...).flatMap(x -> findById/sumBy...)`), **indekslar yo'q**, **connection pool default 10 + cheksiz kutish**, **kesh yo'q**, **rate-limiting yo'q**. Yaxshi tomoni: deyarli barcha kod toza reaktiv (`.block()` yo'q), tashqi mijozlar (SMS/Telegram) WebClient'da.

---

## 🔴 KRITIK (100K'da serverni qulatadi — birinchi navbatda)

### K1. Leaderboard — N+1 (har ochilishda 100K+ so'rov)
`rating/service/RatingServiceImpl.java:187 getLeaderboard` — `findByRole("STUDENT")` (barcha 100K) → har biri uchun `sumByUserId` → xotirada sort. Bitta ochilish = 100K+ so'rov.
**Yechim:** Bitta agregat:
```sql
SELECT u.id, u.first_name, u.last_name, COALESCE(SUM(p.amount),0) total
FROM users u LEFT JOIN points p ON p.user_id=u.id
WHERE u.role='STUDENT' AND (u.is_guest IS NULL OR u.is_guest=false)
GROUP BY u.id ORDER BY total DESC LIMIT 50;
```
`myRank` — alohida `COUNT(*)+1 WHERE total > :my`. **Eng yaxshisi:** ball qo'shilganда `users.ball` (mavjud) yoki Redis Sorted Set (`ZINCRBY`/`ZREVRANGE`) ga inkrement yozish → o'qish O(log N).

### K2. Cron `snapshotAllDailyGoals` — 23:59da ~500K so'rov
`rating/scheduler/DailyGoalScheduler.java` → `RatingServiceImpl.java:327` → har student uchun `buildTodayGoals` (4 SELECT) + upsert. 100K × 5 = 500K so'rov bir zumda → pool tugaydi, ilova qotadi.
**Yechim:** Set-based bitta SQL — `INSERT INTO daily_goal_history SELECT ... FROM users u LEFT JOIN daily_activity ... LEFT JOIN (vocab GROUP BY) ... LEFT JOIN user_goal_settings ... WHERE role='STUDENT' ON CONFLICT DO UPDATE`. 500K → 1 so'rov.

### K3. Cron `InactivityReminderScheduler` — ~300K so'rov + FCM to'foni (18:00)
`notification/service/InactivityReminderScheduler.java:43` — `findByRole` + per-user `filterWhen`(findByUserId) + `findLatestInactivity` + per-push `findById`. FCM `FcmService.java:31` har token uchun alohida HTTP → quota-exceeded, jim yo'qoladi.
**Yechim:** Nomzodlarni bitta JOIN SQL bilan ajratish (`last_active_at < CURRENT_DATE` AND EXISTS user_courses AND eskalatsiya). Notification INSERT batch. FCM **multicast** (`sendEachForMulticast`, 500/batch) + `flatMap(concurrency=20..50)` + throttle.

### K4. R2DBC pool default (10) + cheksiz kutish + `r2dbc-pool` dependency yo'q
`application.yml` da `spring.r2dbc.pool` bloki yo'q → max-size=10, **max-acquire-time=0 (cheksiz)**. Bitta DB sekinlashuvi butun serverni osib qo'yadi.
**Yechim:**
```xml
<dependency><groupId>io.r2dbc</groupId><artifactId>r2dbc-pool</artifactId></dependency>
```
```yaml
spring.r2dbc.pool:
  enabled: true
  initial-size: 10
  max-size: 50            # har instans
  max-acquire-time: 5s    # MUHIM — cheksiz kutishni to'xtatadi
  max-create-connection-time: 5s
  max-life-time: 30m
  max-idle-time: 30m
```
+ Postgres `max_connections=500` (docker-compose `command`), + **PgBouncer** (transaction-mode) — minglab client'ni kam fizik connection'ga multiplex. + **Gorizontal scale: 4-8 instans** LB ortida (pool kattaligi user soniga emas, instans soniga bog'liq).

### K5. DB indekslari yo'q — issiq jadvallarda full-table scan
Eng kritiklari (yangi migration `V19__perf_indexes.sql`):
```sql
CREATE INDEX idx_points_user_id           ON points(user_id);
CREATE INDEX idx_points_user_created      ON points(user_id, created_at DESC);
CREATE INDEX idx_user_lessons_lesson_id   ON user_lessons(lesson_id);
CREATE INDEX idx_user_lessons_user_completed ON user_lessons(user_id, completed_at);
CREATE INDEX idx_notifications_user_read_created ON notifications(user_id, is_read, created_at DESC);
CREATE INDEX idx_users_role               ON users(role);
CREATE INDEX idx_users_last_active        ON users(last_active_at);
CREATE INDEX idx_attendance_attended_at   ON attendance(attended_at);
CREATE INDEX idx_lessons_course_id        ON lessons(course_id);
CREATE INDEX idx_user_courses_course_id   ON user_courses(course_id);
CREATE INDEX idx_vocabulary_lesson_id     ON vocabulary(lesson_id);
CREATE INDEX idx_exercises_lesson_id      ON exercises(lesson_id);
CREATE INDEX idx_tests_lesson_id          ON tests(lesson_id);
CREATE INDEX idx_questions_test_id        ON questions(test_id);
CREATE INDEX idx_orders_status_created    ON orders(status, created_at DESC);
-- to'lov callback'lari:
CREATE INDEX idx_click_merchant_trans     ON click_transaction(merchant_trans_id);
CREATE INDEX idx_payme_merchant_trans     ON payme_transaction(merchant_trans_id);
-- (paynet/uzum/alif uchun ham merchant_trans_id)
```
Katta jadvallarда production'да `CREATE INDEX CONCURRENTLY`.

### K6. `enrichCourse` N+1 — har bosh ekranda 100-250 so'rov
`course/service/CourseServiceImpl.java:80` — har kurs uchun 5 so'rov (til, duration, enrollment, lessons, completed). Bosh ekran eng ko'p ochiladi.
**Yechim:** `courseId`/`languageId` larни yig'ib `IN (:ids)` batch + `GROUP BY` (duration), enrollment'ni bir `findByUserId` Set bilan. + **Caffeine cache** statik qism uchun (TTL 10-30 min).

### K7. TeacherPanel — `findAll()` butun jadvalни RAMga (OOM)
`admin/controller/TeacherPanelController.java` (`userLessonRepository.findAll()`, `lessonRepository.findAll().filter(...)`) — millionlab qator xotiraga, Java'da filter. 100K'da OutOfMemory.
**Yechim:** `findByCourseIdIn(...)`/`findByUserIdInAndCourseIdIn(...)` + DB `GROUP BY` agregat + pagination. `findAll().filter()` ni hech qachon ishlatmaslik.

### K8. Rate-limiting umuman yo'q (xavfsizlik + barqarorlik)
`SecurityConfig.java:34 anyExchange().permitAll()`, hech qanday throttle. `/auth/send-otp` ni spam qilib **real SMS pulini drenaj** qilish, OTP brute-force, API DoS mumkin.
**Yechim:** Bucket4j + Redis WebFilter: send-otp 1/60s + 5/kun, login verify 5 urinish→15min blok, global IP 100 req/min.

### K9. WebSocket chat — single-node in-memory, multi-instansда buzuq
`chat/.../ChatWebSocketHandler.java:34` — `roomSinks` JVM `ConcurrentHashMap`, `onBackpressureBuffer()` cheksiz. 100K WS bitta nodeni bosadi; 2+ instansда A↔B yetkazmaydi.
**Yechim:** **Redis Pub/Sub** broadcast (room kanaliga publish, har instans local relay) + bounded buffer (`onBackpressureBuffer(256)`) + chat history pagination (`ORDER BY created_at DESC LIMIT 50`, hozir LIMITsiz).

---

## 🟠 YUQORI

### Y1. Kesh yo'q (statik/sekin-o'zgaruvchi data har so'rovda DB'dan)
`courses`, `languages`(+studentCount), `achievements katalogi`, `subjects`, `break-music`, `books`, `news` — har GET'da DB. Hozir `@EnableCaching`/Caffeine yo'q (Redis faqat OTP'da).
**Yechim:** **Caffeine** in-memory (statik kataloglar, TTL 10-30 min); **Redis** (leaderboard, cross-instance). WebFlux'да `Mono` ni `@Cacheable` qila olmaysiz — `CacheMono`/`.cache()` ishlatish. Invalidatsiya: admin CRUD'da evict.

### Y2. Bloklovchi fayl I/O event-loop'da
`common/service/FileStorageService.java` — `Files.createDirectories`/`deleteIfExists` `saveFile`да sinxron (event-loop'da); `user/controller/UserController.java:55` avatar `createDirectories` `subscribeOn` siz.
**Yechim:** `Mono.fromRunnable(...).subscribeOn(Schedulers.boundedElastic())` ga o'rash.

### Y3. Statik fayllar Netty orqali + cache header/optimize yo'q
`common/config/WebConfig.java:21` `/files/**` Netty serve, `Cache-Control` yo'q, rasm resize/WebP yo'q. Bandwidth + event-loop yuki.
**Yechim:** nginx'да `/files/**` alohida static `location` (diskdan, `expires 30d, immutable`); strategik — **S3/MinIO + CDN**; upload'да rasm resize+WebP (boundedElastic'да).

### Y4. Pagination yo'q (unbounded)
Admin `users`/`sales`/`income` (LIMITsiz `findAll`/`collectList`), chat history, `notifications`, `getPoints`, `getStreak` (barcha sanalar).
**Yechim:** Keyset pagination (`WHERE created_at < :before ORDER BY ... DESC LIMIT :n`); admin uchun `Pageable`. `sales/stats` ni SQL `SUM/GROUP BY` ga.

### Y5. Per-request og'ir hisob (precompute kerak)
`getStreak`(har safar sana ketma-ketligi), `getProgress`/`getDailyGoals`/`buildTodayGoals`(userLesson xotirada filter), `AchievementService.gatherStats`(`findByUserId` 2 marta + per-course N+1, har submit'да), admin `sales/stats`/`income`(N+1).
**Yechim:** Streak'ni `recordAttendance`да precompute (`user_streak` jadvali); `getDailyGoals` lessonsDone'ni DB `COUNT(... completed_at>=CURRENT_DATE)`; achievement katalogini cache + `findByUserId` bir marta.

### Y6. Tashqi xizmatlarда timeout/circuit-breaker yo'q
`EskizSmsService`, `TelegramBotService`, payment WebClient'lar `responseTimeout` siz → osilsa kaskad failure.
**Yechim:** Har WebClient'ga `responseTimeout(5-10s)` + Resilience4j circuit breaker + cheklangan retry. Eskiz token'ni TTL bilan cache.

### Y7. Netty/observability/shutdown sozlanmagan
`server.netty` idle/connection-timeout yo'q, graceful shutdown yo'q, Actuator/metrics yo'q.
**Yechim:**
```yaml
server: { netty: { connection-timeout: 5s, idle-timeout: 30s }, shutdown: graceful }
spring.lifecycle.timeout-per-shutdown-phase: 25s
```
+ `spring-boot-starter-actuator` + Micrometer/Prometheus + K8s liveness/readiness.

---

## 🟡 O'RTA
- **flatMap concurrency 256 vs pool 10** — cron/og'ir oqimlarда `flatMap(fn, 8..16)` bilan cheklash.
- **`JwtAuthFilter` `activeTouched` Map cheksiz o'sadi** (heap leak) → Redis `SET active:{id} 1 EX 86400 NX`.
- **JVM heap/GC** sozlanmagan → Dockerfile `-XX:MaxRAMPercentage=75 -XX:+UseG1GC`.
- **GlobalExceptionHandler** ichki xato xabarini oshkor qiladi → generic xabar + log.
- **CORS `*` + allowCredentials(true)** → aniq origin ro'yxati.

> Xavfsizlik eslatmasi (alohida): JWT secret + Click key + SMTP parol git'da plain-text, `55555` OTP bypass production kodida — bular performance emas, lekin 100K'дан oldin albatta hал qilinishi shart.

---

## Tavsiya etilgan amal rejasi (bosqichlar)

**1-bosqich — "tez yutuq" (1-2 kun, eng katta ROI):**
- K5 indekslar (V19 migration) — darrov.
- K4 R2DBC pool config + r2dbc-pool dependency.
- K1 leaderboard agregat so'rov.
- K6 enrichCourse batch.
- Y2 bloklovchi fayl I/O → boundedElastic.

**2-bosqich — masshtab (3-5 kun):**
- K2/K3 cron'larni set-based SQL + FCM multicast.
- K7/K8 TeacherPanel findAll + admin pagination.
- Y1 Caffeine cache (kataloglar) + Redis leaderboard.
- Y6 WebClient timeout + circuit breaker.

**3-bosqich — infratuzilma:**
- K9 WebSocket Redis Pub/Sub.
- K8 rate-limiting (Bucket4j+Redis).
- Y3 nginx static + CDN/S3.
- Y7 Actuator + graceful shutdown + JVM + PgBouncer + 4-8 instans.

---
*Tahlil: 10 parallel agent, 2026-06-25. Faqat o'qish — kod o'zgartirilmadi.*
