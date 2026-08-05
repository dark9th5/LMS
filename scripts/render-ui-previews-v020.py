#!/usr/bin/env python3
"""Render deterministic LMSPilot 0.20 role-separated UI previews.

These previews use the shipped 0.18-readable CSS tokens plus representative
markup for the strict ADMIN / INSTRUCTOR / STUDENT portals. They are static
visual QA fixtures and do not replace browser E2E against running services.
"""
from __future__ import annotations

import asyncio
import html
from pathlib import Path
from typing import Callable

from playwright.async_api import async_playwright

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "screenshots" / "0.20.0"
TMP = ROOT / ".preview-render-v020"

PATHS = {
    "dashboard": '<rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/>',
    "book": '<path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H11v16H6.5A2.5 2.5 0 0 0 4 21.5z"/><path d="M20 5.5A2.5 2.5 0 0 0 17.5 3H13v16h4.5a2.5 2.5 0 0 1 2.5 2.5z"/>',
    "users": '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>',
    "building": '<rect x="4" y="3" width="16" height="18" rx="2"/><path d="M8 7h2M14 7h2M8 11h2M14 11h2M8 15h2M14 15h2M10 21v-3h4v3"/>',
    "learn": '<path d="m3 11 9-5 9 5-9 5z"/><path d="M7 13v4c3 2 7 2 10 0v-4M21 11v6"/>',
    "exam": '<path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>',
    "grade": '<path d="m12 2 3 6 7 .9-5 4.8 1.3 6.8-6.3-3.3-6.3 3.3L7 13.7 2 8.9 9 8z"/>',
    "report": '<path d="M4 19V9M10 19V5M16 19v-7M22 19H2"/>',
    "settings": '<circle cx="12" cy="12" r="3"/><path d="M19 12a7 7 0 0 0-.1-1l2-1.5-2-3.4-2.5 1a7 7 0 0 0-1.8-1L14.2 3h-4.4l-.4 3.1a7 7 0 0 0-1.8 1l-2.5-1-2 3.4L5.1 11a7 7 0 0 0 0 2l-2 1.5 2 3.4 2.5-1a7 7 0 0 0 1.8 1l.4 3.1h4.4l.4-3.1a7 7 0 0 0 1.8-1l2.5 1 2-3.4-2-1.5c.1-.3.1-.7.1-1z"/>',
    "bell": '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/>',
    "search": '<circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/>',
    "arrow": '<path d="m9 18 6-6-6-6"/>',
    "back": '<path d="m15 18-6-6 6-6"/>',
    "plus": '<path d="M12 5v14M5 12h14"/>',
    "clock": '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
    "check": '<path d="m5 12 4 4L19 6"/>',
    "play": '<circle cx="12" cy="12" r="9"/><path d="m10 8 6 4-6 4z"/>',
    "lock": '<rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/>',
    "file": '<path d="M6 2h8l4 4v16H6z"/><path d="M14 2v6h6M9 13h6M9 17h6"/>',
    "upload": '<path d="M12 16V4M7 9l5-5 5 5"/><path d="M5 20h14"/>',
    "download": '<path d="M12 4v12M7 11l5 5 5-5"/><path d="M5 20h14"/>',
    "certificate": '<circle cx="12" cy="9" r="6"/><path d="m8 14-1 8 5-3 5 3-1-8"/>',
    "result": '<path d="M4 4h16v16H4z"/><path d="M8 15l3-3 2 2 4-5"/>',
    "filter": '<path d="M4 5h16M7 12h10M10 19h4"/>',
    "menu": '<path d="M4 6h16M4 12h16M4 18h16"/>',
    "trash": '<path d="M4 7h16M9 7V4h6v3M7 7l1 14h8l1-14"/>',
    "image": '<rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="9" r="2"/><path d="m4 17 5-5 4 4 2-2 5 5"/>',
    "edit": '<path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4z"/>',
}

ROLE_META = {
    "ADMIN": {
        "label": "Quản trị viên",
        "workspace": "Không gian quản trị",
        "initials": "QT",
        "name": "Quản trị hệ thống",
        "icon": "settings",
        "nav": [("Tổng quan", "dashboard"), ("Người dùng", "users"), ("Tổ chức", "building"), ("Báo cáo", "report"), ("Cài đặt", "settings")],
    },
    "INSTRUCTOR": {
        "label": "Giảng viên",
        "workspace": "Không gian giảng viên",
        "initials": "GV",
        "name": "Nguyễn Minh Anh",
        "icon": "learn",
        "nav": [("Tổng quan", "dashboard"), ("Khóa học", "book"), ("Bài thi", "exam"), ("Chấm điểm", "grade"), ("Báo cáo", "report")],
    },
    "STUDENT": {
        "label": "Học viên",
        "workspace": "Không gian học viên",
        "initials": "HV",
        "name": "Trần Hải Nam",
        "icon": "book",
        "nav": [("Tổng quan", "dashboard"), ("Khóa học", "book"), ("Bài thi", "exam"), ("Kết quả", "result"), ("Chứng chỉ", "certificate")],
    },
}


def icon(name: str, size: int = 20) -> str:
    return f'<svg width="{size}" height="{size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">{PATHS[name]}</svg>'


def esc(value: object) -> str:
    return html.escape(str(value))


def shell(role: str, active: str, content: str, *, dark: bool = False) -> str:
    meta = ROLE_META[role]
    nav = "".join(
        f'''<a class="app-nav-link {'active' if label == active else ''}"><span class="app-nav-icon">{icon(ic, 19)}</span><span class="app-nav-copy"><strong>{esc(label)}</strong></span></a>'''
        for label, ic in meta["nav"]
    )
    theme = "unified-dark" if dark else "unified-light"
    return f'''<!doctype html><html lang="vi" data-theme="{theme}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width"><style>{{CSS}}</style><style>{{PREVIEW_CSS}}</style></head><body>
    <div class="app-shell role-{role.lower()}" data-portal-role="{role}">
      <aside class="app-sidebar">
        <div class="sidebar-brand-row"><div class="app-brand"><span class="brand-mark">L</span><span class="brand-copy"><strong>Learnova</strong></span></div></div>
        <div class="role-identity"><span class="role-identity-icon role-{role.lower()}">{icon(meta['icon'],18)}</span><span><small>Vai trò đang đăng nhập</small><strong>{meta['label']}</strong></span></div>
        <nav class="app-nav"><div class="sidebar-links">{nav}</div></nav>
        <div class="sidebar-footer"><button class="sidebar-search">{icon('search',18)}<span>Tìm nhanh</span><kbd>Ctrl K</kbd></button><div class="sidebar-profile"><span class="avatar">{meta['initials']}</span><span><strong>{meta['name']}</strong><small>{meta['label']}</small></span></div></div>
      </aside>
      <div class="app-workspace"><header class="app-topbar"><div class="topbar-leading"><div class="topbar-context"><small>{meta['workspace']}</small><strong>{esc(active)}</strong></div></div><button class="topbar-search">{icon('search',18)}<span>Tìm trong chức năng của {meta['label'].lower()}</span><kbd>Ctrl K</kbd></button><div class="topbar-actions"><button class="icon-button theme-toggle">{'☀' if dark else '☾'}</button><button class="icon-button">{icon('bell',19)}</button><span class="topbar-profile"><span class="avatar">{meta['initials']}</span><span><strong>{meta['name']}</strong><small>{meta['label']}</small></span></span></div></header><main class="app-content">{content}</main></div>
    </div></body></html>'''


