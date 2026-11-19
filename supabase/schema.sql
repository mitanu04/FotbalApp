-- Fluierul Final: Supabase (Postgres) schema.
-- Run once in the Supabase dashboard: SQL Editor -> New query -> paste -> Run.
-- Safe to run again: everything is "if not exists" / "or replace".
--
-- Row Level Security (RLS) replaces Firestore rules:
--   * favorites: each user can only read and write their own rows (user_id = auth.uid());
--   * cache tables (teams, leagues, fixtures_by_day): readable and writable by any logged-in user,
--     because the app itself fills the cache from API-Football. Anonymous users get nothing.

-- ---------------------------------------------------------------------------
-- favorites: one row per (user, team)   ~ Firestore users/{uid}/favorites/{teamId}
-- ---------------------------------------------------------------------------
create table if not exists public.favorites (
    user_id    uuid        not null default auth.uid() references auth.users (id) on delete cascade,
    team_id    integer     not null,
    name       text        not null,
    logo_url   text,
    league_id  integer,
    added_at   timestamptz not null default now(),
    alerts     jsonb       not null default
        '{"goalSound": false, "redCardVibrate": false, "penaltyVibrate": false, "lineups": false}'::jsonb,
    primary key (user_id, team_id)
);

alter table public.favorites enable row level security;

drop policy if exists "favorites: owner can read"   on public.favorites;
drop policy if exists "favorites: owner can insert" on public.favorites;
drop policy if exists "favorites: owner can update" on public.favorites;
drop policy if exists "favorites: owner can delete" on public.favorites;

create policy "favorites: owner can read"   on public.favorites for select to authenticated using (user_id = auth.uid());
create policy "favorites: owner can insert" on public.favorites for insert to authenticated with check (user_id = auth.uid());
create policy "favorites: owner can update" on public.favorites for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "favorites: owner can delete" on public.favorites for delete to authenticated using (user_id = auth.uid());

-- ---------------------------------------------------------------------------
-- teams: cached /teams data (expires after 7 days)   ~ Firestore teams/{teamId}
-- ---------------------------------------------------------------------------
create table if not exists public.teams (
    team_id    integer     primary key,
    name       text        not null,
    code       text,
    country    text,
    logo_url   text,
    venue_id   integer,
    updated_at timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- leagues: cached /leagues data per season (expires after 7 days)   ~ leagues/{leagueId_season}
-- ---------------------------------------------------------------------------
create table if not exists public.leagues (
    id         text        primary key,          -- "{leagueId}_{season}", e.g. "283_2026"
    league_id  integer     not null,
    season     integer     not null,
    name       text        not null,
    country    text        not null,
    logo_url   text,
    coverage   jsonb       not null,
    updated_at timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- fixtures_by_day: one league's fixtures on one day   ~ fixtures_by_day/{leagueId_date}
-- Expires after 6 hours, never once every fixture of that day is finished.
-- `fixtures` holds the API-Football items as returned by /fixtures (JSON array).
-- ---------------------------------------------------------------------------
create table if not exists public.fixtures_by_day (
    id         text        primary key,          -- "{leagueId}_{yyyy-MM-dd}", e.g. "140_2026-11-20"
    league_id  integer     not null,
    day        date        not null,
    fixtures   jsonb       not null,
    updated_at timestamptz not null default now()
);

-- Same rules for the three cache tables.
do $$
declare t text;
begin
    foreach t in array array['teams', 'leagues', 'fixtures_by_day'] loop
        execute format('alter table public.%I enable row level security', t);
        execute format('drop policy if exists "cache: logged-in users can read" on public.%I', t);
        execute format('drop policy if exists "cache: logged-in users can insert" on public.%I', t);
        execute format('drop policy if exists "cache: logged-in users can update" on public.%I', t);
        execute format('create policy "cache: logged-in users can read" on public.%I for select to authenticated using (true)', t);
        execute format('create policy "cache: logged-in users can insert" on public.%I for insert to authenticated with check (true)', t);
        execute format('create policy "cache: logged-in users can update" on public.%I for update to authenticated using (true) with check (true)', t);
    end loop;
end $$;

-- The app talks to these tables through the Data API as the "authenticated" role.
grant select, insert, update, delete on public.favorites to authenticated;
grant select, insert, update on public.teams, public.leagues, public.fixtures_by_day to authenticated;
