# Parcelo

Parcel and delivery tracking across India: live map, trips, stops, vehicles and drivers, plus a simple page where customers can track a parcel.
Spring Boot 3 (Java 21), PostgreSQL 17, Thymeleaf, Leaflet.

## Run it (easiest: Docker)
Install [Docker Desktop](https://www.docker.com/products/docker-desktop/), then in this folder:

```
docker compose up
```

Wait for the first build (a few minutes), then open http://localhost:8080 and pick Dispatcher or Driver. The database and sample data from six Indian cities are created automatically. To start over with fresh data: `docker compose down -v`.

The map and fonts load from the internet, so you need a connection.

## Run without Docker (Windows)
1. Install JDK 21 and PostgreSQL 17.
2. Copy `.env.example` to `.env.local` and fill in the passwords.
3. Create the database role, then the database with `scripts\reset-db.ps1`:

```sql
create role parcelo login password 'same-as-FLEET_DB_PASSWORD';
```

4. Start the app with `scripts\run.ps1` and open http://localhost:8080.