def metric(ic: str, value: str, label: str, detail: str, tone: str = "primary") -> str:
    return f'<article class="metric-card tone-{tone}"><span class="metric-icon">{icon(ic)}</span><div><strong>{esc(value)}</strong><span>{esc(label)}</span><small>{esc(detail)}</small></div></article>'


def page_head(title: str, intro: str, ic: str, actions: str = "", eyebrow: str = "") -> str:
    return f'''<header class="page-head"><div class="page-head-main"><span class="page-icon">{icon(ic,25)}</span><div class="page-copy">{f'<span class="page-eyebrow">{esc(eyebrow)}</span>' if eyebrow else ''}<h1>{esc(title)}</h1><p>{esc(intro)}</p></div></div>{f'<div class="page-head-actions">{actions}</div>' if actions else ''}</header>'''


def dashboard(role: str, dark: bool = False) -> str:
    if role == "ADMIN":
        intro = "Quản trị tài khoản, cơ cấu tổ chức, báo cáo và cấu hình nền tảng. Chức năng giảng dạy được tách riêng cho giảng viên."
        metrics = [metric("users","248","Tài khoản","3 vai trò tách biệt"), metric("building","9","Đơn vị","Đang hoạt động","violet"), metric("report","24","Báo cáo","Đã tạo trong tháng","teal"), metric("bell","5","Thông báo","Chưa đọc","warning")]
        actions = [("users","Quản lý người dùng","Tạo tài khoản đúng một vai trò"),("building","Cơ cấu tổ chức","Đơn vị và thành viên"),("settings","Cấu hình hệ thống","Logo, nền đăng nhập và dịch vụ")]
        rows = [("Nguyễn Minh Anh","Giảng viên","Đã kích hoạt"),("Trần Hải Nam","Học viên","Đang học"),("Phạm Thu Hà","Quản trị viên","Đã kích hoạt"),("Lê Quốc Bảo","Học viên","Đang học")]
    elif role == "INSTRUCTOR":
        intro = "Biên soạn khóa học, tạo bài kiểm tra từ tài liệu, quản lý kỳ thi độc lập và chấm bài trong không gian riêng."
        metrics = [metric("book","8","Khóa học","6 đã xuất bản"), metric("exam","4","Bài thi độc lập","2 đang mở","violet"), metric("grade","12","Bài chờ chấm","Tự luận và thực hành","warning"), metric("users","186","Học viên","Được giao khóa học","teal")]
        actions = [("book","Mở khóa học","Biên soạn bài học và bài kiểm tra"),("exam","Quản lý bài thi","Chỉ kỳ thi độc lập"),("grade","Chấm bài","Bài thực hành và tự luận")]
        rows = [("Kỹ năng số nền tảng","84 học viên","76%"),("An toàn thông tin","62 học viên","58%"),("Quản trị dự án thực hành","48 học viên","91%"),("Văn hóa doanh nghiệp","118 học viên","44%")]
    else:
        intro = "Tiếp tục khóa học, làm bài kiểm tra ngay trong bài học và tham gia các kỳ thi độc lập được giao."
        metrics = [metric("book","4","Khóa học","Đang được giao"), metric("learn","68%","Tiến độ","Trung bình","success"), metric("exam","2","Bài thi","Sắp diễn ra","violet"), metric("result","6","Kết quả","Đã ghi nhận","teal")]
        actions = [("play","Tiếp tục học","Video, PDF, DOCX và bài thực hành"),("exam","Vào bài thi","Kỳ thi độc lập được giao"),("result","Xem kết quả","Điểm và phản hồi")]
        rows = [("Kỹ năng số nền tảng","12 bài học","76%"),("An toàn thông tin","9 bài học","58%"),("Giao tiếp hiệu quả","10 bài học","45%"),("Văn hóa doanh nghiệp","8 bài học","32%")]
    action_html = ''.join(f'<a><span class="quick-action-icon">{icon(ic)}</span><span><strong>{label}</strong><small>{hint}</small></span>{icon("arrow",16)}</a>' for ic,label,hint in actions)
    row_html = ''.join(f'<div class="progress-list-item"><span class="progress-course-icon">{icon("users" if role=="ADMIN" else "book",18)}</span><div><div class="progress-list-head"><strong>{esc(a)}</strong><span>{esc(c)}</span></div><small>{esc(b)}</small><div class="progress-track"><span style="width:{100 if role=="ADMIN" else int(c.rstrip("%"))}%"></span></div></div></div>' for a,b,c in rows)
    content = f'''<div class="dashboard-page role-dashboard"><section class="dashboard-welcome"><div class="dashboard-welcome-copy"><span class="dashboard-kicker">{ROLE_META[role]['workspace']}</span><h1>Xin chào, {ROLE_META[role]['name']}</h1><p>{intro}</p><div class="dashboard-welcome-actions"><button class="button primary">{icon(actions[0][0],18)}{actions[0][1]}</button><button class="button secondary">{icon(actions[1][0],18)}{actions[1][1]}</button></div></div><div class="dashboard-visual"><div class="dashboard-progress-card"><span>Vai trò hiện tại</span><strong class="role-dashboard-label">{ROLE_META[role]['label']}</strong><div><i style="width:100%"></i></div></div><div class="dashboard-mini-card card-a">{icon(actions[0][0])}<span><b>{'248' if role=='ADMIN' else '8' if role=='INSTRUCTOR' else '4'}</b><small>{'tài khoản' if role=='ADMIN' else 'khóa học'}</small></span></div><div class="dashboard-mini-card card-b">{icon(actions[1][0])}<span><b>{'9' if role=='ADMIN' else '12' if role=='INSTRUCTOR' else '2'}</b><small>{'đơn vị' if role=='ADMIN' else 'cần xử lý'}</small></span></div><span class="dashboard-orb orb-a"></span><span class="dashboard-orb orb-b"></span></div></section><section class="metric-grid">{''.join(metrics)}</section><section class="dashboard-layout"><div class="dashboard-column-main"><article class="dashboard-panel"><header class="panel-heading"><div><span class="panel-kicker">Dữ liệu gần đây</span><h2>{'Tài khoản mới' if role=='ADMIN' else 'Khóa học của bạn' if role=='INSTRUCTOR' else 'Tiến độ khóa học'}</h2></div><a>Xem tất cả {icon('arrow',15)}</a></header><div class="progress-list">{row_html}</div></article></div><aside class="dashboard-column-side"><article class="dashboard-panel"><header class="panel-heading"><div><span class="panel-kicker">Truy cập nhanh</span><h2>Chức năng của {ROLE_META[role]['label'].lower()}</h2></div></header><div class="quick-action-list">{action_html}</div></article></aside></section></div>'''
    return shell(role, "Tổng quan", content, dark=dark)


