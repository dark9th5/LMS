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
  return <header className="page-header">
    <div className="page-heading">
      {backHref ? <Link className="icon-button page-back" href={backHref} aria-label="Quay lại"><Icon name="back" /></Link> : icon ? <span className="page-heading-icon"><Icon name={icon} /></span> : null}
      <div>
        {eyebrow && <span className="section-eyebrow">{eyebrow}</span>}
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
    </div>
    {actions && <div className="page-actions">{actions}</div>}
  </header>;
}
