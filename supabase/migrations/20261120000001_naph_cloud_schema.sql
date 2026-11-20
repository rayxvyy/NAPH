-- =============================================================================
-- NAPH Cloud Sync — Supabase Free Tier PostgreSQL Schema & RLS Policies
-- Supports: Multi-Profile Household Sync, Stremio Addons, Watch Progress, Watchlist
-- =============================================================================

-- 1. Household Profiles Table
create table if not exists public.profiles (
    id text primary key,
    user_id uuid references auth.users(id) on delete cascade not null,
    name text not null,
    avatar_color_hex bigint not null default 4284704497,
    avatar_emoji text not null default '🎬',
    is_kids_profile boolean not null default false,
    pin_code text,
    preferred_audio_lang text not null default 'eng',
    preferred_sub_lang text not null default 'eng',
    auto_skip_intros boolean not null default true,
    auto_play_next_episode boolean not null default true,
    created_at timestamptz not null default now()
);

alter table public.profiles enable row level security;

create policy "Users can manage their own household profiles"
    on public.profiles for all
    to authenticated
    using ((select auth.uid()) = user_id)
    with check ((select auth.uid()) = user_id);

-- 2. Installed Stremio Addons Table
create table if not exists public.installed_addons (
    id text not null,
    user_id uuid references auth.users(id) on delete cascade not null,
    name text not null,
    version text not null default '1.0.0',
    description text not null default '',
    manifest_url text not null,
    is_enabled boolean not null default true,
    installed_at timestamptz not null default now(),
    primary key (user_id, id)
);

alter table public.installed_addons enable row level security;

create policy "Users can sync their installed addons across devices"
    on public.installed_addons for all
    to authenticated
    using ((select auth.uid()) = user_id)
    with check ((select auth.uid()) = user_id);

-- 3. Cross-Screen Watch Progress ("Continue Watching")
create table if not exists public.watch_progress (
    profile_id text not null,
    media_id text not null,
    user_id uuid references auth.users(id) on delete cascade default auth.uid(),
    media_type text not null,
    title text not null,
    poster_url text,
    backdrop_url text,
    episode_id text,
    season int,
    episode int,
    episode_title text,
    position_ms bigint not null default 0,
    duration_ms bigint not null default 0,
    updated_at_epoch_ms bigint not null,
    primary key (profile_id, media_id)
);

create index if not exists idx_watch_progress_profile_updated
    on public.watch_progress(profile_id, updated_at_epoch_ms desc);

alter table public.watch_progress enable row level security;

create policy "Users can sync watch progress across Windows, Linux, iOS, Android"
    on public.watch_progress for all
    to authenticated
    using ((select auth.uid()) = user_id)
    with check ((select auth.uid()) = user_id);

-- 4. Multi-Profile Watchlist Table
create table if not exists public.watchlist (
    profile_id text not null,
    media_id text not null,
    user_id uuid references auth.users(id) on delete cascade default auth.uid(),
    media_type text not null,
    title text not null,
    poster_url text,
    backdrop_url text,
    release_info text,
    imdb_rating text,
    added_at timestamptz not null default now(),
    primary key (profile_id, media_id)
);

alter table public.watchlist enable row level security;

create policy "Users can manage their profile watchlists"
    on public.watchlist for all
    to authenticated
    using ((select auth.uid()) = user_id)
    with check ((select auth.uid()) = user_id);
