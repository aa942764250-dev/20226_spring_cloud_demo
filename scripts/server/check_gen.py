import urllib.request, json

r = urllib.request.urlopen('http://localhost:8081/api/generation/history?page=1&size=10', timeout=10)
d = json.loads(r.read())
for h in d.get('data', []):
    print(f"id={h['id']} date={h['targetDate']} status={h['status']} items={h['reviewItemCount']}+{h['testItemCount']} dur={h['durationMs']}")