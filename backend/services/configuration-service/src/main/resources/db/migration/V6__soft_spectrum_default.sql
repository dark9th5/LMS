ALTER TABLE branding_profiles
    DROP CONSTRAINT IF EXISTS ck_branding_theme_key;

-- Only replace palettes known to be previous untouched defaults. Custom branding survives.
UPDATE branding_profiles
SET primary_color = '#B95547',
    secondary_color = '#5967B8',
    background_color = '#F6F3EF',
    text_color = '#20232E'
WHERE theme_key = 'enterprise-blue'
  AND (
      (
          upper(primary_color) = '#2563EB'
          AND upper(secondary_color) = '#0F766E'
          AND upper(background_color) = '#F3F6FB'
          AND upper(text_color) = '#172033'
      )
      OR (
          upper(primary_color) = '#8B5CF6'
          AND upper(secondary_color) = '#14B8A6'
          AND upper(background_color) = '#080B18'
          AND upper(text_color) = '#F7F4FF'
      )
  );

UPDATE branding_profiles
SET theme_key = 'soft-spectrum'
WHERE theme_key = 'enterprise-blue';

-- Neutralize the original fantasy seed only when it was never renamed.
UPDATE branding_profiles
SET system_name = 'LMSPilot',
    introduction = 'Không gian học tập và phát triển dành cho mọi thành viên trong tổ chức.'
WHERE profile_key = 'default'
  AND system_name = 'Học viện Huyền Tri';

ALTER TABLE branding_profiles
    ALTER COLUMN theme_key SET DEFAULT 'soft-spectrum';

ALTER TABLE branding_profiles
    ADD CONSTRAINT ck_branding_theme_key CHECK (
        theme_key IN (
            'soft-spectrum',
            'executive-midnight',
            'heritage-academy',
            'bright-school',
            'civic-trust',
            'creative-pop',
            'nature-learning',
            'editorial-burgundy',
            'minimal-calm',
            'digital-grid'
        )
    );
