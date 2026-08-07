import urllib.request, json, sys, time

print("Calling POST /api/review/generate ...")
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
        print(f"Done in {elapsed:.1f}s: code={body.get('code')}, message={body.get('message')}")
except Exception as e:
    elapsed = time.time() - start
    print(f"Failed after {elapsed:.1f}s: {e}")

print("\nCalling GET /api/review/today ...")
try:
    req = urllib.request.Request("http://localhost:8081/api/review/today", method="GET")
    with urllib.request.urlopen(req, timeout=10) as resp:
        body = json.loads(resp.read())
        data = body.get("data", {})
        if not data:
            print("No data returned")
        else:
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
            print(f"\nSample questions:")
            for item in items[:5]:
                print(f"  [{item.get('moduleName')}] {item.get('question')[:60]}")
except Exception as e:
    print(f"Get today failed: {e}")