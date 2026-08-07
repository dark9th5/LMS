export function ProgressBar({
  value,
  label,
}: {
  value: number;
  label?: string;
}) {
  const safe = Math.min(100, Math.max(0, Math.round(value || 0)));
  return (
    <div className="progress-block">
      {label && (
        <div className="progress-label">
          <span>{label}</span>
          <strong>{safe}%</strong>
        </div>
      )}
      <div className="progress-track" aria-label={`${safe}%`}>
        <span style={{ width: `${safe}%` }} />
      </div>
    </div>
  );
}
