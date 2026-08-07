import pymysql
conn = pymysql.connect(host='154.201.68.122', port=3306, user='app_user', password='App@Remote2026#User', database='springcloud_demo', charset='utf8mb4')
cur = conn.cursor()
cur.execute('SELECT id,status,review_item_count,test_item_count,module_count,duration_ms,error_message FROM generation_log ORDER BY id DESC LIMIT 1')
r = cur.fetchone()
print('id=%s status=%s review=%d test=%d modules=%d dur=%s err=%s' % (r[0], r[1], r[2], r[3], r[4], r[5], (r[6] or '')[:100]))
conn.close()