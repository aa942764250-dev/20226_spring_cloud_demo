import pymysql
conn = pymysql.connect(host='154.201.68.122', port=3306, user='app_user', password='App@Remote2026#User', database='springcloud_demo', charset='utf8mb4')
cur = conn.cursor()
cur.execute("SELECT id, member_name, role_label, sort_order, is_active FROM alliance_showcase WHERE section = 'core_member' ORDER BY sort_order")
rows = cur.fetchall()
print(f'核心成员共 {len(rows)} 条记录:')
for r in rows:
    print(f'  id={r[0]} | {r[1]} | {r[2]} | sort={r[3]} | active={r[4]}')
conn.close()