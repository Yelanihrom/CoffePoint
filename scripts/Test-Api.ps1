param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$NodePath = 'node'
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
Get-Command $NodePath -ErrorAction Stop | Out-Null
if ($JavaHome) {
    if (-not (Test-Path (Join-Path $JavaHome 'bin\java.exe'))) {
        throw 'JavaHome must point to a JDK directory containing bin\java.exe.'
    }
    $env:JAVA_HOME = $JavaHome
} elseif (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Set JAVA_HOME or pass -JavaHome with the Android Studio jbr directory.'
}

Push-Location $repoRoot
try {
    & $NodePath --test google-apps-script/tests/http-contract.test.cjs scripts/tests/docs.test.cjs scripts/tests/test-backend.test.cjs scripts/tests/release.test.cjs
    if ($LASTEXITCODE -ne 0) { throw 'Node regression tests failed.' }
    & $NodePath scripts/docs.cjs --check
    if ($LASTEXITCODE -ne 0) { throw 'Documentation checks failed.' }
    & .\gradlew.bat :app:testDebugUnitTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Android API tests failed.' }
} finally {
    Pop-Location
}
