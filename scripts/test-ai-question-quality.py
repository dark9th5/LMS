#!/usr/bin/env python3
"""Offline quality test for the OpenAI-compatible question-generation contract.

This test intentionally uses a local mock model endpoint. It validates document extraction,
provider response parsing, exact difficulty allocation, source-grounded citations, duplicates,
and answer/explanation completeness without requiring a paid API key.
"""
from __future__ import annotations

import json
import re
import threading
import urllib.request
from collections import Counter
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from typing import Any

import fitz
from docx import Document

ROOT = Path(__file__).resolve().parents[1]
FIXTURES = ROOT / "backend/services/ai-service/src/test/resources/fixtures"
OUTPUT = ROOT / "docs/ai-quality/0.20.3"
DOC_IDS = {
    "pdf": "11111111-1111-1111-1111-111111111111",
    "docx": "22222222-2222-2222-2222-222222222222",
}
DISTRIBUTION = {"EASY": 30, "MEDIUM": 50, "HARD": 20}


def normalize(value: str) -> str:
    return re.sub(r"\s+", " ", value.lower().replace("“", '"').replace("”", '"')).strip()


def extract_pdf(path: Path) -> list[dict[str, Any]]:
    doc = fitz.open(path)
    return [{"documentVersionId": DOC_IDS["pdf"], "page": i + 1, "text": page.get_text()} for i, page in enumerate(doc)]


def extract_docx(path: Path) -> list[dict[str, Any]]:
    document = Document(path)
    text = "\n".join(paragraph.text for paragraph in document.paragraphs if paragraph.text.strip())
    return [{"documentVersionId": DOC_IDS["docx"], "page": None, "text": text}]


SOURCE_SENTENCES = [
    "Mật khẩu mạnh phải có độ dài tối thiểu 12 ký tự và kết hợp chữ hoa, chữ thường, chữ số và ký tự đặc biệt.",
    "Xác thực đa yếu tố bổ sung một lớp bảo vệ ngoài mật khẩu và làm giảm nguy cơ tài khoản bị chiếm đoạt.",
    "Nguyên tắc đặc quyền tối thiểu yêu cầu người dùng chỉ được cấp đúng quyền cần thiết để hoàn thành công việc.",
    "Dữ liệu nhạy cảm phải được mã hóa khi lưu trữ và khi truyền qua mạng.",
    "Nhân viên phải báo cáo email đáng ngờ cho bộ phận an toàn thông tin và không nhấp vào liên kết không xác định.",
    "Sao lưu dữ liệu cần được thực hiện định kỳ và bản sao lưu phải được kiểm tra khả năng khôi phục.",
]


def q(index: int, difficulty: str, stem: str, options: list[str], correct: str, explanation: str, quote_index: int, qtype: str = "SINGLE_CHOICE") -> dict[str, Any]:
    ids = [chr(65 + i) for i in range(len(options))]
    return {
        "externalId": f"q-{index}",
        "type": qtype,
        "stem": stem,
        "difficulty": difficulty,
        "points": 1 if difficulty != "HARD" else 2,
        "options": [{"id": option_id, "text": text} for option_id, text in zip(ids, options)],
        "correctOptionIds": [correct],
        "explanation": explanation,
        "tags": ["an-toan-thong-tin"],
        "citations": [{
            "documentVersionId": DOC_IDS["pdf"] if quote_index < 3 else DOC_IDS["docx"],
            "page": 1 if quote_index < 3 else None,
            "section": "An toàn thông tin cơ bản",
            "quote": SOURCE_SENTENCES[quote_index],
        }],
    }


