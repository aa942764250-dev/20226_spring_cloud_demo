import pymysql, re
from datetime import date
from pathlib import Path

conn = pymysql.connect(
    host='154.201.68.122', port=3306,
    user='app_user', password='App@Remote2026#User',
    database='springcloud_demo', charset='utf8mb4',
    ssl_disabled=True, connect_timeout=10,
)

today = date(2026, 7, 28)

def extract_qa_pairs(content, module_name):
    pairs = []
    patterns = [
        r'(\d+[\.\、]\s*[^？\?]+[？\?])',
        r'((?:什么是|为什么|怎么|如何|哪些|区别|比较|说一下|讲一下|谈谈|简述|阐述)[^？\?\n]{5,80}[？\?]?)',
        r'((?:What|Why|How|When)[^？\?\n]{5,80}[？\?]?)',
    ]
    for pat in patterns:
        matches = re.findall(pat, content)
        for m in matches:
            q = m.strip().lstrip('0123456789.、 ')
            if len(q) < 6 or len(q) > 120:
                continue
            if q.endswith('？') or q.endswith('?'):
                pairs.append(q)
    pairs = list(dict.fromkeys(pairs))
    return pairs[:5]

def clean_answer(text, max_len=600):
    if not text:
        return ''
    s = re.sub(r'Page\s+\d+\s+of\s+\d+', '', text, flags=re.IGNORECASE)
    s = re.sub(r'\d{2}/\d{2}/\d{4}', '', s)
    s = re.sub(r'\.{4,}', '', s)
    s = re.sub(r'\n{3,}', '\n\n', s)
    s = s.strip()
    if len(s) > max_len:
        s = s[:max_len] + '...'
    return s

try:
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM review_daily WHERE review_date=%s", (today,))
        row = cur.fetchone()
        if not row:
            print("No data"); exit(1)
        daily_id = row[0]

        cur.execute("SELECT module_name, question, answer, sort_order, source FROM review_item WHERE daily_id=%s ORDER BY sort_order", (daily_id,))
        items = cur.fetchall()

        cur.execute("SELECT item_id, status FROM review_progress WHERE user_id='default'")
        progress = {r[0]: r[1] for r in cur.fetchall()}

    MODULE_ICONS = {
        'Java基础': '[Java]', '集合框架': '[集合]', '多线程与锁': '[并发]', '线程池': '[池]',
        'JVM': '[JVM]', 'Spring': '[Sp]', 'MySQL': '[DB]', 'Redis': '[Rd]', '设计模式': '[模式]',
    }

    STATUS_CHECK = {0: '[ ]', 1: '[Y]', 2: '[N]'}
    STATUS_LABEL = {0: '未看', 1: '已掌握', 2: '未掌握'}

    lines = []
    lines.append("# Java 面试重点复习 — 今日自测清单")
    lines.append("")
    lines.append(f"> 日期：{today.strftime('%Y-%m-%d')}  |  {len(set(i[0] for i in items))} 个模块  |  {len(items)} 道题")
    lines.append("> 基于本地知识库自动检索生成，覆盖 Java 面试核心模块")
    lines.append("")
    lines.append("## 使用说明")
    lines.append("")
    lines.append("- ☑ = 已掌握  |  ☒ = 未掌握  |  ☐ = 未看")
    lines.append("- 点击题目展开答案，自测后标记掌握状态")
    lines.append("- 建议先自测再对答案，标记后系统会记录进度")
    lines.append("")
    lines.append("---")
    lines.append("")

    current_module = None
    q_global = 0
    mastered = 0

    for module_name, question, answer, sort_order, source in items:
        if module_name != current_module:
            current_module = module_name
            icon = MODULE_ICONS.get(module_name, '📘')
            module_items = [i for i in items if i[0] == module_name]
            m_mastered = sum(1 for i in module_items if progress.get(i[3], 0) == 1)
            lines.append(f"## {icon} {module_name}")
            lines.append(f"> {len(module_items)} 道题 · 已掌握 {m_mastered}/{len(module_items)}")
            lines.append("")

        q_global += 1
        p_status = progress.get(sort_order, 0)
        if p_status == 1:
            mastered += 1
        check = STATUS_CHECK[p_status]

        real_questions = extract_qa_pairs(answer, module_name) if answer else []

        lines.append(f"### {check} Q{q_global}. {question}")
        lines.append("")

        if real_questions:
            lines.append("**核心考点：**")
            lines.append("")
            for rq in real_questions:
                lines.append(f"- {rq}")
            lines.append("")

        if answer:
            cleaned = clean_answer(answer, 500)
            lines.append("<details>")
            lines.append("<summary>查看参考答案</summary>")
            lines.append("")
            for para in cleaned.split('\n'):
                para = para.strip()
                if para:
                    lines.append(f"{para}")
            lines.append("")
            lines.append("</details>")
            lines.append("")

        if source:
            lines.append(f"*来源：{source}*")
            lines.append("")

        lines.append("---")
        lines.append("")

    total = len(items)
    pct = round(mastered / total * 100) if total else 0
    lines.append("## 复习统计")
    lines.append("")
    lines.append("| 指标 | 数值 |")
    lines.append("|:-----|:-----|")
    lines.append(f"| 总题数 | {total} |")
    lines.append(f"| 已掌握 | {mastered} |")
    lines.append(f"| 未掌握 | {sum(1 for v in progress.values() if v == 2)} |")
    lines.append(f"| 未看 | {total - mastered - sum(1 for v in progress.values() if v == 2)} |")
    lines.append(f"| 掌握率 | {pct}% |")
    lines.append("")
    if pct < 60:
        lines.append("> [!] 掌握率低于 60%，建议重点复习标记为「未掌握」的题目")
    elif pct < 80:
        lines.append("> 掌握率过半，继续攻克「未掌握」的题目")
    else:
        lines.append("> 掌握率良好，保持复习节奏！")

    md_text = '\n'.join(lines)
    out_dir = Path(r"D:\Workspace\Project_006_LocalKnowledgeMCP")
    md_path = out_dir / f"Java基础重点复习_自测清单_{today.strftime('%Y-%m-%d')}.md"
    md_path.write_text(md_text, encoding='utf-8')
    print(f"Markdown saved: {md_path} ({q_global} questions)")

finally:
    conn.close()