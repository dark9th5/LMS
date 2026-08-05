"use client";

import { useEffect, useState } from "react";
import { apiRequest } from "@/lib/api";
import { ErrorState, LoadingState } from "./Feedback";
import { Icon } from "./Icon";

type DocumentPreview = {
  fileId: string;
  originalName: string;
  paragraphs: string[];
};

export function DocxPreview({ fileId, downloadUrl, compact = false }: { fileId: string; downloadUrl: string; compact?: boolean }) {
  const [preview, setPreview] = useState<DocumentPreview | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    setError("");
    setPreview(null);
    apiRequest<DocumentPreview>(`/api/v1/files/${fileId}/docx-preview`)
      .then((value) => active && setPreview(value))
      .catch((caught) => active && setError(caught instanceof Error ? caught.message : "Không thể xem trước DOCX"));
    return () => { active = false; };
  }, [fileId]);

  if (error) {
    return <div className="resource-viewer docx"><ErrorState message={error} /><a className="button secondary compact" href={downloadUrl}><Icon name="download" />Tải DOCX</a></div>;
  }
  if (!preview) return <LoadingState label="Đang mở tài liệu DOCX..." />;

  return (
    <div className={`resource-viewer docx ${compact ? "compact" : ""}`}>
      <header className="docx-preview-header"><span><Icon name="file" /></span><div><strong>{preview.originalName}</strong><small>Xem trực tiếp trong khóa học</small></div><a className="button secondary compact" href={downloadUrl}><Icon name="download" />Tải DOCX</a></header>
      <article className="docx-page" aria-label={`Nội dung ${preview.originalName}`}>
        {preview.paragraphs.length ? preview.paragraphs.map((paragraph, index) => <p key={`${index}-${paragraph.slice(0, 24)}`}>{paragraph}</p>) : <p>Tài liệu không có đoạn văn bản có thể hiển thị.</p>}
      </article>
    </div>
  );
}
