#!/usr/bin/env python3
"""Render deterministic LMSPilot 0.22.0 visual QA previews.

The pages use the real shipped CSS plus representative HTML. They verify visual
contracts and do not replace browser E2E against running backend services.
"""
from __future__ import annotations

import asyncio
import importlib.util
from pathlib import Path
from typing import Callable

from playwright.async_api import async_playwright

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "screenshots" / "0.22.0"
TMP = ROOT / ".preview-render-v022"

spec = importlib.util.spec_from_file_location("preview_base", ROOT / "scripts" / "render-ui-previews-v0201.py")
assert spec and spec.loader
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)

icon = base.icon
page_head = base.page_head
metric = base.metric


def shell(role: str, active: str, content: str, *, dark: bool = False) -> str:
    return base.shell(role, active, content, dark=dark).replace("LMSPilot", "LMSPilot").replace("@lmspilot.local", "@lmspilot.local")


def login_page() -> str:
    return base.login_page().replace("LMSPilot", "LMSPilot").replace("@lmspilot.local", "@lmspilot.local")


def dashboard(role: str, dark: bool = False) -> str:
    return base.dashboard(role, dark).replace("LMSPilot", "LMSPilot").replace("@lmspilot.local", "@lmspilot.local")


def wrap_base(factory: Callable[[], str]) -> Callable[[], str]:
    return lambda: factory().replace("LMSPilot", "LMSPilot").replace("@lmspilot.local", "@lmspilot.local")


def ai_connection_quick_page() -> str:
    data = [
        ("Qwen3 4B", "qwen3:4b", "Nhẹ", "Phù hợp máy 16 GB RAM và thử nghiệm nhanh."),
        ("Qwen3 8B", "qwen3:8b", "Khuyên dùng", "Cân bằng chất lượng và tài nguyên khi sinh câu hỏi tiếng Việt."),
        ("Llama 3.1 8B", "llama3.1:8b", "Phổ biến", "Tương thích rộng cho nội dung đa ngôn ngữ."),
    ]
    model_cards = []
    for index, (name, model, badge, hint) in enumerate(data):
        active = index == 1
        model_cards.append(f"""
          <article class="ai-model-card {'active' if active else ''}">
            <header><span class="ai-model-mark">AI</span><span class="ai-model-badge">{'Đang dùng' if active else badge}</span></header>
            <h3>{name}</h3><code>{model}</code><p>{hint}</p>
            <button class="workspace-button primary">{icon('download',16)}{'Thiết lập và sử dụng' if active else 'Tải và tự thiết lập'}</button>
          </article>""")
    content = page_head(
        "Kết nối model AI",
        "Tải model Ollama bằng một nút, dùng model local có sẵn hoặc kết nối API key riêng.",
        "settings",
        '<button class="button secondary">Kiểm tra lại</button>',
        "Cấu hình hệ thống",
    ) + f"""
      <div class="ai-connection-center">
        <section class="ai-status-banner"><span class="status-dot online"></span><div><strong>Qwen3 8B đang sẵn sàng</strong><p>AI local · qwen3:8b · endpoint nội bộ đã phản hồi</p></div><button class="workspace-button secondary">Kiểm tra kết nối</button></section>
        <nav class="ai-mode-tabs"><button class="active">{icon('download',17)}Tải model mẫu tự động</button><button>{icon('settings',17)}Kết nối AI local có sẵn</button><button>{icon('lock',17)}Kết nối bằng API key</button></nav>
        <div class="ai-quick-layout">
          <section class="ai-runtime-card"><header><span class="runtime-icon ready">{icon('settings',20)}</span><div><h3>Ollama trong Docker</h3><p>Ollama đang hoạt động.</p></div><span class="runtime-badge ready">Sẵn sàng</span></header><dl><div><dt>Management URL</dt><dd>http://ollama:11434</dd></div><div><dt>OpenAI endpoint</dt><dd>http://ollama:11434/v1</dd></div><div><dt>Model đã có</dt><dd>1 model</dd></div></dl></section>
          <section class="ai-model-grid">{''.join(model_cards)}</section>
        </div>
        <section class="configured-ai-list"><header><div><h2>Kết nối đã cấu hình</h2><p>1 kết nối · 1 đang bật</p></div></header><div class="ai-provider-list"><article><span class="provider-state online"></span><div><strong>LOCAL_QWEN3_8B</strong><p>qwen3:8b</p><small>AI local · http://ollama:11434/v1</small></div><span class="provider-secret">Không cần key</span><div class="provider-actions"><button>Sửa</button><button>Kiểm tra</button></div></article></div></section>
      </div>"""
    return shell("ADMIN", "Cài đặt", content)


