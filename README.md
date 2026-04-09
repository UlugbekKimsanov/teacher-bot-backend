# 7EDU Teacher Bot — Backend

O'zbek tili o'rganish platformasi uchun REST API.

## Tech Stack

- Java 17
- Spring Boot 3.2.5
- Spring WebFlux (reaktiv, non-blocking)
- Spring Security + JWT (jjwt 0.12.5)
- R2DBC + PostgreSQL (reaktiv DB)
- Flyway (DB migratsiyalar)
- Lombok

## Arxitektura

```
src/main/java/uz/sevenEdu/teacherBot/
├── common/
│   ├── config/         # SecurityConfig (WebFlux Security, BCrypt)
│   ├── exception/      # GlobalExceptionHandler, NotFoundException, BadRequestException, UnauthorizedException
│   └── response/       # ApiResponse<T> (success, message, data)
├── auth/
│   ├── controller/     # AuthController  →  POST /auth/register, /auth/login
│   ├── service/        # AuthService (interface) + AuthServiceImpl
│   ├── repository/     # UserRepository (ReactiveCrudRepository)
│   ├── entity/         # User
│   ├── dto/            # RegisterRequest, LoginRequest, AuthResponse
│   └── security/       # JwtUtil, JwtAuthFilter (WebFilter)
├── course/
│   ├── controller/     # CourseController  →  GET /courses, /courses/{id}, POST /courses/{id}/enroll
│   ├── service/        # CourseService + CourseServiceImpl
│   ├── repository/     # CourseRepository, UserCourseRepository
│   ├── entity/         # Course, UserCourse
│   └── dto/            # CourseDto
├── lesson/
│   ├── controller/     # LessonController  →  GET /courses/{id}/lessons, /lessons/{id}, POST /lessons/{id}/test/submit, /lessons/{id}/ask
│   ├── service/        # LessonService + LessonServiceImpl
│   ├── repository/     # LessonRepository, VocabularyRepository, QuestionRepository, TeacherQuestionRepository
│   ├── entity/         # Lesson, Vocabulary, Question, TeacherQuestion
│   └── dto/            # LessonDetailDto, TestSubmitRequest
└── rating/
    ├── controller/     # RatingController  →  GET /rating/attendance/{courseId}, /rating/points, /rating/progress/{courseId}, /rating/certificates
    ├── service/        # RatingService + RatingServiceImpl
    ├── repository/     # AttendanceRepository, PointsRepository, CertificateRepository
    ├── entity/         # Attendance, Points, Certificate
    └── dto/            # RatingDto (AttendanceDto, PointsSummary, ProgressDto, CertificateDto)
```

## API Endpointlar

| Method | URL | Auth | Tavsif |
|--------|-----|------|--------|
| POST | `/api/v1/auth/register` | — | Ro'yxatdan o'tish |
| POST | `/api/v1/auth/login` | — | Kirish, JWT qaytaradi |
| GET | `/api/v1/courses` | optional | Barcha kurslar |
| GET | `/api/v1/courses/category/{category}` | optional | Kategoriya bo'yicha |
| GET | `/api/v1/courses/{id}` | optional | Bitta kurs |
| POST | `/api/v1/courses/{id}/enroll` | JWT | Kursga yozilish |
| GET | `/api/v1/courses/{courseId}/lessons` | JWT | Kurs darslari |
| GET | `/api/v1/lessons/{lessonId}` | JWT | Dars + lug'at + testlar |
| POST | `/api/v1/lessons/{lessonId}/test/submit` | JWT | Test topshirish |
| POST | `/api/v1/lessons/{lessonId}/ask` | JWT | Ustozga savol |
| GET | `/api/v1/rating/attendance/{courseId}` | JWT | Davomat statistikasi |
| GET | `/api/v1/rating/points` | JWT | Ball statistikasi |
| GET | `/api/v1/rating/progress/{courseId}` | JWT | O'zlashtirish |
| GET | `/api/v1/rating/certificates` | JWT | Sertifikatlar |

## Response format

```json
{
  "success": true,
  "data": { ... }
}
```

Xato bo'lsa:
```json
{
  "success": false,
  "message": "Xato matni"
}
```

## Database (PostgreSQL)

Jadvallar (Flyway `V1__init.sql` orqali avtomatik yaratiladi):

| Jadval | Tavsif |
|--------|--------|
| `users` | Foydalanuvchilar |
| `courses` | Kurslar (til, IT, SMM va h.k.) |
| `lessons` | Darslar (video URL, tartib) |
| `vocabulary` | Lug'at (UZ ↔ EN) |
| `tests` | Test (lesson bilan bog'liq) |
| `questions` | Test savollari (A/B/C variantlar) |
| `user_courses` | Foydalanuvchi ↔ Kurs (progress) |
| `user_lessons` | Dars natijasi (vocab/test/questions score) |
| `attendance` | Davomat (kun bo'yicha) |
| `points` | Ball tarixi |
| `certificates` | Sertifikatlar |
| `teacher_questions` | Ustozga yuborilgan savollar |

## Ishga tushirish

**1. PostgreSQL database yarating:**
```sql
CREATE DATABASE teacher_bot;
```

**2. `application.yml` da connection sozlang** (agar kerak bo'lsa):
```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/teacher_bot
    username: postgres
    password: postgres
```

**3. Run:**
```bash
mvn spring-boot:run
```

Flyway avtomatik migrationlarni ishga tushiradi va seed data qo'shadi.

Server: `http://localhost:8080`

## Security

- Public: `POST /api/v1/auth/**`, `GET /api/v1/courses/**`
- Protected: boshqa barcha endpointlar `Authorization: Bearer <token>` talab qiladi
- Token: JWT, 24 soat amal qiladi
