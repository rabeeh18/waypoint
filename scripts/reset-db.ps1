# Drops and recreates the Parcelo database. The next app start migrates and re-seeds the demo.
# Stop the app first.
. (Join-Path $PSScriptRoot 'env.ps1')
$env:PGPASSWORD = $env:PG_SUPERUSER_PASSWORD
& psql -U postgres -d postgres -v ON_ERROR_STOP=1 -q `
    -c "select pg_terminate_backend(pid) from pg_stat_activity where datname = 'Parcelo' and pid <> pg_backend_pid();" `
    -c "drop database if exists Parcelo;" `
    -c "create database Parcelo owner Parcelo encoding 'UTF8';"
if ($LASTEXITCODE -eq 0) { Write-Host 'Database Parcelo recreated.' }