def ai_connection_form_page(remote: bool = False) -> str:
    heading = "Kết nối API riêng" if remote else "Kết nối model trong mạng nội bộ"
    sub = "Dùng API key riêng từ nhà cung cấp tương thích." if remote else "Dùng Ollama, LM Studio, vLLM hoặc server model riêng."
    code = "PRODUCTION_AI" if remote else "LOCAL_LM_STUDIO"
    model = "gpt-4.1-mini" if remote else "qwen3:8b"
    url = "https://api.openai.com/v1" if remote else "http://host.docker.internal:1234/v1"
    api_key = """
      <label class="wide"><span>API key</span><input type="password" value="sk-••••••••••••"><small>Key được mã hóa ở backend và không trả lại trình duyệt.</small></label>
    """ if remote else ""
    content = page_head("Kết nối model AI", "Thiết lập endpoint chuẩn OpenAI-compatible và kiểm tra độ trễ trước khi dùng.", "settings", "", "Cấu hình hệ thống") + f"""
      <div class="ai-connection-center">
        <nav class="ai-mode-tabs"><button>{icon('download',17)}Tải model mẫu tự động</button><button class="{'active' if not remote else ''}">{icon('settings',17)}Kết nối AI local có sẵn</button><button class="{'active' if remote else ''}">{icon('lock',17)}Kết nối bằng API key</button></nav>
        <div class="ai-provider-form-layout">
          <form class="ai-provider-form"><header><div><small>{'OPENAI-COMPATIBLE API' if remote else 'AI LOCAL CÓ SẴN'}</small><h2>{heading}</h2><p>{sub}</p></div></header><div class="ai-form-grid">
            <label><span>Tên cấu hình</span><input value="{code}"></label><label><span>Tên model</span><input value="{model}"></label>
            <label class="wide"><span>Base URL</span><input value="{url}"><small>Không nhập phần /chat/completions.</small></label>{api_key}
            <label><span>Timeout</span><div class="input-with-unit"><input value="180"><b>giây</b></div></label><label><span>Output token tối đa</span><input value="4096"></label>
          </div><label class="workspace-check"><input type="checkbox" checked><span>Bật kết nối ngay sau khi lưu</span></label><div class="ai-form-actions"><button class="workspace-button secondary">Đặt lại</button><button class="workspace-button primary">{icon('check',16)}Lưu kết nối</button></div></form>
          <aside class="ai-connection-guide"><h3>Kiểm tra trước khi lưu</h3><ol><li><span>1</span><p>Endpoint truy cập được từ container ai-service.</p></li><li><span>2</span><p>Model hỗ trợ chat completion và trả JSON ổn định.</p></li><li><span>3</span><p>Nhấn kiểm tra để đo độ trễ trước khi sử dụng.</p></li></ol><div class="ai-security-note">{icon('lock',18)}<p>Không lưu API key trong frontend, Git hoặc ảnh chụp màn hình.</p></div></aside>
        </div>
      </div>"""
    return shell("ADMIN", "Cài đặt", content)


