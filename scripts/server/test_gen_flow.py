import urllib.request, json, time

print("=== 1. 触发生成 ===")
req = urllib.request.Request('http://localhost:8081/api/generation/trigger', method='POST', headers={'Content-Type':'application/json'})
r = urllib.request.urlopen(req, timeout=10)
d = json.loads(r.read())
print("触发结果: code=%d msg=%s" % (d['code'], d['message']))
if d.get('data'):
    print("  id=%s status=%s" % (d['data']['id'], d['data']['status']))

print("\n=== 2. 幂等校验: 再次触发 ===")
try:
    r2 = urllib.request.urlopen(req, timeout=10)
    d2 = json.loads(r2.read())
    print("  code=%d msg=%s" % (d2['code'], d2['message']))
except urllib.error.HTTPError as e:
    body = json.loads(e.read())
    print("  code=%d msg=%s" % (body['code'], body['message']))

print("\n=== 3. 轮询状态 ===")
for i in range(20):
    time.sleep(5)
    r3 = urllib.request.urlopen('http://localhost:8081/api/generation/status', timeout=10)
    d3 = json.loads(r3.read())
    if d3.get('data'):
        s = d3['data']
        print("  [%ds] status=%s review=%d test=%d dur=%s" % (i*5, s['status'], s['reviewItemCount'], s['testItemCount'], s['durationMs']))
        if s['status'] in ('success', 'failed', 'skipped'):
            break
    else:
        print("  [%ds] no data" % (i*5))
        break

print("\n=== 4. 最终历史 ===")
r4 = urllib.request.urlopen('http://localhost:8081/api/generation/history?page=1&size=5', timeout=10)
d4 = json.loads(r4.read())
for h in d4.get('data', []):
    print("  id=%d date=%s type=%s status=%s review=%d test=%d dur=%s" % (h['id'], h['targetDate'], h['type'], h['status'], h['reviewItemCount'], h['testItemCount'], h['durationMs']))
