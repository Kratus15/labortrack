# LaborTrack AWS Deployment

This document contains the current AWS deployment setup for LaborTrack and the main commands I use to manage it.

## Current Deployment

LaborTrack is currently deployed on AWS in:

- Region: `us-east-1` (N. Virginia)
- EC2: `t3.small`
- OS: Ubuntu Server 24.04 LTS
- Storage: 30 GiB gp3 EBS
- Docker Compose

Current URL:

http://54.227.19.178

The EC2 instance currently uses an auto-assigned public IPv4 address, so the IP may change if the instance is stopped and started again.

## Current Version

Git commit deployed:

```text
8851a8d86b51b94c1c9670d093e789f215aa5b41
```

Docker images:

```text
labortrack-backend:8851a8d86b51b94c1c9670d093e789f215aa5b41
labortrack-frontend:8851a8d86b51b94c1c9670d093e789f215aa5b41
postgres:17-alpine
```

## Architecture

```text
Internet
   |
   | HTTP :80
   v
Nginx / React
   |
   | /api
   v
Spring Boot :8080
   |
   v
PostgreSQL :5432
```

## AWS Resources

The current deployment uses:

* 1 `t3.small` EC2 instance
* 1 encrypted 30 GiB gp3 EBS volume
* 1 auto-assigned public IPv4 address
* 1 EC2 security group

This MVP deployment does not currently use:

* RDS
* ECS / Fargate
* Application Load Balancer
* NAT Gateway
* WAF
* Redis / ElastiCache
* Route 53

## Security Group

Inbound rules:

```text
SSH   TCP 22   My current IPv4 /32
HTTP  TCP 80   0.0.0.0/0
```

There are no public inbound rules for:

```text
8080
5432
```

The default outbound rule is currently allowed so the server can access package repositories, GitHub, and Docker registries.

## Project Location on EC2

The repository is stored at:

```text
/opt/labortrack
```

The production Docker Compose file is:

```text
docker-compose.prod.yml
```

The production environment file is:

```text
.env.production
```

## Production Environment Variables

`.env.production` exists only on the EC2 server.

It contains values such as:

```text
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
JWT_SECRET
JWT_EXPIRATION
CORS_ALLOWED_ORIGINS
RATE_LIMIT_ENABLED
IMAGE_TAG
```

The file has Linux permissions:

```text
600
```

It is ignored by Git and should never be committed.

The repository only contains:

```text
.env.production.example
```

as a safe reference.

## GitHub Access

The EC2 instance uses a read-only GitHub deploy key to access the private LaborTrack repository.

The deploy key can clone and pull the LaborTrack repository but does not have write access.

No GitHub Personal Access Token is stored on the server.

## SSH Into EC2

From Windows PowerShell:

```powershell
ssh -i "$HOME\.ssh\labortrack-ec2-key.pem" ubuntu@54.227.19.178
```

Then:

```bash
cd /opt/labortrack
```

## Check Running Containers

```bash
sudo docker compose \
  --env-file .env.production \
  -f docker-compose.prod.yml \
  ps
```

Expected services:

```text
db
backend
frontend
```

Only the frontend should show a host port mapping:

```text
0.0.0.0:80->80/tcp
```

The backend should only show:

```text
8080/tcp
```

and PostgreSQL:

```text
5432/tcp
```

## View Backend Logs

```bash
sudo docker compose \
  --env-file .env.production \
  -f docker-compose.prod.yml \
  logs --tail=150 backend
```

## Restart the Application

```bash
sudo docker compose \
  --env-file .env.production \
  -f docker-compose.prod.yml \
  restart
```

## Stop Containers

```bash
sudo docker compose \
  --env-file .env.production \
  -f docker-compose.prod.yml \
  stop
```

## Start Containers

```bash
sudo docker compose \
  --env-file .env.production \
  -f docker-compose.prod.yml \
  start
```

## Deploying a New Version

Pull the latest code:

```bash
git pull --ff-only
```

Check the commit:

```bash
git rev-parse HEAD
```

Update `IMAGE_TAG` in `.env.production` with the Git commit SHA being deployed.

Build the backend:

```bash
sudo docker compose \
  --env-file .env.production \
  -f docker-compose.prod.yml \
  build backend
```

Build the frontend:

```bash
sudo docker compose \
  --env-file .env.production \
  -f docker-compose.prod.yml \
  build frontend
```

Start the new version:

```bash
sudo docker compose \
  --env-file .env.production \
  -f docker-compose.prod.yml \
  up -d --no-build
```

Then verify:

```bash
sudo docker compose \
  --env-file .env.production \
  -f docker-compose.prod.yml \
  ps
```

## PostgreSQL Persistence

PostgreSQL uses the Docker volume:

```text
labortrack_postgres_data
```

The database survived a full container restart during the AWS deployment test.

Normal container restarts do not delete the database.

I should not run this casually in production:

```bash
docker compose down -v
```

The `-v` option removes Docker volumes and could delete the PostgreSQL database.

## Swap

The EC2 instance has 2 GiB of RAM, so I added a 2 GiB swap file to reduce the chance of Maven or Node builds being killed because of memory pressure.

The swap file is configured in `/etc/fstab` so it remains available after a reboot.

## Production Tests Completed

The following flows were manually tested after deploying to AWS:

* frontend loads from the public EC2 URL
* company registration
* admin login
* employee creation
* employee temporary login
* forced password change
* employee dashboard
* clock in
* clock out
* admin dashboard updates
* currently clocked-in employee view
* employee list
* backend pagination
* unauthenticated API returns `401`
* wrong-role API request returns `403`
* frontend role protection
* rate limiting does not interfere with normal usage
* PostgreSQL data survives container restart

## Backups

For the MVP deployment:

* use `pg_dump` before risky database changes
* create EBS snapshots periodically
* never depend on the running Docker volume as the only copy of important data

## Cost Notes

This deployment was intentionally kept small for the MVP.

Current expected AWS resources are:

```text
1 EC2 t3.small
1 encrypted 30 GiB gp3 EBS volume
1 public IPv4
```

AWS budgets and Free Plan credits are being monitored separately.