def exam_overview_page() -> str:
    stat_data = [
        ("Tổng bài thi", "24", "Tất cả kỳ thi", "exam", "violet"),
        ("Đang mở", "6", "Đang diễn ra", "check", "green"),
        ("Bản nháp", "5", "Chưa xuất bản", "file", "amber"),
        ("Đã hoàn thành", "13", "Đã kết thúc", "report", "blue"),
    ]
    stats = "".join(f'<article class="exam-stat-card {tone}"><span>{icon(ic,22)}</span><div><small>{label}</small><strong>{value}</strong><p>{hint}</p></div></article>' for label,value,hint,ic,tone in stat_data)
    exam_data = [
        ("Kỳ thi năng lực số 2026", "EX2026-001", "ACTIVE", "120 phút", 60, "60%", 2),
        ("Thi cuối kỳ An toàn thông tin", "SEC-FINAL-2025", "ACTIVE", "90 phút", 45, "50%", 1),
        ("Đánh giá giữa kỳ Lập trình Python", "PY-MID-2025", "DRAFT", "60 phút", 10, "60%", 2),
        ("Thi chứng chỉ Quản trị hệ thống", "SYS-ADMIN-2025", "INACTIVE", "90 phút", 50, "65%", 1),
    ]
    rows = []
    for title, code, status, duration, count, passing, attempts in exam_data:
        label = {"ACTIVE": "Đang mở", "DRAFT": "Bản nháp", "INACTIVE": "Đã hoàn thành"}[status]
        tone = "success" if status == "ACTIVE" else "warning" if status == "DRAFT" else "info"
        rows.append(f"""
          <a class="exam-list-item"><span class="exam-list-icon status-{status.lower()}">{icon('exam',22)}</span><div class="exam-list-primary"><div class="exam-title-line"><h2>{title}</h2><span class="exam-code">{code}</span><span class="status-pill {tone}">{label}</span></div><p>Kỳ thi độc lập <i>•</i> Phiên bản 1</p><div class="exam-date-line">{icon('clock',15)}<span>Mở: 20/05/2026 08:00</span><i>•</i><span>Đóng: 27/05/2026 23:59</span></div></div><dl class="exam-list-metrics"><div><dt>Thời lượng</dt><dd>{duration}</dd></div><div><dt>Số câu</dt><dd>{count}</dd></div><div><dt>Điểm đạt</dt><dd>{passing}</dd></div><div><dt>Lượt làm</dt><dd>{attempts}</dd></div></dl><span class="exam-row-action">{icon('arrow',18)}</span></a>
        """)
    content = f"""
      <header class="exam-page-heading"><div><nav><span>Trang chủ</span><i>•</i><strong>Bài thi</strong></nav><h1>Bài thi</h1><p>Quản lý, tạo và theo dõi các kỳ thi độc lập trong hệ thống.</p></div><div class="exam-heading-actions"><button class="button secondary">{icon('file',17)}Tạo từ PDF/DOCX</button><button class="button primary">{icon('plus',17)}Tạo bài thi</button></div></header>
      <section class="exam-stat-grid">{stats}</section><div class="exam-overview-layout"><section class="exam-main-column"><div class="exam-filter-panel"><label class="exam-search-field">{icon('search',18)}<input placeholder="Tìm theo tên hoặc mã bài thi…"></label><div class="exam-filter-row"><label><span>Trạng thái</span><select><option>Tất cả</option></select></label><label><span>Sắp xếp</span><select><option>Mới nhất</option></select></label><button class="button secondary compact">Làm mới</button></div></div><div class="exam-list">{''.join(rows)}</div></section>
      <aside class="exam-side-column"><section class="exam-side-card"><header><h2>Tổng quan nhanh</h2></header><dl class="exam-quick-stats"><div><dt>Kỳ thi đang mở</dt><dd>6</dd></div><div><dt>Tổng câu hỏi</dt><dd>428</dd></div><div><dt>Cần xuất bản</dt><dd>5</dd></div><div><dt>Tỷ lệ hoạt động</dt><dd>25%</dd></div></dl></section><section class="exam-side-card"><header><h2>Bài thi gần đây</h2></header><div class="upcoming-exams"><a><span><b>01</b><small>KỲ THI</small></span><div><strong>Kỳ thi năng lực số 2026</strong><small>120 phút · 60 câu</small></div></a><a><span><b>02</b><small>KỲ THI</small></span><div><strong>Thi cuối kỳ An toàn thông tin</strong><small>90 phút · 45 câu</small></div></a></div></section><section class="exam-side-card"><header><h2>Thao tác nhanh</h2></header><div class="exam-quick-actions"><button>{icon('plus',16)}Tạo bài thi mới</button><button>{icon('file',16)}Sinh đề từ tài liệu</button><button>{icon('edit',16)}Thêm câu hỏi</button></div></section></aside></div>
    """
    return shell("INSTRUCTOR", "Bài thi", content)


