"use client";

import { useEffect, useId, useState } from "react";

export function RepeatableField({
  name,
  initialValues = [""],
  addLabel = "Thêm dòng",
  placeholder,
  required = false,
  minItems = 1,
  maxItems = 50,
  onValuesChange,
}: {
  name: string;
  initialValues?: string[];
  addLabel?: string;
  placeholder?: string;
  required?: boolean;
  minItems?: number;
  maxItems?: number;
  onValuesChange?: (values: string[]) => void;
}) {
  const baseId = useId();
  const normalized = initialValues.length ? initialValues : [""];
  const [values, setValues] = useState(normalized);
  const initialSignature = initialValues.join("\u0000");

  useEffect(() => {
    setValues(initialValues.length ? initialValues : [""]);
    // The signature prevents reset loops when callers create an equivalent array.
  }, [initialSignature]);

  function commit(next: string[]) {
    setValues(next);
    onValuesChange?.(next);
  }

  function update(index: number, value: string) {
    commit(values.map((item, itemIndex) => (itemIndex === index ? value : item)));
  }

  function add() {
    if (values.length < maxItems) commit([...values, ""]);
  }

  function remove(index: number) {
    if (values.length <= minItems) {
      commit(values.map((item, itemIndex) => (itemIndex === index ? "" : item)));
      return;
    }
    commit(values.filter((_, itemIndex) => itemIndex !== index));
  }

  return (
    <div className="repeatable-field">
      {values.map((value, index) => (
        <div className="repeatable-row" key={`${baseId}-${index}`}>
          <input
            id={`${baseId}-${index}`}
            name={name}
            value={value}
            onChange={(event) => update(index, event.target.value)}
            placeholder={placeholder}
            required={required && index === 0}
            aria-label={`${placeholder || name} ${index + 1}`}
          />
          <button
            className="repeatable-remove"
            type="button"
            onClick={() => remove(index)}
            aria-label={`Xóa dòng ${index + 1}`}
            title="Xóa dòng"
          >
            −
          </button>
        </div>
      ))}
      <button
        className="repeatable-add"
        type="button"
        onClick={add}
        disabled={values.length >= maxItems}
      >
        + {addLabel}
      </button>
    </div>
  );
}
