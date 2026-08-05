#!/usr/bin/env python3
"""Render deterministic LMSPilot UI previews from the shipped CSS design system.

The previews are static representative screens. They intentionally use the same
CSS files and class names as the Next.js application, so visual regressions in
core tokens/layouts are visible without requiring backend services.
"""
from __future__ import annotations

import asyncio
import html
from pathlib import Path
from typing import Callable

from playwright.async_api import async_playwright

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "screenshots" / "0.17.0"
TMP = ROOT / ".preview-render"

PATHS = {
    "dashboard": '<rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/>',
    "book": '<path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H11v16H6.5A2.5 2.5 0 0 0 4 21.5z"/><path d="M20 5.5A2.5 2.5 0 0 0 17.5 3H13v16h4.5a2.5 2.5 0 0 1 2.5 2.5z"/>',
    "users": '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>',
    "class": '<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M7 8h10M7 12h6M7 16h4"/>',
    "learn": '<path d="m3 11 9-5 9 5-9 5z"/><path d="M7 13v4c3 2 7 2 10 0v-4M21 11v6"/>',
    "exam": '<path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>',
    "grade": '<path d="m12 2 3 6 7 .9-5 4.8 1.3 6.8-6.3-3.3-6.3 3.3L7 13.7 2 8.9 9 8z"/>',
    "report": '<path d="M4 19V9M10 19V5M16 19v-7M22 19H2"/>',
    "settings": '<circle cx="12" cy="12" r="3"/><path d="M19 12a7 7 0 0 0-.1-1l2-1.5-2-3.4-2.5 1a7 7 0 0 0-1.8-1L14.2 3h-4.4l-.4 3.1a7 7 0 0 0-1.8 1l-2.5-1-2 3.4L5.1 11a7 7 0 0 0 0 2l-2 1.5 2 3.4 2.5-1a7 7 0 0 0 1.8 1l.4 3.1h4.4l.4-3.1a7 7 0 0 0 1.8-1l2.5 1 2-3.4-2-1.5c.1-.3.1-.7.1-1z"/>',
    "bell": '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/>',
    "search": '<circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/>',
    "arrow": '<path d="m9 18 6-6-6-6"/>',
    "plus": '<path d="M12 5v14M5 12h14"/>',
    "clock": '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
    "check": '<path d="m5 12 4 4L19 6"/>',
    "play": '<circle cx="12" cy="12" r="9"/><path d="m10 8 6 4-6 4z"/>',
    "chevron": '<path d="m9 6 6 6-6 6"/>',
    "lock": '<rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/>',
    "calendar": '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/>',
    "menu": '<path d="M4 6h16M4 12h16M4 18h16"/>',
}


def icon(name: str, size: int = 20) -> str:
    return f'<svg width="{size}" height="{size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">{PATHS[name]}</svg>'


def nav_item(label: str, hint: str, icon_name: str, active: bool = False) -> str:
    return f'''<a class="app-nav-link {'active' if active else ''}">
      <span class="app-nav-icon">{icon(icon_name, 19)}</span>
      <span class="app-nav-copy"><strong>{html.escape(label)}</strong><small>{html.escape(hint)}</small></span>
      {icon('chevron', 15)}
    </a>'''


