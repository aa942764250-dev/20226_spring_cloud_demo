import pymysql

conn = pymysql.connect(
    host='154.201.68.122', port=3306,
    user='app_user', password='App@Remote2026#User',
    database='springcloud_demo', charset='utf8mb4',
    ssl_disabled=True,
)

try:
    with conn.cursor() as cur:
        cur.execute("SELECT id, review_date, title, module_count, item_count, status FROM review_daily ORDER BY id DESC LIMIT 5")
        rows = cur.fetchall()
        if not rows:
            print("review_daily: EMPTY - generate not completed yet")
        else:
            print(f"review_daily: {len(rows)} record(s)")
            for r in rows:
                print(f"  id={r[0]}, date={r[1]}, title={r[2]}, modules={r[3]}, items={r[4]}, status={r[5]}")

        cur.execute("SELECT COUNT(*) FROM review_item")
        cnt = cur.fetchone()[0]
        print(f"\nreview_item: {cnt} total")

        cur.execute("SELECT module_name, COUNT(*) FROM review_item GROUP BY module_name ORDER BY COUNT(*) DESC")
        for r in cur.fetchall():
            print(f"  {r[0]}: {r[1]}")

        cur.execute("SELECT COUNT(*) FROM review_progress")
        cnt = cur.fetchone()[0]
        print(f"\nreview_progress: {cnt} total")
finally:
    conn.close()