# Resume AI Helper

A full-stack portfolio project built with Spring Boot, React, PostgreSQL, PDFBox, and optional OpenAI analysis.

## Features

- Upload PDF, DOCX, or TXT resumes
- Enter a job title, company name, and job description
- Generate a resume-to-job match score
- Identify strengths, missing skills, and recommendations
- Generate an improved professional summary
- Save analyses in PostgreSQL
- View dashboard statistics
- Download any analysis as a PDF report
- Reopen or delete previous analyses
- Fall back to local analysis when OpenAI is unavailable

## Run the database

Create a local `.env` file from `.env.example`, then replace the placeholder values with your own credentials. Never commit the `.env` file.

```bash
docker compose up -d
```

The included Docker setup uses:

- Database: `resume_ai`
- Username: `resume_user`
- Password: supplied through the `DB_PASSWORD` environment variable
- Host port: `5433`

## Run the backend

```bash
cd backend
mvn clean spring-boot:run
```

Backend URL: `http://localhost:8080`

## Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend URL: `http://localhost:5173`

## API endpoints

- `GET /api/analyses/stats` — dashboard statistics
- `GET /api/analyses/{id}/pdf` — download a PDF report