def exam_editor_page() -> str:
    bank_data = [
        ("exam", "Trắc nghiệm", "Hàm print() trong Python được dùng để…", "Dễ", 1),
        ("check", "Đúng / Sai", "Python là ngôn ngữ lập trình thông dịch.", "Dễ", 1),
        ("file", "Điền khuyết", "Trong Python, để khai báo biến…", "Trung bình", 1),
        ("settings", "Ghép đôi", "Ghép kiểu dữ liệu với mô tả tương ứng.", "Khó", 2),
        ("exam", "Trắc nghiệm", "Kết quả biểu thức 3 ** 2 là?", "Dễ", 1),
    ]
    bank = "".join(f'<article class="preview-question-bank-item"><span class="lesson-type-icon">{icon(ic,17)}</span><div><div><strong>Câu {2451+i}</strong><span class="status-pill info">{kind}</span></div><p>{prompt}</p><small>{difficulty} · {points} điểm</small></div><button class="icon-button">{icon("plus",15)}</button></article>' for i,(ic,kind,prompt,difficulty,points) in enumerate(bank_data))
    selected_data = bank_data + [
        ("check", "Đúng / Sai", "List là kiểu dữ liệu có thứ tự.", "Trung bình", 1),
        ("file", "Điền khuyết", "Phương thức .append() dùng để…", "Trung bình", 1),
        ("exam", "Trắc nghiệm", "Từ khóa dùng để định nghĩa hàm?", "Khó", 1),
    ]
    selected = "".join(f'<article class="preview-selected-question"><span class="drag-handle">⋮⋮</span><b>{i}</b><span class="lesson-type-icon">{icon(ic,16)}</span><div><span class="status-pill info">{kind}</span><strong>{prompt}</strong><small>{difficulty} · {points} điểm</small></div><span class="exam-code">{points} điểm</span><button>•••</button></article>' for i,(ic,kind,prompt,difficulty,points) in enumerate(selected_data,1))
    content = f"""
      <div class="preview-editor-heading"><nav>Trang chủ　•　Bài thi　•　Chỉnh sửa</nav><h1>Chỉnh sửa bài thi</h1><p>Chỉnh sửa nội dung và cài đặt cho bài thi.</p><button class="button secondary">Xem trước đề thi</button></div>
      <div class="preview-exam-editor"><section class="workspace-panel"><header><div><h2>Ngân hàng câu hỏi</h2><p>1.248 câu hỏi</p></div></header><label class="exam-search-field">{icon('search',17)}<input placeholder="Tìm câu hỏi theo nội dung, từ khóa…"></label><div class="preview-filter-pills"><button>Tất cả loại</button><button>Tất cả độ khó</button></div><div class="preview-question-bank">{bank}</div></section>
      <section class="workspace-panel"><header><div><h2>Nội dung đề thi</h2><p>8 câu hỏi</p></div><button class="button secondary compact">Xáo trộn thứ tự</button></header><div class="preview-selected-list">{selected}</div><div class="preview-drop-zone">Kéo thả để sắp xếp thứ tự câu hỏi</div></section>
      <section class="workspace-panel preview-exam-settings"><header><div><h2>Cài đặt bài thi</h2><p>Thông số xuất bản</p></div></header><label>Khóa học<select><option>Lập trình Python cơ bản</option></select></label><label>Hình thức làm bài<select><option>Làm bài trên hệ thống</option></select></label><label>Thời lượng làm bài<div class="input-with-unit"><input value="60"><b>phút</b></div></label><div class="form-grid two"><label>Số câu hỏi<input value="8"></label><label>Tổng điểm<input value="10"></label></div><label>Điểm đạt<input value="6 (60%)"></label><label>Trạng thái<select><option>Đang hoạt động</option></select></label><label>Hiển thị kết quả<select><option>Sau khi nộp bài</option></select></label><button class="button primary">{icon('check',16)}Lưu thay đổi</button><button class="button secondary">Hủy bỏ</button></section></div>
    """
    return shell("INSTRUCTOR", "Bài thi", content)


