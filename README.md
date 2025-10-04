# Homework Service


This Spring Boot microservice manages homework assignments, their tasks, and students’ task progress. It exposes REST APIs for teachers to create and manage assignments and for students to start, update, and complete tasks.

## Features
- Create homework assignments with one or more tasks
- Support for task types and content references (e.g., linking to lesson content or vocab)
- Idempotent assignment creation via idempotencyKey
- Paginated listing of assignments for a student or a teacher
- Track task lifecycle: not started → in progress → completed
- Partial progress updates and metadata storage per task

## Tech Stack
- Java 17+
- Spring Boot
- Spring Data JPA (Hibernate)
- PostgreSQL
- Liquibase changelog present (db/changelog), with JPA ddl-auto=update for dev
- Maven

## Configuration
Configuration defaults are defined in `src/main/resources/application.yml` and can be overridden via environment variables:

- `SPRING_DATASOURCE_URL` 
- `SPRING_DATASOURCE_USERNAME` 
- `SPRING_DATASOURCE_PASSWORD` 

JPA:
- `spring.jpa.hibernate.ddl-auto=update`
- Dialect: `org.hibernate.dialect.PostgreSQLDialect`

## Domain Overview
- Assignment: belongs to a teacher and a student; contains a list of tasks.
- Task: has type, source kind, title, instructions, ordinal, contentRef and optional vocab words; tracks status and progress.
- Progress: tracked per task, with optional metadata.

Key DTOs (simplified):
- CreateAssignmentDto: studentId, title, instructions, dueAt, lessonId, idempotencyKey, tasks[]
- CreateTaskDto: type, sourceKind, title, instructions, ordinal, contentRef{...}, vocabWordIds[]
- ProgressDto: progressPct (0..100), meta{...}

## REST API
Base paths:
- Assignment management: `/api/homeworks`
- Task progress: `/api/homeworks/tasks`

Endpoints:
1) Create assignment
- POST `/api/homeworks?teacherId={UUID}`
- Body: CreateAssignmentDto
- Returns: AssignmentDto
- Status: 201 Created

2) List assignments for a student
- GET `/api/homeworks/{studentId}`
- Query: pageable params (`page`, `size`, `sort`)
- Returns: Page<AssignmentDto>

3) Delete assignment
- DELETE `/api/homeworks/{assignmentId}`
- Returns: 204 No Content

4) List assignments for a teacher (optionally filtered by student)
- GET `/api/homeworks/tutor/{tutorId}?studentId={UUID?}`
- Query: pageable params (`page`, `size`, `sort`)
- Returns: Page<AssignmentDto>

5) Start a task (student)
- POST `/api/homeworks/tasks/{taskId}/start?studentId={UUID}`
- Returns: AssignmentDto (the entire assignment containing the task)

6) Update task progress (student)
- POST `/api/homeworks/tasks/{taskId}/progress?studentId={UUID}`
- Body: ProgressDto
- Returns: AssignmentDto

7) Complete task (student)
- POST `/api/homeworks/tasks/{taskId}/complete?studentId={UUID}`
- Optional body: `{ "key": "value" }` metadata
- Returns: AssignmentDto

Validation and errors:
- Uses standard HTTP status codes (400/403/404); see `exception` package and `RestExceptionHandler`.

## Build and Run
Prerequisites: Java 17+, Maven, PostgreSQL reachable via configured URL.

Build:
- `mvn clean package`

Run (local):
- Ensure database is up and accessible with the configured credentials
- `java -jar target/homework-service-0.0.1-SNAPSHOT.jar`

Run with Maven:
- `mvn spring-boot:run`

## Docker
A simple Dockerfile is provided. Example build and run:
- Build: `docker build -t speakshire/homework-service:local .`
- Run: `docker run -p 8086:8086 -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/homework_service -e SPRING_DATASOURCE_USERNAME=admin -e SPRING_DATASOURCE_PASSWORD=admin speakshire/homework-service:local`

## Testing
- Unit tests: `mvn test`
- A basic application context test exists under `src/test/java`

## Notes
- Assignment creation enforces at least one task; idempotent if `idempotencyKey` is reused by the same teacher for the same student.
- Task progress returns the full assignment to simplify frontend state updates.

## Future Enhancements
- Richer task types and validation
- Webhook/events on completion

## License
Proprietary — internal service for speakshire.com (update as needed).



## Student → Homework list: filtering, sorting, and counts

New parameters on GET /api/homeworks/student/{studentId}:
- status=active|notFinished|completed|all (default active)
- from,to=ISO8601 timestamps (optional). When absent, default window is the last 7 days ending now.
- includeOverdue=true|false (default true)
- hideCompleted=true|false (default true when status=active)
- sort=assigned_desc|assigned_asc|due_asc|due_desc (default assigned_desc)
- view=summary|full (default summary). When view is absent or set to full, legacy behavior is preserved and a Page<AssignmentDto> (full payload) is returned.
- page,size (existing Spring Data pagination)

Summary view returns lightweight items only (no task bodies):
- id, title, createdAt, dueAt, totalTasks, completedTasks, progressPct, completed, overdue

Status semantics:
- active → not finished within [from,to] + ALL overdue (outside range allowed if includeOverdue=true)
- notFinished → all not finished (ignore [from,to])
- completed → completed within [from,to]
- all → any within [from,to]

Sorting:
- assigned_desc/assigned_asc → createdAt desc/asc
- due_asc/due_desc → dueAt asc/desc

Examples:
- GET /api/homeworks/student/{id}?view=summary  // default active, last 7d, include overdue, hide completed
- GET /api/homeworks/student/{id}?view=summary&status=completed&from=2025-10-01T00:00:00Z&to=2025-10-08T00:00:00Z
- GET /api/homeworks/student/{id}?view=summary&status=active&includeOverdue=false&sort=due_asc

Counts endpoint:
- GET /api/homeworks/student/{studentId}/counts?from=&to=&includeOverdue=true
- Returns JSON: { notFinished, completed, overdue, active, all } computed with the same rules as above.
