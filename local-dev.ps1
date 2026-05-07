<#
.SYNOPSIS
  KAL 本地一键启动 / 停止 / 重启 / 状态查询。
.EXAMPLE
  .\local-dev.ps1                      # 等同于 -Action Start
  .\local-dev.ps1 -Action Restart      # 重新构建并重启
  .\local-dev.ps1 -Action Stop
  .\local-dev.ps1 -Action Status
  .\local-dev.ps1 -Force               # 占用端口直接 kill 不询问
#>
param(
    [ValidateSet('Start', 'Stop', 'Restart', 'Status')]
    [string]$Action = 'Start',

    [int]$BackendPort = 8080,
    [int]$FrontendPort = 5173,
    [switch]$SkipBuild,
    [switch]$AllowMail,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

$script:Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$script:RuntimeDir = Join-Path $script:Root '.local-run'
$script:StateFile = Join-Path $script:RuntimeDir 'state.json'

function Write-Info($Message) {
    Write-Host "[INFO] $Message" -ForegroundColor Cyan
}

function Write-WarnLine($Message) {
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-Ok($Message) {
    Write-Host "[OK]   $Message" -ForegroundColor Green
}

function Ensure-Directory($Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

function Test-CommandExists($Name) {
    return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Require-Command($Name, $Hint) {
    if (-not (Test-CommandExists $Name)) {
        throw "Missing required command '$Name'. $Hint"
    }
}

function Get-JavaExe() {
    $java = Get-Command java -ErrorAction Stop
    return $java.Source
}

function Get-BackendDir() {
    return Join-Path $script:Root 'backend'
}

function Get-FrontendDir() {
    return Join-Path $script:Root 'frontend-vue'
}

function Get-BackendJarPath() {
    return Join-Path (Get-BackendDir) 'target\kal-backend-app.jar'
}

function Import-DotEnvDefaults($FilePath) {
    if (-not (Test-Path -LiteralPath $FilePath)) {
        return
    }

    foreach ($line in Get-Content -LiteralPath $FilePath -Encoding UTF8) {
        $raw = [string]$line
        if ([string]::IsNullOrWhiteSpace($raw)) {
            continue
        }

        $trimmed = $raw.Trim()
        if ($trimmed.StartsWith('#')) {
            continue
        }

        $eq = $raw.IndexOf('=')
        if ($eq -le 0) {
            continue
        }

        $name = $raw.Substring(0, $eq).Trim()
        $value = $raw.Substring($eq + 1)
        if ([string]::IsNullOrWhiteSpace($name)) {
            continue
        }

        if (-not [string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable($name, 'Process'))) {
            continue
        }

        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    }
}

function Set-LocalDevDefaults($FrontPort) {
    # 这些是「本地一键启动」专用的覆盖：
    # 因为仓库根目录的 .env 是为线上服务器准备的（CORS_ALLOWED_ORIGINS / WEB_BASE_URL 都指向公网 IP），
    # 直接被 Apply-BackendRuntimeEnv 注入会让本地 5173 被 CORS 拦截，验证码图片拉不到。
    # 这里在加载 .env 之前先把这两个 key 强制设成本地友好的值，
    # Import-DotEnvDefaults 只会在变量未设时再写入，所以本地 dev 永远拿到 localhost。
    $localOrigins = @(
        "http://localhost:$FrontPort",
        "http://127.0.0.1:$FrontPort",
        'http://localhost:5173', 'http://127.0.0.1:5173',
        'http://localhost:5174', 'http://127.0.0.1:5174',
        'http://localhost:4173', 'http://127.0.0.1:4173',
        'http://localhost:3000', 'http://127.0.0.1:3000'
    ) | Select-Object -Unique
    [Environment]::SetEnvironmentVariable('KAL_CORS_ALLOWED_ORIGINS', ($localOrigins -join ','), 'Process')
    [Environment]::SetEnvironmentVariable('KAL_WEB_BASE_URL', "http://localhost:$FrontPort", 'Process')
}

function Set-FrontendDevEnv($ApiPort) {
    $envFile = Join-Path (Get-FrontendDir) '.env.development'
    $content = "VITE_API_BASE=http://localhost:$ApiPort/api/v1`r`n"
    $current = if (Test-Path -LiteralPath $envFile) {
        Get-Content -LiteralPath $envFile -Raw -Encoding UTF8
    } else {
        ''
    }

    if ($current -ne $content) {
        Set-Content -LiteralPath $envFile -Value $content -Encoding UTF8 -NoNewline
        Write-Info "Updated frontend-vue/.env.development -> http://localhost:$ApiPort/api/v1"
    }
}

function Apply-BackendRuntimeEnv() {
    Set-LocalDevDefaults -FrontPort $script:FrontendPort

    foreach ($candidate in @(
        (Join-Path $script:Root '.env'),
        (Join-Path $script:Root '.runtime.env'),
        (Join-Path (Get-BackendDir) '.env')
    )) {
        Import-DotEnvDefaults -FilePath $candidate
    }
}

function ConvertTo-HashtableCompat($Value) {
    if ($null -eq $Value) {
        return $null
    }

    if ($Value -is [System.Collections.IDictionary]) {
        $table = [ordered]@{}
        foreach ($key in $Value.Keys) {
            $table[$key] = ConvertTo-HashtableCompat $Value[$key]
        }
        return $table
    }

    if ($Value -is [System.Management.Automation.PSCustomObject]) {
        $table = [ordered]@{}
        foreach ($property in $Value.PSObject.Properties) {
            $table[$property.Name] = ConvertTo-HashtableCompat $property.Value
        }
        return $table
    }

    if ($Value -is [System.Collections.IEnumerable] -and -not ($Value -is [string])) {
        $items = @()
        foreach ($item in $Value) {
            $items += ,(ConvertTo-HashtableCompat $item)
        }
        return $items
    }

    return $Value
}

function Load-State() {
    if (-not (Test-Path -LiteralPath $script:StateFile)) {
        return [ordered]@{}
    }

    $raw = Get-Content -LiteralPath $script:StateFile -Raw -Encoding UTF8
    if ([string]::IsNullOrWhiteSpace($raw)) {
        return [ordered]@{}
    }

    $data = ConvertTo-HashtableCompat ($raw | ConvertFrom-Json)
    if ($null -eq $data) {
        return [ordered]@{}
    }

    return $data
}

function Save-State($State) {
    Ensure-Directory $script:RuntimeDir
    $json = $State | ConvertTo-Json -Depth 8
    Set-Content -LiteralPath $script:StateFile -Value $json -Encoding UTF8
}

function Get-ProcessInfo($ProcessId) {
    if (-not $ProcessId) {
        return $null
    }

    return Get-CimInstance Win32_Process -Filter "ProcessId = $ProcessId" -ErrorAction SilentlyContinue
}

function Test-TrackedProcess($ProcessId, [string[]]$Patterns) {
    $proc = Get-ProcessInfo $ProcessId
    if ($null -eq $proc) {
        return $false
    }

    $cmdLine = [string]$proc.CommandLine
    foreach ($pattern in $Patterns) {
        if ($cmdLine -match $pattern) {
            return $true
        }
    }

    return $false
}

function Get-PortOwners($Port) {
    $connections = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue |
        Where-Object { $_.State -eq 'Listen' } |
        Select-Object -ExpandProperty OwningProcess -Unique

    if ($null -eq $connections) {
        return @()
    }

    return @($connections)
}

function Resolve-ServicePid($Name, $Port, $TrackedPid) {
    $patterns = Get-ServicePatterns $Name
    if ($TrackedPid -and (Test-TrackedProcess -ProcessId $TrackedPid -Patterns $patterns)) {
        return [int]$TrackedPid
    }

    foreach ($owner in (Get-PortOwners -Port $Port)) {
        if (Test-TrackedProcess -ProcessId $owner -Patterns $patterns) {
            return [int]$owner
        }
    }

    return $null
}

function Stop-ProcessTreeSafe($ProcessId, $Label) {
    if (-not $ProcessId) {
        return
    }

    $proc = Get-ProcessInfo $ProcessId
    if ($null -eq $proc) {
        return
    }

    Write-Info "Stopping $Label (PID $ProcessId)..."
    taskkill /PID $ProcessId /T /F | Out-Null
}

function Wait-Until($TimeoutSeconds, [scriptblock]$Condition, $Description) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (& $Condition) {
            return $true
        }
        Start-Sleep -Milliseconds 500
    }

    throw "Timed out waiting for $Description."
}

function Test-HttpReady($Url) {
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
        return ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500)
    }
    catch {
        return $false
    }
}

