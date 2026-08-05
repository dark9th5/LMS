"use client";

import { useMemo, useState } from "react";
import { Icon } from "./Icon";

export type DifficultyKey = "EASY" | "MEDIUM" | "HARD";
export type DifficultyDistribution = Record<DifficultyKey, number>;

export type GeneratedCitation = {
  documentVersionId: string;
  page?: number | null;
  section?: string | null;
  quote: string;
};

export type GeneratedOption = { id: string; text: string };
export type GeneratedQuestion = {
  externalId: string;
  type: "SINGLE_CHOICE" | "MULTIPLE_CHOICE" | "TRUE_FALSE";
  stem: string;
  difficulty: DifficultyKey;
  points: number;
  options: GeneratedOption[];
  correctOptionIds: string[];
  explanation: string;
  citations: GeneratedCitation[];
  tags?: string[];
};

export type GenerationJob = {
  id: string;
  status: "QUEUED" | "EXTRACTING" | "GENERATING" | "VALIDATING" | "REVIEW_REQUIRED" | "APPROVED" | "IMPORTED" | "FAILED";
  questionSet?: { questions?: GeneratedQuestion[] } | null;
  validationProblems?: Array<{ path: string; message: string }>;
  errorMessage?: string | null;
};

const PRESETS: Array<{ key: string; label: string; hint: string; value: DifficultyDistribution }> = [
  { key: "BASIC", label: "Cơ bản", hint: "60% dễ · 30% trung bình · 10% khó", value: { EASY: 60, MEDIUM: 30, HARD: 10 } },
  { key: "BALANCED", label: "Cân bằng", hint: "30% dễ · 50% trung bình · 20% khó", value: { EASY: 30, MEDIUM: 50, HARD: 20 } },
  { key: "ADVANCED", label: "Nâng cao", hint: "10% dễ · 40% trung bình · 50% khó", value: { EASY: 10, MEDIUM: 40, HARD: 50 } },
];

export function allocateQuestionCounts(total: number, distribution: DifficultyDistribution): DifficultyDistribution {
  const keys: DifficultyKey[] = ["EASY", "MEDIUM", "HARD"];
  const raw = keys.map((key) => ({ key, exact: (Math.max(0, total) * distribution[key]) / 100 }));
  const result = { EASY: 0, MEDIUM: 0, HARD: 0 } satisfies DifficultyDistribution;
  raw.forEach(({ key, exact }) => { result[key] = Math.floor(exact); });
  let remaining = Math.max(0, total) - keys.reduce((sum, key) => sum + result[key], 0);
  raw
    .map((item) => ({ ...item, fraction: item.exact - Math.floor(item.exact) }))
    .sort((a, b) => b.fraction - a.fraction || keys.indexOf(a.key) - keys.indexOf(b.key))
    .forEach(({ key }) => {
      if (remaining > 0) {
        result[key] += 1;
        remaining -= 1;
      }
    });
  return result;
}

export function DifficultyDistributionSelector({
  value,
  onChange,
  numberOfQuestions,
}: {
  value: DifficultyDistribution;
  onChange: (value: DifficultyDistribution) => void;
  numberOfQuestions: number;
}) {
  const counts = useMemo(() => allocateQuestionCounts(numberOfQuestions, value), [numberOfQuestions, value]);
  const total = value.EASY + value.MEDIUM + value.HARD;

  function update(key: DifficultyKey, next: number) {
    onChange({ ...value, [key]: Math.max(0, Math.min(100, next)) });
  }

  return (
    <section className="ai-difficulty-card" aria-label="Phân bố độ khó câu hỏi">
      <div className="section-title compact-title">
        <div>
          <h3>Mức độ câu hỏi</h3>
          <p>Chọn cấu hình nhanh hoặc tự điều chỉnh. Tổng tỷ lệ phải bằng 100%.</p>
        </div>
        <span className={`ai-total ${total === 100 ? "valid" : "invalid"}`}>Tổng {total}%</span>
      </div>
      <div className="ai-difficulty-presets">
        {PRESETS.map((preset) => {
          const selected = (Object.keys(preset.value) as DifficultyKey[]).every((key) => preset.value[key] === value[key]);
          return (
            <button
              type="button"
              key={preset.key}
              className={`ai-preset ${selected ? "selected" : ""}`}
              onClick={() => onChange(preset.value)}
            >
              <strong>{preset.label}</strong>
              <small>{preset.hint}</small>
            </button>
          );
        })}
      </div>
      <div className="ai-difficulty-grid">
        {([
          ["EASY", "Dễ", "Nắm kiến thức và nhận biết"],
          ["MEDIUM", "Trung bình", "Hiểu và vận dụng trực tiếp"],
          ["HARD", "Khó", "Phân tích, suy luận và tình huống"],
        ] as Array<[DifficultyKey, string, string]>).map(([key, label, hint]) => (
          <label className={`ai-difficulty-row difficulty-${key.toLowerCase()}`} key={key}>
            <span><strong>{label}</strong><small>{hint}</small></span>
            <span className="ai-percent-input"><input type="number" min="0" max="100" value={value[key]} onChange={(event) => update(key, Number(event.target.value) || 0)} /><b>%</b></span>
            <em>{counts[key]} câu</em>
          </label>
        ))}
      </div>
    </section>
  );
}

const DIFFICULTY_LABEL: Record<DifficultyKey, string> = {
  EASY: "Dễ",
  MEDIUM: "Trung bình",
  HARD: "Khó",
};

