import type { LessonType } from "@/lib/models";
import { Icon } from "./Icon";
import { DocxPreview } from "./DocxPreview";

export function LessonResource({ fileId, type, compact = false }: { fileId: string; type: LessonType; compact?: boolean }) {
  const inlineUrl = `/api/gateway/api/v1/files/${fileId}/content?inline=true`;
  const downloadUrl = `/api/gateway/api/v1/files/${fileId}/content`;

  if (type === "VIDEO") return <div className={`resource-viewer ${compact ? "compact" : ""}`}>
    <video controls preload="metadata" src={inlineUrl}>Trình duyệt không hỗ trợ phát video.</video>
    <ResourceActions downloadUrl={downloadUrl} label="Tải video" />
  </div>;

  if (type === "AUDIO") return <div className={`resource-viewer audio ${compact ? "compact" : ""}`}>
    <div className="resource-audio-copy"><span><Icon name="play" size={26}/></span><div><strong>Nội dung âm thanh</strong><p>Phát trực tiếp từ kho tệp nội bộ.</p></div></div>
    <audio controls preload="metadata" src={inlineUrl}>Trình duyệt không hỗ trợ phát âm thanh.</audio>
    <ResourceActions downloadUrl={downloadUrl} label="Tải âm thanh" />
  </div>;

  if (type === "PDF") return <div className={`resource-viewer pdf ${compact ? "compact" : ""}`}>
    <iframe src={inlineUrl} title="Tài liệu PDF của bài học" />
    <ResourceActions downloadUrl={downloadUrl} label="Tải PDF" />
  </div>;

  if (type === "DOCX") return <DocxPreview fileId={fileId} downloadUrl={downloadUrl} compact={compact} />;

  return <div className="file-preview"><span><Icon name="file" size={34}/></span><div><strong>Tài nguyên bài học</strong><p>Tệp được lưu trong File Storage Service và chỉ tải qua quyền của người dùng.</p></div><a className="button primary" href={downloadUrl}><Icon name="download"/>Tải tệp</a></div>;
}

function ResourceActions({ downloadUrl, label }: { downloadUrl: string; label: string }) {
  return <div className="resource-actions"><a className="button secondary compact" href={downloadUrl}><Icon name="download"/>{label}</a></div>;
}
