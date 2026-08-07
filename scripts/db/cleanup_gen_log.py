import pymysql
conn = pymysql.connect(host='154.201.68.122', port=3306, user='app_user', password='App@Remote2026#User', database='springcloud_demo', charset='utf8mb4')
cur = conn.cursor()
cur.execute("UPDATE generation_log SET status='failed', error_message='进程中断', finished_at=NOW() WHERE status='running'")
print(f"cleaned {cur.rowcount} stale records")
conn.commit()
conn.close()