"use client";

import { useEffect, useState } from "react";

export function NumberStepper({
  name,
  defaultValue,
  value: controlledValue,
  onChange,
  min = 0,
  max = 9999,
  step = 1,
  ariaLabel,
}: {
  name: string;
  defaultValue: number;
  value?: number;
  onChange?: (value: number) => void;
  min?: number;
  max?: number;
  step?: number;
  ariaLabel: string;
}) {
  const [internalValue, setInternalValue] = useState(defaultValue);
  const clamp = (next: number) => Math.min(max, Math.max(min, next));
  const value = controlledValue ?? internalValue;

  useEffect(() => {
    if (controlledValue === undefined) setInternalValue(clamp(defaultValue));
  }, [defaultValue, min, max, controlledValue]);

  function update(next: number) {
    const clamped = clamp(next);
    if (controlledValue === undefined) setInternalValue(clamped);
    onChange?.(clamped);
  }

  return (
    <div className="number-stepper">
      <button type="button" onClick={() => update(value - step)} disabled={value <= min} aria-label={`Giảm ${ariaLabel}`}>−</button>
      <input name={name} type="number" min={min} max={max} step={step} value={value} onChange={(event) => update(Number(event.target.value) || min)} aria-label={ariaLabel} />
      <button type="button" onClick={() => update(value + step)} disabled={value >= max} aria-label={`Tăng ${ariaLabel}`}>+</button>
    </div>
  );
}
