import urllib.request, json, sys, time

print("Calling POST /api/review/generate ...", flush=True)
start = time.time()
try:
    req = urllib.request.Request(
        "http://localhost:8081/api/review/generate",
        data=b'{}',
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    with urllib.request.urlopen(req, timeout=300) as resp:
        body = json.loads(resp.read())
        elapsed = time.time() - start
        print(f"Done in {elapsed:.1f}s: code={body.get('code')}, message={body.get('message')}", flush=True)
except Exception as e:
    elapsed = time.time() - start
    print(f"Failed after {elapsed:.1f}s: {e}", flush=True)

print("\nCalling GET /api/review/today ...", flush=True)
try:
    req = urllib.request.Request("http://localhost:8081/api/review/today", method="GET")
    with urllib.request.urlopen(req, timeout=10) as resp:
        body = json.loads(resp.read())
        data = body.get("data", {})
        if not data:
            print("No data", flush=True)
        else:
            print(f"Date: {data.get('reviewDate')}", flush=True)
            print(f"Title: {data.get('title')}", flush=True)
            print(f"Modules: {data.get('moduleCount')}, Items: {data.get('itemCount')}, Mastered: {data.get('masteredCount')}", flush=True)
            items = data.get("items", [])
            modules = {}
            for item in items:
                m = item.get("moduleName", "")
                modules.setdefault(m, 0)
                modules[m] += 1
            print("\nPer module:", flush=True)
            for m, c in modules.items():
                print(f"  {m}: {c}", flush=True)
except Exception as e:
    print(f"Failed: {e}", flush=True)

print("\nDONE", flush=True)