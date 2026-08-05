# LMSPilot 0.8.2 — Verified-flow hardening release candidate

0.8.2 tập trung biến các luồng Assignment/Exam/File/News từ hợp đồng rời rạc thành luồng dữ liệu thật, exact-scope và nhất quán phía máy chủ.

Điểm chính:

- Server-verified completion cho Assignment/Exam; assignment chỉ hoàn thành sau chấm chấp nhận.
- Exam session/attempt/grade/report gắn exact enrollment/course/lesson và được khóa chống race.
- Shuffle, auto/manual grade, HIGHEST/LATEST/AVERAGE và effective result hoạt động trong service/event consumer.
- Hàng đợi chấm assignment theo class scope; giao diện exam/assignment grading đầy đủ hơn.
- File ACL theo file-người dùng cho course/assignment/news; kiểm owner/purpose trước khi attach.
- HTML news sanitizer allowlist idempotent và luồng attachment thật.
- Domain event gửi sau DB commit; 96 regression test, frontend build và npm audit đạt.

Đây vẫn là release candidate. Xem `docs/AUDIT_0.8.2.md` và `docs/BUILD_VERIFICATION_0.8.2.md` trước khi nghiệm thu.
