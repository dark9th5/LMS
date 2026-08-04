ALTER TABLE branding_profiles
    DROP CONSTRAINT IF EXISTS ck_branding_theme_key;

UPDATE branding_profiles
SET theme_key = CASE theme_key
    WHEN 'cosmic-observatory' THEN 'executive-midnight'
    WHEN 'quantum-cyan' THEN 'digital-grid'
    WHEN 'lunar-silver' THEN 'minimal-calm'
    WHEN 'aurora-research' THEN 'nature-learning'
    WHEN 'mars-expedition' THEN 'editorial-burgundy'
    WHEN 'abyssal-ocean' THEN 'civic-trust'
    WHEN 'biosphere-lab' THEN 'nature-learning'
    WHEN 'solar-archive' THEN 'heritage-academy'
    WHEN 'neo-academia' THEN 'enterprise-blue'
    WHEN 'mono-terminal' THEN 'digital-grid'
    ELSE 'enterprise-blue'
END;

ALTER TABLE branding_profiles
    ALTER COLUMN theme_key SET DEFAULT 'enterprise-blue';

ALTER TABLE branding_profiles
    ADD CONSTRAINT ck_branding_theme_key CHECK (
        theme_key IN (
            'enterprise-blue',
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
