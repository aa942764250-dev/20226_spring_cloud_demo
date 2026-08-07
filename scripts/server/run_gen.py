import urllib.request, json, time
req = urllib.request.Request('http://localhost:8081/api/generation/trigger', method='POST', headers={'Content-Type':'application/json'})
r = urllib.request.urlopen(req, timeout=10)
d = json.loads(r.read())
print('trigger: code=%d status=%s' % (d['code'], d.get('data',{}).get('status','')))

print('polling...')
for i in range(40):
    time.sleep(10)
    r2 = urllib.request.urlopen('http://localhost:8081/api/generation/status', timeout=10)
    d2 = json.loads(r2.read())
    s = d2.get('data',{})
    print('[%ds] status=%s review=%d test=%d dur=%s' % (i*10, s.get('status'), s.get('reviewItemCount',0), s.get('testItemCount',0), s.get('durationMs')))
    if s.get('status') in ('success','failed','skipped'):
        if s.get('errorMessage'):
            print('ERROR:', s['errorMessage'][:200])
        break