#!/usr/bin/env python3
"""Render LMSPilot 0.24 source UI QA pages with Chromium.

These pages use the shipped globals.css/unified.css and the same class hierarchy
as the real React components changed in 0.24. They are deterministic source UI
screenshots; they do not claim to be backend/database E2E captures.
"""
from __future__ import annotations

import asyncio
import hashlib
import importlib.util
import json
from pathlib import Path
from typing import Callable

from PIL import Image, ImageDraw
from playwright.async_api import async_playwright

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "screenshots" / "0.24.0"
PUBLIC = ROOT / "apps" / "web" / "public" / "ui-qa" / "0.24.0"
TMP = ROOT / ".source-ui-qa-v024"

spec = importlib.util.spec_from_file_location("preview_base", ROOT / "scripts" / "render-ui-previews-v0201.py")
assert spec and spec.loader
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)
icon = base.icon
shell = base.shell

EXTRA = r"""
.source-proof{position:fixed;right:18px;bottom:16px;z-index:400;padding:7px 10px;border:1px solid var(--ui-border);border-radius:10px;background:color-mix(in srgb,var(--ui-surface) 92%,transparent);color:var(--ui-muted);font:750 11px/1.2 ui-monospace,SFMono-Regular,Consolas,monospace;box-shadow:var(--shadow-xs);backdrop-filter:blur(10px)}
.preview-page-heading{display:flex;align-items:flex-start;justify-content:space-between;gap:18px;margin-bottom:18px}.preview-page-heading nav{display:flex;gap:8px;color:var(--ui-muted);font-size:12px}.preview-page-heading h1{margin-top:7px;font-size:30px;letter-spacing:-.04em}.preview-page-heading p{margin-top:5px;color:var(--ui-muted);font-size:14px}.preview-heading-actions{display:flex;gap:9px}
.workspace-panel{padding:0}.workspace-panel>header{display:flex;justify-content:space-between}.workspace-button{display:inline-flex;align-items:center;justify-content:center;gap:7px;min-height:38px;padding:8px 12px;border:1px solid var(--ui-border);border-radius:10px;background:var(--ui-surface);color:var(--ui-text);font-weight:750}.workspace-button.primary{border-color:var(--ui-primary);background:var(--ui-primary);color:var(--ui-on-primary)}.workspace-button.ghost{background:transparent}.workspace-tag{display:inline-flex;align-items:center;min-height:26px;padding:4px 8px;border-radius:999px;background:var(--ui-muted-surface);color:var(--ui-muted);font-size:12px;font-weight:800}.workspace-tag.teal{background:var(--ui-success-soft);color:var(--ui-success)}
.workspace-hero{display:grid;grid-template-columns:52px minmax(0,1fr) auto;gap:15px;align-items:center;margin-bottom:20px;padding:22px;border:1px solid var(--ui-border);border-radius:20px;background:var(--ui-surface)}.workspace-hero-glyph{width:52px;height:52px;display:grid;place-items:center;border-radius:15px;background:var(--ui-primary-soft);color:var(--ui-primary)}.workspace-hero-copy>span{color:var(--ui-primary);font-size:11px;font-weight:850;letter-spacing:.1em}.workspace-hero-copy h1{margin-top:4px;font-size:28px}.workspace-hero-copy p{margin-top:5px;color:var(--ui-muted)}.workspace-hero-stats{display:flex;gap:9px}.workspace-hero-stats>div{min-width:92px;padding:10px 12px;border-radius:12px;background:var(--ui-muted-surface);text-align:center}.workspace-hero-stats strong,.workspace-hero-stats span{display:block}.workspace-hero-stats strong{font-size:20px}.workspace-hero-stats span{margin-top:2px;color:var(--ui-muted);font-size:11px}
.status-badge{display:inline-flex;align-items:center;min-height:28px;padding:4px 9px;border-radius:999px;background:var(--ui-success-soft);color:var(--ui-success);font-size:12px;font-weight:800}.question-number{display:grid;place-items:center}.empty-state{min-height:130px}
.exam-taking-header>div:first-child{display:flex;align-items:center;gap:12px}.exam-session-signals{display:flex;align-items:center;gap:12px}.exam-timer{display:flex;align-items:center;gap:9px}.exam-timer span{display:grid}.exam-timer small{color:var(--ui-muted);font-size:11px}.exam-timer strong{font-size:18px}.exam-progress-row{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:12px;align-items:center}.exam-navigator>div:nth-child(2){display:grid;grid-template-columns:repeat(5,1fr)}.exam-legend{display:grid;gap:7px;margin-top:16px;padding-top:14px;border-top:1px solid var(--ui-border)}.exam-legend span{display:flex;align-items:center;gap:7px;color:var(--ui-muted);font-size:12px}.exam-legend i{width:12px;height:12px;border:1px solid var(--ui-border);border-radius:4px}.exam-legend i.answered{border-color:var(--ui-primary);background:var(--ui-primary)}
"""


