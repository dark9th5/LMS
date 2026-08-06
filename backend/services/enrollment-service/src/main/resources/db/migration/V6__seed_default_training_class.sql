INSERT INTO training_classes (id, code, name, course_id, course_version, status, created_by, created_at, updated_at, version)
VALUES (
    '00000000-0000-0000-0000-000000000100',
    'CLASS-DEFAULT',
    'Lớp học mặc định',
    '00000000-0000-0000-0002-000000000001',
    1,
    'ACTIVE',
    '00000000-0000-0000-0000-000000000001',
    now(),
    now(),
    0
)
ON CONFLICT (id) DO NOTHING;
