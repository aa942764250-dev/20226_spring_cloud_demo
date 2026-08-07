"""将自测清单 Markdown 转为 PDF（fpdf2 + 中文字体）"""
import re
import sys
from pathlib import Path
from fpdf import FPDF

MD_PATH = Path(__file__).parent / "Java基础重点复习_自测清单.md"
OUT_PATH = Path(__file__).parent / "data" / "documents" / "Java基础重点复习_自测清单.pdf"

FONT_DIR = Path(r"C:\Windows\Fonts")
FONT_REGULAR = FONT_DIR / "msyh.ttc"
FONT_BOLD = FONT_DIR / "msyhbd.ttc"


class ChinesePDF(FPDF):
    def header(self):
        if self.page_no() > 1:
            self.set_font("zh", "B", 9)
            self.set_text_color(128, 128, 128)
            self.cell(0, 8, "Java 后端基础重点复习 — 自测清单", align="C", new_x="LMARGIN", new_y="NEXT")
            self.line(10, self.get_y(), 200, self.get_y())
            self.ln(3)

    def footer(self):
        self.set_y(-15)
        self.set_font("zh", "", 8)
        self.set_text_color(128, 128, 128)
        self.cell(0, 10, f"第 {self.page_no()} 页", align="C")


def parse_md(text):
    blocks = []
    for line in text.split("\n"):
        stripped = line.strip()
        if not stripped:
            blocks.append(("blank", ""))
            continue
        if stripped.startswith("```"):
            blocks.append(("code_fence", stripped[3:]))
            continue
        if stripped.startswith("---"):
            blocks.append(("hr", ""))
            continue
        if stripped.startswith("## "):
            blocks.append(("h2", stripped[3:]))
            continue
        if stripped.startswith("### "):
            blocks.append(("h3", stripped[4:]))
            continue
        if stripped.startswith("**Q") and stripped.endswith("?**"):
            blocks.append(("question", stripped))
            continue
        if stripped.startswith("| "):
            blocks.append(("table", stripped))
            continue
        if stripped.startswith("- ") or stripped.startswith("> "):
            blocks.append(("bullet", stripped))
            continue
        if re.match(r"^\d+\.", stripped):
            blocks.append(("ordered", stripped))
            continue
        blocks.append(("text", stripped))
    return blocks


def clean_md(text):
    text = re.sub(r"\*\*(.+?)\*\*", r"\1", text)
    text = re.sub(r"`(.+?)`", r"\1", text)
    text = text.replace("\u2705", "[Y]")
    text = text.replace("\u274c", "[N]")
    text = text.replace("\u2713", "[Y]")
    text = text.replace("\u2717", "[N]")
    text = text.replace("\u2192", "->")
    text = text.replace("\u2190", "<-")
    text = text.replace("\u251c", "|")
    text = text.replace("\u2514", "|")
    text = text.replace("\u2502", "|")
    text = text.replace("\u2500", "-")
    text = text.replace("\u251c", "|")
    text = text.replace("\u2514", "|")
    text = text.replace("\u2502", "|")
    text = text.replace("\u2500", "-")
    text = text.replace("\u2265", ">=")
    text = text.replace("\u2264", "<=")
    return text


def render_table_row(pdf, cells, widths, bold=False):
    style = "B" if bold else ""
    h = 7
    x_start = pdf.get_x()
    y_start = pdf.get_y()
    max_h = h
    for i, cell in enumerate(cells):
        pdf.set_xy(x_start + sum(widths[:i]), y_start)
        pdf.set_font("zh", style, 8.5)
        pdf.multi_cell(widths[i], h, clean_md(cell), border=1, new_x="RIGHT", new_y="TOP")
        cell_h = pdf.get_y() - y_start
        if cell_h > max_h:
            max_h = cell_h
    pdf.set_y(y_start + max_h)


