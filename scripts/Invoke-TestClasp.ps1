param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ClaspArgs = @('--help')
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$node = Get-Command node -ErrorAction SilentlyContinue
$bundledNode = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe'
if ($node) {
    $nodePath = $node.Source
} elseif (Test-Path -LiteralPath $bundledNode) {
    $nodePath = $bundledNode
} else {
    throw 'Node.js was not found in PATH or the bundled Codex runtime. Install Node.js 22 or newer.'
}
& $nodePath (Join-Path $PSScriptRoot 'prepare-test-backend.cjs')
if ($LASTEXITCODE -ne 0) { throw 'Test backend preparation failed.' }

$pnpm = Get-Command pnpm -ErrorAction SilentlyContinue
$npx = Get-Command npx -ErrorAction SilentlyContinue
$bundledPnpm = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\bin\fallback\pnpm.cmd'

Push-Location (Join-Path $root '.codex\apps-script-test')
$previousPath = $env:PATH
try {
    # Package runners and their child processes also need the selected Node.js.
    $env:PATH = "$(Split-Path -Parent $nodePath);$previousPath"
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
    $env:PATH = $previousPath
    Pop-Location
}
