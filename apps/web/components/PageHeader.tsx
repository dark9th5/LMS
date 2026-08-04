import Link from "next/link";
import type { IconName } from "@/lib/types";
import { Icon } from "./Icon";

export function PageHeader({
  eyebrow,
  title,
  description,
  icon,
  backHref,
  actions,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  icon?: IconName;
  backHref?: string;
  actions?: React.ReactNode;
}) {
  return (
    <header className="page-header cosmic-page-header">
      <div className="page-header-orbit" aria-hidden="true">
        <i />
        <i />
        <span />
      </div>
      <div className="page-header-index" aria-hidden="true">
        <span>LP</span>
        <b>↗</b>
      </div>
      <div className="page-heading">
        {backHref ? (
          <Link
            className="icon-button page-back"
            href={backHref}
            aria-label="Quay lại"
          >
            <Icon name="back" />
          </Link>
        ) : icon ? (
          <span className="page-heading-icon">
            <Icon name={icon} size={28} />
          </span>
        ) : null}
        <div className="page-heading-copy">
          {eyebrow && <span className="section-eyebrow">{eyebrow}</span>}
          <h1>{title}</h1>
          {description && <p>{description}</p>}
          <div className="header-tags" aria-hidden="true">
            <span>DỮ LIỆU TRỰC TIẾP</span>
            <span>ĐÚNG PHẠM VI</span>
            <span>BẢO MẬT NỘI BỘ</span>
          </div>
        </div>
      </div>
      <div className="page-header-sculpture" aria-hidden="true">
        <span />
        <i />
        <b />
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </header>
  );
}
