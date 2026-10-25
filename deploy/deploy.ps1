[CmdletBinding()]
param(
  [ValidateSet('deploy','up','down','logs','status','rollback')]
  [string]$Action = 'deploy',
  [string]$Version
)
$ErrorActionPreference = 'Stop'
$DeployDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $DeployDir
$EnvFile = if ($env:DEPLOY_ENV_FILE) { $env:DEPLOY_ENV_FILE } else { Join-Path $DeployDir '.env' }
$StateFile = Join-Path $DeployDir '.deploy.env'
$Compose = @('compose','--env-file',$EnvFile,'--env-file',$StateFile,'-f',(Join-Path $DeployDir 'docker-compose.yml'))

if (-not (Test-Path $EnvFile)) { throw "Missing $EnvFile. Copy deploy/.env.example to deploy/.env and fill secrets." }
if (Select-String -LiteralPath $EnvFile -Pattern '(^|=)replace-with-' -Quiet) { throw "Deployment secrets in $EnvFile still contain replace-with- placeholders." }
docker version | Out-Null
docker compose version | Out-Null
function Write-Version([string]$Value) { Set-Content -LiteralPath $StateFile -Value "APP_VERSION=$Value" -Encoding ascii }
if (-not (Test-Path $StateFile)) { Write-Version 'local' }
switch ($Action) {
  'deploy' { if (-not $Version) { $Version = (Get-Date).ToUniversalTime().ToString('yyyyMMddHHmmss') }; Write-Version $Version; docker @Compose build --pull; docker @Compose up -d --remove-orphans; docker @Compose ps }
  'up' { if (-not (Test-Path $StateFile)) { Write-Version 'local' }; docker @Compose up -d --remove-orphans; docker @Compose ps }
  'down' { docker @Compose down }
  'logs' { docker @Compose logs -f --tail=200 $Version }
  'status' { docker @Compose ps }
  'rollback' { if (-not $Version) { throw 'rollback requires a previously built version, for example 20261025213000.' }; Write-Version $Version; docker @Compose up -d --no-build --remove-orphans; docker @Compose ps }
}