function Read-ZipEntryText($ZipPath, $EntryPath) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($ZipPath)
    try {
        $entry = $zip.Entries | Where-Object { $_.FullName -eq $EntryPath } | Select-Object -First 1
        if ($null -eq $entry) {
            return $null
        }

        $stream = $entry.Open()
        $reader = New-Object System.IO.StreamReader($stream)
        try {
            return $reader.ReadToEnd()
        }
        finally {
            $reader.Dispose()
            $stream.Dispose()
        }
    }
    finally {
        $zip.Dispose()
    }
}

function Test-BootJar($JarPath) {
    if (-not (Test-Path -LiteralPath $JarPath)) {
        return $false
    }

    $manifest = Read-ZipEntryText -ZipPath $JarPath -EntryPath 'META-INF/MANIFEST.MF'
    if ([string]::IsNullOrWhiteSpace($manifest)) {
        return $false
    }

    return ($manifest -match 'Main-Class:\s+' -and $manifest -match 'Start-Class:\s+')
}

function Get-ServicePatterns($Name) {
    switch ($Name) {
        'backend' {
            return @(
                'kal-backend-app\.jar',
                'cn\.edu\.ruc\.kal',
                'spring-boot:run',
                'C:\\PROGRAMING\\KAL\\src\\backend'
            )
        }
        'frontend' {
            return @(
                'frontend-vue',
                'vite',
                'npm(\.cmd)? run dev'
            )
        }
        default {
            return @()
        }
    }
}

