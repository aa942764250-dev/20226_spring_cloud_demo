import pymysql
from datetime import date, datetime

conn = pymysql.connect(
    host='154.201.68.122', port=3306,
    user='app_user', password='App@Remote2026#User',
    database='springcloud_demo', charset='utf8mb4',
    ssl_disabled=True, connect_timeout=10,
)

today = date(2026, 7, 28)
today_str = today.strftime('%Y-%m-%d')

try:
    with conn.cursor() as cur:
        cur.execute("SELECT id, title, module_count, item_count FROM review_daily WHERE review_date=%s", (today,))
        daily = cur.fetchone()
        if not daily:
            print(f"No review data for {today_str}")
            exit(1)

        daily_id, title, module_count, item_count = daily

        cur.execute("SELECT id, module_name, question, answer, sort_order, source FROM review_item WHERE daily_id=%s ORDER BY sort_order", (daily_id,))
        items = cur.fetchall()

        cur.execute("SELECT item_id, status FROM review_progress WHERE user_id='default'")
        progress = {row[0]: row[1] for row in cur.fetchall()}

    STATUS_MAP = {0: '[ ]', 1: '[Y]', 2: '[N]'}
    STATUS_LABEL = {0: '未看', 1: '已掌握', 2: '未掌握'}

    lines = []
    lines.append(f"# Java 基础重点复习 — 自测清单")
    lines.append(f"")
    lines.append(f"> 生成日期：{today_str}  |  模块数：{module_count}  |  题目数：{item_count}")
    lines.append(f"> 来源：本地知识库自动检索生成")
    lines.append(f"")
    lines.append(f"---")
    lines.append(f"")

    current_module = None
    q_num = 0

    for item_id, module_name, question, answer, sort_order, source in items:
        if module_name != current_module:
            current_module = module_name
            lines.append(f"## {module_name}")
            lines.append(f"")

        q_num += 1
        p_status = progress.get(item_id, 0)
        check = STATUS_MAP[p_status]

        lines.append(f"**Q{q_num}. {question}？**")
        lines.append(f"")

        if answer:
            clean = answer.strip()
            clean = clean.replace('Page \\d+ of \\d+', '')
            for line in clean.split('\n'):
                line = line.strip()
                if not line:
                    continue
                if line.startswith(('1.', '2.', '3.', '4.', '5.', '6.', '7.', '8.', '9.')):
                    lines.append(f"  {line}")
                elif line.startswith('-'):
                    lines.append(f"  {line}")
                else:
                    lines.append(f"  {line}")
            lines.append(f"")

        if source:
            lines.append(f"> 来源：{source}")
            lines.append(f"")

        lines.append(f"| 状态 | {check} {STATUS_LABEL[p_status]} |")
        lines.append(f"|------|------|")
        lines.append(f"")

    lines.append(f"---")
    lines.append(f"")
    lines.append(f"## 复习统计")
    lines.append(f"")
    mastered = sum(1 for v in progress.values() if v == 1)
    unmastered = sum(1 for v in progress.values() if v == 2)
    unseen = item_count - mastered - unmastered
    lines.append(f"| 指标 | 数值 |")
    lines.append(f"|------|------|")
    lines.append(f"| 总题数 | {item_count} |")
    lines.append(f"| 已掌握 | {mastered} |")
    lines.append(f"| 未掌握 | {unmastered} |")
    lines.append(f"| 未看 | {unseen} |")
    lines.append(f"| 掌握率 | {round(mastered/item_count*100) if item_count else 0}% |")

    md_text = '\n'.join(lines)

    from pathlib import Path
    out_dir = Path(r"D:\Workspace\Project_006_LocalKnowledgeMCP")
    md_path = out_dir / f"Java基础重点复习_自测清单_{today_str}.md"
    md_path.write_text(md_text, encoding='utf-8')
    print(f"Markdown saved: {md_path}")

finally:
    conn.close()