def proof(markup: str) -> str:
    return markup.replace("</body>", '<div class="source-proof">SOURCE UI QA · v0.24.0</div></body>')


def org_page() -> str:
    stats = [
        ("violet", "building", "Tổng đơn vị", "28", "Toàn bộ cơ cấu đang quản lý"),
        ("green", "check", "Đang hoạt động", "26", "Đơn vị sẵn sàng sử dụng"),
        ("amber", "users", "Phòng ban", "9", "Các đơn vị nghiệp vụ trực thuộc"),
        ("blue", "menu", "Nhóm / bộ môn", "12", "Cấp tổ chức chuyên môn"),
    ]
    summary = "".join(
        f'<article class="org-summary-card {tone}"><span>{icon(ic,21)}</span><div><small>{label}</small><strong>{value}</strong><p>{hint}</p></div></article>'
        for tone, ic, label, value, hint in stats
    )
    def node(name: str, meta: str, tone: str = "organization", active: bool = False) -> str:
        return f'<button class="org-chart-node {"active" if active else ""}"><span class="org-chart-icon {tone}">{icon("users" if tone=="group" else "building",18)}</span><span class="org-chart-copy"><strong>{name}</strong><small>{meta}</small></span><i class="org-chart-status"></i></button>'
    chart = f'''
    <div class="org-chart-scroll"><div class="org-chart-roots"><div class="org-chart-branch level-0">
      {node("Trường Learnova", "Tổ chức · SCHOOL", active=True)}
      <div class="org-chart-children">
        <div class="org-chart-branch level-1">{node("Khoa Công nghệ thông tin", "Phòng ban · CNTT", "department")}<div class="org-chart-children"><div class="org-chart-branch level-2">{node("Bộ môn Khoa học máy tính", "Nhóm · KHMT", "group")}</div><div class="org-chart-branch level-2">{node("Bộ môn Hệ thống thông tin", "Nhóm · HTTT", "group")}</div></div></div>
        <div class="org-chart-branch level-1">{node("Khoa Kinh tế", "Phòng ban · KTE", "department")}</div>
        <div class="org-chart-branch level-1">{node("Phòng Đào tạo", "Phòng ban · PDT", "department")}</div>
        <div class="org-chart-branch level-1">{node("Phòng Khảo thí & ĐBCL", "Phòng ban · KTDB", "department")}</div>
      </div>
    </div></div></div>'''
    tree_rows = "".join(
        f'<div class="org-node-wrap"><button class="org-node {"active" if i==0 else ""}"><span class="org-type">{icon("building",17)}</span><strong class="org-node-name">{name}</strong><span class="org-node-side"><span class="org-node-kind">{kind}</span><span class="org-status-dot"></span></span></button></div>'
        for i, (name,kind) in enumerate([
            ("Trường Learnova","Tổ chức"),("Khoa Công nghệ thông tin","Phòng ban"),("Bộ môn Khoa học máy tính","Nhóm"),("Khoa Kinh tế","Phòng ban"),("Phòng Đào tạo","Phòng ban")
        ])
    )
    members = "".join(
        f'<div><span class="mini-avatar">{initial}</span><div><strong>{name}</strong><small>{email} · {role}</small></div><span class="workspace-tag teal">Đang hoạt động</span></div>'
        for initial,name,email,role in [("NA","Nguyễn Văn An","an.nguyen@learnova.vn","Trưởng đơn vị"),("MD","Trần Minh Đức","duc.tran@learnova.vn","Giảng viên"),("LH","Lê Thu Hương","huong.le@learnova.vn","Điều phối viên")]
    )
    content = f'''
    <header class="workspace-hero"><span class="workspace-hero-glyph">{icon('building',29)}</span><div class="workspace-hero-copy"><span>TỔ CHỨC</span><h1>Cơ cấu và thành viên</h1><p>Quản lý cây tổ chức, đơn vị trực thuộc và thành viên trong một không gian làm việc rõ ràng.</p></div><div class="workspace-hero-stats"><div><strong>28</strong><span>Đơn vị</span></div><div><strong>542</strong><span>Thành viên</span></div></div></header>
    <section class="org-summary-grid">{summary}</section>
    <article class="workspace-panel org-chart-panel"><header><div><h2>Sơ đồ cơ cấu tổ chức</h2><p>Quan sát quan hệ giữa các cấp; chọn một đơn vị để xem thành viên và cấu hình chi tiết.</p></div><span class="org-chart-legend"><i class="active"></i>Đang hoạt động<i class="inactive"></i>Ngừng hoạt động</span></header><div class="workspace-panel-body">{chart}</div></article>
    <div class="org-layout"><article class="workspace-panel org-tree-panel"><header><div><h2>Danh sách đơn vị</h2><p>28 đơn vị · chọn để xem chi tiết</p></div><button class="workspace-button ghost">{icon('plus',15)} Thêm</button></header><div class="workspace-panel-body"><div class="org-tree">{tree_rows}</div></div></article><div class="org-main-column"><article class="workspace-panel"><header><div><h2>Trường Learnova</h2><p>Tổ chức · SCHOOL</p></div><span class="workspace-tag teal">3 người</span></header><div class="workspace-panel-body"><div class="member-list">{members}</div></div></article></div></div>'''
    return proof(shell("ADMIN", "Tổ chức", content))