def build_question_set() -> dict[str, Any]:
    questions = [
        q(1, "EASY", "Mật khẩu mạnh trong tài liệu phải có độ dài tối thiểu bao nhiêu ký tự?", ["8 ký tự", "10 ký tự", "12 ký tự", "16 ký tự"], "C", "Tài liệu quy định độ dài tối thiểu là 12 ký tự.", 0),
        q(2, "EASY", "Xác thực đa yếu tố bổ sung lớp bảo vệ nào?", ["Một lớp ngoài mật khẩu", "Chỉ thay đổi tên tài khoản", "Loại bỏ mọi mật khẩu", "Tắt phân quyền"], "A", "Xác thực đa yếu tố bổ sung một lớp bảo vệ ngoài mật khẩu.", 1),
        q(3, "EASY", "Dữ liệu nhạy cảm cần được mã hóa khi nào?", ["Chỉ khi lưu trữ", "Chỉ khi truyền", "Khi lưu trữ và khi truyền", "Không cần mã hóa"], "C", "Tài liệu yêu cầu mã hóa ở cả hai trạng thái: lưu trữ và truyền qua mạng.", 3),
        q(4, "MEDIUM", "Một nhân viên được cấp quyền quản trị dù chỉ cần xem báo cáo. Nguyên tắc nào đang bị vi phạm?", ["Đặc quyền tối thiểu", "Sao lưu định kỳ", "Mã hóa khi truyền", "Xác thực đa yếu tố"], "A", "Quyền quản trị vượt quá nhu cầu công việc, trái với nguyên tắc đặc quyền tối thiểu.", 2),
        q(5, "MEDIUM", "Khi nhận email có liên kết không xác định, hành động phù hợp nhất là gì?", ["Nhấp để kiểm tra", "Chuyển cho mọi đồng nghiệp", "Báo cáo bộ phận an toàn thông tin và không nhấp liên kết", "Trả lời người gửi để hỏi mật khẩu"], "C", "Tài liệu hướng dẫn báo cáo email đáng ngờ và không nhấp vào liên kết không xác định.", 4),
        q(6, "MEDIUM", "Nhận định 'Có bản sao lưu là đủ, không cần thử khôi phục' đúng hay sai?", ["Đúng", "Sai"], "B", "Sai, vì tài liệu yêu cầu bản sao lưu phải được kiểm tra khả năng khôi phục.", 5, "TRUE_FALSE"),
        q(7, "MEDIUM", "Biện pháp nào trực tiếp làm giảm nguy cơ tài khoản bị chiếm đoạt ngoài việc dùng mật khẩu mạnh?", ["Xác thực đa yếu tố", "Tăng quyền quản trị", "Tắt mã hóa", "Không sao lưu"], "A", "Xác thực đa yếu tố bổ sung lớp bảo vệ ngoài mật khẩu và giảm nguy cơ chiếm đoạt tài khoản.", 1),
        q(8, "MEDIUM", "Cấu hình nào phù hợp nhất với nguyên tắc đặc quyền tối thiểu?", ["Mọi người đều là quản trị viên", "Chỉ cấp đúng quyền cần cho công việc", "Dùng chung một tài khoản", "Không phân quyền dữ liệu"], "B", "Nguyên tắc đặc quyền tối thiểu yêu cầu chỉ cấp đúng quyền cần thiết.", 2),
        q(9, "HARD", "Doanh nghiệp đã mã hóa dữ liệu trên máy chủ nhưng gửi dữ liệu nhạy cảm qua mạng ở dạng rõ. Đánh giá nào đúng?", ["Đã đáp ứng đầy đủ", "Chưa đáp ứng vì phải mã hóa cả khi truyền", "Chỉ cần đổi mật khẩu", "Không liên quan an toàn thông tin"], "B", "Tài liệu yêu cầu mã hóa dữ liệu nhạy cảm cả khi lưu trữ và khi truyền qua mạng.", 3),
        q(10, "HARD", "Một kế hoạch ứng phó email lừa đảo chỉ yêu cầu nhân viên xóa thư. Thành phần nào còn thiếu theo tài liệu?", ["Báo cáo cho bộ phận an toàn thông tin và tránh nhấp liên kết", "Cấp thêm quyền quản trị", "Tắt xác thực đa yếu tố", "Ngừng sao lưu dữ liệu"], "A", "Ngoài việc không tương tác với liên kết, nhân viên còn phải báo cáo email đáng ngờ cho bộ phận an toàn thông tin.", 4),
    ]
    return {
        "schemaVersion": "1.0",
        "source": {
            "courseId": "33333333-3333-3333-3333-333333333333",
            "documentVersionIds": list(DOC_IDS.values()),
            "provider": "LOCAL_OPENAI_COMPATIBLE",
            "model": "quality-mock-v1",
            "generatedAt": "2026-08-05T13:00:00Z",
        },
        "language": "vi",
        "questions": questions,
    }


class MockProvider(BaseHTTPRequestHandler):
    question_set: dict[str, Any] = {}
    request_body: dict[str, Any] = {}

    def do_POST(self) -> None:  # noqa: N802
        length = int(self.headers.get("Content-Length", "0"))
        MockProvider.request_body = json.loads(self.rfile.read(length))
        payload = {"choices": [{"message": {"content": json.dumps(MockProvider.question_set, ensure_ascii=False)}}]}
        body = json.dumps(payload, ensure_ascii=False).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *_: Any) -> None:
        return