def shell(content: str, *, active: str = "Tổng quan", dark: bool = False) -> str:
    items = [
        ("Tổng quan", "Việc cần làm hôm nay", "dashboard"),
        ("Học tập của tôi", "Tiếp tục bài đang học", "learn"),
        ("Khóa học", "Nội dung và tài liệu", "book"),
        ("Lớp học", "Lịch và học viên", "class"),
        ("Bài kiểm tra & kỳ thi", "Đề thi và phiên làm bài", "exam"),
        ("Chấm điểm", "Bài tự luận và phản hồi", "grade"),
        ("Báo cáo", "Tiến độ và kết quả", "report"),
        ("Người dùng", "Tài khoản và gói quyền", "users"),
        ("Cài đặt", "Giao diện và kết nối", "settings"),
    ]
    learning = ''.join(nav_item(*item, active=item[0] == active) for item in items[:4])
    assessment = ''.join(nav_item(*item, active=item[0] == active) for item in items[4:7])
    admin = ''.join(nav_item(*item, active=item[0] == active) for item in items[7:])
    theme = "unified-dark" if dark else "unified-light"
    return f'''<!doctype html><html lang="vi" data-theme="{theme}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width"><style>{{CSS}}</style></head><body>
    <div class="app-shell">
      <aside class="app-sidebar">
        <div class="sidebar-brand-row"><div class="app-brand"><span class="brand-mark">L</span><span class="brand-copy"><strong>LMSPilot</strong><small>Không gian học tập</small></span></div></div>
        <nav class="app-nav"><section class="sidebar-section"><h2>Học tập</h2><div class="sidebar-links">{learning}</div></section><section class="sidebar-section"><h2>Đánh giá</h2><div class="sidebar-links">{assessment}</div></section><section class="sidebar-section"><h2>Quản trị</h2><div class="sidebar-links">{admin}</div></section></nav>
        <div class="sidebar-footer"><button class="sidebar-search">{icon('search',18)}<span>Tìm nhanh</span><kbd>Ctrl K</kbd></button><div class="sidebar-profile"><span class="avatar">QT</span><span><strong>Quản trị hệ thống</strong><small>Quản trị viên</small></span><button class="icon-button">{icon('arrow',18)}</button></div></div>
      </aside>
      <div class="app-workspace"><header class="app-topbar"><div class="topbar-leading"><div class="topbar-context"><small>Không gian làm việc</small><strong>{html.escape(active)}</strong></div></div><button class="topbar-search">{icon('search',18)}<span>Tìm khóa học, lớp học, báo cáo...</span><kbd>Ctrl K</kbd></button><div class="topbar-actions"><button class="icon-button">{icon('bell',20)}</button><span class="avatar">QT</span></div></header><main class="app-content">{content}</main></div>
    </div></body></html>'''


def dashboard() -> str:
    metrics = [
        ("users", "248", "Người dùng", "312 lượt ghi danh", "primary"),
        ("book", "18", "Khóa học đã xuất bản", "4 bản nháp", "success"),
        ("class", "7", "Lớp đang mở", "9 lớp trong phạm vi", "violet"),
        ("grade", "12", "Bài chờ chấm", "3 lượt học quá hạn", "warning"),
    ]
    metric_html = ''.join(f'<article class="metric-card tone-{tone}"><span class="metric-icon">{icon(ic)}</span><div><strong>{value}</strong><span>{label}</span><small>{detail}</small></div></article>' for ic,value,label,detail,tone in metrics)
    progress = [("Kỹ năng số nền tảng",76),("An toàn thông tin",58),("Quản trị dự án thực hành",91),("Văn hóa doanh nghiệp",44)]
    progress_html=''.join(f'<div class="progress-list-item"><span class="progress-course-icon">{icon("book",18)}</span><div><div class="progress-list-head"><strong>{name}</strong><span>{value}%</span></div><div class="progress-track"><span style="width:{value}%"></span></div></div></div>' for name,value in progress)
    content=f'''<div class="dashboard-page">
      <section class="dashboard-welcome"><div class="dashboard-welcome-copy"><span class="dashboard-kicker">Tổng quan hôm nay</span><h1>Xin chào, Quản trị hệ thống</h1><p>Theo dõi hoạt động đào tạo và xử lý những công việc quan trọng trong phạm vi được cấp.</p><div class="dashboard-welcome-actions"><button class="button primary">{icon('plus',18)}Tạo khóa học</button><button class="button secondary">{icon('class',18)}Quản lý lớp</button></div></div><div class="dashboard-visual"><div class="dashboard-progress-card"><span>Tiến độ trung bình</span><strong>72%</strong><div><i style="width:72%"></i></div></div><div class="dashboard-mini-card card-a">{icon('book')}<span><b>18</b><small>khóa đã xuất bản</small></span></div><div class="dashboard-mini-card card-b">{icon('check')}<span><b>126</b><small>lượt hoàn thành</small></span></div><span class="dashboard-orb orb-a"></span><span class="dashboard-orb orb-b"></span></div></section>
      <section class="metric-grid">{metric_html}</section>
      <section class="dashboard-layout"><div class="dashboard-column-main"><article class="dashboard-panel"><header class="panel-heading"><div><span class="panel-kicker">Tiến độ học tập</span><h2>Các lượt học gần đây</h2></div><a>Xem tất cả {icon('arrow',15)}</a></header><div class="progress-list">{progress_html}</div></article><article class="dashboard-panel"><header class="panel-heading"><div><span class="panel-kicker">Cần xử lý</span><h2>Việc cần chú ý</h2></div></header><div class="attention-list"><a class="attention-item tone-danger"><span>3</span><strong>Lượt học quá hạn</strong>{icon('arrow',16)}</a><a class="attention-item tone-warning"><span>12</span><strong>Bài tự luận chờ chấm</strong>{icon('arrow',16)}</a><a class="attention-item tone-primary"><span>5</span><strong>Thông báo chưa đọc</strong>{icon('arrow',16)}</a></div></article></div><aside class="dashboard-column-side"><article class="dashboard-panel"><header class="panel-heading"><div><span class="panel-kicker">Truy cập nhanh</span><h2>Công việc thường dùng</h2></div></header><div class="quick-action-list"><a><span class="quick-action-icon">{icon('plus')}</span><span><strong>Tạo khóa học</strong><small>Xây dựng nội dung mới</small></span>{icon('arrow',16)}</a><a><span class="quick-action-icon">{icon('class')}</span><span><strong>Quản lý lớp</strong><small>Lịch và học viên</small></span>{icon('arrow',16)}</a><a><span class="quick-action-icon">{icon('grade')}</span><span><strong>Chấm điểm</strong><small>Xử lý bài tự luận</small></span>{icon('arrow',16)}</a></div></article><article class="dashboard-panel"><header class="panel-heading"><div><span class="panel-kicker">Cập nhật mới</span><h2>Thông báo</h2></div><span class="panel-count">5 chưa đọc</span></header><div class="dashboard-notification-list"><div class="unread"><span class="notification-avatar">{icon('bell',17)}</span><span><strong>Lớp Kỹ năng số đã đủ học viên</strong><p>Lớp sẽ bắt đầu lúc 09:00 ngày mai.</p><small>12 phút trước</small></span></div><div><span class="notification-avatar">{icon('check',17)}</span><span><strong>Đã xuất bản khóa An toàn thông tin</strong><p>Nội dung đã sẵn sàng để ghi danh.</p><small>2 giờ trước</small></span></div></div></article></aside></section>
    </div>'''
    return shell(content, active="Tổng quan")


