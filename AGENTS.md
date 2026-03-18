# AGENTS.md

## Project goal
Build an AI mock interview and education platform.

## Tech stack
- Backend: Spring Boot, Gradle, Java 21, MySQL
- Frontend: React, Vite, JavaScript
- Auth: JWT access/refresh token
- State management: zustand
- Charts: recharts

## Architecture rules
- Use feature-based packages on backend
- Keep controller/service/repository/dto/entity separated
- Use REST API conventions
- Use standard API response format
- Add validation annotations for request DTOs
- Add a global exception handler
- Prefer clear names over abbreviations

## Backend package direction
Use this package style:
com.aimentor
- common
- domain.user
- domain.profile
- domain.interview
- domain.learning
- domain.report
- domain.recommendation
- external.ai
- external.speech

## Frontend direction
- Use pages/components/api/hooks/store separation
- Keep route structure simple
- Separate interview pages, dashboard pages, and auth pages
- Prefer reusable UI components

## Coding rules
- Do not add unnecessary dependencies
- Before large changes, propose a short plan first
- After implementing, run tests or build checks
- Summarize changed files and remaining TODOs
- Do not modify unrelated files
- Do not remove existing comments unless outdated

## Output rules
When a task is completed, always provide:
1. What changed
2. Validation result
3. Remaining risks
4. Next recommended task