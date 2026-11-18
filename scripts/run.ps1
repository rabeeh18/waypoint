# Starts the Parcelo app on http://localhost:8080 (PostgreSQL runs as a Windows service).
#   scripts/run.ps1               demo mode: sample data from six Indian cities and quick sign-in
. (Join-Path $PSScriptRoot 'env.ps1')
$service = Get-Service 'postgresql-x64-17' -ErrorAction SilentlyContinue
if ($service -and $service.Status -ne 'Running') {
    Write-Host 'PostgreSQL service is not running. Start it from Services (postgresql-x64-17) and try again.'
    exit 1
}
Set-Location (Join-Path $PSScriptRoot '..')
# Keep the JVM small: the app needs well under 512 MB.
$env:MAVEN_OPTS = '-Xmx256m'
.\mvnw.cmd -q spring-boot:run "-Dspring-boot.run.jvmArguments=-Xms128m -Xmx384m -XX:+UseSerialGC"