def users_page() -> str:
    rows = [
        ("QT","Quản trị hệ thống","admin@learnova.vn","Quản trị viên","Đang hoạt động"),
        ("MA","Nguyễn Minh Anh","minh.anh@learnova.vn","Giảng viên","Đang hoạt động"),
        ("HN","Trần Hải Nam","hai.nam@learnova.vn","Học viên","Đang hoạt động"),
        ("TH","Phạm Thu Hà","thu.ha@learnova.vn","Giảng viên","Đang hoạt động"),
        ("QB","Lê Quốc Bảo","quoc.bao@learnova.vn","Học viên","Tạm khóa"),
    ]
    body = ''.join(f'<tr><td><div class="person-cell"><span class="avatar small">{a}</span><span><strong>{n}</strong><small>{e}</small></span></div></td><td><span class="status-pill {"success" if r!="Học viên" else "info"}">{r}</span></td><td>{"Không gian quản trị" if r=="Quản trị viên" else "Không gian giảng viên" if r=="Giảng viên" else "Không gian học viên"}</td><td><span class="status-pill {"success" if st=="Đang hoạt động" else "warning"}">{st}</span></td><td><button class="icon-button">{icon("edit",17)}</button></td></tr>' for a,n,e,r,st in rows)
    content = page_head("Người dùng", "Mỗi tài khoản chỉ có đúng một vai trò. Muốn dùng chức năng khác phải đăng nhập bằng tài khoản của vai trò đó.", "users", f'<button class="button secondary">{icon("upload",17)}Nhập danh sách</button><button class="button primary">{icon("plus",17)}Tạo tài khoản</button>', "Quản trị") + f'''<section class="metric-grid">{metric('users','248','Tổng tài khoản','Đang quản lý')}{metric('settings','3','Vai trò cố định','Admin · Giảng viên · Học viên','violet')}{metric('check','236','Đang hoạt động','95% tài khoản','success')}{metric('lock','12','Tạm khóa','Cần rà soát','warning')}</section><section class="toolbar-card"><label class="search-field">{icon('search',18)}<input placeholder="Tìm tên, email hoặc mã tài khoản"></label><button class="button secondary">{icon('filter',17)}Vai trò</button><button class="button secondary">Trạng thái</button></section><section class="section-card"><div class="responsive-table"><table><thead><tr><th>Tài khoản</th><th>Vai trò</th><th>Cổng chức năng</th><th>Trạng thái</th><th></th></tr></thead><tbody>{body}</tbody></table></div></section>'''
    return shell("ADMIN", "Người dùng", content)


def organization_page() -> str:
    tree = ''.join(f'<button class="org-tree-row {"active" if i==0 else ""}" style="margin-left:{level*22}px"><span class="list-icon">{icon("building",17)}</span><span><strong>{name}</strong><small>{count} thành viên</small></span></button>' for i,(name,count,level) in enumerate([("Công ty TNHH Demo",23,0),("Khối Công nghệ",11,1),("Phòng Phát triển",6,2),("Nhóm Nền tảng",5,2),("Trung tâm Đào tạo",7,1),("Khối Kinh doanh",5,1)]))
    members = ''.join(f'<div class="member-row"><span class="avatar small">{initial}</span><span><strong>{name}</strong><small>{email}</small></span><span class="status-pill info">{role}</span></div>' for initial,name,email,role in [("AC","Anh Chi","anh.chi@example.com","Quản trị viên"),("BM","Bảo Minh","bao.minh@example.com","Giảng viên"),("HN","Hà Nguyễn","ha.nguyen@example.com","Học viên")])
    content = page_head("Tổ chức", "Quản lý cơ cấu và thành viên. Vai trò tài khoản vẫn độc lập với đơn vị tổ chức.", "building", f'<button class="button primary">{icon("plus",17)}Thêm đơn vị</button>', "Quản trị") + f'''<section class="metric-grid">{metric('building','5','Đơn vị','Khối, phòng, nhóm')}{metric('users','23','Thành viên','Đang thuộc tổ chức','violet')}{metric('check','19','Đang hoạt động','82% thành viên','success')}{metric('report','3','Cấp cơ cấu','Tối đa hiện tại','teal')}</section><section class="preview-three-columns"><article class="section-card"><div class="section-title"><div><h2>Cơ cấu tổ chức</h2><p>Chọn đơn vị để xem chi tiết</p></div></div><label class="search-field">{icon('search',17)}<input placeholder="Tìm đơn vị"></label><div class="org-tree">{tree}</div></article><article class="section-card"><div class="section-title"><div><h2>Công ty TNHH Demo</h2><p>Đơn vị gốc · đang hoạt động</p></div></div><div class="form-stack"><label>Tên đơn vị<input value="Công ty TNHH Demo"></label><div class="form-grid two"><label>Mã đơn vị<input value="DEMO"></label><label>Đơn vị cha<select><option>Không có</option></select></label></div><label>Trạng thái<select><option>Đang hoạt động</option></select></label><label>Mô tả<textarea>Quản lý cơ cấu, đơn vị trực thuộc và thành viên trong một không gian gọn hơn.</textarea></label><button class="button primary">Lưu thay đổi</button></div></article><article class="section-card"><div class="section-title"><div><h2>Thành viên trong đơn vị</h2><p>3 thành viên</p></div><button class="button secondary compact">{icon('plus',16)}Gán thành viên</button></div><label class="search-field">{icon('search',17)}<input placeholder="Tên hoặc email"></label><div class="member-list">{members}</div></article></section>'''
    return shell("ADMIN", "Tổ chức", content)


def settings_page() -> str:
    swatches = ''.join(f'<button class="color-swatch {"selected" if i==0 else ""}" style="--swatch:{c}"></button>' for i,c in enumerate(["#6C4EF6","#2563EB","#0F9F8F","#16A34A","#F97316","#E11D48"]))
    content = page_head("Cài đặt", "Cấu hình thương hiệu và nền đăng nhập. Nút sáng/tối vẫn nằm trên thanh trên cùng.", "settings", '<button class="button primary">Lưu thay đổi</button>', "Quản trị") + f'''<div class="workspace-tabs"><button class="workspace-tab active">Cấu hình thông tin</button><button class="workspace-tab">Dịch vụ ngoài</button></div><section class="preview-two-columns settings-preview"><article class="section-card"><div class="section-title"><div><h2>Cá nhân hóa thương hiệu</h2><p>Chỉ quản trị viên được thay đổi các tài nguyên công khai.</p></div></div><div class="brand-assets"><div class="brand-logo-preview">L</div><div><strong>Logo hệ thống</strong><p>PNG/JPG · tối đa 5 MB</p><button class="button secondary compact">{icon('upload',16)}Chọn ảnh logo</button></div></div><div class="login-background-card"><div class="login-bg-sample"><span>Ảnh nền đăng nhập</span></div><div><strong>Nền đăng nhập</strong><p>PNG, JPG hoặc WebP · tối đa 12 MB · khuyến nghị 1920×1080.</p><button class="button secondary compact">{icon('image',16)}Chọn ảnh nền</button></div></div><div class="form-grid two"><label>Tên thương hiệu<input value="Learnova"></label><label>Tên miền<input value="learnova.vn"></label></div><label>Giới thiệu ngắn<textarea>Nền tảng đào tạo trực tuyến hiện đại, rõ ràng và linh hoạt cho mọi tổ chức.</textarea></label><label>Màu chủ đạo<div class="swatches">{swatches}</div></label><label>Màu tùy chỉnh<input value="#6C4EF6"></label></article><article class="section-card"><div class="section-title"><div><h2>Xem trước trang đăng nhập</h2><p>Ảnh nền được phủ lớp màu để chữ luôn dễ đọc.</p></div></div><div class="login-live-preview"><div class="login-preview-copy"><span class="brand-mark">L</span><h2>Learnova</h2><p>Mỗi ngày tiến thêm một bước.</p></div><div class="login-preview-form"><strong>Đăng nhập</strong><label>Tên đăng nhập<input value="hocvien@learnova.vn"></label><label>Mật khẩu<input type="password" value="password"></label><button class="button primary">Đăng nhập</button></div></div></article></section>'''
    return shell("ADMIN", "Cài đặt", content)


