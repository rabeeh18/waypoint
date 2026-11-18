# Waypoint

Shipment and fleet tracking for Punjab: live map, trips, stops, vehicles, drivers, warehouses and delay alerts.
Spring Boot 3 (Java 21), PostgreSQL 17, Thymeleaf, Leaflet.

## Run locally
1. Install JDK 21 and PostgreSQL 17.
2. Copy `.env.example` to `.env.local` and fill in the passwords.
3. Create the database: `scripts\reset-db.ps1` (the `waypoint` role must exist first; see the SQL below).
4. Start: `scripts\run.ps1` then open http://localhost:8080 (demo mode seeds Punjab data and a simulator).

```sql
create role waypoint login password 'same-as-FLEET_DB_PASSWORD';
```
