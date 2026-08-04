export function MysticBackdrop({ compact = false }: { compact?: boolean }) {
  return (
    <div
      className={`mystic-backdrop ${compact ? "mystic-backdrop--compact" : ""}`}
      aria-hidden="true"
    >
      <div className="cosmic-vignette" />
      <div className="aurora-ribbon aurora-ribbon-one" />
      <div className="aurora-ribbon aurora-ribbon-two" />
      <div className="aether aether-one" />
      <div className="aether aether-two" />
      <div className="aether aether-three" />
      <div className="star-field star-field-far" />
      <div className="star-field star-field-near" />
      <div className="mystic-grid" />
      <div className="celestial-disc">
        <i />
        <b />
        <span>✦</span>
      </div>
      <svg
        className="constellation constellation-one"
        viewBox="0 0 360 220"
        fill="none"
      >
        <path d="M18 168L82 104L145 128L214 42L338 88" />
        <circle cx="18" cy="168" r="3" />
        <circle cx="82" cy="104" r="4" />
        <circle cx="145" cy="128" r="3" />
        <circle cx="214" cy="42" r="5" />
        <circle cx="338" cy="88" r="3" />
      </svg>
      <svg
        className="constellation constellation-two"
        viewBox="0 0 280 260"
        fill="none"
      >
        <path d="M18 36L104 84L76 182L190 222L260 142" />
        <circle cx="18" cy="36" r="3" />
        <circle cx="104" cy="84" r="4" />
        <circle cx="76" cy="182" r="3" />
        <circle cx="190" cy="222" r="4" />
        <circle cx="260" cy="142" r="3" />
      </svg>
      <svg
        className="constellation constellation-three"
        viewBox="0 0 440 160"
        fill="none"
      >
        <path d="M12 118L74 42L151 78L218 24L309 83L426 38" />
        <path d="M74 42L108 142L218 24L267 136L309 83" />
        <circle cx="12" cy="118" r="2" />
        <circle cx="74" cy="42" r="4" />
        <circle cx="151" cy="78" r="2" />
        <circle cx="218" cy="24" r="5" />
        <circle cx="309" cy="83" r="3" />
        <circle cx="426" cy="38" r="2" />
      </svg>
      <div className="rune-dust">
        <span>ᚨ</span>
        <span>◈</span>
        <span>ᛟ</span>
        <span>✧</span>
        <span>ᚱ</span>
      </div>
    </div>
  );
}
