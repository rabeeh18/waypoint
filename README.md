# Parcelo

Parcel and delivery tracking across India: live map, trips, stops, vehicles and drivers, plus a simple page where customers can track a parcel.
Spring Boot 3 (Java 21), PostgreSQL 17, Thymeleaf, Leaflet.

## Run locally
1. Install JDK 21 and PostgreSQL 17.
2. Copy `.env.example` to `.env.local` and fill in the passwords.
3. Create the database role, then the database with `scripts\reset-db.ps1`:

```sql
create role parcelo login password 'same-as-FLEET_DB_PASSWORD';
```

4. Start the app with `scripts\run.ps1` and open http://localhost:8080. Demo mode loads sample deliveries from six Indian cities and lets you sign in as a dispatcher or a driver.
