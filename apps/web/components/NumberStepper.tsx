"use client";

import { useEffect, useState } from "react";

export function NumberStepper({
  name,
  defaultValue,
  min = 0,
  max = 9999,
  step = 1,
  ariaLabel,
}: {
  name: string;
  defaultValue: number;
  min?: number;
  max?: number;
  step?: number;
  ariaLabel: string;
}) {
  const [value, setValue] = useState(defaultValue);
  const clamp = (next: number) => Math.min(max, Math.max(min, next));

  useEffect(() => {
    setValue(clamp(defaultValue));
  }, [defaultValue, min, max]);

  return (
    <div className="number-stepper">
      <button
        type="button"
        onClick={() => setValue((current) => clamp(current - step))}
        disabled={value <= min}
        aria-label={`Giảm ${ariaLabel}`}
      >
        −
      </button>
      <input
        name={name}
        type="number"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={(event) => setValue(clamp(Number(event.target.value) || min))}
        aria-label={ariaLabel}
      />
      <button
        type="button"
        onClick={() => setValue((current) => clamp(current + step))}
        disabled={value >= max}
        aria-label={`Tăng ${ariaLabel}`}
      >
        +
      </button>
    </div>
  );
}
