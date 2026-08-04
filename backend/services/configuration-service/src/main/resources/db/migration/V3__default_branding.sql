INSERT INTO branding_profiles(
    id, profile_key, system_name, introduction, primary_color, secondary_color,
    background_color, text_color, updated_by, updated_at, version
)
VALUES (
    '00000000-0000-0000-0000-000000000001', 'default', 'Học viện Huyền Tri',
    'Không gian học tập doanh nghiệp, nơi tri thức được khai mở qua từng hành trình.',
    '#8B5CF6', '#14B8A6', '#080B18', '#F7F4FF',
    '00000000-0000-0000-0000-000000000001', now(), 0
)
ON CONFLICT (profile_key) DO NOTHING;
