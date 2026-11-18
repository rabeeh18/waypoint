# Signs in as the demo dispatcher and requests every main page; reports any that do not return 200.
param([string]$Base = 'http://localhost:8080')
$ProgressPreference = 'SilentlyContinue'
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$login = Invoke-WebRequest "$Base/login" -WebSession $session -UseBasicParsing
$token = [regex]::Match($login.Content, 'name="_csrf" value="([^"]+)"').Groups[1].Value
Invoke-WebRequest "$Base/demo-login" -Method Post -Body @{ as = 'dispatcher'; _csrf = $token } -WebSession $session -UseBasicParsing | Out-Null

$tripLink = (Invoke-WebRequest "$Base/trips" -WebSession $session -UseBasicParsing).Links.href |
    Where-Object { $_ -match '^/trips/\d+$' } | Select-Object -First 1
$shipmentLink = (Invoke-WebRequest "$Base/shipments" -WebSession $session -UseBasicParsing).Links.href |
    Where-Object { $_ -match '^/shipments/\d+$' } | Select-Object -First 1

$pages = @('/dashboard', '/trips', $tripLink, '/planning', '/shipments', $shipmentLink, '/shipments/new',
           '/fleet/vehicles', '/fleet/vehicles/1', '/fleet/drivers', '/fleet/drivers/1', '/track',
           '/api/dashboard/snapshot')
$failed = 0
foreach ($p in $pages | Where-Object { $_ }) {
    try {
        $r = Invoke-WebRequest "$Base$p" -WebSession $session -UseBasicParsing
        "{0,-28} {1}" -f $p, $r.StatusCode
    } catch {
        $failed++
        "{0,-28} FAILED {1}" -f $p, $_.Exception.Response.StatusCode
    }
}
"failures: $failed"