def courses_page() -> str:
    rows = [("Kỹ năng số nền tảng","DIG-101","12 bài","84","Đã xuất bản","76%"),("An toàn thông tin","SEC-204","9 bài","62","Đã xuất bản","58%"),("Quản trị dự án thực hành","PM-310","14 bài","48","Đã xuất bản","91%"),("Văn hóa doanh nghiệp","CUL-100","8 bài","118","Bản nháp","44%"),("Giao tiếp hiệu quả","COM-110","10 bài","76","Đã xuất bản","65%")]
    body = ''.join(f'<tr><td><div class="course-cell"><span class="list-icon">{icon("book",18)}</span><span><strong>{name}</strong><small>{code}</small></span></div></td><td>{lessons}</td><td>{students}</td><td><div class="mini-progress"><span style="width:{progress}"></span></div><small>{progress}</small></td><td><span class="status-pill {"success" if status=="Đã xuất bản" else "warning"}">{status}</span></td><td><button class="button secondary compact">Mở</button></td></tr>' for name,code,lessons,students,status,progress in rows)
    content = page_head("Khóa học", "Khóa học là không gian đào tạo duy nhất: bài học, tài liệu, bài thực hành, bài kiểm tra và học viên.", "book", f'<button class="button primary">{icon("plus",17)}Tạo khóa học</button>', "Giảng dạy") + f'''<section class="metric-grid">{metric('book','8','Khóa học','6 đã xuất bản')}{metric('users','186','Học viên','Được giao trực tiếp','violet')}{metric('exam','14','Bài kiểm tra','Nằm trong khóa học','teal')}{metric('grade','12','Bài chờ chấm','Thực hành và tự luận','warning')}</section><section class="toolbar-card"><label class="search-field">{icon('search',18)}<input placeholder="Tìm theo tên hoặc mã khóa học"></label><button class="button secondary">Trạng thái</button><button class="button secondary">Danh mục</button><button class="button secondary">{icon('filter',17)}Bộ lọc</button></section><section class="section-card"><div class="responsive-table"><table><thead><tr><th>Khóa học</th><th>Nội dung</th><th>Học viên</th><th>Tiến độ</th><th>Trạng thái</th><th></th></tr></thead><tbody>{body}</tbody></table></div></section>'''
    return shell("INSTRUCTOR", "Khóa học", content)


def course_detail_page() -> str:
    lessons = ''.join(f'<button class="lesson-outline-row {"active" if i==0 else ""}"><span class="lesson-type-icon">{icon(ic,17)}</span><span><strong>{name}</strong><small>{kind} · {mins}</small></span><span class="completion-dot done">{icon("check",13)}</span></button>' for i,(name,kind,mins,ic) in enumerate([("Tổng quan chuyển đổi số","Video","8 phút","play"),("Tài liệu khởi động","PDF","6 trang","file"),("Cẩm nang thực hành","DOCX","12 trang","file"),("Bài thực hành cá nhân","Bài thực hành","Bắt buộc","upload"),("Bài kiểm tra cuối chương","Bài kiểm tra","15 phút","exam")]))
    content = page_head("Kỹ năng số nền tảng", "Biên soạn nội dung, giao học viên và tạo bài kiểm tra ngay trong khóa học.", "book", '<button class="button secondary">Xem trước</button><button class="button primary">Lưu thay đổi</button>', "DIG-101") + f'''<div class="workspace-tabs"><button class="workspace-tab active">Nội dung</button><button class="workspace-tab">Bài kiểm tra</button><button class="workspace-tab">Học viên</button><button class="workspace-tab">Thông tin</button></div><section class="course-editor-layout"><aside class="section-card course-outline"><div class="section-title"><div><h2>Cấu trúc khóa học</h2><p>5 nội dung</p></div><button class="button secondary compact">{icon('plus',16)}Thêm</button></div>{lessons}</aside><article class="section-card course-editor-main"><div class="section-title"><div><span class="page-eyebrow">Bài học video</span><h2>Tổng quan chuyển đổi số</h2></div><span class="status-pill success">Đã xuất bản</span></div><div class="form-grid two"><label>Tiêu đề<input value="Tổng quan chuyển đổi số"></label><label>Thời lượng<input value="08:00 phút"></label></div><label>Mô tả<textarea>Bài học giúp học viên hiểu khái niệm, tầm quan trọng và các xu hướng chính trong thời đại số.</textarea></label><div class="video-preview"><div class="video-art"><span class="video-play">{icon('play',38)}</span><div><small>TỔNG QUAN</small><strong>Chuyển đổi số</strong></div></div><div class="video-controls"><span>00:00 / 08:00</span><div class="progress-track"><span style="width:18%"></span></div></div></div></article><aside class="course-editor-side"><article class="section-card"><div class="section-title"><h2>Trạng thái xuất bản</h2></div><label>Trạng thái<select><option>Đã xuất bản</option></select></label><label>Hiển thị từ<input value="Hôm nay, 14:20"></label></article><article class="section-card"><div class="section-title"><h2>Quy tắc hoàn thành</h2></div><label class="check-row"><input type="checkbox" checked><span><strong>Xem ít nhất 90% video</strong></span></label><label class="check-row"><input type="checkbox" checked><span><strong>Vượt qua bài kiểm tra</strong></span></label></article></aside></section>'''
    return shell("INSTRUCTOR", "Khóa học", content)