function Stop-ServiceInstance($State, $Name, $Port) {
    $patterns = Get-ServicePatterns $Name
    $stopped = $false

    if ($State.Contains($Name) -and $State[$Name].Pid) {
        $trackedPid = [int]$State[$Name].Pid
        if (Test-TrackedProcess -ProcessId $trackedPid -Patterns $patterns) {
            Stop-ProcessTreeSafe -ProcessId $trackedPid -Label $Name
            $stopped = $true
        }
        $State.Remove($Name) | Out-Null
    }

    foreach ($owner in (Get-PortOwners -Port $Port)) {
        if (Test-TrackedProcess -ProcessId $owner -Patterns $patterns) {
            Stop-ProcessTreeSafe -ProcessId $owner -Label "$Name on port $Port"
            $stopped = $true
        }
    }

    try {
        Wait-Until -TimeoutSeconds 15 -Condition { (Get-PortOwners -Port $Port).Count -eq 0 } -Description "port $Port to be free" | Out-Null
    }
    catch {
        $remaining = Get-PortOwners -Port $Port
        if ($remaining.Count -gt 0) {
            throw "Port $Port is still busy after stopping $Name. Remaining PIDs: $($remaining -join ', ')"
        }
    }

    if ($stopped) {
        Write-Ok "Stopped $Name."
    }
    else {
        Write-Info "$Name is not running."
    }
}

function Format-PortOwnerInfo($OwnerPid) {
    $proc = Get-ProcessInfo $OwnerPid
    if ($null -eq $proc) {
        return "PID $OwnerPid (already gone)"
    }
    $cmd = [string]$proc.CommandLine
    if ($cmd.Length -gt 120) { $cmd = $cmd.Substring(0, 117) + '...' }
    return "PID $OwnerPid :: $($proc.Name) :: $cmd"
}

