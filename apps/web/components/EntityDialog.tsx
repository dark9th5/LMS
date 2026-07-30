"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";

export type FormOption = { label: string; value: string };
export type FormField = {
  name: string;
  label: string;
  type?: "text" | "password" | "email" | "number" | "date" | "select" | "textarea";
  required?: boolean;
  placeholder?: string;
  defaultValue?: string;
  options?: FormOption[];
  min?: number;
  max?: number;
};

type Props = {
  open: boolean;
  title: string;
  description?: string;
  submitLabel?: string;
  fields: FormField[];
  busy?: boolean;
  error?: string;
  onClose: () => void;
  onSubmit: (values: Record<string, string>) => Promise<void> | void;
};

export function EntityDialog({
  open,
  title,
  description,
  submitLabel = "Lưu",
  fields,
  busy = false,
  error,
  onClose,
  onSubmit,
}: Props) {
  const initialValues = useMemo(
    () => Object.fromEntries(fields.map((field) => [field.name, field.defaultValue ?? ""])),
    [fields],
  );
  const [values, setValues] = useState<Record<string, string>>(initialValues);

  useEffect(() => {
    if (open) setValues(initialValues);
  }, [open, initialValues]);

  if (!open) return null;

  async function submit(event: FormEvent) {
    event.preventDefault();
    await onSubmit(values);
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="entity-dialog-title">
        <header className="modal-head">
          <div>
            <h2 id="entity-dialog-title">{title}</h2>
            {description && <p>{description}</p>}
          </div>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Đóng">×</button>
        </header>
        <form onSubmit={submit} className="entity-form">
          <div className="form-grid">
            {fields.map((field) => (
              <label key={field.name} className={field.type === "textarea" ? "field-wide" : undefined}>
                <span>{field.label}{field.required && <b> *</b>}</span>
                {field.type === "select" ? (
                  <select
                    required={field.required}
                    value={values[field.name] ?? ""}
                    onChange={(event) => setValues((current) => ({ ...current, [field.name]: event.target.value }))}
                  >
                    <option value="">Chọn giá trị</option>
                    {field.options?.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                  </select>
                ) : field.type === "textarea" ? (
                  <textarea
                    required={field.required}
                    placeholder={field.placeholder}
                    value={values[field.name] ?? ""}
                    onChange={(event) => setValues((current) => ({ ...current, [field.name]: event.target.value }))}
                  />
                ) : (
                  <input
                    type={field.type ?? "text"}
                    required={field.required}
                    placeholder={field.placeholder}
                    min={field.min}
                    max={field.max}
                    value={values[field.name] ?? ""}
                    onChange={(event) => setValues((current) => ({ ...current, [field.name]: event.target.value }))}
                  />
                )}
              </label>
            ))}
          </div>
          {error && <div className="form-error">{error}</div>}
          <footer className="modal-actions">
            <button type="button" className="soft-button" onClick={onClose} disabled={busy}>Hủy</button>
            <button type="submit" className="primary-button" disabled={busy}>{busy ? "Đang lưu..." : submitLabel}</button>
          </footer>
        </form>
      </section>
    </div>
  );
}
