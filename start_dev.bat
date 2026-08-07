@echo off
setlocal
cd /d D:\Workspace\springcloud-demo

REM check jdk
if not exist "D:\Program Files\jdk1.8\bin\java.exe" (
  echo JDK not found at D:\Program Files\jdk1.8\bin\java.exe
  echo Please install JDK 1.8 or edit this bat with your java path
  pause
  exit /b 1
)

REM check jars
if not exist "springcloud-gateway\target\springcloud-gateway-1.0.0-SNAPSHOT.jar" (
  echo Missing jar files. Run first: mvn -pl springcloud-gateway,workbench-service,ai-teacher-service -am -DskipTests package
  pause
  exit /b 1
)

echo Killing old java processes...
taskkill /F /IM java.exe >nul 2>&1
timeout /t 3 /nobreak >nul

echo Starting 3 services (each in its own window)...
start "gateway" "D:\Program Files\jdk1.8\bin\java.exe" -jar springcloud-gateway\target\springcloud-gateway-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
start "workbench" "D:\Program Files\jdk1.8\bin\java.exe" -jar workbench-service\target\workbench-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
start "ai-teacher" "D:\Program Files\jdk1.8\bin\java.exe" -jar ai-teacher-service\target\ai-teacher-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev

echo Waiting 45s for services to register with Nacos...
timeout /t 45 /nobreak >nul

echo Checking ports 8080 / 8083 / 8084 ...
netstat -ano | findstr "8080 8083 8084"
echo.
echo If 8080 shows LISTENING, the gateway is up.
echo Open your FRONTEND dev port (e.g. http://localhost:5173) in the browser, not 8080.
echo Login: admin / 123456
echo.
pause
