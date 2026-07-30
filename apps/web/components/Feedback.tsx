"use client";

import { useEffect } from "react";
import { Icon } from "./Icon";

export function LoadingState({ label = "Đang tải dữ liệu từ hệ thống..." }: { label?: string }) {
  return <div className="state-box"><span className="loader"/><strong>Đang xử lý</strong><p>{label}</p></div>;
}

export function EmptyState({ title, description, action }: { title: string; description: string; action?: React.ReactNode }) {
  return <div className="state-box empty-state"><span className="state-icon"><Icon name="book" size={30}/></span><strong>{title}</strong><p>{description}</p>{action}</div>;
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return <div className="state-box error-state"><span className="state-icon"><Icon name="warning" size={30}/></span><strong>Không thể tải dữ liệu</strong><p>{message}</p>{onRetry && <button className="button secondary" onClick={onRetry}><Icon name="refresh"/>Thử lại</button>}</div>;
}

export function Toast({ message, tone = "success", onClose }: { message: string; tone?: "success" | "error" | "info"; onClose: () => void }) {
  useEffect(() => {
    const timer = window.setTimeout(onClose, 3200);
    return () => window.clearTimeout(timer);
  }, [onClose]);
  return <div className={`toast toast-${tone}`} role="status"><Icon name={tone === "error" ? "warning" : "check"}/><span>{message}</span><button onClick={onClose} aria-label="Đóng"><Icon name="close" size={17}/></button></div>;
}
