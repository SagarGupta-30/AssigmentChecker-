# AI Assignment Checker

Full-stack platform where teachers create assignments and students submit answers. The system evaluates responses automatically (objective + subjective), supports OCR from uploaded images, and provides analytics dashboards.

New: teachers can attach a question-set image to an assignment, and students can view that question image directly in their submission page.

## Stack

- Backend: Spring Boot, Spring Security (JWT), Spring Data JPA
- Databases:
  - Dev default: H2 (file-based)
  - Prod: MySQL 8 (profile-based)
- Frontend: React (Vite), Axios, Recharts
- AI: OpenAI API (with lexical fallback when API key is missing)
- OCR: Tesseract CLI

## Production Hardening Included

- JWT auth + role-based access control (`TEACHER`, `STUDENT`)
- Environment-driven CORS allowlist (`CORS_ALLOWED_ORIGINS`)
- Auth endpoint rate limiting (login/register)
- Safer 500 error responses (no internal stack leakage to clients)
- Profile split (`dev` / `prod`)
- Dockerfiles for backend and frontend
- Deployment compose file (`docker-compose.deploy.yml`)
- GitHub Actions CI (`.github/workflows/ci.yml`)
- Env templates (`.env.example`, `frontend/.env.example`)

## Project Structure

```
.
├── backend
│   ├── Dockerfile
│   ├── pom.xml
│   └── src
├── frontend
│   ├── Dockerfile
│   ├── .env.example
│   ├── nginx/default.conf
│   └── src
├── docker-compose.deploy.yml
└── .github/workflows/ci.yml
```

## Quick Start (Local Dev)

### 1) Run backend (dev profile, H2)

```bash
cd backend
mvn spring-boot:run
```

Backend: `http://localhost:8080`
Health: `http://localhost:8080/actuator/health`

### 2) Run frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend: `http://localhost:5173`

## Local Alternative (Open Built Index)

```bash
cd frontend
npm install
npm run start:index
```

Then open `http://localhost:5500`.

## Run With Docker (Production-like)

1. Copy env template and set secrets:

```bash
cp .env.example .env
```

2. Build and run:

```bash
docker compose -f docker-compose.deploy.yml up -d --build
```

Services:
- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- MySQL: `localhost:3306`

## API Endpoints

### Auth
- `POST /api/auth/register`
- `POST /api/auth/login`

### Assignments
- `POST /api/assignments` (Teacher)
- `POST /api/assignments/with-image` (Teacher, multipart form)
- `GET /api/assignments/my` (Teacher)
- `GET /api/assignments/available` (Student/Teacher)
- `GET /api/assignments/{assignmentId}`
- `GET /api/assignments/{assignmentId}/question-image` (Student/Teacher, protected)

### Submissions
- `POST /api/submissions/assignment/{assignmentId}`
- `POST /api/submissions/assignment/{assignmentId}/with-image`
- `POST /api/submissions/ocr/extract`
- `GET /api/submissions/assignment/{assignmentId}` (Teacher)
- `GET /api/submissions/my` (Student)
- `GET /api/submissions/{submissionId}/result`

### Analytics
- `GET /api/analytics/teacher`

## Environment Variables

Key vars (see `.env.example` for full list):

- `SPRING_PROFILES_ACTIVE` = `dev` or `prod`
- `DB_URL` or (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`)
- `JWT_SECRET`
- `OPENAI_API_KEY`
- `CORS_ALLOWED_ORIGINS`
- `RATE_LIMIT_ENABLED`
- `AUTH_RATE_LIMIT_PER_MINUTE`
- `VITE_API_BASE_URL` (frontend)

## Deploy Notes (GitHub)

- GitHub repo alone does not run backend APIs.
- Deploy frontend and backend separately (or with containers):
  - Frontend: Vercel / Netlify / Cloudflare Pages
  - Backend: Render / Railway / Fly.io / ECS / VPS
  - DB: Managed MySQL
- Set `VITE_API_BASE_URL` to your deployed backend URL.
- Set backend `CORS_ALLOWED_ORIGINS` to your deployed frontend URL.

## Security Notes

- Change `JWT_SECRET` before production.
- Keep `OPENAI_API_KEY` server-side only.
- For strong production posture, place backend behind HTTPS + reverse proxy and use managed DB backups.
# AssigmentChecker-