def exam_taking_page(dark: bool = False) -> str:
    answers = "".join(f'<label class="answer-choice {"selected" if letter == "B" else ""}"><input type="radio" {"checked" if letter == "B" else ""}><span class="answer-letter">{letter}</span><span class="answer-text">{answer}</span></label>' for letter,answer in zip("ABCD", ["size()", "len()", "count()", "length()"] ))
    nav = "".join(f'<button class="{"active" if number == 12 else "answered" if number in [1,2,5,8,11] else ""}">{number}</button>' for number in range(1,31))
    content = f"""
      <div class="exam-taking"><header class="player-header exam-taking-header"><div><button class="icon-button">{icon('back')}</button><div><small>KỲ THI ĐỘC LẬP</small><h1>Thi cuối kỳ: Lập trình Python</h1></div></div><div class="exam-session-signals"><div class="exam-timer">{icon('clock',18)}<span><small>Còn lại</small><strong>00:59:32</strong></span></div><span class="status-pill success">Tự động lưu 10:24:31</span></div></header><div class="exam-progress-row"><div class="progress-track"><span style="width:27%"></span></div><span>12 / 45 câu</span></div>
      <div class="exam-taking-layout"><main class="exam-question-panel"><div class="question-editor"><div class="question-editor-head"><span>Câu 12 / 45</span><strong>3 điểm</strong></div><h2>Hàm nào trong Python được sử dụng để trả về số lượng phần tử trong một danh sách?</h2><p class="muted">Chọn một đáp án đúng.</p><div class="answer-options">{answers}</div></div><footer class="exam-actions"><button class="button secondary">Câu trước</button><div><button class="button secondary">Đánh dấu câu hỏi</button><button class="button primary">Câu tiếp theo　→</button></div></footer></main>
      <aside class="exam-navigator"><h2>Danh sách câu hỏi</h2><div>{nav}</div><div class="exam-legend"><span><i class="answered"></i>Đã trả lời</span><span><i></i>Chưa trả lời</span></div><div class="preview-exam-info"><h3>Thông tin bài thi</h3><dl><div><dt>Thời gian</dt><dd>90 phút</dd></div><div><dt>Số câu</dt><dd>45</dd></div><div><dt>Tổng điểm</dt><dd>100</dd></div><div><dt>Điểm đạt</dt><dd>≥ 50</dd></div></dl></div></aside></div></div>
    """
    return shell("STUDENT", "Bài thi", content, dark=dark)

def organization_page() -> str:
    def node(name: str, kind: str, level: int = 0, active: bool = False) -> str:
        active_class = "active" if active else ""
        indent = level * 20
        return f'''<div class="org-node-wrap"><button class="org-node {active_class}" style="margin-left:{indent}px;width:calc(100% - {indent}px)"><span class="org-type">◇</span><strong class="org-node-name">{name}</strong><span class="org-node-side"><span class="org-node-kind">{kind}</span><span class="org-status-dot"></span></span></button></div>'''

    tree = "".join([
        node("Công ty TNHH Demo", "Tổ chức", 0, True),
        node("Khối Công nghệ", "Khối", 1),
        node("Phòng Phát triển", "Phòng ban", 2),
        node("Nhóm Nền tảng", "Nhóm", 3),
        node("Trung tâm Đào tạo", "Đơn vị", 1),
    ])
    members = "".join(
        f'''<div><span class="mini-avatar">{initial}</span><div><strong>{name}</strong><small>{email}</small></div><span class="status-pill {tone}">{role}</span></div>'''
        for initial, name, email, role, tone in [
            ("AC", "Anh Chi", "anh.chi@example.com", "Quản trị viên", "violet"),
            ("BM", "Bảo Minh", "bao.minh@example.com", "Giảng viên", "info"),
            ("HN", "Hà Nguyên", "ha.nguyen@example.com", "Học viên", "teal"),
        ]
    )
    content = f'''<header class="workspace-hero"><span class="workspace-hero-glyph">{icon('building',25)}</span><div class="workspace-hero-copy"><span class="workspace-hero-eyebrow">TỔ CHỨC</span><h1>Cơ cấu và thành viên</h1><p>Quản lý cây tổ chức, đơn vị trực thuộc và thành viên trong một không gian làm việc gọn hơn.</p></div><div class="workspace-hero-actions"><button class="workspace-button secondary">Làm mới</button></div></header>
    <section class="metric-grid">{metric('building','5','Đơn vị','Khối, phòng, nhóm')}{metric('users','23','Thành viên','Đang thuộc tổ chức','violet')}{metric('check','19','Đang hoạt động','82% thành viên','success')}{metric('report','3','Cấp cơ cấu','Tối đa hiện tại','teal')}</section>
    <div class="org-layout"><section class="workspace-panel org-tree-panel"><header><div><h2>Cây tổ chức</h2><p>5 đơn vị</p></div><button class="workspace-button ghost">{icon('plus',15)}Thêm</button></header><label class="exam-search-field">{icon('search',17)}<input placeholder="Tìm đơn vị"></label><div class="org-tree">{tree}</div></section><div class="org-main-column"><section class="workspace-panel"><header><div><h2>Công ty TNHH Demo</h2><p>Tổ chức · DEMO</p></div><span class="status-pill success">3 người</span></header><div class="member-list">{members}</div></section><section class="workspace-panel compact-form-panel"><header><div><h2>Thông tin đơn vị</h2><p>Các trường được tách rõ, không dính nội dung.</p></div></header><form class="workspace-form"><label class="wide"><span>Tên đơn vị</span><input value="Công ty TNHH Demo"></label><label><span>Mã đơn vị</span><input value="DEMO"></label><label><span>Loại đơn vị</span><select><option>Tổ chức</option></select></label><label class="wide"><span>Đơn vị cha</span><select><option>Cấp gốc</option></select></label><label class="wide"><span>Mô tả</span><textarea>Quản lý cơ cấu, đơn vị trực thuộc và thành viên trong một không gian gọn hơn.</textarea></label><button class="workspace-button primary">{icon('check',16)}Lưu thay đổi</button></form></section></div></div>'''
    return shell("ADMIN", "Tổ chức", content)


