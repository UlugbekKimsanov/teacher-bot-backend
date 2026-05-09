# TeacherBot Backend — Project Overview

## What It Is
A **language learning platform** backend (like Duolingo) built for Uzbek students.
Students enroll in language courses, complete lessons with vocabulary/tests/exercises, earn points, and track attendance.
A **Flutter mobile app** (`teacher_bot_flutter`) is the frontend client.

## Tech Stack
| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.2.5 + WebFlux (reactive, non-blocking) |
| Database | PostgreSQL via R2DBC (reactive driver) |
| Auth | JWT Bearer tokens + Spring Security |
| Cache / OTP | Redis |
| Email | Gmail SMTP (OTP delivery) |
| File storage | Local disk (`./storage/`) |
| API Docs | SpringDoc OpenAPI → `/swagger-ui.html` |
| Base URL | `http://localhost:8080/api/v1` |

## Module Map
```
teacherBot/
├── user/          # Auth: register → OTP email → login → JWT
├── course/        # Languages + Courses + user enrollment
├── lesson/        # Lessons, Vocabulary, Tests, Exercises, Teacher Q&A
├── rating/        # Progress, Attendance, Points, Certificates
├── activity/      # Activity tracking (streaks, daily usage)
├── subject/       # Subject categories
├── news/          # News feed
├── file/          # File upload/download
└── common/        # Security config, JWT, exceptions, FileStorageService
```

## Key API Endpoints
```
POST   /auth/register          → send OTP to email
POST   /auth/verify-otp        → confirm OTP, get JWT
POST   /auth/login             → email+password login

GET    /language               → all languages (English, Uzbek, Russian…)
GET    /courses                → all courses with progress for current user
GET    /courses/category/{name}→ courses filtered by language name
POST   /courses/{id}/enroll   → enroll user in course

GET    /courses/{id}/lessons   → lesson list with lock/unlock status
GET    /lessons/{id}           → lesson detail (vocab + questions + exercises)
POST   /lessons/{id}/test/submit    → submit test answers → score
POST   /lessons/{id}/exercise/submit→ submit exercise answers → score
POST   /lessons/{id}/vocab/submit   → submit vocab score
POST   /lessons/{id}/ask            → send question to teacher

GET    /rating/progress/{courseId}  → vocab/test/exercise scores
GET    /rating/attendance/{courseId}→ missed days (weekly/monthly/quarterly)
GET    /rating/points               → earned points list + total
GET    /rating/certificates         → completed course certificates
```

## Database Tables (PostgreSQL)
`users`, `languages`, `courses`, `user_courses`, `lessons`, `vocabulary`,
`tests`, `questions`, `exercises`, `files`, `attendance`, `certificates`,
`points`, `teacher_questions`, `activities`, `user_lessons`

Schema + mock seed data: `src/main/resources/schema.sql`

## Auth Flow
1. `POST /auth/register` → OTP sent to email via Redis (5 min TTL)
2. `POST /auth/verify-otp` → validates OTP → creates user → returns JWT
3. All protected routes require `Authorization: Bearer <token>`
4. JWT principal = `userId` (Long) extracted in controllers via `Authentication`

## Response Format
All endpoints return:
```json
{ "success": true, "data": <payload> }
{ "success": false, "message": "Error description" }
```

## Lesson Progression
- Lessons are **locked** until the previous one is completed
- Completing a lesson requires: vocab submission + test submission + exercise submission
- Completion is stored in `user_lessons` table (`is_completed = true`)
- Course progress = completed lessons / total lessons

## File Storage
- Files stored at `./storage/languages/{name}_{id}/` and `./storage/lessons/{id}/`
- Public URLs served via `GET /files/{path}`
- Language images: flag image + background image (uploaded via `POST /language/{id}/upload`)

## Local Setup
```bash
# PostgreSQL: create DB
createdb teacher_bot

# Redis: run on default port 6379

# Run
./mvnw spring-boot:run

# Swagger UI
open http://localhost:8080/swagger-ui.html
```
