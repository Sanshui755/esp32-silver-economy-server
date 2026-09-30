# =============================================================================
# 自动探测本机局域网 IP 并写入 .env 的 HOST_IP
#
# 背景: Windows/macOS 的 Docker Desktop 里，容器只能看到虚拟机网络，
#       ServerIpProbe 在容器内探测不到宿主机真实局域网 IP，所以要宿主机侧注入。
#       Linux 可留空 HOST_IP 自动探测，无需此脚本。
#
# 用法: powershell -ExecutionPolicy Bypass -File scripts\sync-host-ip.ps1
# 生效: IP 变化后重跑一次本脚本，再执行 docker compose up -d 重建容器
# =============================================================================
$ErrorActionPreference = 'Stop'

$RootDir        = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$EnvFile        = Join-Path $RootDir '.env'
$EnvExampleFile = Join-Path $RootDir '.env.example'

# 这些名称的虚拟网卡 IP 设备连不上，全部排除
$VirtualAdapterPattern = 'vEthernet|VMware|VirtualBox|Hyper-V|WSL|Loopback|docker|Bluetooth|蓝牙|TAP|TUN|VPN'

function Get-LanIPv4 {
    # 方法1: 默认路由所在网卡 —— 一般就是真实出口，最准
    $routes = Get-NetRoute -DestinationPrefix '0.0.0.0/0' -AddressFamily IPv4 -ErrorAction SilentlyContinue |
              Where-Object { $_.InterfaceAlias -notmatch $VirtualAdapterPattern }
    $best = $routes | Sort-Object RouteMetric | Select-Object -First 1
    if ($best) {
        $ip = Get-NetIPAddress -InterfaceIndex $best.InterfaceIndex -AddressFamily IPv4 -ErrorAction SilentlyContinue |
              Where-Object { $_.IPAddress -notmatch '^(127\.|169\.254\.)' } |
              Select-Object -First 1
        if ($ip) { return $ip.IPAddress }
    }

    # 方法2: 兜底枚举所有已启用网卡的 IPv4（.NET API，兼容没有 Get-NetRoute 的老系统）
    $nics = [System.Net.NetworkInformation.NetworkInterface]::GetAllNetworkInterfaces() |
            Where-Object {
                $_.OperationalStatus -eq 'Up' -and
                $_.NetworkInterfaceType -ne [System.Net.NetworkInformation.NetworkInterfaceType]::Loopback -and
                $_.Name -notmatch $VirtualAdapterPattern
            }
    foreach ($nic in $nics) {
        foreach ($addr in $nic.GetIPProperties().UnicastAddresses) {
            $text = $addr.Address.ToString()
            if ($addr.Address.AddressFamily -eq [System.Net.Sockets.AddressFamily]::InterNetwork -and
                -not $text.StartsWith('169.254.')) {
                return $text
            }
        }
    }
    return $null
}

# ---- .env 不存在时先从 .env.example 复制 ----
if (-not (Test-Path $EnvFile)) {
    if (-not (Test-Path $EnvExampleFile)) {
        Write-Host '[sync-host-ip] .env 与 .env.example 都不存在，无法写入' -ForegroundColor Red
        exit 1
    }
    Copy-Item $EnvExampleFile $EnvFile
    Write-Host "[sync-host-ip] .env 不存在，已从 .env.example 复制" -ForegroundColor Cyan
}

# ---- 探测 ----
$ip = Get-LanIPv4
if (-not $ip) {
    Write-Host '[sync-host-ip] 未探测到局域网 IPv4，HOST_IP 保持原值' -ForegroundColor Yellow
    exit 1
}

# ---- 只替换未注释的 HOST_IP= 行，其余内容原样保留 ----
$content = [System.IO.File]::ReadAllText($EnvFile, [System.Text.Encoding]::UTF8)
$pattern = '(?m)^HOST_IP=.*$'
$old = $null
if ($content -match $pattern) {
    $old = $Matches[0] -replace '^HOST_IP=', ''
    if ($old -eq $ip) {
        Write-Host "[sync-host-ip] HOST_IP 已是 $ip，无需改动" -ForegroundColor Green
        exit 0
    }
    $content = [regex]::Replace($content, $pattern, "HOST_IP=$ip")
} else {
    $content = $content.TrimEnd() + "`r`nHOST_IP=$ip`r`n"
}

# UTF-8 无 BOM 写回，避免首行变量名前出现 BOM
[System.IO.File]::WriteAllText($EnvFile, $content, (New-Object System.Text.UTF8Encoding($false)))

if ($old) {
    Write-Host "[sync-host-ip] HOST_IP: $old -> $ip" -ForegroundColor Green
} else {
    Write-Host "[sync-host-ip] HOST_IP 已写入: $ip" -ForegroundColor Green
}
Write-Host '[sync-host-ip] 需重启容器生效: docker compose up -d' -ForegroundColor Cyan
