import subprocess, time, json, urllib.request, sys

JAR = r"D:\Workspace\springcloud-demo\springcloud-service\target\springcloud-service-1.0.0-SNAPSHOT.jar"

print("Starting Spring Boot...")
proc = subprocess.Popen(
    ["java", "-jar", JAR],
    stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    cwd=r"D:\Workspace\springcloud-demo"
)

print(f"PID: {proc.pid}")

ready = False
for i in range(40):
    time.sleep(2)
    try:
        req = urllib.request.Request("http://localhost:8081/api/review/today", method="GET")
        with urllib.request.urlopen(req, timeout=3) as resp:
            ready = True
            print(f"Server ready after {(i+1)*2}s")
            break
    except:
        if i % 5 == 4:
            print(f"  Waiting... {(i+1)*2}s")

if not ready:
    print("Server did not start in 80s, aborting")
    proc.kill()
    sys.exit(1)

print("\nCalling POST /api/review/generate ...")
try:
    req = urllib.request.Request(
        "http://localhost:8081/api/review/generate",
        data=b'{}',
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    with urllib.request.urlopen(req, timeout=300) as resp:
        body = json.loads(resp.read())
        print(f"Generate result: code={body.get('code')}, message={body.get('message')}")
except Exception as e:
    print(f"Generate failed: {e}")

print("\nCalling GET /api/review/today ...")
try:
    req = urllib.request.Request("http://localhost:8081/api/review/today", method="GET")
    with urllib.request.urlopen(req, timeout=10) as resp:
        body = json.loads(resp.read())
        data = body.get("data", {})
        print(f"Date: {data.get('reviewDate')}")
        print(f"Title: {data.get('title')}")
        print(f"Modules: {data.get('moduleCount')}, Items: {data.get('itemCount')}, Mastered: {data.get('masteredCount')}")
        items = data.get("items", [])
        modules = {}
        for item in items:
            m = item.get("moduleName", "")
            if m not in modules:
                modules[m] = 0
            modules[m] += 1
        print("\nPer module:")
        for m, c in modules.items():
            print(f"  {m}: {c} items")
except Exception as e:
    print(f"Get today failed: {e}")

print("\nDone. Leaving server running (PID {}).".format(proc.pid))