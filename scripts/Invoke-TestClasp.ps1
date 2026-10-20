param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ClaspArgs = @('--help')
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$node = Get-Command node -ErrorAction Stop
& $node.Source (Join-Path $PSScriptRoot 'prepare-test-backend.cjs')
if ($LASTEXITCODE -ne 0) { throw 'Test backend preparation failed.' }

$pnpm = Get-Command pnpm -ErrorAction SilentlyContinue
$npx = Get-Command npx -ErrorAction SilentlyContinue
$bundledPnpm = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\bin\fallback\pnpm.cmd'

Push-Location (Join-Path $root '.codex\apps-script-test')
try {
    if ($pnpm) {
        & $pnpm.Source dlx '@google/clasp@3.4.1' @ClaspArgs
    } elseif ($npx) {
        & $npx.Source --yes '--package=@google/clasp@3.4.1' clasp @ClaspArgs
    } elseif (Test-Path -LiteralPath $bundledPnpm) {
        & $bundledPnpm dlx '@google/clasp@3.4.1' @ClaspArgs
    } else {
        throw 'Install Node.js with npm or pnpm to run the official Apps Script CLI.'
    }
    if ($LASTEXITCODE -ne 0) { throw "clasp exited with code $LASTEXITCODE." }
} finally {
    Pop-Location
}
