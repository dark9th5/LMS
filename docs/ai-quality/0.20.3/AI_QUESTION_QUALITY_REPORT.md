# Báo cáo kiểm thử AI sinh câu hỏi 0.20.3

## Phạm vi
- Trích xuất một PDF 2 trang và một DOCX tiếng Việt.
- Gọi endpoint OpenAI-compatible giả lập tại local `/chat/completions`.
- Yêu cầu 10 câu theo phân bố 30% Dễ, 50% Trung bình, 20% Khó.
- Kiểm tra trích dẫn nguyên văn, câu trùng, phương án trùng, đáp án và lời giải.

## Kết quả
- questions: **10**
- difficulty: **{'EASY': 3, 'MEDIUM': 5, 'HARD': 2}**
- groundedCitations: **10/10**
- uniqueOptions: **10/10**
- completeExplanations: **10/10**
- duplicateStems: **0**

- Kiểm thử âm phát hiện lỗi: **Có**

## Đánh giá
Bộ câu hỏi mẫu đạt toàn bộ tiêu chí tự động. Các câu khó sử dụng tình huống nhưng đáp án vẫn bám trực tiếp tài liệu nguồn.
