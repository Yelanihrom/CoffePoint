param(
    [Parameter(Mandatory = $true)][string]$WebAppUrl,
    [string]$Token = $env:COFFEEPOINT_SYNC_TOKEN
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($Token)) {
    throw 'Set COFFEEPOINT_SYNC_TOKEN or pass -Token before running the read-only check.'
}
$target = [Uri]$WebAppUrl
if ($target.Scheme -ne 'https' -or $target.Host -ne 'script.google.com' -or
    -not $target.AbsolutePath.EndsWith('/exec') -or $target.Query) {
    throw 'WebAppUrl must be an HTTPS script.google.com /exec deployment URL without query parameters.'
}
$schemaPath = Join-Path $PSScriptRoot '..\app\src\test\resources\contracts\sheet-schema.json'
$schema = Get-Content -LiteralPath $schemaPath -Raw -Encoding UTF8 | ConvertFrom-Json
$queryUrl = $WebAppUrl + '?action=full_sync&token=' + [Uri]::EscapeDataString($Token)
try {
    $response = Invoke-RestMethod -Uri $queryUrl -Method Get -TimeoutSec 60
} catch {
    # Do not print the exception message: it can contain the URL and its token.
    throw ('Deployment transport failed: ' + $_.Exception.GetType().Name)
}
if ($response.success -ne $true) {
    throw ('Apps Script error ' + $response.error.status + ': ' + $response.error.message)
}
if ($null -eq $response.data) { throw 'Successful response is missing data.' }
foreach ($entry in $schema.PSObject.Properties) {
    $definition = $entry.Value
    $responseKey = $definition.responseKey
    if ($response.data.PSObject.Properties.Name -notcontains $responseKey) {
        throw ('Missing response collection: ' + $responseKey)
    }
    $rows = $response.data.$responseKey
    if ($rows -isnot [System.Array]) { throw ('Expected an array: ' + $responseKey) }
    foreach ($row in $rows) {
        foreach ($header in $definition.headers) {
            if ($row.PSObject.Properties.Name -notcontains $header) {
                throw ('Missing column ' + $header + ' in ' + $entry.Name)
            }
        }
        if ($entry.Name -eq 'coffee_station') {
            $sales = $row.salesAfterService
            if ($null -eq $sales -or $sales -is [string] -or $sales -is [bool] -or
                $sales -isnot [ValueType] -or [double]$sales -lt 0 -or
                [Math]::Truncate([double]$sales) -ne [double]$sales) {
                throw 'salesAfterService must be a non-negative integer, not a date or formatted text.'
            }
        }
    }
    Write-Output ($entry.Name + ': OK, rows=' + $rows.Count)
}
Write-Output 'Read-only deployment check passed. No remote data was changed.'