def md_to_pdf(md_text, output_path):
    pdf = ChinesePDF()
    pdf.add_font("zh", "", str(FONT_REGULAR))
    pdf.add_font("zh", "B", str(FONT_BOLD))
    pdf.set_auto_page_break(auto=True, margin=20)
    pdf.add_page()

    blocks = parse_md(md_text)
    in_code = False
    code_lines = []
    table_rows = []

    for btype, content in blocks:
        if btype == "code_fence":
            if in_code:
                in_code = False
                pdf.set_font("zh", "", 7.5)
                pdf.set_fill_color(245, 245, 245)
                for cl in code_lines:
                    pdf.cell(0, 5, cl, new_x="LMARGIN", new_y="NEXT", fill=True)
                pdf.ln(2)
                code_lines = []
            else:
                in_code = True
                code_lines = []
            continue

        if in_code:
            code_lines.append(content)
            continue

        if table_rows and btype != "table":
            widths = [190 / max(len(table_rows[0]), 1)] * len(table_rows[0])
            if len(table_rows[0]) <= 3:
                widths = [50, 70, 70] if len(table_rows[0]) == 3 else [60, 130]
            elif len(table_rows[0]) == 4:
                widths = [35, 55, 55, 45]
            elif len(table_rows[0]) == 5:
                widths = [28, 40, 40, 40, 42]
            elif len(table_rows[0]) >= 6:
                widths = [190 / len(table_rows[0])] * len(table_rows[0])
            for ri, row in enumerate(table_rows):
                render_table_row(pdf, row, widths, bold=(ri == 0))
            pdf.ln(3)
            table_rows = []

        if btype == "blank":
            pdf.ln(2)
            continue

        if btype == "hr":
            pdf.ln(2)
            pdf.set_draw_color(200, 200, 200)
            pdf.line(10, pdf.get_y(), 200, pdf.get_y())
            pdf.ln(3)
            continue

        if btype == "h2":
            pdf.ln(4)
            pdf.set_font("zh", "B", 14)
            pdf.set_text_color(0, 51, 102)
            pdf.cell(0, 10, clean_md(content), new_x="LMARGIN", new_y="NEXT")
            pdf.set_draw_color(0, 102, 204)
            pdf.line(10, pdf.get_y(), 120, pdf.get_y())
            pdf.ln(3)
            pdf.set_text_color(0, 0, 0)
            continue

        if btype == "h3":
            pdf.ln(2)
            pdf.set_font("zh", "B", 11)
            pdf.set_text_color(51, 51, 51)
            pdf.cell(0, 8, clean_md(content), new_x="LMARGIN", new_y="NEXT")
            pdf.ln(1)
            pdf.set_text_color(0, 0, 0)
            continue

        if btype == "question":
            pdf.ln(2)
            pdf.set_font("zh", "B", 10)
            pdf.set_text_color(153, 51, 0)
            pdf.multi_cell(0, 7, clean_md(content), new_x="LMARGIN", new_y="NEXT")
            pdf.ln(1)
            pdf.set_text_color(0, 0, 0)
            continue

        if btype == "table":
            cells = [c.strip() for c in content.split("|")[1:-1]]
            if all(set(c) <= {"-", ":", " "} for c in cells):
                continue
            table_rows.append(cells)
            continue

        if btype == "bullet":
            indent = 5 if content.startswith("> ") else 0
            pdf.set_x(12 + indent)
            pdf.set_font("zh", "", 9)
            bullet_char = "  " if content.startswith("> ") else chr(8226) + " "
            text = content[2:] if content.startswith("- ") or content.startswith("> ") else content
            pdf.multi_cell(0, 6, bullet_char + clean_md(text), new_x="LMARGIN", new_y="NEXT")
            continue

        if btype == "ordered":
            pdf.set_x(12)
            pdf.set_font("zh", "", 9)
            pdf.multi_cell(0, 6, clean_md(content), new_x="LMARGIN", new_y="NEXT")
            continue

        pdf.set_font("zh", "", 9)
        pdf.multi_cell(0, 6, clean_md(content), new_x="LMARGIN", new_y="NEXT")

    if table_rows:
        widths = [190 / max(len(table_rows[0]), 1)] * len(table_rows[0])
        for ri, row in enumerate(table_rows):
            render_table_row(pdf, row, widths, bold=(ri == 0))

    pdf.output(str(output_path))
    print(f"PDF saved: {output_path} ({pdf.page_no()} pages)")


if __name__ == "__main__":
    md_text = MD_PATH.read_text(encoding="utf-8")
    md_to_pdf(md_text, OUT_PATH)