def courses() -> str:
    cards=[]
    data=[("Kỹ năng số nền tảng","KS-101","12 bài học","76 học viên","primary"),("An toàn thông tin","AT-204","9 bài học","54 học viên","teal"),("Quản trị dự án thực hành","PM-310","16 bài học","38 học viên","violet"),("Văn hóa doanh nghiệp","VH-108","7 bài học","91 học viên","coral"),("Kỹ năng lãnh đạo","LD-225","11 bài học","42 học viên","primary"),("Phân tích dữ liệu cơ bản","DA-120","14 bài học","64 học viên","teal")]
    for idx,(name,code,lessons,learners,tone) in enumerate(data,1):
        cards.append(f'''<article class="course-card"><div class="course-cover tone-{tone}"><span class="course-card-index">{idx:02d}</span><span class="course-symbol">{icon('book',32)}</span><span class="course-color-shapes"><i></i><b></b></span></div><div class="course-card-body"><div class="course-card-top"><span class="course-code">{code}</span><span class="status-pill success">Đã xuất bản</span></div><h2>{name}</h2><p>Nội dung thực hành rõ ràng, theo dõi tiến độ và đánh giá ngay trên hệ thống.</p><div class="course-meta"><span>{icon('book',15)}{lessons}</span><span>{icon('users',15)}{learners}</span></div></div><footer class="course-card-footer"><span>Cập nhật hôm nay</span><button class="button secondary compact">Mở khóa học {icon('arrow',15)}</button></footer></article>''')
    content=f'''<header class="page-head"><div class="page-head-main"><span class="page-icon">{icon('book',25)}</span><div class="page-copy"><span class="page-eyebrow">Đào tạo</span><h1>Khóa học</h1><p>Tạo, xuất bản và theo dõi toàn bộ nội dung đào tạo trong một không gian nhất quán.</p></div></div><div class="page-head-actions"><button class="button primary">{icon('plus',18)}Tạo khóa học</button></div></header><div class="toolbar-card"><label class="search-field">{icon('search',18)}<input placeholder="Tìm theo tên hoặc mã khóa học"></label><button class="button secondary">Tất cả trạng thái</button></div><section class="course-grid">{''.join(cards)}</section>'''
    return shell(content, active="Khóa học")


