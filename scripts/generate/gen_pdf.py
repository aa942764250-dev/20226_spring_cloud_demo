import sys
sys.path.insert(0, r"D:\Workspace\Project_006_LocalKnowledgeMCP")
from md2pdf import md_to_pdf
from pathlib import Path

today = "2026-07-28"
md_path = Path(rf"D:\Workspace\Project_006_LocalKnowledgeMCP\Java基础重点复习_自测清单_{today}.md")
out_path = Path(rf"D:\Workspace\Project_006_LocalKnowledgeMCP\data\documents\Java基础重点复习_自测清单_{today}.pdf")

md_text = md_path.read_text(encoding='utf-8')
md_to_pdf(md_text, out_path)