export function GeneratedQuestionReview({
  job,
  questions: providedQuestions,
  selectedIds,
  onToggle,
  onSelectAll,
  onQuestionChange,
}: {
  job: GenerationJob;
  questions?: GeneratedQuestion[];
  selectedIds: string[];
  onToggle: (id: string) => void;
  onSelectAll: (selected: boolean) => void;
  onQuestionChange?: (question: GeneratedQuestion) => void;
}) {
  const questions = providedQuestions ?? job.questionSet?.questions ?? [];
  const [editingIds, setEditingIds] = useState<string[]>([]);
  const selectedAll = questions.length > 0 && selectedIds.length === questions.length;

  function updateQuestion(question: GeneratedQuestion, patch: Partial<GeneratedQuestion>) {
    onQuestionChange?.({ ...question, ...patch });
  }

  function updateOption(question: GeneratedQuestion, optionId: string, text: string) {
    updateQuestion(question, { options: question.options.map((option) => option.id === optionId ? { ...option, text } : option) });
  }

  function toggleCorrect(question: GeneratedQuestion, optionId: string) {
    const multiple = question.type === "MULTIPLE_CHOICE";
    const current = question.correctOptionIds.includes(optionId);
    const next = multiple
      ? current ? question.correctOptionIds.filter((id) => id !== optionId) : [...question.correctOptionIds, optionId]
      : [optionId];
    if (next.length) updateQuestion(question, { correctOptionIds: next });
  }
  return (
    <div className="ai-review-layout">
      <div className="ai-review-toolbar">
        <div>
          <strong>{questions.length} câu AI đã sinh</strong>
          <span>{selectedIds.length} câu được chọn để nhập</span>
        </div>
        <button type="button" className="button secondary compact" onClick={() => onSelectAll(!selectedAll)}>
          <Icon name={selectedAll ? "close" : "check"} /> {selectedAll ? "Bỏ chọn tất cả" : "Chọn tất cả"}
        </button>
      </div>
      {job.validationProblems?.length ? (
        <div className="form-alert error"><Icon name="warning" />{job.validationProblems.map((problem) => problem.message).join(" · ")}</div>
      ) : null}
      <div className="ai-review-list">
        {questions.map((question, index) => {
          const selected = selectedIds.includes(question.externalId);
          const correct = new Set(question.correctOptionIds);
          const citation = question.citations?.[0];
          return (
            <article className={`ai-review-question ${selected ? "selected" : ""}`} key={question.externalId}>
              <label className="ai-review-select">
                <input type="checkbox" checked={selected} onChange={() => onToggle(question.externalId)} />
                <span>{index + 1}</span>
              </label>
              <div className="ai-review-content">
                <header>
                  <div className="ai-review-tags">
                    <span>{DIFFICULTY_LABEL[question.difficulty]}</span>
                    <span>{question.type === "TRUE_FALSE" ? "Đúng / Sai" : question.type === "MULTIPLE_CHOICE" ? "Nhiều đáp án" : "Một đáp án"}</span>
                    <span>{question.points} điểm</span>
                  </div>
                </header>
                <div className="ai-review-question-head">
                  {editingIds.includes(question.externalId) ? (
                    <textarea value={question.stem} onChange={(event) => updateQuestion(question, { stem: event.target.value })} aria-label={`Nội dung câu ${index + 1}`} />
                  ) : <h4>{question.stem}</h4>}
                  {onQuestionChange && (
                    <button type="button" className="button secondary compact" onClick={() => setEditingIds((current) => current.includes(question.externalId) ? current.filter((id) => id !== question.externalId) : [...current, question.externalId])}>
                      <Icon name="edit" /> {editingIds.includes(question.externalId) ? "Xong" : "Sửa"}
                    </button>
                  )}
                </div>
                {editingIds.includes(question.externalId) && (
                  <label className="ai-review-difficulty-edit">Độ khó<select value={question.difficulty} onChange={(event) => updateQuestion(question, { difficulty: event.target.value as DifficultyKey })}><option value="EASY">Dễ</option><option value="MEDIUM">Trung bình</option><option value="HARD">Khó</option></select></label>
                )}
                <div className="ai-review-options">
                  {question.options.map((option) => (
                    <div className={correct.has(option.id) ? "correct" : ""} key={option.id}>
                      <button type="button" className="ai-answer-key" onClick={() => editingIds.includes(question.externalId) && toggleCorrect(question, option.id)} aria-label={`Đặt ${option.id} là đáp án đúng`}>{option.id}</button>
                      {editingIds.includes(question.externalId) ? <input value={option.text} onChange={(event) => updateOption(question, option.id, event.target.value)} aria-label={`Phương án ${option.id}`} /> : <span>{option.text}</span>}
                      {correct.has(option.id) && <Icon name="check" />}
                    </div>
                  ))}
                </div>
                <div className="ai-review-explanation"><strong>Giải thích</strong>{editingIds.includes(question.externalId) ? <textarea value={question.explanation} onChange={(event) => updateQuestion(question, { explanation: event.target.value })} /> : <p>{question.explanation}</p>}</div>
                {citation && (
                  <blockquote>
                    <strong>Nguồn{citation.page ? ` · trang ${citation.page}` : citation.section ? ` · ${citation.section}` : ""}</strong>
                    <p>“{citation.quote}”</p>
                  </blockquote>
                )}
              </div>
            </article>
          );
        })}
      </div>
    </div>
  );
}