def validate(question_set: dict[str, Any], chunks: list[dict[str, Any]]) -> tuple[list[str], dict[str, Any]]:
    errors: list[str] = []
    questions = question_set.get("questions", [])
    counts = Counter(question.get("difficulty") for question in questions)
    expected = {"EASY": 3, "MEDIUM": 5, "HARD": 2}
    if len(questions) != 10:
        errors.append(f"Số câu không đúng: {len(questions)}/10")
    if dict(counts) != expected:
        errors.append(f"Phân bố sai: {dict(counts)}, cần {expected}")

    source_by_id: dict[str, str] = {}
    for chunk in chunks:
        source_by_id[chunk["documentVersionId"]] = source_by_id.get(chunk["documentVersionId"], "") + " " + chunk["text"]
    stems: set[str] = set()
    grounded = 0
    unique_options = 0
    complete_explanations = 0
    for index, question in enumerate(questions, 1):
        stem = normalize(question.get("stem", ""))
        if stem in stems:
            errors.append(f"Câu {index} trùng nội dung")
        stems.add(stem)
        option_texts = [normalize(option.get("text", "")) for option in question.get("options", [])]
        if len(option_texts) == len(set(option_texts)) and all(option_texts):
            unique_options += 1
        else:
            errors.append(f"Câu {index} có phương án trùng/rỗng")
        option_ids = {option.get("id") for option in question.get("options", [])}
        if not set(question.get("correctOptionIds", [])).issubset(option_ids):
            errors.append(f"Câu {index} có đáp án đúng không tồn tại")
        explanation = question.get("explanation", "").strip()
        if len(explanation) >= 20:
            complete_explanations += 1
        else:
            errors.append(f"Câu {index} giải thích quá ngắn")
        citations = question.get("citations", [])
        citation_ok = bool(citations)
        for citation in citations:
            quote = normalize(citation.get("quote", ""))
            source = normalize(source_by_id.get(citation.get("documentVersionId", ""), ""))
            citation_ok = citation_ok and bool(quote) and quote in source
        if citation_ok:
            grounded += 1
        else:
            errors.append(f"Câu {index} trích dẫn không khớp nguồn")

    metrics = {
        "questions": len(questions),
        "difficulty": dict(counts),
        "groundedCitations": f"{grounded}/{len(questions)}",
        "uniqueOptions": f"{unique_options}/{len(questions)}",
        "completeExplanations": f"{complete_explanations}/{len(questions)}",
        "duplicateStems": len(questions) - len(stems),
    }
    return errors, metrics


def main() -> int:
    chunks = extract_pdf(FIXTURES / "an-toan-thong-tin.pdf") + extract_docx(FIXTURES / "an-toan-thong-tin.docx")
    combined = normalize(" ".join(chunk["text"] for chunk in chunks))
    for sentence in SOURCE_SENTENCES:
        if normalize(sentence) not in combined:
            raise AssertionError(f"Không trích xuất được câu nguồn: {sentence}")

    MockProvider.question_set = build_question_set()
    server = HTTPServer(("127.0.0.1", 0), MockProvider)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        request_payload = {
            "model": "quality-mock-v1",
            "messages": [{"role": "system", "content": "Sinh đúng 10 câu: EASY=3, MEDIUM=5, HARD=2"}],
            "temperature": 0.2,
            "response_format": {"type": "json_object"},
        }
        request = urllib.request.Request(
            f"http://127.0.0.1:{server.server_port}/chat/completions",
            data=json.dumps(request_payload).encode(),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        with urllib.request.urlopen(request, timeout=5) as response:
            provider_envelope = json.load(response)
        question_set = json.loads(provider_envelope["choices"][0]["message"]["content"])
    finally:
        server.shutdown()
        thread.join(timeout=2)

    errors, metrics = validate(question_set, chunks)
    # Negative control: a bad citation and wrong difficulty must fail.
    bad = json.loads(json.dumps(question_set))
    bad["questions"][0]["citations"][0]["quote"] = "Nội dung không có trong tài liệu"
    bad["questions"][0]["difficulty"] = "HARD"
    negative_errors, _ = validate(bad, chunks)
    if not any("trích dẫn" in error for error in negative_errors) or not any("Phân bố" in error for error in negative_errors):
        errors.append("Kiểm thử âm không phát hiện được citation/phân bố sai")

    OUTPUT.mkdir(parents=True, exist_ok=True)
    (OUTPUT / "mock-generated-question-set.json").write_text(json.dumps(question_set, ensure_ascii=False, indent=2), encoding="utf-8")
    report = [
        "# Báo cáo kiểm thử AI sinh câu hỏi 0.20.3",
        "",
        "## Phạm vi",
        "- Trích xuất một PDF 2 trang và một DOCX tiếng Việt.",
        "- Gọi endpoint OpenAI-compatible giả lập tại local `/chat/completions`.",
        "- Yêu cầu 10 câu theo phân bố 30% Dễ, 50% Trung bình, 20% Khó.",
        "- Kiểm tra trích dẫn nguyên văn, câu trùng, phương án trùng, đáp án và lời giải.",
        "",
        "## Kết quả",
    ]
    report += [f"- {key}: **{value}**" for key, value in metrics.items()]
    report += ["", f"- Kiểm thử âm phát hiện lỗi: **{'Có' if negative_errors else 'Không'}**", ""]
    if errors:
        report += ["## Lỗi", *[f"- {error}" for error in errors]]
    else:
        report += ["## Đánh giá", "Bộ câu hỏi mẫu đạt toàn bộ tiêu chí tự động. Các câu khó sử dụng tình huống nhưng đáp án vẫn bám trực tiếp tài liệu nguồn."]
    (OUTPUT / "AI_QUESTION_QUALITY_REPORT.md").write_text("\n".join(report) + "\n", encoding="utf-8")
    print(json.dumps({"passed": not errors, "metrics": metrics, "negativeErrors": negative_errors[:3]}, ensure_ascii=False, indent=2))
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
