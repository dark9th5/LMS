const labels: Record<string, string> = {
  DRAFT: "Bản nháp",
  PUBLISHED: "Đã xuất bản",
  HIDDEN: "Tạm ẩn",
  ARCHIVED: "Lưu trữ",
  OPEN: "Đang mở",
  CLOSED: "Đã đóng",
  CANCELLED: "Đã hủy",
  ACTIVE: "Hoạt động",
  INACTIVE: "Ngừng hoạt động",
  NOT_STARTED: "Chưa bắt đầu",
  IN_PROGRESS: "Đang học",
  COMPLETED: "Đã hoàn thành",
  OVERDUE: "Quá hạn",
  SUBMITTED: "Đã nộp",
  EXPIRED: "Hết giờ",
  PENDING_MANUAL: "Chờ chấm",
  REVOKED: "Đã thu hồi",
};
export function StatusBadge({ value }: { value: string }) {
  const tone = /PUBLISHED|OPEN|ACTIVE|COMPLETED/.test(value)
    ? "success"
    : /DRAFT|NOT_STARTED|IN_PROGRESS|PENDING/.test(value)
      ? "warning"
      : /CANCELLED|EXPIRED|OVERDUE|REVOKED/.test(value)
        ? "danger"
        : "neutral";
  return (
    <span className={`status-badge status-${tone}`}>
      {labels[value] ?? value}
    </span>
  );
}
