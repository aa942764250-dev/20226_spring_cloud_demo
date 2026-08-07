import subprocess, json, pymysql, time
from datetime import date, datetime

KB_SCRIPT = r"D:\Workspace\Project_006_LocalKnowledgeMCP\kb.py"
PYTHON = r"C:\Users\黄\AppData\Local\Programs\Python\Python312\python.exe"

MODULE_QUERIES = [
    ("Java基础", ["Java基本类型 String final"]),
    ("集合框架", ["HashMap ArrayList ConcurrentHashMap"]),
    ("多线程与锁", ["synchronized ReentrantLock volatile"]),
    ("线程池", ["ThreadPoolExecutor 拒绝策略 工作原理"]),
    ("JVM", ["JVM内存模型 GC算法 类加载"]),
    ("Spring", ["Spring IOC AOP Bean生命周期"]),
    ("MySQL", ["MySQL索引 事务隔离 InnoDB"]),
    ("Redis", ["Redis数据类型 持久化 集群"]),
    ("设计模式", ["单例模式 工厂模式 策略模式"]),
]

def search_kb(query, top_k=2):
    env = {"HF_HUB_OFFLINE": "1"}
    import os
    for key in ["ALL_PROXY", "all_proxy"]:
        os.environ.pop(key, None)
    try:
        result = subprocess.run(
            [PYTHON, KB_SCRIPT, "search", query, "--top-k", str(top_k)],
            capture_output=True, encoding='utf-8', errors='replace', timeout=60,
            cwd=r"D:\Workspace\Project_006_LocalKnowledgeMCP",
            env={**os.environ, "HF_HUB_OFFLINE": "1"}
        )
        if result.returncode != 0:
            return []
        data = json.loads(result.stdout.strip())
        return data.get("results", [])
    except Exception as e:
        print(f"  search error: {e}")
        return []

conn = pymysql.connect(
    host='154.201.68.122', port=3306,
    user='app_user', password='App@Remote2026#User',
    database='springcloud_demo', charset='utf8mb4',
    ssl_disabled=True,
)

today = date.today()
all_items = []
seen = set()
sort_order = 0

print(f"Generating review for {today}...")
start = time.time()

for module_name, queries in MODULE_QUERIES:
    print(f"\n[{module_name}]")
    for q in queries:
        print(f"  searching: {q}...")
        results = search_kb(q, top_k=3)
        for r in results:
            title = r.get("title", "")
            content = r.get("content", "")
            source = r.get("source", "")
            question = title if title else q
            if question in seen:
                continue
            seen.add(question)
            all_items.append({
                "module_name": module_name,
                "question": question[:500],
                "answer": content,
                "source": source[:200],
                "sort_order": sort_order,
            })
            sort_order += 1
            print(f"    + {question[:50]}")

elapsed = time.time() - start
print(f"\nTotal: {len(all_items)} items in {elapsed:.1f}s")

if not all_items:
    print("No items found, aborting")
    conn.close()
    exit(1)

module_count = len(set(item["module_name"] for item in all_items))

try:
    with conn.cursor() as cur:
        cur.execute("DELETE FROM review_progress")
        cur.execute("DELETE FROM review_item")
        cur.execute("DELETE FROM review_daily WHERE review_date = %s", (today,))

        cur.execute(
            "INSERT INTO review_daily (review_date, title, module_count, item_count, status, created_at, updated_at) VALUES (%s,%s,%s,%s,%s,%s,%s)",
            (today, f"{today} Java面试复习重点", module_count, len(all_items), 1, datetime.now(), datetime.now())
        )
        daily_id = cur.lastrowid
        print(f"review_daily id={daily_id}")

        for item in all_items:
            cur.execute(
                "INSERT INTO review_item (daily_id, module_name, question, answer, sort_order, source, created_at) VALUES (%s,%s,%s,%s,%s,%s,%s)",
                (daily_id, item["module_name"], item["question"], item["answer"], item["sort_order"], item["source"], datetime.now())
            )

        conn.commit()
        print(f"\nInserted: 1 daily + {len(all_items)} items")

        cur.execute("SELECT module_name, COUNT(*) FROM review_item WHERE daily_id=%s GROUP BY module_name ORDER BY COUNT(*) DESC", (daily_id,))
        print("\nPer module:")
        for r in cur.fetchall():
            print(f"  {r[0]}: {r[1]}")
finally:
    conn.close()