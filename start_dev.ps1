# start_dev.ps1 —— 本机一键启动 springcloud-demo 三服务 + 健康检查
# 用法: 在 PowerShell 里 cd 到本脚本目录, 执行  .\start_dev.ps1
# 前提: 已 mvn package 重打包出三个 jar(含 fail-fast 配置)

$ErrorActionPreference = "Continue"
$ROOT = "D:\Workspace\springcloud-demo"
$JAVA = "D:\Program Files\jdk1.8\bin\java.exe"
$LOGDIR = "D:\Workspace\.workbuddy\tmp"

$services = @(
  @{name="gateway";    path="$ROOT\springcloud-gateway\target\springcloud-gateway-1.0.0-SNAPSHOT.jar"; port=8080; profile="dev"},
  @{name="workbench";  path="$ROOT\workbench-service\target\workbench-service-1.0.0-SNAPSHOT.jar";     port=8083; profile="dev"},
  @{name="ai-teacher"; path="$ROOT\ai-teacher-service\target\ai-teacher-service-1.0.0-SNAPSHOT.jar";   port=8084; profile="dev"}
)

# 1) 检查 jar 是否齐全
foreach ($s in $services) {
  if (-not (Test-Path $s.path)) {
    Write-Host "✗ 缺少 $($s.name) 的 jar: $($s.path)" -ForegroundColor Red
    Write-Host "  请先在 $ROOT 执行: mvn -pl springcloud-gateway,workbench-service,ai-teacher-service -am -DskipTests package" -ForegroundColor Yellow
    exit 1
  }
}

# 2) 停掉旧的 java 进程
Write-Host "停止旧的 java 进程..." -ForegroundColor Gray
taskkill /F /IM java.exe 2>$null
Start-Sleep -Seconds 2

# 3) 启动三服务(后台)
foreach ($s in $services) {
  Write-Host "启动 $($s.name) (port $($s.port), profile $($s.profile))..." -ForegroundColor Cyan
  Start-Process -FilePath $JAVA -ArgumentList "-jar", $s.path, "--spring.profiles.active=$($s.profile)" `
    -RedirectStandardOutput "$LOGDIR\$($s.name).log" -RedirectStandardError "$LOGDIR\$($s.name).err.log" -NoNewWindow
}

# 4) 健康检查(轮询端口)
foreach ($s in $services) {
  $ok = $false
  for ($i = 0; $i -lt 45; $i++) {
    try {
      $r = Test-NetConnection -ComputerName localhost -Port $s.port -WarningAction SilentlyContinue
      if ($r.TcpTestSucceeded) { $ok = $true; break }
    } catch {}
    Start-Sleep -Seconds 2
  }
  if ($ok) { Write-Host "✓ $($s.name) 端口 $($s.port) 已监听" -ForegroundColor Green }
  else    { Write-Host "✗ $($s.name) 端口 $($s.port) 未就绪, 查日志: $LOGDIR\$($s.name).log" -ForegroundColor Red }
}

# 5) 测登录接口
Start-Sleep -Seconds 3
try {
  $body = '{"username":"admin","password":"123456"}'
  $resp = Invoke-RestMethod -Uri "http://localhost:8080/api/wb/auth/login" -Method Post `
            -ContentType "application/json" -Body $body -TimeoutSec 30
  if ($resp.code -eq 200) {
    $menus = ($resp.data.menus | ForEach-Object { $_.name }) -join ", "
    Write-Host "✓ 登录成功! token 已获取, 菜单: $menus" -ForegroundColor Green
  } else {
    Write-Host "✗ 登录返回异常: code=$($resp.code) message=$($resp.message)" -ForegroundColor Red
  }
} catch {
  Write-Host "✗ 登录请求失败: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "=================================================" -ForegroundColor White
Write-Host " 浏览器请访问【前端项目】的 dev 端口(如 http://localhost:5173 或 5180)" -ForegroundColor Cyan
Write-Host " 8080 是后端 API 网关, 浏览器直接打开看到空白/404 是正常的" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor White
