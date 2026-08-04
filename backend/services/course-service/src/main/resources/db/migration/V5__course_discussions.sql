create table if not exists discussion_threads (
    id uuid primary key,
    course_id uuid not null references courses(id) on delete cascade,
    lesson_id uuid,
    title varchar(240) not null,
    author_id uuid not null,
    status varchar(20) not null,
    pinned boolean not null default false,
    post_count integer not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint ck_discussion_post_count check (post_count >= 0)
);
create table if not exists discussion_posts (
    id uuid primary key,
    thread_id uuid not null references discussion_threads(id) on delete cascade,
    author_id uuid not null,
    parent_post_id uuid references discussion_posts(id),
    content text not null,
    status varchar(20) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0
);
create index if not exists idx_discussion_thread_course on discussion_threads(course_id, updated_at desc);
create index if not exists idx_discussion_post_thread on discussion_posts(thread_id, created_at asc);