def course_quiz_docs_page() -> str:
    docs = ''.join(f'<label class="question-option"><input type="checkbox" checked><span class="stacked-copy"><strong>{name}</strong><small>{kind} · tài liệu trong khóa học</small></span></label>' for name,kind in [("Tài liệu khởi động","PDF"),("Cẩm nang chuyển đổi số","DOCX")])
    generated = ''.join(f'<div class="generated-question"><span>{i}</span><div class="stacked-copy"><strong>{q}</strong><small>Trắc nghiệm · 1 điểm · Có trích dẫn nguồn</small></div><span class="status-pill success">Hợp lệ</span></div>' for i,q in enumerate(["Yếu tố nào là trung tâm của chuyển đổi số?","Dữ liệu được xem là tài sản của tổ chức?","Mục tiêu của quản trị thay đổi là gì?","Cấp độ trưởng thành số đầu tiên là gì?"],1))
    content = page_head("Bài kiểm tra trong khóa học", "Tạo câu hỏi từ PDF/DOCX thuộc đúng khóa học, duyệt kết quả AI rồi xuất bản vào bài học kiểm tra.", "exam", '<button class="button secondary">Lưu nháp</button><button class="button primary">Tạo bài kiểm tra</button>', "Kỹ năng số nền tảng") + f'''<div class="workflow-steps"><span class="done">1 <b>Chọn tài liệu</b></span><span class="done">2 <b>AI tạo câu hỏi</b></span><span class="active">3 <b>Duyệt câu hỏi</b></span><span>4 <b>Tạo đề</b></span></div><section class="preview-three-columns quiz-doc-layout"><article class="section-card"><div class="section-title"><div><h2>Tài liệu nguồn</h2><p>Chỉ PDF/DOCX thuộc khóa học</p></div></div>{docs}<div class="form-grid two"><label>Số câu<input value="10"></label><label>Ngôn ngữ<select><option>Tiếng Việt</option></select></label></div><button class="button secondary">{icon('upload',16)}Thay đổi tài liệu</button></article><article class="section-card generated-list"><div class="section-title"><div><h2>Câu hỏi đã tạo</h2><p>10 câu · đã kiểm tra cấu trúc</p></div><span class="status-pill success">Đã duyệt</span></div>{generated}<button class="button secondary">Xem đủ 10 câu</button></article><article class="section-card"><div class="section-title"><div><h2>Cài đặt đề</h2><p>Gắn trực tiếp với bài học</p></div></div><label>Tên bài kiểm tra<input value="Bài kiểm tra cuối chương 1"></label><label>Bài học<select><option>Bài kiểm tra cuối chương</option></select></label><div class="form-grid two"><label>Thời lượng<input value="15 phút"></label><label>Điểm đạt<input value="70%"></label></div><label>Trạng thái<select><option>Bản nháp</option></select></label><div class="form-alert info">Khi học viên đạt, hệ thống tự hoàn thành bài học kiểm tra.</div><button class="button primary">Tạo và mở trình soạn đề</button></article></section>'''
    return shell("INSTRUCTOR", "Khóa học", content)


def standalone_exams_page() -> str:
    rows=[("Kỳ thi năng lực số 2026","60 phút","342","Đang mở"),("Kỳ thi chứng nhận nội bộ","90 phút","78","Đã xuất bản"),("Đánh giá đầu vào tháng 8","45 phút","126","Bản nháp"),("Kỳ thi an toàn thông tin","60 phút","94","Đã xuất bản")]
    body=''.join(f'<tr><td><div class="course-cell"><span class="list-icon">{icon("exam",18)}</span><span><strong>{name}</strong><small>Kỳ thi độc lập</small></span></div></td><td>{duration}</td><td>{attempts}</td><td><span class="status-pill {"success" if status!="Bản nháp" else "warning"}">{status}</span></td><td><button class="button secondary compact">Mở</button></td></tr>' for name,duration,attempts,status in rows)
    content = page_head("Bài thi", "Chỉ quản lý kỳ thi độc lập, không thuộc khóa học. Có thể dựng đề từ PDF/DOCX do giảng viên tải lên.", "exam", f'<button class="button secondary">{icon("file",17)}Tạo từ PDF/DOCX</button><button class="button primary">{icon("plus",17)}Tạo bài thi</button>', "Giảng dạy") + f'''<section class="metric-grid">{metric('exam','4','Kỳ thi độc lập','2 đang mở')}{metric('users','640','Lượt được giao','Theo đối tượng','violet')}{metric('check','86%','Đã xuất bản','Tỷ lệ đề hoạt động','success')}{metric('file','128','Câu hỏi','Ngân hàng cá nhân','teal')}</section><section class="toolbar-card"><label class="search-field">{icon('search',18)}<input placeholder="Tìm bài thi"></label><button class="button secondary">Trạng thái</button><button class="button secondary">Thời gian</button></section><section class="section-card"><div class="responsive-table"><table><thead><tr><th>Bài thi</th><th>Thời lượng</th><th>Lượt làm</th><th>Trạng thái</th><th></th></tr></thead><tbody>{body}</tbody></table></div></section>'''
    return shell("INSTRUCTOR", "Bài thi", content)


def grading_page() -> str:
    rows=[("Trần Hải Nam","Bài thực hành cá nhân","Kỹ năng số nền tảng","Chờ chấm"),("Lê Quốc Bảo","Câu tự luận 5","Kỳ thi năng lực số 2026","Chờ chấm"),("Nguyễn Thu Trang","Báo cáo phân tích","Quản trị dự án thực hành","Đã trả lại"),("Đỗ Minh Khôi","Câu tự luận 8","Kỳ thi chứng nhận nội bộ","Chờ chấm")]
    cards=''.join(f'<article class="grading-card"><span class="list-icon">{icon("grade",18)}</span><div class="grading-card-main"><div class="grading-heading"><h2>{name}</h2><span class="status-pill {"warning" if status=="Chờ chấm" else "info"}">{status}</span></div><div class="grading-meta"><span>{item}</span><span>{course}</span><span>Nộp 2 giờ trước</span></div></div><button class="button primary compact">Chấm bài</button></article>' for name,item,course,status in rows)
    content = page_head("Chấm điểm", "Chấm câu tự luận và bài thực hành của các khóa học do chính giảng viên phụ trách.", "grade", '<button class="button secondary">Xuất danh sách</button>', "Giảng dạy") + f'''<div class="workspace-tabs"><button class="workspace-tab active">Bài thực hành</button><button class="workspace-tab">Câu tự luận</button></div><section class="metric-grid">{metric('grade','12','Chờ chấm','Cần xử lý')}{metric('book','5','Khóa học','Có bài nộp','violet')}{metric('clock','18 giờ','Thời gian TB','Xử lý bài nộp','warning')}{metric('check','94%','Đã hoàn tất','Trong tuần','success')}</section><section class="toolbar-card"><label class="search-field">{icon('search',18)}<input placeholder="Tìm học viên hoặc khóa học"></label><button class="button secondary">Khóa học</button><button class="button secondary">Trạng thái</button></section><section class="grading-list">{cards}</section>'''
    return shell("INSTRUCTOR", "Chấm điểm", content)


def student_courses_page() -> str:
    cards=[]
    for name,code,progress,next_item,tone in [("Kỹ năng số nền tảng","DIG-101",76,"Bài kiểm tra cuối chương","primary"),("An toàn thông tin","SEC-204",58,"Bảo vệ tài khoản","teal"),("Giao tiếp hiệu quả","COM-110",45,"Lắng nghe chủ động","violet"),("Văn hóa doanh nghiệp","CUL-100",32,"Giá trị cốt lõi","warning")]:
        cards.append(f'''<article class="course-card"><div class="course-cover tone-{tone}"><span class="course-symbol">{icon('book',32)}</span><span class="course-color-shapes"><i></i><b></b></span></div><div class="course-card-body"><div class="course-card-top"><span class="course-code">{code}</span><span class="status-pill info">Đang học</span></div><h2>{name}</h2><p>Bài tiếp theo: {next_item}</p><div class="progress-track"><span style="width:{progress}%"></span></div><div class="course-meta"><span>{progress}% hoàn thành</span><span>Hạn 30/08/2026</span></div></div><footer class="course-card-footer"><span>Đã lưu tiến độ</span><button class="button primary compact">Tiếp tục học</button></footer></article>''')
    content = page_head("Khóa học của tôi", "Video, PDF, DOCX, bài thực hành và bài kiểm tra được học ngay trong từng khóa học.", "book", "", "Học tập") + f'''<section class="metric-grid">{metric('book','4','Khóa học','Đang được giao')}{metric('learn','68%','Tiến độ','Trung bình','success')}{metric('clock','2','Sắp đến hạn','Trong 7 ngày','warning')}{metric('certificate','3','Chứng chỉ','Đã đạt','violet')}</section><section class="toolbar-card"><label class="search-field">{icon('search',18)}<input placeholder="Tìm khóa học"></label><button class="button secondary">Đang học</button><button class="button secondary">Sắp đến hạn</button></section><section class="course-grid">{''.join(cards)}</section>'''
    return shell("STUDENT", "Khóa học", content)


