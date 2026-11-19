# Fluierul Final

Aplicație Android de fotbal (în stilul Flashscore), centrată pe echipele favorite ale utilizatorului.
Proiect SMA. Milestone curent: **S8**.

- **Stack:** Kotlin, Jetpack Compose (Material 3), Navigation Compose (rute type-safe), MVVM, Hilt,
  Retrofit + OkHttp + kotlinx.serialization, Coil, Coroutines + Flow.
- **Date despre meciuri:** [API-Football v3](https://www.api-football.com/documentation-v3).
- **Backend:** [Supabase](https://supabase.com), cu Auth (email + parolă) și Postgres
  (favorite + cache comun pentru datele din API). Înlocuiește Firebase din planul inițial.

## Ce face (S8)

| Ecran | Ce conține |
|---|---|
| Login / Register | email + parolă, validare, erori clare, stare de încărcare; logout din Setări |
| Favorite | CRUD: căutare echipă (min. 3 litere, debounce 500 ms) → adăugare; listă cu siglă, nume, țară; setări de alerte (4 comutatoare); swipe pentru ștergere, cu confirmare și Undo |
| Explorează | ligă → zi (azi ± 7) → meciurile zilei, grupate Live / Programate / Terminate |
| Biletul meu, Detalii meci, Detalii echipă etc. | placeholder-e navigabile („Disponibil în S10/S12”), din Setări |
| Setări → Dezvoltare | test de acoperire API (plan, id-uri ligi, coverage) |

## Setup

### 1. Cerințe
- Android Studio (recent), JDK-ul inclus în Android Studio.
- Un emulator cu **API 34** sau un telefon cu Android 8.0+ (minSdk 26).

### 2. Cheia API-Football
1. Cont gratuit pe [dashboard.api-football.com](https://dashboard.api-football.com).
2. **Account → My Access** → copiază cheia.

### 3. Proiectul Supabase
1. [supabase.com](https://supabase.com) → **New project** (regiune din Europa).
2. **SQL Editor → New query** → lipește tot fișierul [`supabase/schema.sql`](supabase/schema.sql) → **Run**.
   Creează tabelele `favorites`, `teams`, `leagues`, `fixtures_by_day`, cu politicile RLS.
   Poate fi rulat din nou fără probleme.
3. **Project Settings → API Keys** → copiază **Project URL** și cheia **publishable** (`sb_publishable_...`).
   ⚠️ Nu folosi cheia **secret** (`sb_secret_...`) în aplicație: ocolește RLS și dă acces la datele tuturor.
4. Opțional, pentru teste: **Authentication → Sign In / Providers → Email** → dezactivează „Confirm email”,
   sau creează conturi din **Authentication → Users → Add user** cu bifa **Auto Confirm User**.

### 4. `local.properties`
În rădăcina proiectului (fișierul **nu** intră în Git):

```properties
API_FOOTBALL_KEY=cheia_ta_api_football
SUPABASE_URL=https://xxxxxxxx.supabase.co
SUPABASE_ANON_KEY=sb_publishable_...
# opțional: date de test, fără API și fără Supabase (pentru lucrul la UI)
# USE_FAKE_DATA=true
```

Gradle le pune în `BuildConfig`. Cheile nu apar în cod.

### 5. Rulare
Android Studio → **Sync Project with Gradle Files** → alegi emulatorul → **Run ▶**.
Din terminal: `./gradlew installDebug` (cu emulatorul pornit).

### 6. Google Sign-In (în lucru)
Necesită un **OAuth Client ID** din Google Cloud Console:
- unul de tip **Web**, trecut în Supabase la **Authentication → Providers → Google**;
- unul de tip **Android**, cu package `ro.fluierulfinal` și **SHA-1**-ul cheii de semnare.

SHA-1-ul cheii de debug:
```bash
./gradlew :app:signingReport     # caută "SHA1" la Variant: debug
```
Fiecare calculator are propria cheie de debug, deci fiecare membru al echipei își adaugă SHA-1-ul.

## Planul gratuit API-Football (100 cereri / zi): ce înseamnă pentru aplicație
Constatări din testul de acoperire (19.11.2026):
- `/fixtures?date=` merge doar pentru **ieri, azi și mâine**.
- Cererile cu `season=2026` sunt refuzate („Free plans do not have access to this season, try from 2022 to 2024”).
  De aceea Explorează folosește `/fixtures?date=` **fără** ligă și sezon. O singură cerere aduce toate ligile
  din acea zi, iar aplicația salvează câte un rând în cache pentru fiecare dintre cele 8 ligi suportate.
- Siglele (`media.api-sports.io`) nu consumă din cotă.

Cum economisim cereri:
- **cache comun în Supabase:** echipe și ligi 7 zile; meciurile unei zile 6 ore; o zi în care toate meciurile
  s-au terminat nu mai expiră deloc;
- **cache în memorie:** a doua deschidere a aceleiași zile nu mai ajunge nici la Supabase;
- **căutare:** debounce 500 ms, minim 3 litere, rezultate ținute în memorie;
- **fără polling** și fără descărcări în avans.

Logcat, cu filtrul `ApiQuota|FixtureRepo`, arată fiecare cerere reală și câte au mai rămas, plus
`... served from cache, no API request` când nu s-a cerut nimic.

Id-uri ligi (verificate prin `/leagues`): Liga I 283, Liga II 284, Premier League 39, La Liga 140,
Serie A 135, Bundesliga 78, Ligue 1 61, Liga Campionilor 2. Sezonul curent: 2026.

## Arhitectură

```
ui/<feature>   Compose screens + ViewModels (StateFlow)
   │
data/repository  interfețe (Auth, Team, League, Fixture, Favorites)
   │             + implementări reale și Fake… (alese cu USE_FAKE_DATA, în di/AppModules.kt)
   ├── data/remote     Retrofit (ApiFootballService), DTO-uri, mappere DTO → domeniu
   ├── data/supabase   rânduri Supabase, cache (CacheDataSource)
   └── data/cache      reguli de expirare (CachePolicy)
domain/model     Team, League, Fixture, MatchEvent, Favorite, AlertSettings
navigation       rute type-safe, bara de jos, AppRoot (Login ↔ aplicație)
di               module Hilt
```

## Teste

```bash
./gradlew :app:testDebugUnitTest
```

Testele unitare nu fac cereri reale; folosesc fake-uri și răspunsuri API salvate (`app/src/test/resources/api`):
- `MappersTest`: DTO → domeniu, pe răspunsuri reale API-Football;
- `CachePolicyTest`: expirarea cache-ului;
- `FixtureRepositoryImplTest`: a doua deschidere a aceleiași zile nu cheamă API-ul; zilele din afara planului;
- `AuthValidationTest`, `WeekendPlannerTest`.