def exam_list_page() -> str:
    stats = [("violet","exam","Tổng bài thi","24","Tất cả kỳ thi"),("green","check","Đang mở","6","Đang diễn ra"),("amber","file","Bản nháp","5","Chưa xuất bản"),("blue","report","Đã hoàn thành","13","Đã kết thúc")]
    stat_html = "".join(f'<article class="exam-stat-card {tone}"><span>{icon(ic,22)}</span><div><small>{label}</small><strong>{value}</strong><p>{hint}</p></div></article>' for tone,ic,label,value,hint in stats)
    exams = [
        ("Kỳ thi năng lực số 2026","EX2026-001","ACTIVE","120 phút","60","60%","356"),
        ("Thi cuối kỳ An toàn thông tin","SEC-FINAL-2025","ACTIVE","90 phút","45","50%","241"),
        ("Đánh giá giữa kỳ Lập trình Python","PY-MID-2025","DRAFT","60 phút","10","60%","—"),
        ("Thi chứng chỉ Quản trị hệ thống","SYS-ADMIN-2025","INACTIVE","90 phút","50","65%","512"),
    ]
    rows=[]
    for title,code,status,duration,count,score,attempts in exams:
        status_label={"ACTIVE":"Đang mở","DRAFT":"Bản nháp","INACTIVE":"Đã hoàn thành"}[status]
        rows.append(f'''<a class="exam-list-item"><span class="exam-list-icon status-{status.lower()}">{icon('exam',22)}</span><div class="exam-list-primary"><div class="exam-title-line"><h2>{title}</h2><span class="exam-code">{code}</span><span class="status-badge">{status_label}</span></div><p>Kỳ thi độc lập <i>•</i> Trắc nghiệm</p><div class="exam-date-line">{icon('clock',15)}<span>Mở: 20/05/2026 08:00</span><i>•</i><span>Đóng: 27/05/2026 23:59</span></div></div><dl class="exam-list-metrics"><div><dt>Thời lượng</dt><dd>{duration}</dd></div><div><dt>Số câu</dt><dd>{count}</dd></div><div><dt>Điểm đạt</dt><dd>{score}</dd></div><div><dt>Thí sinh</dt><dd>{attempts}</dd></div></dl><span class="exam-row-action">{icon('arrow',18)}</span></a>''')
    content=f'''<header class="exam-page-heading"><div><nav><span>Trang chủ</span><i>•</i><strong>Bài thi</strong></nav><h1>Bài thi</h1><p>Quản lý, tạo và theo dõi các kỳ thi độc lập trong hệ thống.</p></div><div class="exam-heading-actions"><button class="button secondary">{icon('file',17)}Tạo từ PDF/DOCX</button><button class="button primary">{icon('plus',17)}Tạo bài thi</button></div></header><section class="exam-stat-grid">{stat_html}</section><div class="exam-overview-layout"><section class="exam-main-column"><div class="exam-filter-panel"><label class="exam-search-field">{icon('search',18)}<input placeholder="Tìm theo tên hoặc mã bài thi…"></label><div class="exam-filter-row"><label><span>Trạng thái</span><select><option>Tất cả</option></select></label><label><span>Loại bài thi</span><select><option>Tất cả</option></select></label><label><span>Sắp xếp</span><select><option>Mới nhất</option></select></label><button class="button secondary compact">{icon('filter',16)}Làm mới</button></div></div><div class="exam-list">{''.join(rows)}</div></section><aside class="exam-side-column"><section class="exam-side-card"><header><h2>Tổng quan nhanh</h2></header><dl class="exam-quick-stats"><div><dt>Kỳ thi đang mở</dt><dd>6</dd></div><div><dt>Tổng câu hỏi</dt><dd>428</dd></div><div><dt>Cần xuất bản</dt><dd>5</dd></div><div><dt>Tỷ lệ hoạt động</dt><dd>25%</dd></div></dl></section><section class="exam-side-card"><header><h2>Thao tác nhanh</h2></header><div class="exam-quick-actions"><button>{icon('plus',17)}Tạo bài thi mới</button><button>{icon('file',17)}Sinh đề từ tài liệu</button><button>{icon('file',17)}Thêm câu hỏi</button></div></section></aside></div>'''
    return proof(shell("INSTRUCTOR","Bài thi",content))


