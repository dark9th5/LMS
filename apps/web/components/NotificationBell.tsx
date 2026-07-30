"use client";

import { useEffect, useState } from "react";
import { apiRequest } from "@/lib/api";
import { Icon } from "./Icon";

type NotificationItem = { id: string; title: string; body: string; read: boolean; createdAt: string };
type NotificationSummary = { unread: number; items: NotificationItem[] };

function when(value: string): string {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return "";
  return new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(parsed);
}

export function NotificationBell() {
  const [summary, setSummary] = useState<NotificationSummary>({ unread: 0, items: [] });
  const [open, setOpen] = useState(false);

  async function load() {
    try { setSummary(await apiRequest<NotificationSummary>("/api/v1/notifications")); } catch { /* Notification failure must not block the portal. */ }
  }

  useEffect(() => { void load(); }, []);

  async function markRead(item: NotificationItem) {
    if (!item.read) {
      try {
        await apiRequest(`/api/v1/notifications/${item.id}/read`, { method: "PUT" });
        setSummary((current) => ({
          unread: Math.max(0, current.unread - 1),
          items: current.items.map((value) => value.id === item.id ? { ...value, read: true } : value),
        }));
      } catch { return; }
    }
  }

  return <div className="notification-wrap">
    <button className="icon-button" type="button" aria-label="Thông báo" aria-expanded={open} onClick={() => { setOpen((value) => !value); if (!open) void load(); }}>
      <Icon name="bell" />{summary.unread > 0 && <span className="badge">{summary.unread > 9 ? "9+" : summary.unread}</span>}
    </button>
    {open && <div className="notification-menu">
      <div className="notification-head"><div><b>Thông báo</b><span>{summary.unread} chưa đọc</span></div><button type="button" onClick={() => setOpen(false)}>×</button></div>
      <div className="notification-list">{summary.items.length ? summary.items.slice(0, 8).map((item) => <button type="button" className={`notification-item ${item.read ? "" : "unread"}`} key={item.id} onClick={() => void markRead(item)}><span className="notification-dot" /><span><b>{item.title}</b><p>{item.body}</p><small>{when(item.createdAt)}</small></span></button>) : <div className="notification-empty">Chưa có thông báo mới.</div>}</div>
    </div>}
  </div>;
}
