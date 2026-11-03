# LaborTrack

**Employee Time Tracking & Workforce Management**

LaborTrack is a full-stack application designed to help small businesses manage employees, track work hours, and monitor workforce activity.

Built with **Java 21, Spring Boot, PostgreSQL, React, and TypeScript.** Deployed using Docker Compose on AWS Lightsail, following an initial deployment on AWS EC2.

**[Live Application](#live-app) · [90-Second Walkthrough](#90-second-demo) · [Local Setup](#running-locally) · [Architecture](#architecture)**

### Engineering Highlights

- **Security:** JWT authentication, role-based authorization, BCrypt password hashing, and company-level data isolation.
- **Data integrity:** PostgreSQL constraints prevent multiple open work sessions for the same employee.
- **Backend design:** Paginated REST APIs, timezone-aware workforce calculations, and Flyway database migrations.
- **Testing:** 66 backend automated tests with GitHub Actions CI.
- **Deployment:** Containerized Nginx, Spring Boot, and PostgreSQL with Docker Compose. Initially deployed on AWS EC2, then migrated to Lightsail to reduce infrastructure costs.

### Current Limitations

LaborTrack currently runs on a single AWS Lightsail instance, so it is not highly available. HTTPS, automated deployment, application-level monitoring, and additional employee-management capabilities are planned improvements.

## Live App

**Application:** [Open LaborTrack](http://184.197.205.17/)

You can explore the application using either demo account:

### Admin Demo

- **Email:** `demoadmin@example.test`
- **Password:** `DemoAdmin123$`

### Employee Demo

- **Email:** `demoemployee1@example.test`
- **Password:** `DemoEmployee123$`

The admin account can create and manage employees, view workforce activity, and monitor<br>
current work sessions. Employees can clock in/out and review their own activity.

Demo data may change as other people use the application. You can also register a new<br>
company and go through the complete workflow yourself.

## Project Walkthrough

A typical LaborTrack workflow looks like this:

**Register company → Create employee → Employee changes temporary password → Clock in/out → Admin monitors activity**

### 90-Second Demo

A short walkthrough of LaborTrack covering the main admin and employee workflows:

**[Watch the LaborTrack Demo](https://youtu.be/Ukqaczwpn0s)**

## Screenshots

### Admin Dashboard

<img src="docs/images/AdminDashboardPage.png" alt="LaborTrack Admin Dashboard" width="900">

### Employee Management

<img src="docs/images/AdminCreateEmployeePage.png" alt="LaborTrack Create Employee Page" width="900">

### Employee Details

<img src="docs/images/AdminEmployeeDetailPage.png" alt="LaborTrack Employee Detail Page" width="900">

### Current Workforce Activity

<img src="docs/images/AdminCurrentlyEmployeeClockInPage.png" alt="LaborTrack Currently Clocked-In Employees" width="900">

### Employee Dashboard

<img src="docs/images/EmployeeDashboardAndClockOutPage.png" alt="LaborTrack Employee Dashboard" width="900">

## Why I Built LaborTrack

I first learned Java in college, where I built an early version of LaborTrack for a class.<br>
That project made me want to build something bigger that could be useful outside of school.

The idea came from thinking about small businesses I saw around NYC—bodegas, cafés,<br>
restaurants, clothing stores, and other family-owned businesses. My sister had also worked<br>
at a bodega when she was younger and told me they did not really have a system for tracking<br>
employee work time.

That made me interested in building a simple system where business owners could manage<br>
employees and track work activity, while employees could clock in and out and review their<br>
own work sessions.

My original college version used Java, Spark, PostgreSQL, HTML, CSS, and JavaScript.<br>
I was proud of it at the time, but as I learned more about backend development, I started<br>
seeing problems in my own design—especially around security, authorization, validation,<br>
testing, and how the application would actually be deployed.

After graduating, I decided to rebuild LaborTrack from the ground up using Spring Boot,<br>
React, and the production concepts I had been learning.

The goal was not just to make the old project look better. I wanted to understand how to<br>
build the same idea more carefully and make engineering decisions I could actually explain.

## Features

### Application Features

- Company registration with an admin account
- Employee account creation and management
- Admin and employee dashboards
- Employee clock in / clock out
- Employee work-session history
- Employee status filtering
- Paginated employee and work-session data
- Temporary employee credentials with required password change

### Engineering Features

- JWT-based authentication
- Role-based authorization for admins and employees
- Company-level data isolation
- BCrypt password hashing
- Rate limiting for sensitive endpoints
- PostgreSQL constraints for important business rules
- Flyway database migrations
- Automated backend tests
- GitHub Actions continuous integration
- Dockerized local and production environments

## Tech Stack

### Backend

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- JWT
- Bucket4j
- Maven

### Frontend

- React
- TypeScript
- Vite
- React Router
- CSS

### DevOps / Deployment

- Docker
- Docker Compose
- Nginx
- GitHub Actions
- AWS Lightsail (current deployment)
- AWS EC2 (initial deployment)
- AWS Budgets (cost monitoring)

## Architecture

LaborTrack uses a simple three-tier architecture:

```
Browser
   |
   v
Nginx + React
   |
   | /api
   v
Spring Boot
   |
   v
PostgreSQL
```

Nginx serves the React frontend and proxies `/api` requests to the Spring Boot backend.<br>
The backend handles authentication, business rules, and database access before communicating<br>
with PostgreSQL.

### Local Development

Docker Compose runs the frontend, backend, and PostgreSQL as separate containers on the<br>
same Docker network.

### AWS Deployment

The current deployment runs on a single AWS Lightsail Ubuntu instance using Docker Compose.<br>
LaborTrack was initially deployed on AWS EC2 before being migrated to Lightsail.

```
Internet
    |
    v
AWS Lightsail (Ubuntu)
    |
    v
Docker Compose
    |
    v
Nginx / React (Public port 80)
    |
    | /api
    v
Spring Boot (Internal port 8080)
    |
    v
PostgreSQL (Internal port 5432)
    |
    v
Persistent Docker Volume
```

Only the Nginx/frontend entry point is exposed publicly. Spring Boot and PostgreSQL<br>
communicate through Docker's internal network and are not directly exposed to the internet.<br>
Lightsail uses a static IPv4 address, while backend and database traffic remain within Docker's internal network.

## Database Design

LaborTrack uses PostgreSQL with Flyway migrations to keep schema changes versioned and<br>
reproducible.

The main relationships are:

<img src="docs/images/DB_EDR.png" alt="LaborTrack Database EDR" width="900">

Each company owns its users, employees, and work-session data. Employees are linked to<br>
user accounts for authentication, while work sessions belong to the employee who created them.

Important business rules are enforced both in the application and, where appropriate,<br>
at the database level. For example, a PostgreSQL partial unique index prevents an employee<br>
from having more than one open work session at the same time.

## Security

Security was one of the main areas I wanted to improve compared with the original version<br>
of LaborTrack.

Current security measures include:

- Passwords are hashed with BCrypt and are never stored in plain text.
- JWT tokens are used to authenticate users after login.
- Admin and employee endpoints are protected with role-based authorization.
- Company ownership is validated so users cannot access employees or work sessions<br>
  belonging to another company.
- Newly created employees receive temporary credentials and must change their password<br>
  before accessing the rest of the application.
- Login, registration, and password-change endpoints use stricter rate limits.
- Database credentials, JWT secrets, and other production configuration are provided<br>
  through environment variables instead of being hard-coded in the repository.
- The production backend and PostgreSQL database are not directly exposed to the public<br>
  internet. Requests enter through Nginx, which proxies API traffic to Spring Boot.

This is not a complete security setup, but I wanted the main protections in place from the<br>
beginning instead of adding them later.

## Engineering Decisions

### One Open Work Session

An employee should never have multiple active clock-ins at the same time. LaborTrack checks<br>
this rule in the application and also uses a PostgreSQL partial unique index as a second<br>
layer of protection at the database level.

### Timezone-Aware Dashboard

Daily workforce calculations use the company's configured timezone instead of assuming every<br>
company operates in the same timezone. This matters when calculating activity for "today"<br>
because the same timestamp can belong to a different calendar day depending on the company.

### Pagination Instead of Loading Everything

Employee lists and work-session history use pagination instead of returning every record at<br>
once. This keeps API responses smaller and gives the frontend a predictable way to navigate<br>
larger datasets.

### Infrastructure Right-Sizing and Cost Optimization

I initially deployed LaborTrack on a single AWS EC2 t3.small instance using Docker Compose, rather<br>
than introducing services such as ECS, RDS, or an Application Load Balancer. This kept the infrastructure<br>
simple while allowing me to learn how to deploy and operate the full application on AWS.<br>

After monitoring actual CPU, memory, and disk usage, I realized the application did not need the flexibility<br>
or ongoing cost of my original EC2 setup. I migrated LaborTrack to a **2 GB AWS Lightsail** instance while keeping<br>
the same Docker Compose architecture. This reduced estimated **monthly hosting costs from approximately $22 to<br>
$12 without removing application functionality.**

Before retiring EC2, I backed up PostgreSQL, verified the backup checksum, restored the database on Lightsail,<br>
and tested the application using existing accounts. I also performed a separate test restore before deleting the<br>
original EC2 resources. This reinforced an important lesson: infrastructure should match actual application requirements,<br>
not simply use more services because they are available.<br>

For now, I only want to add more infrastructure when LaborTrack actually needs it.

## Challenges & Lessons Learned

The hardest parts were designing authentication, company isolation, work-session rules,<br>
and deployment without making the system harder to understand than it needed to be.

Things I learned while building it:
- More complexity does not automatically make a system better. I originally considered<br>
  microservices, caching, more AWS services, and gateway-level rate limiting before the<br>
  application had a real need for them.
- Migrating a live application requires more than moving containers. I learned to verify<br>
  database backups, test restores, compare records, and validate the new deployment before<br>
  removing the old infrastructure.
- Security is easier to design into a feature from the beginning than to add afterward.
- Important business rules should not rely only on application code when the database can<br>
  help enforce them too.
- Automated tests make it much safer to refactor code and catch edge cases.
- CI can expose problems that do not appear in a local development environment.
- Docker simplifies environment consistency, but it also requires understanding networking,<br>
  persistence, ports, and configuration.
- Production secrets and environment-specific configuration should stay outside source code<br>
  and container images.
- Cloud cost is an engineering constraint too. Infrastructure choices should match the actual<br>
  needs of the application.

The biggest change in how I approached this project was learning to ask "why does the system<br>
need this?" before adding another technology or layer of infrastructure.

## Testing & Continuous Integration

The backend currently includes 66 automated tests covering areas such as:

- Authentication and authorization
- Employee management
- Work-session behavior
- Dashboard endpoints
- Pagination
- Rate limiting

Backend tests can be run locally with:

```
./mvnw clean test
```

GitHub Actions also runs automatically on every push and pull request.

The CI pipeline:

- Starts a PostgreSQL test database
- Runs the Spring Boot test suite
- Installs frontend dependencies
- Builds the React frontend

## Deployment

LaborTrack is currently hosted on AWS Lightsail using a single Ubuntu instance running three<br>
Docker containers: React/Nginx, Spring Boot, and PostgreSQL.

The application was originally deployed on AWS EC2 and later migrated to Lightsail as a cost<br>
optimization. This reduced the estimated recurring hosting cost **from approximately $22/month<br>
to $12/month** while preserving the existing application architecture and database.

Production Docker images are tagged using exact Git commit SHAs for deployment traceability.<br>
GitHub Actions handles continuous integration, while production deployments are currently<br>
performed manually.

The tradeoff is that LaborTrack runs on a single server and is not highly available.

## Running Locally

### With Docker

1. Clone the repository:

```
git clone https://github.com/Kratus15/labortrack.git
cd labortrack
```

2. Create the local environment files from the included examples and add the required <br>
database and JWT values.

3. Build and start the application:

```
docker compose up --build
```

Once the containers are running:

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`

To stop the application:

```
docker compose down
```

## Project Status

LaborTrack is an actively developed portfolio project. The current MVP is <br>
deployed and covers the main admin and employee workflows.

### What's Next

The next improvements I want to focus on are:

- Automated deployment (CD)
- HTTPS and a custom domain
- Admin work-session editing
- Employee and company profile editing
- Profile-picture support with AWS S3
- Application monitoring, logs, and uptime alerts

Larger infrastructure changes such as RDS or caching will be added only when the application<br>
has a real reason to need them.