def exam_editor_page() -> str:
    bank=[]
    for i,(title,kind,pts) in enumerate([("Hàm print() trong Python được dùng để…","SINGLE CHOICE",1),("Python là ngôn ngữ lập trình thông dịch.","TRUE FALSE",1),("Trong Python, để khai báo biến, ta sử dụng…","SHORT TEXT",1),("Ghép các kiểu dữ liệu ở cột A với mô tả ở cột B.","MULTIPLE CHOICE",2),("Kết quả của biểu thức 3 ** 2 là?","SINGLE CHOICE",1)]):
        bank.append(f'<article class="exam-editor-bank-item"><span class="exam-editor-type-icon">{icon("file",17)}</span><div><div class="exam-editor-item-title"><strong>{title}</strong><span>{kind}</span></div><small>{pts} điểm · 4 phương án</small></div><button class="icon-button">{icon("plus",17)}</button></article>')
    selected=[]
    for i,title in enumerate(["Hàm print() trong Python được dùng để…","Python là ngôn ngữ lập trình thông dịch.","Trong Python, để khai báo biến, ta sử dụng…","Ghép các kiểu dữ liệu ở cột A với mô tả ở cột B.","Kết quả của biểu thức 3 ** 2 là?","List trong Python là một kiểu dữ liệu có thứ tự."]):
        selected.append(f'<article class="exam-editor-selected-item"><span class="exam-editor-drag">⋮⋮</span><span class="question-number">{i+1}</span><span class="exam-editor-type-icon">{icon("file",17)}</span><div><div class="exam-editor-item-title"><strong>{title}</strong><span>TRẮC NGHIỆM</span></div><small>4 phương án</small></div><strong class="exam-editor-points">{2 if i==3 else 1} điểm</strong><button class="icon-button">{icon("trash",16)}</button></article>')
    settings="".join(f'<div><dt>{a}</dt><dd>{b}</dd></div>' for a,b in [("Khóa học","Lập trình Python cơ bản"),("Thời lượng","60 phút"),("Số câu hỏi","8 câu"),("Tổng điểm","10 điểm"),("Điểm đạt","60%"),("Số lượt làm","2"),("Thời gian mở","20/05/2026 08:00"),("Thời gian đóng","27/05/2026 23:59")])
    content=f'''<div class="preview-page-heading"><div><nav><span>Trang chủ</span><span>›</span><span>Bài thi</span><span>›</span><strong>Chỉnh sửa</strong></nav><h1>Chỉnh sửa bài thi</h1><p>Chỉnh sửa nội dung và cài đặt cho bài thi.</p></div><div class="preview-heading-actions"><button class="button secondary">{icon('exam',17)}Xem trước đề thi</button></div></div><section class="exam-editor-source-layout"><article class="workspace-panel exam-editor-bank-panel"><header><div><h2>Ngân hàng câu hỏi</h2><p>1.248 câu khả dụng · câu đã chọn được ẩn.</p></div><span class="workspace-tag">1.240 còn lại</span></header><div class="workspace-panel-body"><label class="exam-editor-search">{icon('search',17)}<input placeholder="Tìm nội dung, loại hoặc mã câu hỏi…"></label><div class="exam-editor-bank-list">{''.join(bank)}</div></div></article><article class="workspace-panel exam-editor-content-panel"><header><div><h2>Nội dung đề thi</h2><p>Phiên bản 3 · 8 câu · 10 điểm</p></div><span class="status-badge">Bản nháp</span></header><div class="workspace-panel-body"><div class="exam-editor-selected-list">{''.join(selected)}</div><div class="exam-editor-drop-zone">{icon('menu',19)}<span>Kéo thả để sắp xếp thứ tự; từng thẻ câu hỏi có khoảng cách rõ ràng.</span></div></div></article><aside class="workspace-panel exam-editor-settings-panel"><header><div><h2>Cài đặt bài thi</h2><p>Cấu hình thời gian, lần làm và điều kiện đạt.</p></div></header><div class="workspace-panel-body"><dl class="exam-editor-settings-list">{settings}</dl><button class="button primary full">{icon('edit',17)}Lưu / sửa cấu hình</button><div class="form-alert info">{icon('lock',17)}Khi bài thi đã xuất bản hoặc có lượt làm, cấu trúc câu hỏi được khóa.</div></div></aside></section>'''
    return proof(shell("INSTRUCTOR","Bài thi",content))