EXTRA_CSS = r'''
.preview-editor-heading{position:relative;margin-bottom:16px;padding-right:190px}.preview-editor-heading nav{color:var(--ui-muted);font-size:12px}.preview-editor-heading h1{margin-top:6px;font-size:30px;letter-spacing:-.035em}.preview-editor-heading p{margin-top:5px;color:var(--ui-muted)}.preview-editor-heading>.button{position:absolute;right:0;top:8px}.preview-exam-editor{display:grid;grid-template-columns:minmax(270px,.82fr) minmax(430px,1.35fr) minmax(290px,.9fr);gap:14px;align-items:start}.preview-exam-editor>.workspace-panel{min-height:700px;padding:16px}.preview-exam-editor>.workspace-panel>header{display:flex;align-items:center;justify-content:space-between;gap:10px}.preview-exam-editor>.workspace-panel>header h2{font-size:17px}.preview-exam-editor>.workspace-panel>header p{color:var(--ui-muted);font-size:11px}.preview-filter-pills{display:flex;gap:7px;margin:10px 0}.preview-filter-pills button{min-height:34px;padding:6px 10px;border:1px solid var(--ui-border);border-radius:9px;background:var(--ui-surface);color:var(--ui-muted);font-size:12px}.preview-question-bank,.preview-selected-list{display:grid;gap:8px}.preview-question-bank-item{display:grid;grid-template-columns:36px minmax(0,1fr) 32px;align-items:center;gap:9px;padding:10px;border:1px solid var(--ui-border);border-radius:12px;background:var(--ui-surface)}.preview-question-bank-item>div{min-width:0}.preview-question-bank-item>div>div{display:flex;align-items:center;gap:6px}.preview-question-bank-item p{overflow:hidden;margin-top:5px;color:var(--ui-muted);font-size:12px;text-overflow:ellipsis;white-space:nowrap}.preview-question-bank-item small{color:var(--ui-muted);font-size:11px}.preview-selected-question{display:grid;grid-template-columns:18px 32px 34px minmax(0,1fr) auto 28px;align-items:center;gap:8px;padding:9px;border:1px solid var(--ui-border);border-radius:11px;background:var(--ui-surface)}.preview-selected-question>.drag-handle{color:var(--ui-muted);font-size:16px}.preview-selected-question>b{width:30px;height:30px;display:grid;place-items:center;border-radius:9px;background:var(--ui-muted-surface)}.preview-selected-question>div{min-width:0;display:grid;grid-template-columns:auto minmax(0,1fr);align-items:center;gap:7px}.preview-selected-question>div strong{overflow:hidden;font-size:12px;text-overflow:ellipsis;white-space:nowrap}.preview-selected-question>div small{grid-column:1/-1;color:var(--ui-muted);font-size:10px}.preview-selected-question>button{border:0;background:transparent;color:var(--ui-muted)}.preview-drop-zone{margin-top:10px;padding:18px;border:1px dashed color-mix(in srgb,var(--ui-primary) 35%,var(--ui-border));border-radius:11px;text-align:center;color:var(--ui-muted);font-size:12px}.preview-exam-settings{display:grid;align-content:start;gap:11px}.preview-exam-settings label{display:grid;gap:5px;font-size:12px;font-weight:750}.preview-exam-settings input,.preview-exam-settings select{min-height:40px}.preview-exam-info{margin-top:18px;padding-top:16px;border-top:1px solid var(--ui-border)}.preview-exam-info h3{font-size:15px}.preview-exam-info dl{display:grid;gap:9px;margin-top:12px}.preview-exam-info dl>div{display:flex;justify-content:space-between;gap:12px;font-size:12px}.preview-exam-info dt{color:var(--ui-muted)}.preview-exam-info dd{font-weight:800}.ai-connection-center{display:grid;gap:16px}.status-dot{width:10px;height:10px;border-radius:50%;background:var(--ui-muted)}.status-dot.online{background:var(--ui-success);box-shadow:0 0 0 5px var(--ui-success-soft)}.ai-status-banner{display:grid;grid-template-columns:12px minmax(0,1fr) auto;align-items:center;gap:12px;padding:14px 16px;border:1px solid var(--ui-border);border-radius:15px;background:var(--ui-surface)}.ai-status-banner p{margin-top:3px;color:var(--ui-muted);font-size:12px}
@media(max-width:1100px){.preview-exam-editor{grid-template-columns:1fr}.preview-exam-editor>.workspace-panel{min-height:auto}}
'''

