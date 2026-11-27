# Dars videolari: faststart, tejamkor variant va nginx orqali berish

Backend har bir yuklangan dars videosi uchun fonda (`VideoTranscodeService`) ikki ish bajaradi:

1. **Faststart** — MP4/MOV faylda `moov` atomi boshiga ko'chiriladi (qayta kodlashsiz,
   `-c copy`). Pleer butun faylni kutmasdan darhol o'ynay boshlaydi. Natija yangi nom
   (`video_{id}_{uid}.mp4`) bilan yoziladi, `lessons.video_url` almashtiriladi, eski fayl
   (boshqa dars ishlatmasa) o'chiriladi.
2. **Tejamkor variant** — ~480p, H.264 (CRF 27, maxrate 450–750 kbps) + AAC 64 kbps:
   `lessons/{id}/video_low_{id}_{uid}.mp4` → `lessons.video_url_low`.
   Asl video allaqachon tejamkor bo'lsa (qisqa tomoni ≤ 480 va bitreyti ≤ 1000 kbps) yoki
   natija asl hajmning 85% idan kichik chiqmasa — yaratilmaydi (`SKIPPED`).

Ishlar **qat'iy bittadan**, alohida fon oqimida, `nice -n 10` va `-threads 2` bilan bajariladi —
API so'rovlariga xalaqit bermaydi.

Holatlar (`lessons.video_low_status`): `NULL` (hali ishlanmagan), `PENDING`, `PROCESSING`,
`READY`, `SKIPPED`, `FAILED` (sababi `video_low_error` da).

Mobil API (`LessonDetailDto`): `videoUrlLow` (faqat `READY` va fayl mavjud bo'lsa),
`videoSize`, `videoLowSize` (bayt).

## 1. Serverga ffmpeg o'rnatish

```bash
apt update && apt install -y ffmpeg
ffmpeg -version && ffprobe -version
```

ffmpeg o'rnatilmagan bo'lsa ilova ishlashda davom etadi: videolar `FAILED` ("ffmpeg topilmadi")
holatiga o'tadi va logda bitta ogohlantirish chiqadi. ffmpeg o'rnatilgach keyingi sweep
(≤ 10 daqiqa) ularni o'zi qayta navbatga qo'yadi — restart shart emas.

Docker image'da ffmpeg allaqachon bor (`Dockerfile`: `apk add --no-cache ffmpeg`).

## 2. Muhit o'zgaruvchilari (`/etc/teacher-bot.env`)

| O'zgaruvchi | Standart | Ma'nosi |
|---|---|---|
| `FFMPEG_PATH` | `ffmpeg` | ffmpeg bajariluvchi fayli |
| `FFPROBE_PATH` | `ffprobe` | ffprobe bajariluvchi fayli |
| `VIDEO_TRANSCODE_ENABLED` | `true` | `false` — fon ishlovi butunlay o'chiq |
| `STORAGE_PATH` | `/root/storage` | Fayllar papkasi (jonli serverda `/opt/teacher-bot/storage`) |

Odatda hech narsa qo'shish shart emas — standart qiymatlar yetarli.

## 3. Mavjud videolar (backfill) qanday ishlanadi

- Ilova ishga tushganda va keyin har 10 daqiqada sweep `video_url` lokal fayl bo'lgan va
  holati `NULL` / `PENDING` / `PROCESSING` bo'lgan darslarni navbatga qo'yadi.
  Demak deploydan keyin barcha eski videolar avtomatik, bittadan ishlanadi.
- Restartda uzilib qolgan ish (`PROCESSING`) qayta boshlanadi; yarim yozilgan `.part`
  fayllar o'chiriladi yoki nginx tomonidan berilmaydi.
- Admin yangi video yuklasa yoki `videoUrl` ni o'zgartirsa — eski tejamkor fayl o'chiriladi,
  holat `PENDING` bo'ladi va darhol navbatga qo'yiladi.
- Qo'lda qayta ishlash (masalan `FAILED` bo'lsa):
  `POST /api/v1/admin/lessons/{id}/reprocess-video` (faqat admin).
- Kuzatish: `journalctl -u teacher-bot -f | grep -i video`;
  SQL: `SELECT video_low_status, COUNT(*) FROM lessons GROUP BY 1;`

Diskda kamida *asl fayl hajmi + 500 MB* bo'sh joy bo'lmasa ish `FAILED` bo'ladi
("Diskda joy yetarli emas").

## 4. nginx: `/files/` ni to'g'ridan-to'g'ri diskdan berish

`deploy/api.oazisedu.uz.conf` dagi yangi `location /files/` blokini jonli
`/etc/nginx/sites-enabled/api.oazisedu.uz` ga (Certbot boshqaradigan `listen 443 ssl`
server blokiga) **qo'lda** ko'chiring. Faylni butunlay ustiga yozmang — TLS direktivalari yo'qoladi.

```bash
cp /etc/nginx/sites-enabled/api.oazisedu.uz /root/api.oazisedu.uz.bak.$(date +%F)
nano /etc/nginx/sites-enabled/api.oazisedu.uz   # location /files/ blokini almashtiring
nginx -t && systemctl reload nginx
```

Tekshirish:

```bash
# nginx (www-data) storage'ni o'qiy olishi kerak
namei -l /opt/teacher-bot/storage/lessons
curl -sI https://api.oazisedu.uz/files/<mavjud-fayl-yo'li> | grep -iE 'cache-control|accept-ranges|content-type|server'
curl -s -o /dev/null -w '%{http_code}\n' -H 'Range: bytes=0-99' https://api.oazisedu.uz/files/<video.mp4>   # 206
```

Muammo bo'lsa — zaxira nusxani qaytaring yoki konfiguratsiyadagi izohga olingan
proxy (fallback) blokini yoqing, `nginx -t` va reload.

Eslatma: `alias` yo'li `STORAGE_PATH` bilan aynan bir xil bo'lishi shart. Backend ham
`/files/**` ga `Cache-Control: public, max-age=31536000, immutable` qo'yadi (fallback rejimi uchun).
