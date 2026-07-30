"use client";

import { useEffect } from "react";
import { Icon } from "./Icon";

export function Modal({ open, title, description, children, onClose, wide = false }: { open: boolean; title: string; description?: string; children: React.ReactNode; onClose: () => void; wide?: boolean }) {
  useEffect(() => {
    if (!open) return;
    const handler = (event: KeyboardEvent) => { if (event.key === "Escape") onClose(); };
    document.addEventListener("keydown", handler);
    document.body.classList.add("modal-open");
    return () => { document.removeEventListener("keydown", handler); document.body.classList.remove("modal-open"); };
  }, [open, onClose]);
  if (!open) return null;
  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
    <section className={`modal ${wide ? "modal-wide" : ""}`} role="dialog" aria-modal="true" aria-label={title}>
      <header className="modal-header"><div><h2>{title}</h2>{description && <p>{description}</p>}</div><button className="icon-button" onClick={onClose} aria-label="Đóng"><Icon name="close"/></button></header>
      <div className="modal-body">{children}</div>
    </section>
  </div>;
}
