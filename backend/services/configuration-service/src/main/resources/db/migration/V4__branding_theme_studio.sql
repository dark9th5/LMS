ALTER TABLE branding_profiles
    ADD COLUMN theme_key varchar(64) NOT NULL DEFAULT 'cosmic-observatory';

ALTER TABLE branding_profiles
    ADD CONSTRAINT ck_branding_theme_key CHECK (
        theme_key IN (
            'cosmic-observatory',
            'quantum-cyan',
            'lunar-silver',
            'aurora-research',
            'mars-expedition',
            'abyssal-ocean',
            'biosphere-lab',
            'solar-archive',
            'neo-academia',
            'mono-terminal'
        )
    );
