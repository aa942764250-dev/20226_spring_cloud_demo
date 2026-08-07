import pymysql
conn = pymysql.connect(host='154.201.68.122', port=3306, user='app_user', password='App@Remote2026#User', database='springcloud_demo', charset='utf8mb4')
cur = conn.cursor()
cur.execute("UPDATE generation_log SET status='failed', error_message='kb.py path error', finished_at=NOW() WHERE status='running'")
print('cleaned', cur.rowcount)
conn.commit()
conn.close()