def learning_video_page(dark: bool = False) -> str:
    lessons=''.join(f'<button class="player-lesson {"active" if i==1 else ""}"><span class="completion-dot {"done" if i==0 else ""}">{icon("check",13) if i==0 else i+1}</span><span><strong>{name}</strong><small>{kind} · {mins}</small></span></button>' for i,(name,kind,mins) in enumerate([("Tổng quan chuyển đổi số","Video","8 phút"),("Tài liệu khởi động","PDF","6 trang"),("Cẩm nang thực hành","DOCX","12 trang"),("Bài thực hành cá nhân","Bài thực hành","Bắt buộc"),("Bài kiểm tra cuối chương","Bài kiểm tra","15 phút")]))
    content=f'''<div class="learning-player"><header class="player-header"><div class="player-title"><button class="icon-button">{icon('back')}</button><div><small>DIG-101</small><strong>Kỹ năng số nền tảng</strong></div></div><div class="player-progress"><div class="progress-track"><span style="width:38%"></span></div><span>38% hoàn thành</span></div><button class="button secondary">{icon('menu',17)}Mục lục</button></header><div class="player-layout"><aside class="player-outline open"><header class="player-outline-header"><div><strong>Nội dung khóa học</strong><small>1/5 bài đã hoàn thành</small></div></header><div class="player-lesson-list">{lessons}</div></aside><main class="player-content"><article class="learning-lesson"><header><span class="content-type">Video</span><h1>Tổng quan chuyển đổi số</h1><p>Nội dung bắt buộc · 8 phút</p></header><section class="lesson-body"><div class="video-preview student-video"><div class="video-art"><span class="video-play">{icon('play',42)}</span><div><small>KỸ NĂNG SỐ</small><strong>Chuyển đổi số</strong></div></div><div class="video-controls"><span>03:04 / 08:00</span><div class="progress-track"><span style="width:38%"></span></div></div></div></section><footer class="lesson-navigation"><button class="button secondary" disabled>Bài trước</button><div><button class="button primary">Đánh dấu hoàn thành</button></div></footer></article></main></div></div>'''
    return shell("STUDENT", "Khóa học", content, dark=dark)


def learning_documents_assignment_page() -> str:
    content = page_head("Cẩm nang thực hành", "Xem DOCX trực tiếp và nộp bài thực hành ngay trong khóa học.", "file", '<button class="button secondary">Quay lại khóa học</button>', "Kỹ năng số nền tảng") + '''<section class="preview-two-columns doc-assignment-layout"><article class="section-card"><div class="docx-preview-header"><span class="list-icon">''' + icon('file',18) + '''</span><div><strong>Cẩm nang chuyển đổi số.docx</strong><small>Xem trực tiếp trong khóa học</small></div><button class="button secondary compact">''' + icon('download',15) + '''Tải DOCX</button></div><div class="docx-page"><h2>Cẩm nang chuyển đổi số</h2><p>Chuyển đổi số là quá trình kết hợp con người, quy trình và công nghệ để tạo ra giá trị mới.</p><h3>1. Xác định mục tiêu</h3><p>Mục tiêu cần đo lường được và gắn với nhu cầu thực tế của tổ chức.</p><h3>2. Xây dựng lộ trình</h3><p>Ưu tiên các thay đổi tạo tác động sớm nhưng vẫn bảo đảm khả năng mở rộng.</p></div></article><article class="section-card"><div class="section-title"><div><h2>Bài thực hành cá nhân</h2><p>Nộp tệp trực tiếp cho giảng viên chấm</p></div><span class="status-pill warning">Chưa nộp</span></div><div class="assignment-brief"><span class="list-icon">''' + icon('upload',18) + '''</span><div><strong>Xây dựng lộ trình số cho đơn vị</strong><p>Hoàn thiện biểu mẫu theo tài liệu và tải lên một tệp PDF hoặc DOCX.</p></div></div><label>Chọn tệp bài làm<input type="file"></label><label>Ghi chú<textarea placeholder="Nội dung cần gửi cho giảng viên"></textarea></label><div class="form-alert info">Tệp được lưu riêng cho tài khoản học viên và chỉ giảng viên phụ trách khóa học được xem khi chấm.</div><button class="button primary">''' + icon('upload',17) + '''Nộp bài thực hành</button><div class="section-title compact-top"><div><h3>Lịch sử nộp</h3><p>Chưa có lần nộp nào</p></div></div></article></section>'''
    return shell("STUDENT", "Khóa học", content)


def student_course_quiz_page() -> str:
    opts = ''.join(f'<label class="question-option {"selected" if i==1 else ""}"><input type="radio" name="q" {"checked" if i==1 else ""}><span><b>{chr(65+i)}</b>{answer}</span></label>' for i,answer in enumerate(["Mua công nghệ mới nhất ngay lập tức","Con người, quy trình và sự tham gia của các bên liên quan","Tăng số lượng báo cáo mỗi tháng","Chuyển toàn bộ dữ liệu lên đám mây"] ))
    nav = ''.join(f'<button class="{ "active" if i==4 else "answered" if i<4 else ""}">{i}</button>' for i in range(1,11))
    content=f'''<div class="exam-taking"><header class="player-header exam-taking-header"><div><button class="icon-button">{icon('back')}</button><span><small>BÀI KIỂM TRA TRONG KHÓA HỌC</small><h1>Bài kiểm tra cuối chương 1</h1><p>Kỹ năng số nền tảng · đúng ghi danh hiện tại</p></span></div><div class="exam-session-signals"><span class="status-pill success">Đã tự động lưu</span><div class="exam-timer">{icon('clock',19)}<span><small>Còn lại</small><strong>11:42</strong></span></div></div></header><div class="exam-progress-row"><div class="progress-track"><span style="width:40%"></span></div><span>4/10 câu</span></div><div class="exam-taking-layout"><main class="exam-question-panel"><div class="question-editor"><div class="question-editor-head"><span>Câu 4 trên 10</span><strong>1 điểm</strong></div><h2>Yếu tố nào cần được ưu tiên để thay đổi số được chấp nhận và duy trì lâu dài?</h2><div class="answer-options">{opts}</div></div><footer class="exam-actions"><button class="button secondary">Câu trước</button><div><button class="button secondary">Đánh dấu xem lại</button><button class="button primary">Lưu và sang câu tiếp</button></div></footer></main><aside class="exam-navigator"><h2>Danh sách câu hỏi</h2><div>{nav}</div><div class="exam-legend"><span><i class="answered"></i>Đã trả lời</span><span><i></i>Chưa trả lời</span></div><div class="form-alert info">Kết quả đạt sẽ hoàn thành bài học kiểm tra.</div></aside></div></div>'''
    return shell("STUDENT", "Khóa học", content)


