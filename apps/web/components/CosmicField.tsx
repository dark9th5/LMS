export function CosmicField({ compact = false }: { compact?: boolean }) {
  return (
    <div
      className={`cosmic-field ${compact ? "cosmic-field--compact" : ""}`}
      aria-hidden="true"
    >
      <div className="cosmic-stars stars-near" />
      <div className="cosmic-stars stars-far" />
      <div className="cosmic-grid" />
      <div className="cosmic-nebula nebula-blue" />
      <div className="cosmic-nebula nebula-violet" />
      <div className="cosmic-horizon" />
      <div className="orbital-telemetry">
        <i className="orbit-track orbit-track-a" />
        <i className="orbit-track orbit-track-b" />
        <i className="orbit-object" />
        <span>47.3769° N</span>
        <b>LINK / 01</b>
      </div>
    </div>
  );
}
