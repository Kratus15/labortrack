# LaborTrack Docker Setup

LaborTrack can run locally in a production-like environment using Docker Compose.

The setup runs 3 containers:

```text
Browser
   |
   v
React + Nginx
localhost:3000
   |
   v
Spring Boot
backend:8080
   |
   v
PostgreSQL
db:5432
```

## Requirements

Before starting:

* Docker Desktop must be installed and running.
* On Windows, WSL 2 and hardware virtualization must be enabled.

Check Docker:

```powershell
docker --version
docker compose version
```

## Environment Variables

Create a local `.env` file in the project root:

```env
POSTGRES_DB=labortrack
POSTGRES_USER=labortrack
POSTGRES_PASSWORD=your_database_password

JWT_SECRET=your_base64_jwt_secret
JWT_EXPIRATION=1h
```

The real `.env` file is ignored by Git and should never be committed.

A JWT secret can be generated in PowerShell with:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(64))
```

## Build the Containers

From the project root:

```powershell
docker compose build
```

The backend and frontend images are built from their Dockerfiles.

PostgreSQL uses the official:

```text
postgres:17-alpine
```

image.

## Start LaborTrack

Run:

```powershell
docker compose up -d
```

The application will be available at:

```text
Frontend:   http://localhost:3000
Backend:    http://localhost:8080
PostgreSQL: localhost:5432
```

Check container status:

```powershell
docker compose ps
```

The expected services are:

```text
db
backend
frontend
```

The PostgreSQL container should show as `healthy`.

## View Logs

All containers:

```powershell
docker compose logs -f
```

Backend only:

```powershell
docker compose logs -f backend
```

Database only:

```powershell
docker compose logs -f db
```

Frontend only:

```powershell
docker compose logs -f frontend
```

## Stop LaborTrack

Stop and remove the containers:

```powershell
docker compose down
```

This keeps the PostgreSQL Docker volume and database data.

Start the project again with:

```powershell
docker compose up -d
```

## Reset the Docker Database

To delete the containers and the local PostgreSQL Docker volume:

```powershell
docker compose down -v
```

This deletes the Docker database data.

The next time the project starts, PostgreSQL will create a new database and Flyway will run the migrations again.

## Rebuild After Code Changes

To rebuild and restart:

```powershell
docker compose build
docker compose down
docker compose up -d
```

Or:

```powershell
docker compose up -d --build
```

## Configuration

Normal local development can use:

```text
config/application.properties
```

Docker does not copy the local `config/` directory.

Instead, Docker Compose reads the project `.env` file and passes the values into the backend container as environment variables.

For example:

```text
.env
  |
  v
Docker Compose
  |
  v
Backend environment variables
  |
  v
Spring Boot
```

## Frontend API Requests

During normal Vite development, the frontend can use:

```env
VITE_API_BASE_URL=http://localhost:8080
```

Inside the Docker production build, the frontend uses relative `/api/...` requests.

Nginx forwards those requests to:

```text
http://backend:8080
```

This avoids hardcoding `localhost:8080` inside the production frontend build.

## Database Persistence

PostgreSQL uses the Docker volume:

```text
postgres_data
```

Because of this:

```powershell
docker compose down
docker compose up -d
```

does not delete the database.

Use:

```powershell
docker compose down -v
```

only when a completely fresh Docker database is needed.

## Verification

The Docker setup was tested successfully with:

* PostgreSQL startup and health check
* Spring Boot connection to PostgreSQL
* Flyway migration
* React/Nginx frontend
* React Router refresh
* Company registration
* Admin login
* Employee creation
* Forced password change
* Employee dashboard
* Clock in and clock out
* Admin dashboard updates
* PostgreSQL data persistence after container recreation

## Production Note

This setup is for local production-like testing and deployment preparation.

AWS deployment is not included yet.

For a real deployment, Docker images should use specific version tags or Git commit tags instead of relying only on `latest`.