def exam_taking_page() -> str:
    nav="".join(f'<button class="{"answered" if i in {1,2,5,8,11} else ""} {"active" if i==12 else ""}">{i}</button>' for i in range(1,31))
    options=[("A","size()",False),("B","len()",True),("C","count()",False),("D","length()",False)]
    answer="".join(f'<label class="answer-choice {"selected" if selected else ""}"><input type="radio" {"checked" if selected else ""}><span class="answer-letter">{letter}</span><span class="answer-text">{text}</span></label>' for letter,text,selected in options)
    content=f'''<div class="exam-taking"><header class="exam-taking-header"><div><button class="icon-button">{icon('back',18)}</button><div><small>BÀI KIỂM TRA ĐANG DIỄN RA</small><h1>Thi cuối kỳ: Lập trình Python</h1></div></div><div class="exam-session-signals"><div class="exam-timer">{icon('clock',18)}<span><small>Thời gian làm bài còn lại</small><strong>00:59:32</strong></span></div></div></header><div class="exam-progress-row"><div class="progress-track"><span style="width:27%"></span></div><span>12/45 câu đã trả lời</span></div><div class="exam-taking-layout"><aside class="exam-navigator"><h2>Danh sách câu hỏi</h2><div>{nav}</div><div class="exam-legend"><span><i class="answered"></i>Đã trả lời</span><span><i></i>Chưa trả lời</span></div></aside><main class="exam-question-panel"><article class="question-editor"><div class="question-editor-head"><span>Câu 12 / 45</span><strong>3 điểm</strong></div><h2>Hàm nào trong Python được sử dụng để trả về số lượng phần tử trong một danh sách?</h2><p class="muted">Chọn một đáp án đúng.</p><div class="answer-options">{answer}</div></article><footer class="exam-actions"><button class="button secondary">{icon('back',17)}Câu trước</button><div><button class="button secondary">{icon('check',17)}Lưu bài</button><button class="button primary">Câu tiếp theo{icon('arrow',17)}</button></div></footer></main></div></div>'''
    return proof(shell("STUDENT","Bài thi",content))