SCREENS: list[tuple[str, Callable[[], str], tuple[int,int]]] = [
    ("01-login-custom-background.png", login_page, (1440,900)),
    ("02-admin-dashboard.png", lambda: dashboard("ADMIN"), (1440,900)),
    ("03-admin-users.png", wrap_base(base.users_page), (1440,900)),
    ("04-admin-organization.png", organization_page, (1440,900)),
    ("05-admin-branding-login-background.png", wrap_base(base.settings_page), (1440,900)),
    ("06-admin-ai-quick-install.png", ai_connection_quick_page, (1440,900)),
    ("07-admin-ai-local-endpoint.png", lambda: ai_connection_form_page(False), (1440,900)),
    ("08-admin-ai-api-key.png", lambda: ai_connection_form_page(True), (1440,900)),
    ("09-instructor-dashboard.png", lambda: dashboard("INSTRUCTOR"), (1440,900)),
    ("10-instructor-courses.png", wrap_base(base.courses_page), (1440,900)),
    ("11-instructor-course-content.png", wrap_base(base.course_detail_page), (1440,900)),
    ("12-instructor-course-quiz-from-documents.png", wrap_base(base.course_quiz_docs_page), (1440,900)),
    ("13-instructor-exam-overview.png", exam_overview_page, (1440,900)),
    ("14-instructor-exam-editor.png", exam_editor_page, (1440,900)),
    ("15-instructor-grading.png", wrap_base(base.grading_page), (1440,900)),
    ("16-student-dashboard.png", lambda: dashboard("STUDENT"), (1440,900)),
    ("17-student-courses.png", wrap_base(base.student_courses_page), (1440,900)),
    ("18-student-learning-video.png", wrap_base(base.learning_video_page), (1440,900)),
    ("19-student-docx-assignment.png", wrap_base(base.learning_documents_assignment_page), (1440,900)),
    ("20-student-course-quiz.png", wrap_base(base.student_course_quiz_page), (1440,900)),
    ("21-student-standalone-exam.png", exam_taking_page, (1440,900)),
    ("22-dark-mode-exam.png", lambda: exam_taking_page(True), (1440,900)),
]


async def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    TMP.mkdir(parents=True, exist_ok=True)
    css = (ROOT / "apps/web/app/globals.css").read_text(encoding="utf-8") + "\n" + (ROOT / "apps/web/app/unified.css").read_text(encoding="utf-8")
    preview_css = base.PREVIEW_CSS + "\n" + EXTRA_CSS
    async with async_playwright() as playwright:
        browser = await playwright.chromium.launch(executable_path="/usr/bin/chromium", headless=True, args=["--no-sandbox", "--disable-dev-shm-usage"])
        for filename, factory, viewport in SCREENS:
            page = await browser.new_page(viewport={"width": viewport[0], "height": viewport[1]}, device_scale_factor=1)
            markup = factory().replace("{CSS}", css).replace("{PREVIEW_CSS}", preview_css)
            (TMP / filename.replace(".png", ".html")).write_text(markup, encoding="utf-8")
            await page.set_content(markup, wait_until="load")
            await page.screenshot(path=str(OUT / filename), full_page=False)
            await page.close()
        await browser.close()
    print(f"Rendered {len(SCREENS)} previews to {OUT}")


if __name__ == "__main__":
    asyncio.run(main())