def student_exams_page() -> str:
    cards = ''.join(f'<article class="exam-assignment-card"><span class="list-icon">{icon("exam",18)}</span><div><strong>{name}</strong><p>{date} · {duration} · {questions} câu</p><span class="status-pill {tone}">{status}</span></div><button class="button {"primary" if status=="Sẵn sàng" else "secondary"} compact">{"Vào bài thi" if status=="Sẵn sàng" else "Xem chi tiết"}</button></article>' for name,date,duration,questions,status,tone in [("Kỳ thi năng lực số 2026","24/08/2026 09:00","60 phút","50","Sẵn sàng","success"),("Kỳ thi chứng nhận nội bộ","29/08/2026 14:00","90 phút","70","Sắp mở","info"),("Đánh giá đầu vào","Đã hoàn thành","45 phút","30","Đã nộp","neutral")])
    content=page_head("Bài thi", "Các kỳ thi độc lập được giao cho tài khoản học viên; bài kiểm tra khóa học không xuất hiện tại đây.", "exam", "", "Học tập")+f'''<section class="metric-grid">{metric('exam','2','Sắp diễn ra','Trong tháng')}{metric('clock','60 phút','Gần nhất','Kỳ thi năng lực số','warning')}{metric('check','1','Đã hoàn thành','Có kết quả','success')}{metric('result','86%','Điểm gần nhất','Đã đạt','teal')}</section><section class="exam-assignment-list">{cards}</section>'''
    return shell("STUDENT", "Bài thi", content)


def login_page() -> str:
    return f'''<!doctype html><html lang="vi" data-theme="unified-light"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width"><style>{{CSS}}</style><style>{{PREVIEW_CSS}}</style></head><body><main class="auth-page"><section class="auth-showcase custom-login-bg"><div class="auth-brand"><span class="auth-brand-mark">L</span><div><strong>Learnova</strong><small>Không gian học tập của tổ chức</small></div></div><div class="auth-showcase-copy"><span class="auth-pill">{icon('learn',16)} Học tập liền mạch</span><h2>Mỗi ngày tiến thêm một bước.</h2><p>Khách hàng có thể tải ảnh nền riêng; hệ thống tự phủ gradient để logo và nội dung luôn dễ đọc.</p></div><div class="auth-preview"><article class="auth-course-card auth-course-primary"><span class="auth-course-icon">{icon('book',22)}</span><div><small>Đang học</small><strong>Kỹ năng số nền tảng</strong></div><span class="auth-progress-value">72%</span><div class="auth-progress-track"><i></i></div></article><article class="auth-course-card auth-course-secondary"><span class="auth-course-icon">{icon('exam',22)}</span><div><small>Sắp tới</small><strong>Kỳ thi năng lực số</strong></div><span class="auth-date">09:30</span></article></div><p class="auth-privacy">{icon('lock',16)} Dữ liệu được bảo vệ trong hệ thống của tổ chức.</p></section><section class="auth-access"><div class="auth-card"><header class="auth-card-heading"><span class="auth-card-icon">{icon('lock',22)}</span><div><p class="auth-eyebrow">Chào mừng trở lại</p><h1>Đăng nhập</h1><p>Đăng nhập bằng đúng tài khoản Admin, Giảng viên hoặc Học viên.</p></div></header><form class="auth-form"><label class="field-group"><span>Tên đăng nhập hoặc email</span><span class="input-shell">{icon('users',19)}<input value="hocvien@learnova.vn"></span></label><label class="field-group"><span>Mật khẩu</span><span class="input-shell">{icon('lock',19)}<input type="password" value="password"></span></label><button class="button primary auth-submit">Đăng nhập {icon('arrow',18)}</button></form><div class="role-login-note"><span>Admin</span><span>Giảng viên</span><span>Học viên</span></div></div></section></main></body></html>'''


