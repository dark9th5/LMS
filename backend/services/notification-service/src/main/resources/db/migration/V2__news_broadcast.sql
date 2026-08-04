CREATE TABLE IF NOT EXISTS news_articles (
    id uuid PRIMARY KEY,
    title varchar(300) NOT NULL,
    summary varchar(1000) NULL,
    content_html text NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    audience_type varchar(30) NOT NULL DEFAULT 'SYSTEM' CHECK (audience_type IN ('SYSTEM', 'BRANCH', 'DEPARTMENT', 'GROUP')),
    audience_id uuid NULL,
    pinned boolean NOT NULL DEFAULT false,
    priority integer NOT NULL DEFAULT 0,
    acknowledgement_required boolean NOT NULL DEFAULT false,
    publish_from timestamptz NULL,
    publish_until timestamptz NULL,
    author_id uuid NOT NULL,
    published_by uuid NULL,
    published_at timestamptz NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_news_audience CHECK (
        (audience_type = 'SYSTEM' AND audience_id IS NULL) OR
        (audience_type <> 'SYSTEM' AND audience_id IS NOT NULL)
    ),
    CONSTRAINT ck_news_publish_window CHECK (publish_until IS NULL OR publish_from IS NULL OR publish_until > publish_from)
);
CREATE INDEX IF NOT EXISTS idx_news_feed ON news_articles (status, pinned DESC, priority DESC, publish_from DESC);

CREATE TABLE IF NOT EXISTS news_attachments (
    news_id uuid NOT NULL REFERENCES news_articles(id) ON DELETE CASCADE,
    file_id uuid NOT NULL,
    sort_order integer NOT NULL DEFAULT 0,
    PRIMARY KEY (news_id, file_id)
);

CREATE TABLE IF NOT EXISTS news_receipts (
    news_id uuid NOT NULL REFERENCES news_articles(id) ON DELETE CASCADE,
    user_id uuid NOT NULL,
    read_at timestamptz NOT NULL DEFAULT now(),
    acknowledged_at timestamptz NULL,
    PRIMARY KEY (news_id, user_id)
);
