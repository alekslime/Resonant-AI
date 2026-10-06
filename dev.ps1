$root = $PSScriptRoot
$agent = Join-Path $root "agent"
$python = Join-Path $agent ".venv\Scripts\python.exe"

if (-not (Test-Path $python)) {
    Write-Host "Missing $python. Run this from the repo that has agent\.venv."
    exit 1
}

function Start-Window($title, $dir, $command) {
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$Host.UI.RawUI.WindowTitle='$title'; Set-Location '$dir'; $command"
}

if (Get-NetTCPConnection -LocalPort 11434 -State Listen -ErrorAction SilentlyContinue) {
    Write-Host "Ollama is already running."
} else {
    Start-Window "Ollama" $root "`$env:OLLAMA_HOST='0.0.0.0'; ollama serve"
}

Start-Window "Resonant token server" $agent "& '$python' token_server.py"
Start-Window "Resonant agent" $agent "& '$python' agent.py dev"

$ip = (Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias "Wi-Fi" -ErrorAction SilentlyContinue).IPAddress
Write-Host "PC address: $ip  (phone token address: ${ip}:8787)"