function Resolve-PortAvailable($Port, $Name) {
    while ($true) {
        $owners = Get-PortOwners -Port $Port
        if ($owners.Count -eq 0) {
            return [int]$Port
        }

        $patterns = Get-ServicePatterns $Name
        $isOurOwn = $true
        foreach ($owner in $owners) {
            if (-not (Test-TrackedProcess -ProcessId $owner -Patterns $patterns)) {
                $isOurOwn = $false
                break
            }
        }

        Write-WarnLine "Port $Port is busy ($Name)."
        foreach ($owner in $owners) {
            Write-Host ('  - ' + (Format-PortOwnerInfo $owner))
        }
        if ($isOurOwn) {
            Write-Host '  (looks like a previous KAL instance you can safely kill)' -ForegroundColor DarkGray
        }

        if ($Force) {
            Write-Info "[-Force] killing port $Port owners automatically..."
            foreach ($owner in $owners) {
                Stop-ProcessTreeSafe -ProcessId $owner -Label "$Name on port $Port"
            }
            Start-Sleep -Seconds 1
            continue
        }

        $prompt = '[k] 杀掉占用进程  [n] 换一个端口  [a] 放弃 (默认 k)'
        $choice = (Read-Host $prompt).Trim().ToLower()
        if ([string]::IsNullOrWhiteSpace($choice)) { $choice = 'k' }

        switch ($choice) {
            'k' {
                foreach ($owner in $owners) {
                    Stop-ProcessTreeSafe -ProcessId $owner -Label "$Name on port $Port"
                }
                Start-Sleep -Seconds 1
            }
            'n' {
                $newPortRaw = Read-Host "请输入 $Name 的新端口 (当前: $Port)"
                $newPort = 0
                if (-not [int]::TryParse($newPortRaw, [ref]$newPort) -or $newPort -le 0 -or $newPort -gt 65535) {
                    Write-WarnLine '端口号无效，请重新选择。'
                }
                else {
                    Write-Info "$Name 端口切换为 $newPort"
                    return [int]$newPort
                }
            }
            'a' {
                throw "Aborted by user; port $Port is still busy."
            }
            default {
                Write-WarnLine '只接受 k / n / a，请重新输入。'
            }
        }
    }
}

function Build-Backend() {
    Write-Info 'Building backend package...'
    Push-Location (Get-BackendDir)
    try {
        & mvn '-DskipTests' 'clean' 'package'
    }
    finally {
        Pop-Location
    }
}

