ALTER TABLE branding_profiles
    DROP CONSTRAINT IF EXISTS ck_branding_theme_key;

-- Preserve custom palettes. Only normalize the theme selector.
UPDATE branding_profiles
SET theme_key = CASE
    WHEN theme_key IN ('executive-midnight', 'digital-grid', 'unified-dark')
        THEN 'unified-dark'
    ELSE 'unified-light'
END;

ALTER TABLE branding_profiles
    ALTER COLUMN theme_key SET DEFAULT 'unified-light';

ALTER TABLE branding_profiles
    ADD CONSTRAINT ck_branding_theme_key CHECK (
        theme_key IN ('unified-light', 'unified-dark')
    );
