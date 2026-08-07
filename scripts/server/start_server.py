import subprocess, time, sys

JAR = r"D:\Workspace\springcloud-demo\springcloud-service\target\springcloud-service-1.0.0-SNAPSHOT.jar"

proc = subprocess.Popen(
    ["java", "-jar", JAR],
    stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    cwd=r"D:\Workspace\springcloud-demo"
)
print(f"PID: {proc.pid}")

for i in range(30):
    time.sleep(2)
    try:
        import urllib.request
        req = urllib.request.Request("http://localhost:8081/api/review/today", method="GET")
        with urllib.request.urlopen(req, timeout=3):
            print(f"READY after {(i+1)*2}s")
            sys.exit(0)
    except Exception:
        pass
    if (i+1) % 5 == 0:
        print(f"  waiting {(i+1)*2}s...")

print("TIMEOUT")
proc.kill()
sys.exit(1)