def learning() -> str:
    lessons=''.join(f'<button class="player-lesson {"active" if i==3 else ""}"><span class="completion-dot {"done" if i<3 else ""}">{icon("check",14) if i<3 else i}</span><span><strong>{name}</strong><small>{mins} phút · {"Đã hoàn thành" if i<3 else "Đang học" if i==3 else "Chưa học"}</small></span></button>' for i,(name,mins) in enumerate([("Tổng quan khóa học",8),("Nhận diện nguy cơ",18),("Bảo vệ tài khoản",22),("Xử lý sự cố",25),("Bài kiểm tra cuối chương",15)],1))
    content=f'''<div class="learning-player"><header class="player-header"><div class="player-title"><button class="icon-button">{icon('arrow')}</button><div><small>AT-204</small><strong>An toàn thông tin</strong></div></div><div class="player-progress"><div class="progress-track"><span style="width:42%"></span></div><span>42% hoàn thành</span></div><button class="button secondary player-outline-toggle">{icon('menu')}Mục lục</button></header><div class="player-layout"><main class="player-content"><article class="learning-lesson"><header><span class="content-type">Bài học văn bản</span><h1>Bảo vệ tài khoản và dữ liệu cá nhân</h1><p>22 phút · Chương 2/5</p></header><div class="lesson-body"><div class="learning-rich-text"><p>Mật khẩu mạnh chỉ là bước đầu. Một tài khoản an toàn cần kết hợp xác thực nhiều lớp, thói quen kiểm tra đường dẫn và khả năng nhận biết những yêu cầu bất thường.</p><h2>Ba nguyên tắc cần ghi nhớ</h2><p><strong>1. Không dùng lại mật khẩu.</strong> Mỗi dịch vụ quan trọng nên có một mật khẩu riêng và được lưu bằng trình quản lý mật khẩu đáng tin cậy.</p><div class="learning-callout">{icon('lock',30)}<div><strong>Thực hành ngay</strong><p>Kiểm tra các tài khoản quan trọng và bật xác thực hai bước ở nơi được hỗ trợ.</p><button class="button primary">Mở bài thực hành {icon('arrow',16)}</button></div></div><p><strong>2. Xác minh trước khi thao tác.</strong> Không đăng nhập từ liên kết lạ trong email hoặc tin nhắn. Hãy mở trực tiếp trang chính thức.</p></div></div><footer class="lesson-navigation"><button class="button secondary">Bài trước</button><div><button class="button secondary">Đánh dấu hoàn thành</button><button class="button primary">Hoàn thành và tiếp tục {icon('arrow',16)}</button></div></footer></article></main><aside class="player-outline open"><header class="player-outline-header"><div><strong>Nội dung khóa học</strong><small>2/5 bài đã hoàn thành</small></div></header><div class="player-lesson-list">{lessons}</div></aside></div></div>'''
    return shell(content, active="Học tập của tôi")


def exam() -> str:
    buttons=''.join(f'<button class="{"active" if i==4 else "answered" if i<4 else ""}">{i}</button>' for i in range(1,16))
    answers=['Dùng cùng một mật khẩu mạnh cho mọi dịch vụ','Bật xác thực hai bước và dùng mật khẩu riêng','Đổi mật khẩu mỗi ngày nhưng ghi ra giấy','Chỉ đăng nhập bằng mạng Wi-Fi công cộng']
    opts=''.join(f'<label class="question-option"><input type="radio" name="q"><span>{answer}</span></label>' for answer in answers)
    content=f'''<div class="exam-taking"><header class="player-header exam-taking-header"><div><button class="icon-button">{icon('arrow')}</button><span><small>BÀI KIỂM TRA CUỐI KHÓA</small><h1>An toàn thông tin cơ bản</h1></span></div><div class="exam-session-signals"><span class="status-pill success">Đã lưu</span><div class="exam-timer">{icon('clock',19)}<span><small>Thời gian còn lại</small><strong>28:42</strong></span></div></div></header><div class="exam-progress-row"><div class="progress-track"><span style="width:27%"></span></div><span>4/15 câu</span></div><div class="exam-taking-layout"><main class="exam-question-panel"><div class="question-editor"><div class="question-editor-head"><span>Câu 4 trên 15</span><strong>1 điểm</strong></div><h2>Phương án nào giúp bảo vệ tài khoản hiệu quả nhất?</h2><div class="answer-options">{opts}</div></div><footer class="exam-actions"><button class="button secondary">Câu trước</button><div><button class="button secondary">Đánh dấu xem lại</button><button class="button primary">Lưu và sang câu tiếp {icon('arrow',16)}</button></div></footer></main><aside class="exam-navigator"><h2>Danh sách câu hỏi</h2><div>{buttons}</div><div class="exam-legend"><span><i class="answered"></i>Đã trả lời</span><span><i></i>Chưa trả lời</span></div></aside></div></div>'''
    return shell(content, active="Bài kiểm tra & kỳ thi")