PREVIEW_CSS = r'''
.app-content{max-width:none}.page-head{margin-bottom:18px}.preview-two-columns{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:18px}.preview-three-columns{display:grid;grid-template-columns:minmax(280px,.9fr) minmax(360px,1.15fr) minmax(300px,.95fr);gap:16px}.section-card{min-width:0}.section-card label{display:grid;gap:7px;margin-bottom:14px;font-weight:700;color:var(--ui-text)}.section-card input,.section-card select,.section-card textarea{width:100%}.section-card textarea{min-height:88px}.member-list,.org-tree,.generated-list,.exam-assignment-list{display:grid;gap:10px}.member-row,.org-tree-row,.generated-question,.exam-assignment-card,.assignment-brief{display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:center;gap:12px;padding:13px;border:1px solid var(--ui-border);border-radius:14px;background:var(--ui-surface)}.org-tree-row{text-align:left;width:calc(100% - var(--indent,0px))}.org-tree-row.active{border-color:var(--ui-primary);background:var(--ui-primary-soft)}.brand-assets,.login-background-card{display:grid;grid-template-columns:auto 1fr;gap:16px;align-items:center;padding:15px;border:1px solid var(--ui-border);border-radius:16px;margin-bottom:16px}.brand-logo-preview{width:78px;height:78px;border-radius:20px;display:grid;place-items:center;background:linear-gradient(135deg,var(--ui-primary),#8b5cf6);color:#fff;font-size:32px;font-weight:900}.login-bg-sample{width:170px;height:96px;border-radius:15px;display:flex;align-items:flex-end;padding:12px;color:#fff;font-weight:800;background:linear-gradient(135deg,rgba(18,25,60,.74),rgba(108,78,246,.48)),radial-gradient(circle at 25% 25%,#7c3aed,transparent 35%),linear-gradient(120deg,#172554,#5b21b6 55%,#0f766e)}.swatches{display:flex;gap:10px}.color-swatch{width:34px;height:34px;border-radius:11px;border:3px solid var(--ui-surface);box-shadow:0 0 0 1px var(--ui-border);background:var(--swatch)}.color-swatch.selected{box-shadow:0 0 0 2px var(--ui-primary)}.settings-preview{grid-template-columns:minmax(480px,.9fr) minmax(560px,1.1fr)}.login-live-preview{min-height:560px;border:1px solid var(--ui-border);border-radius:18px;padding:32px;display:grid;grid-template-columns:1fr 320px;gap:26px;align-items:center;color:#fff;background:linear-gradient(145deg,rgba(18,25,60,.82),rgba(62,49,151,.68)),radial-gradient(circle at 22% 18%,#8b5cf6,transparent 30%),linear-gradient(130deg,#172554,#5b21b6 58%,#0f766e)}.login-preview-copy h2{font-size:36px}.login-preview-form{display:grid;gap:14px;padding:22px;border-radius:17px;background:rgba(255,255,255,.94);color:#172033;box-shadow:var(--shadow-lg)}.course-cell{display:flex;align-items:center;gap:12px}.mini-progress{width:120px;height:7px;border-radius:999px;background:var(--ui-border);overflow:hidden}.mini-progress span{height:100%;display:block;background:var(--ui-primary);border-radius:inherit}.course-editor-layout{display:grid;grid-template-columns:320px minmax(520px,1fr) 290px;gap:16px}.course-outline{display:grid;align-content:start;gap:9px}.lesson-outline-row{display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:center;gap:11px;text-align:left;padding:12px;border:1px solid transparent;border-radius:13px;background:transparent;color:var(--ui-text)}.lesson-outline-row.active{background:var(--ui-primary-soft);border-color:var(--ui-primary)}.lesson-outline-row small{display:block;color:var(--ui-muted);margin-top:3px}.lesson-type-icon{width:36px;height:36px;display:grid;place-items:center;border-radius:11px;background:var(--ui-surface-alt);color:var(--ui-primary)}.course-editor-side{display:grid;align-content:start;gap:14px}.video-preview{overflow:hidden;border:1px solid var(--ui-border);border-radius:18px;background:var(--ui-surface-alt)}.video-art{min-height:300px;display:flex;align-items:center;justify-content:center;gap:24px;background:radial-gradient(circle at 70% 25%,color-mix(in srgb,var(--ui-primary) 30%,transparent),transparent 32%),linear-gradient(145deg,var(--ui-primary-soft),var(--ui-surface-alt));color:var(--ui-text)}.video-art strong{display:block;font-size:38px;color:var(--ui-primary)}.video-play{width:74px;height:74px;display:grid;place-items:center;border-radius:50%;background:var(--ui-primary);color:var(--ui-on-primary);box-shadow:var(--shadow-lg)}.video-controls{padding:14px;display:flex;align-items:center;gap:14px}.video-controls .progress-track{flex:1}.workspace-tabs{display:flex;gap:8px;margin-bottom:16px;border-bottom:1px solid var(--ui-border)}.workspace-tab{padding:12px 16px;border:0;border-bottom:2px solid transparent;background:transparent;color:var(--ui-muted);font-weight:800}.workspace-tab.active{color:var(--ui-primary);border-color:var(--ui-primary)}.workflow-steps{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;margin-bottom:16px}.workflow-steps span{padding:12px 14px;border:1px solid var(--ui-border);border-radius:13px;background:var(--ui-surface);color:var(--ui-muted)}.workflow-steps span.done{color:var(--ui-success);background:var(--ui-success-soft)}.workflow-steps span.active{color:var(--ui-primary);border-color:var(--ui-primary);background:var(--ui-primary-soft)}.quiz-doc-layout{grid-template-columns:minmax(270px,.8fr) minmax(430px,1.25fr) minmax(300px,.9fr)}.generated-question>span:first-child{width:34px;height:34px;display:grid;place-items:center;border-radius:10px;background:var(--ui-primary-soft);color:var(--ui-primary);font-weight:900}.exam-assignment-list{grid-template-columns:repeat(3,minmax(0,1fr))}.exam-assignment-card{grid-template-columns:auto 1fr}.exam-assignment-card .button{grid-column:1/-1}.doc-assignment-layout{grid-template-columns:minmax(560px,1.15fr) minmax(430px,.85fr)}.docx-preview-header{display:grid;grid-template-columns:auto 1fr auto;align-items:center;gap:12px;padding-bottom:14px;border-bottom:1px solid var(--ui-border)}.docx-page{margin-top:16px;padding:38px 48px;min-height:560px;border:1px solid var(--ui-border);border-radius:10px;background:var(--ui-surface);box-shadow:var(--shadow-sm)}.docx-page h2{text-align:center;margin-bottom:28px}.docx-page h3{margin-top:24px}.assignment-brief{grid-template-columns:auto 1fr;margin-bottom:16px}.role-login-note{display:flex;gap:8px;justify-content:center;margin-top:18px}.role-login-note span{padding:6px 10px;border-radius:999px;background:var(--ui-primary-soft);color:var(--ui-primary);font-size:12px;font-weight:800}.custom-login-bg{background:linear-gradient(145deg,rgba(18,25,60,.84),rgba(62,49,151,.72)),radial-gradient(circle at 25% 20%,#7c3aed,transparent 30%),radial-gradient(circle at 80% 75%,#0f766e,transparent 28%),linear-gradient(125deg,#172554,#5b21b6 55%,#0f766e)}.student-video .video-art{min-height:470px}.compact-top{margin-top:24px}.form-alert.info{line-height:1.5}.responsive-table td,.responsive-table th{font-size:14px}.course-card h2{font-size:17px}.auth-showcase.has-custom-background,.custom-login-bg{background-size:cover;background-position:center}.stacked-copy{display:grid;gap:4px;min-width:0}.stacked-copy small{display:block;color:var(--ui-muted);font-weight:600;line-height:1.35}.app-nav-link:after{display:none!important}
@media(max-width:1100px){.preview-three-columns,.course-editor-layout,.quiz-doc-layout{grid-template-columns:1fr}.preview-two-columns,.settings-preview,.doc-assignment-layout{grid-template-columns:1fr}.exam-assignment-list{grid-template-columns:1fr}}
'''

SCREENS: list[tuple[str, Callable[[], str], tuple[int,int]]] = [
    ("01-login-custom-background.png", login_page, (1440,900)),
    ("02-admin-dashboard.png", lambda: dashboard("ADMIN"), (1440,900)),
    ("03-admin-users.png", users_page, (1440,900)),
    ("04-admin-organization.png", organization_page, (1440,900)),
    ("05-admin-branding-login-background.png", settings_page, (1440,900)),
    ("06-instructor-dashboard.png", lambda: dashboard("INSTRUCTOR"), (1440,900)),
    ("07-instructor-courses.png", courses_page, (1440,900)),
    ("08-instructor-course-content.png", course_detail_page, (1440,900)),
    ("09-instructor-course-quiz-from-documents.png", course_quiz_docs_page, (1440,900)),
    ("10-instructor-standalone-exams.png", standalone_exams_page, (1440,900)),
    ("11-instructor-grading.png", grading_page, (1440,900)),
    ("12-student-dashboard.png", lambda: dashboard("STUDENT"), (1440,900)),
    ("13-student-courses.png", student_courses_page, (1440,900)),
    ("14-student-learning-video.png", learning_video_page, (1440,900)),
    ("15-student-docx-assignment.png", learning_documents_assignment_page, (1440,900)),
    ("16-student-course-quiz.png", student_course_quiz_page, (1440,900)),
    ("17-student-standalone-exams.png", student_exams_page, (1440,900)),
    ("18-dark-mode-learning.png", lambda: learning_video_page(True), (1440,900)),
]


async def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    TMP.mkdir(parents=True, exist_ok=True)
    css = (ROOT / "apps/web/app/globals.css").read_text(encoding="utf-8") + "\n" + (ROOT / "apps/web/app/unified.css").read_text(encoding="utf-8")
    async with async_playwright() as p:
        browser = await p.chromium.launch(executable_path="/usr/bin/chromium", headless=True, args=["--no-sandbox", "--disable-dev-shm-usage"])
        for filename, factory, viewport in SCREENS:
            page = await browser.new_page(viewport={"width": viewport[0], "height": viewport[1]}, device_scale_factor=1)
            markup = factory().replace("{CSS}", css).replace("{PREVIEW_CSS}", PREVIEW_CSS)
            (TMP / filename.replace(".png", ".html")).write_text(markup, encoding="utf-8")
            await page.set_content(markup, wait_until="load")
            await page.screenshot(path=str(OUT / filename), full_page=False)
            await page.close()
        await browser.close()
    print(f"Rendered {len(SCREENS)} previews to {OUT}")


if __name__ == "__main__":
    asyncio.run(main())