function Start-Backend($State) {
    Require-Command java 'Install JDK 21+ and ensure java is on PATH.'
    Require-Command mvn 'Install Maven 3.9+ and ensure mvn is on PATH.'

    Apply-BackendRuntimeEnv

    if (-not $SkipBuild) {
        Build-Backend
    }

    $jarPath = Get-BackendJarPath
    $logPath = Join-Path $script:RuntimeDir 'backend.log'
    $errPath = Join-Path $script:RuntimeDir 'backend.err.log'

    Ensure-Directory $script:RuntimeDir
    if (Test-Path -LiteralPath $logPath) { Remove-Item -LiteralPath $logPath -Force }
    if (Test-Path -LiteralPath $errPath) { Remove-Item -LiteralPath $errPath -Force }

    if (Test-BootJar -JarPath $jarPath) {
        Write-Info 'Starting backend from boot jar...'
        $javaExe = Get-JavaExe
        $process = Start-Process -FilePath $javaExe `
            -ArgumentList @('-jar', $jarPath, "--server.port=$BackendPort") `
            -WorkingDirectory (Get-BackendDir) `
            -RedirectStandardOutput $logPath `
            -RedirectStandardError $errPath `
            -PassThru
    }
    else {
        Write-WarnLine 'Boot jar is missing or not executable. Falling back to mvn spring-boot:run.'
        $mailValue = if ($AllowMail) { '$env:KAL_MAIL_ENABLED=''true'';' } else { '$env:KAL_MAIL_ENABLED=''false'';' }
        $command = "$mailValue Set-Location '$((Get-BackendDir).Replace("'", "''"))'; mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=$BackendPort"
        $shellExe = (Get-Process -Id $PID).Path
        $process = Start-Process -FilePath $shellExe `
            -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', $command) `
            -WorkingDirectory (Get-BackendDir) `
            -RedirectStandardOutput $logPath `
            -RedirectStandardError $errPath `
            -PassThru
    }

    Write-Info 'Waiting for backend to accept requests...'
    Wait-Until -TimeoutSeconds 90 -Condition {
        Test-HttpReady "http://localhost:$BackendPort/api/v1/public/news?page=1&pageSize=1"
    } -Description 'backend HTTP readiness' | Out-Null

    $backendPid = Resolve-ServicePid -Name 'backend' -Port $BackendPort -TrackedPid $process.Id
    if (-not $backendPid) {
        $backendPid = $process.Id
    }

    $State['backend'] = [ordered]@{
        Pid = $backendPid
        Port = $BackendPort
        LogPath = $logPath
        ErrorLogPath = $errPath
        StartedAt = (Get-Date).ToString('s')
    }

    Write-Ok "Backend ready on http://localhost:$BackendPort"
}

function Start-Frontend($State) {
    Require-Command npm 'Install Node.js 18+ and ensure npm is on PATH.'

    $frontendDir = Get-FrontendDir
    $nodeModules = Join-Path $frontendDir 'node_modules'
    if (-not (Test-Path -LiteralPath $nodeModules)) {
        Write-Info 'Installing frontend dependencies...'
        Push-Location $frontendDir
        try {
            & npm install
        }
        finally {
            Pop-Location
        }
    }

    $logPath = Join-Path $script:RuntimeDir 'frontend.log'
    $errPath = Join-Path $script:RuntimeDir 'frontend.err.log'
    if (Test-Path -LiteralPath $logPath) { Remove-Item -LiteralPath $logPath -Force }
    if (Test-Path -LiteralPath $errPath) { Remove-Item -LiteralPath $errPath -Force }

    Write-Info 'Starting Vite dev server...'
    $process = Start-Process -FilePath 'npm.cmd' `
        -ArgumentList @('run', 'dev', '--', '--host', '0.0.0.0', '--port', "$FrontendPort", '--strictPort') `
        -WorkingDirectory $frontendDir `
        -RedirectStandardOutput $logPath `
        -RedirectStandardError $errPath `
        -PassThru

    Write-Info 'Waiting for frontend page...'
    Wait-Until -TimeoutSeconds 45 -Condition {
        Test-HttpReady "http://localhost:$FrontendPort/login"
    } -Description 'frontend HTTP readiness' | Out-Null

    $frontendPid = Resolve-ServicePid -Name 'frontend' -Port $FrontendPort -TrackedPid $process.Id
    if (-not $frontendPid) {
        $frontendPid = $process.Id
    }

    $State['frontend'] = [ordered]@{
        Pid = $frontendPid
        Port = $FrontendPort
        LogPath = $logPath
        ErrorLogPath = $errPath
        StartedAt = (Get-Date).ToString('s')
    }

    Write-Ok "Frontend ready on http://localhost:$FrontendPort"
}

function Show-Status($State) {
    foreach ($name in @('backend', 'frontend')) {
        if (-not $State.Contains($name)) {
            Write-Host ("{0,-8}: stopped" -f $name)
            continue
        }

        $item = $State[$name]
        $resolvedPid = Resolve-ServicePid -Name $name -Port ([int]$item.Port) -TrackedPid ([int]$item.Pid)
        $isAlive = $null -ne $resolvedPid
        $status = if ($isAlive) { 'running' } else { 'stale-state' }
        $pidText = if ($resolvedPid) { $resolvedPid } else { $item.Pid }
        Write-Host ("{0,-8}: {1} | pid={2} | port={3}" -f $name, $status, $pidText, $item.Port)
    }
}

Ensure-Directory $script:RuntimeDir
$state = Load-State

function Invoke-StartStack($State) {
    # 端口冲突先交互式决策：避免后端先用旧端口启动后才发现前端端口要改，导致 CORS 还得重启一次。
    $script:BackendPort = Resolve-PortAvailable -Port $script:BackendPort -Name 'backend'
    $script:FrontendPort = Resolve-PortAvailable -Port $script:FrontendPort -Name 'frontend'
    Set-FrontendDevEnv -ApiPort $script:BackendPort

    Start-Backend -State $State
    Save-State $State
    Start-Frontend -State $State
    Save-State $State

    Write-Host ''
    Write-Ok 'Local development stack is ready.'
    Write-Host "Frontend : http://localhost:$script:FrontendPort"
    Write-Host "Backend  : http://localhost:$script:BackendPort"
    Write-Host "Captcha  : http://localhost:$script:BackendPort/api/v1/public/captcha"
    Write-Host "Logs     : $script:RuntimeDir"
}

switch ($Action) {
    'Stop' {
        Stop-ServiceInstance -State $state -Name 'frontend' -Port $FrontendPort
        Stop-ServiceInstance -State $state -Name 'backend' -Port $BackendPort
        Save-State $state
    }
    'Start' {
        if ($Force) {
            Stop-ServiceInstance -State $state -Name 'frontend' -Port $FrontendPort
            Stop-ServiceInstance -State $state -Name 'backend' -Port $BackendPort
        }

        Invoke-StartStack -State $state
    }
    'Restart' {
        Stop-ServiceInstance -State $state -Name 'frontend' -Port $FrontendPort
        Stop-ServiceInstance -State $state -Name 'backend' -Port $BackendPort
        Save-State $state

        Invoke-StartStack -State $state
    }
    'Status' {
        Show-Status -State $state
        Write-Host "Logs: $script:RuntimeDir"
    }
}