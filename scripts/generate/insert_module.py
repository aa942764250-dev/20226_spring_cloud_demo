import subprocess, json, pymysql, sys, os
from datetime import date, datetime

KB_SCRIPT = r"D:\Workspace\Project_006_LocalKnowledgeMCP\kb.py"
PYTHON = r"C:\Users\黄\AppData\Local\Programs\Python\Python312\python.exe"

module = sys.argv[1]
query = sys.argv[2]
top_k = int(sys.argv[3]) if len(sys.argv) > 3 else 3

for key in ["ALL_PROXY", "all_proxy"]:
    os.environ.pop(key, None)
os.environ["HF_HUB_OFFLINE"] = "1"

print(f"Searching: [{module}] {query} ...", flush=True)
result = subprocess.run(
    [PYTHON, KB_SCRIPT, "search", query, "--top-k", str(top_k)],
    capture_output=True, encoding='utf-8', errors='replace', timeout=60,
    cwd=r"D:\Workspace\Project_006_LocalKnowledgeMCP",
)
if result.returncode != 0:
    print(f"Error: exit code {result.returncode}", flush=True)
    sys.exit(1)

data = json.loads(result.stdout.strip())
results = data.get("results", [])
print(f"Found {len(results)} results", flush=True)

conn = pymysql.connect(
    host='154.201.68.122', port=3306,
    user='app_user', password='App@Remote2026#User',
    database='springcloud_demo', charset='utf8mb4',
    ssl_disabled=True, connect_timeout=10,
)

today = date(2026, 7, 28)

try:
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM review_daily WHERE review_date=%s", (today,))
        row = cur.fetchone()
        if not row:
            cur.execute(
                "INSERT INTO review_daily (review_date, title, module_count, item_count, status, created_at, updated_at) VALUES (%s,%s,%s,%s,%s,%s,%s)",
                (today, f"{today} Java面试复习重点", 0, 0, 1, datetime.now(), datetime.now())
            )
            daily_id = cur.lastrowid
            print(f"Created review_daily id={daily_id}", flush=True)
        else:
            daily_id = row[0]
            print(f"Using existing review_daily id={daily_id}", flush=True)

        cur.execute("SELECT MAX(sort_order) FROM review_item WHERE daily_id=%s", (daily_id,))
        max_sort = cur.fetchone()[0] or 0

        inserted = 0
        for r in results:
            title = r.get("title", "")[:500]
            content = r.get("content", "")
            source = r.get("source", "")[:200]
            question = title if title else query
            content_key = content[:100]

            cur.execute("SELECT id FROM review_item WHERE daily_id=%s AND answer LIKE %s", (daily_id, content_key[:50] + '%'))
            if cur.fetchone():
                continue

            max_sort += 1
            cur.execute(
                "INSERT INTO review_item (daily_id, module_name, question, answer, sort_order, source, created_at) VALUES (%s,%s,%s,%s,%s,%s,%s)",
                (daily_id, module, question, content, max_sort, source, datetime.now())
            )
            inserted += 1
            print(f"  + {question[:50]}", flush=True)

        cur.execute("SELECT COUNT(DISTINCT module_name) FROM review_item WHERE daily_id=%s", (daily_id,))
        mc = cur.fetchone()[0]
        cur.execute("SELECT COUNT(*) FROM review_item WHERE daily_id=%s", (daily_id,))
        ic = cur.fetchone()[0]
        cur.execute("UPDATE review_daily SET module_count=%s, item_count=%s, updated_at=%s WHERE id=%s", (mc, ic, datetime.now(), daily_id))

        conn.commit()
        print(f"Inserted {inserted} items (total: {ic})", flush=True)
finally:
    conn.close()