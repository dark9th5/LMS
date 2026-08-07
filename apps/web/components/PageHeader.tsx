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
    <header className="page-head">
      <div className="page-head-main">
        {backHref ? (
          <Link
            className="icon-button page-back"
            href={backHref}
            aria-label="Quay lại"
          >
            <Icon name="back" />
          </Link>
        ) : icon ? (
          <span className="page-icon" aria-hidden="true">
            <Icon name={icon} size={25} />
          </span>
        ) : null}
        <div className="page-copy">
          {eyebrow && <span className="page-eyebrow">{eyebrow}</span>}
          <h1>{title}</h1>
          {description && <p>{description}</p>}
        </div>
      </div>
      {actions && <div className="page-head-actions">{actions}</div>}
      <div className="page-decoration" aria-hidden="true" />
    </header>
  );
}