SCREENS: list[tuple[str, Callable[[], str], tuple[int,int]]] = [
    ("01-source-organization.png", org_page, (1600,1000)),
    ("02-source-exam-list.png", exam_list_page, (1600,1000)),
    ("03-source-exam-editor.png", exam_editor_page, (1600,1000)),
    ("04-source-exam-taking.png", exam_taking_page, (1600,1000)),
]


def contact_sheet(paths: list[Path], target: Path) -> None:
    thumbs=[]
    for p in paths:
        img=Image.open(p).convert("RGB")
        img.thumbnail((760,475))
        thumbs.append((p.name,img.copy()))
    canvas=Image.new("RGB",(1600,1050),"white")
    draw=ImageDraw.Draw(canvas)
    draw.text((28,18),"LMSPilot 0.24.0 · source UI QA screenshots",fill="black")
    for idx,(name,img) in enumerate(thumbs):
        x=28+(idx%2)*786; y=55+(idx//2)*495
        canvas.paste(img,(x,y+24)); draw.text((x,y),name,fill="black")
    canvas.save(target,quality=94)


async def main() -> None:
    OUT.mkdir(parents=True,exist_ok=True); PUBLIC.mkdir(parents=True,exist_ok=True); TMP.mkdir(parents=True,exist_ok=True)
    css=(ROOT/"apps/web/app/globals.css").read_text(encoding="utf-8")+"\n"+(ROOT/"apps/web/app/unified.css").read_text(encoding="utf-8")
    rendered=[]; manifest=[]
    async with async_playwright() as pw:
        browser=await pw.chromium.launch(executable_path="/usr/bin/chromium",headless=True,args=["--no-sandbox","--disable-dev-shm-usage"])
        for filename,factory,viewport in SCREENS:
            html=factory().replace("{CSS}",css).replace("{PREVIEW_CSS}",EXTRA)
            html_path=PUBLIC/(Path(filename).stem+".html")
            html_path.write_text(html,encoding="utf-8")
            page=await browser.new_page(viewport={"width":viewport[0],"height":viewport[1]},device_scale_factor=1)
            await page.set_content(html, wait_until="load")
            await page.screenshot(path=str(OUT/filename),full_page=True)
            await page.close()
            digest=hashlib.sha256((OUT/filename).read_bytes()).hexdigest()
            rendered.append(OUT/filename)
            manifest.append({"file":filename,"sha256":digest,"html":str(html_path.relative_to(ROOT))})
        await browser.close()
    contact_sheet(rendered,OUT/"00-source-ui-contact-sheet.png")
    (OUT/"SOURCE_UI_QA_MANIFEST.json").write_text(json.dumps({"version":"0.24.0","screens":manifest,"sourceFiles":["apps/web/components/WorkspaceControlCenter.tsx","apps/web/components/ExamsPage.tsx","apps/web/components/ExamDetail.tsx","apps/web/app/unified.css"]},ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(f"Rendered {len(rendered)} source UI QA screenshots to {OUT}")

if __name__=="__main__":
    asyncio.run(main())
