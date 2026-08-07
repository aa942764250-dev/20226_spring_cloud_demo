import pymysql

conn = pymysql.connect(
    host='154.201.68.122', port=3306,
    user='app_user', password='App@Remote2026#User',
    database='springcloud_demo', charset='utf8mb4',
    ssl_disabled=True, connect_timeout=10,
)

try:
    with conn.cursor() as cur:
        cur.execute("DELETE FROM review_progress WHERE item_id IN (SELECT id FROM review_item WHERE daily_id=2)")
        cur.execute("DELETE FROM review_item WHERE daily_id=2")
        cur.execute("DELETE FROM review_daily WHERE id=2")
        conn.commit()
        print("Cleaned daily_id=2 (2026-07-29)")

        cur.execute("SELECT COUNT(*) FROM review_item WHERE daily_id=1")
        cnt = cur.fetchone()[0]
        print(f"Remaining items for 2026-07-28: {cnt}")
finally:
    conn.close()