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
    <header className="page-header celestial-page-header">
      <div className="page-header-map" aria-hidden="true">
        <i />
        <i />
        <i />
        <span>✦</span>
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
            <Icon name={icon} />
            <i aria-hidden="true" />
          </span>
        ) : null}
        <div className="page-heading-copy">
          {eyebrow && (
            <span className="section-eyebrow">
              <i /> {eyebrow} <b />
            </span>
          )}
          <h1>{title}</h1>
          {description && <p>{description}</p>}
          <div className="page-coordinate" aria-hidden="true">
            <span>CLS · KNOWLEDGE REALM</span>
            <i />
          </div>
        </div>
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </header>
  );
}