def login() -> str:
    return f'''<!doctype html><html lang="vi" data-theme="unified-light"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width"><style>{{CSS}}</style></head><body><main class="auth-page"><section class="auth-showcase"><div class="auth-brand"><span class="auth-brand-mark">L</span><div><strong>LMSPilot</strong><small>Không gian học tập của tổ chức</small></div></div><div class="auth-showcase-copy"><span class="auth-pill">{icon('learn',16)} Học tập liền mạch</span><h2>Mỗi ngày tiến thêm một bước.</h2><p>Học, kiểm tra và theo dõi tiến độ trong một không gian rõ ràng, tập trung.</p></div><div class="auth-preview"><article class="auth-course-card auth-course-primary"><span class="auth-course-icon">{icon('book',22)}</span><div><small>Đang học</small><strong>Kỹ năng số nền tảng</strong></div><span class="auth-progress-value">72%</span><div class="auth-progress-track"><i></i></div></article><article class="auth-course-card auth-course-secondary"><span class="auth-course-icon">{icon('exam',22)}</span><div><small>Sắp tới</small><strong>Bài kiểm tra cuối khóa</strong></div><span class="auth-date">09:30</span></article><span class="auth-float auth-float-a">{icon('check',18)}</span><span class="auth-float auth-float-b">{icon('learn',18)}</span></div><p class="auth-privacy">{icon('lock',16)} Dữ liệu được bảo vệ trong hệ thống của tổ chức.</p></section><section class="auth-access"><div class="auth-card"><header class="auth-card-heading"><span class="auth-card-icon">{icon('lock',22)}</span><div><p class="auth-eyebrow">Chào mừng trở lại</p><h1>Đăng nhập</h1><p>Truy cập khóa học, bài kiểm tra và công việc được giao.</p></div></header><form class="auth-form"><label class="field-group"><span>Tên đăng nhập hoặc email</span><span class="input-shell">{icon('users',19)}<input value="quantri@lmspilot.vn"></span></label><label class="field-group"><span>Mật khẩu</span><span class="input-shell password-shell">{icon('lock',19)}<input type="password" value="password"><button class="password-visibility">Hiện</button></span></label><button class="button primary auth-submit">Đăng nhập {icon('arrow',18)}</button></form><p class="auth-help">Không đăng nhập được? Liên hệ quản trị viên của tổ chức để được hỗ trợ.</p></div></section></main></body></html>'''


SCREENS: list[tuple[str, Callable[[], str], tuple[int, int]]] = [
    ("01-login.png", login, (1440, 900)),
    ("02-dashboard-light.png", lambda: dashboard(), (1440, 900)),
    ("03-course-catalog.png", courses, (1440, 900)),
    ("04-learning-player.png", learning, (1440, 900)),
    ("05-exam-focus.png", exam, (1440, 900)),
    ("06-dashboard-dark.png", lambda: dashboard().replace('data-theme="unified-light"','data-theme="unified-dark"'), (1440, 900)),
    ("07-dashboard-mobile.png", lambda: dashboard(), (390, 844)),
]


async def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    TMP.mkdir(parents=True, exist_ok=True)
    css = (ROOT / "apps/web/app/globals.css").read_text(encoding="utf-8") + "\n" + (ROOT / "apps/web/app/unified.css").read_text(encoding="utf-8")
    async with async_playwright() as p:
        browser = await p.chromium.launch(executable_path="/usr/bin/chromium", headless=True, args=["--no-sandbox", "--disable-dev-shm-usage"])
        for filename, factory, viewport in SCREENS:
            page = await browser.new_page(viewport={"width": viewport[0], "height": viewport[1]}, device_scale_factor=1)
            markup = factory().replace("{CSS}", css)
            html_path = TMP / filename.replace(".png", ".html")
            html_path.write_text(markup, encoding="utf-8")
            await page.set_content(markup, wait_until="load")
            await page.screenshot(path=str(OUT / filename), full_page=False)
            await page.close()
        await browser.close()
    print(f"Rendered {len(SCREENS)} previews to {OUT}")


if __name__ == "__main__":
    asyncio.run(main())
