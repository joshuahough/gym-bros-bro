# Gym Bros Bro — App Spec (v0.1)

> **Gym Bros Bro**: the gym bro's bro. An Android-only workout tracker with a retro 8-bit / arcade theme.
> Application ID: `com.gymbrosbro.app`. This is permanent once the app is first uploaded to Play.
> Owner tests and steers; Claude writes most of the code.

---

## 1. Goals & principles

- **Track lifting fast.** Built for a typical 3-day week of short sessions, mostly weightlifting with some calisthenics. Logging a set should take one tap when you repeat last time's numbers.
- **Positive feedback, never punishment.** Progress (XP, unlocks, lifetime totals) only goes up. Streaks are a bonus, not the core reward.
- **Offline-first.** No account or server is needed. All data lives on the device.
- **Minimal notifications.** Workout reminders only, and they're opt-in. Nothing else, ever.
- **Retro look, readable data.** Pixel font for headers, big numbers and celebrations. A clean readable font for lists, inputs and anything you read mid-set.
- **Free.** The app launches free with no in-app purchases. A cosmetic-only Supporter Pack may come later if the app gets traction (§10).

## 2. Tech stack

| Concern | Choice |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 (heavily re-themed) |
| Architecture | Single activity, MVVM (ViewModel + StateFlow), pure-Kotlin `domain` package for all math |
| Navigation | Navigation Compose (type-safe routes) |
| DI | Manual `AppContainer` (small app; avoids Hilt's annotation-processing overhead). Revisit if it grows |
| Database | Room (+ KSP), pre-populated from a bundled seed file |
| Settings | DataStore (Preferences) |
| Serialization | kotlinx.serialization (seed data, export/import) |
| Dates | `java.time` (native on minSdk 26) |
| Charts | Custom Compose `Canvas` drawing (pixel-styled) |
| Sprites | Pixel grids defined in code + palette, drawn with nearest-neighbor scaling |
| Reminders | `AlarmManager.setAndAllowWhileIdle` (inexact, so no exact-alarm permission), re-registered on boot and on schedule change |
| Billing (deferred) | Not in v1. If the Supporter Pack is added: Google Play Billing Library, one non-consumable product |
| Min / target SDK | min 26 (Android 8.0); target the latest level Google Play requires |
| Tests | JUnit for domain logic, in-memory Room tests for DAOs, Compose UI tests for the main flows |

### Package layout (single `:app` module to start)

```
app/src/main/
  java/<appId>/
    data/        db, entities, DAOs, repositories, seed loader, export/import
    domain/      stats, streaks, PR detection, overload suggestions, XP, unit conversion
    ui/
      theme/     palette, typography, shapes
      components/  PixelButton, PixelPanel, PixelTopBar, NumberStepper, SpriteImage...
      screens/   home, library, workouts, session, history, calendar, stats, locker, settings
    sprites/     sprite definitions (pixel grids + palettes)
    notifications/
  assets/
    exercises.json   generated seed library
    fonts/
scripts/
  build_seed/    import + mapping of the open exercise dataset into exercises.json
```

## 3. Units & conventions

- Settings let you pick **Imperial (lb, mi)** or **Metric (kg, km)**. You can switch at any time.
- **Storage is canonical:** weight in kg (`Double`), distance in meters, time in seconds. Values are converted and rounded only for display, so switching units never loses data.
- **Dumbbells and other paired implements:** log the weight of **one** implement. Curling 15 lb in each hand is logged as 15 lb, and volume is 15 × reps.
- **Unilateral exercises** (one-arm row, Bulgarian split squat): log reps **per side**. Volume counts one side, which keeps the same one-limb convention as paired dumbbells.
- **Default weight increments** (used by suggestions):
  - Barbell: 5 lb / 2.5 kg
  - Dumbbell: 5 lb / 2 kg
  - Machine and cable: 10 lb / 5 kg
  - Each exercise can override its increment.
- **Bodyweight:** set in your profile, with a history of entries. Each session stores a **bodyweight snapshot**, so old volume numbers don't change when your weight does.
  - The app **never asks every workout.** When a session starts, it copies your latest bodyweight entry.
  - It only asks when the session contains a bodyweight exercise and no bodyweight has ever been entered. That's a one-time inline prompt, and you can skip it, in which case calisthenics volume is just left out.
  - If your latest entry is over 30 days old, the finish screen shows a small, dismissible chip: "Still 180 lb? [Yes] [Update]". It's in-app only, never a notification.
  - You can update bodyweight anytime in Me → Settings, and edit a past session's snapshot in History.

## 4. Data model

### 4.1 Exercise

| Field | Type | Notes |
|---|---|---|
| `id` | String | Stable slug, e.g. `barbell-bench-press`. Custom exercises get `custom-<uuid>` |
| `name` | String | |
| `aliases` | List<String> | Search helpers ("bench", "flat bench") |
| `discipline` | Enum | `WEIGHTLIFTING`, `CALISTHENICS`, `CARDIO`, `MOBILITY`, `PLYOMETRIC` |
| `trackingType` | Enum | See 4.2. Decides which inputs show and how stats count |
| `equipment` | Enum | `BARBELL`, `DUMBBELL`, `KETTLEBELL`, `MACHINE`, `CABLE`, `BAND`, `BODYWEIGHT`, `CARDIO_MACHINE`, `OTHER`, `NONE` |
| `primaryMuscle` | Enum | Drives the pixel icon. See 4.3 |
| `secondaryMuscles` | List<Enum> | |
| `mechanic` | Enum? | `COMPOUND`, `ISOLATION` |
| `recommendedLoad` | Enum | `LIGHT`, `MEDIUM`, `HEAVY`. The exercise's typical load class (bench = heavy, curls = light). Advisory only |
| `recommendedSets` | Int? | e.g. 3 |
| `recommendedRepsMin` / `Max` | Int? | e.g. 6–10. Duration or distance types use the duration/distance fields instead |
| `recommendedDurationSec` | Int? | Duration types |
| `recommendedDistanceM` | Double? | Distance types |
| `isUnilateral` | Boolean | Reps logged per side |
| `bodyweightFactor` | Double? | Share of bodyweight moved per rep (push-up ≈ 0.65). `null` = no weight counted. See §5 |
| `weightIncrementKg` | Double? | Overrides the equipment default |
| `instructions` | List<String> | Step list |
| `source` | Enum | `SEED`, `CUSTOM` |
| `isFavorite`, `isHidden` | Boolean | User flags (allowed on seed exercises too) |
| `userNotes` | String? | |
| `archivedAt` | Instant? | Custom exercises used in history get archived, never hard-deleted |

Seed exercises are read-only apart from the user flags and notes. "Duplicate & edit" turns one into a custom exercise.

### 4.2 Tracking types

| Type | Inputs per set | Counts toward |
|---|---|---|
| `WEIGHT_REPS` | weight, reps | reps, volume |
| `BODYWEIGHT_REPS` | reps, optional added weight (negative = assisted) | reps, volume = (bodyweight × factor + added) × reps |
| `DURATION` | seconds (+ optional added weight) | time |
| `DISTANCE_DURATION` | distance, seconds | distance, time |
| `WEIGHT_DISTANCE` | weight, distance (carries, sled) | distance (weight is shown but not added to volume) |
| `REPS` | reps only (jump rope counted in skips) | reps |

**Two ways to log one exercise.** An exercise can name an `alternateId`: the same movement logged another way, stored as its own library entry so each mode keeps its own records, stats and "last time". Jump Rope (Timed) ↔ Jump Rope (Reps) is the first pair. In a workout, a TIME / REPS switch under the name swaps one for the other and pre-fills from that mode's last time. If sets are already checked off, it asks first because they'll be cleared. Finishing offers to save the choice to the workout, like any other exercise change.

### 4.3 Muscle groups (one pixel icon each)

Chest, Upper Back, Lats, Lower Back, Shoulders, Biceps, Triceps, Forearms, Core, Glutes, Quads, Hamstrings, Calves, Full Body, Cardio. That's 15 icons.

### 4.4 Workouts & sessions

```
WorkoutTemplate   id, name, notes, sortOrder, createdAt, archivedAt?
TemplateExercise  templateId, exerciseId, position, targetSets, targetRepsMin?, targetRepsMax?,
                  targetDurationSec?, targetDistanceM?, notes?

WorkoutSession    id, templateId?, name, startedAt, endedAt?, status (IN_PROGRESS | COMPLETED),
                  bodyweightKgSnapshot?, notes?, scheduledOccurrenceDate?
SessionExercise   id, sessionId, exerciseId, position, notes?
SetEntry          id, sessionExerciseId, position, isWarmup, isCompleted,
                  weightKg?, reps?, addedWeightKg?, durationSec?, distanceM?, completedAt?
```

### 4.5 Records, progress, schedule, profile

```
ScheduleRule      id, templateId? (null = any workout), kind (WEEKLY | EVERY_N_DAYS | ONE_OFF),
                  daysOfWeek (bitmask), intervalDays?, startDate, endDate?, createdAt
ScheduleException ruleId, originalDate, action (MOVE | SKIP), newDate?
SchedulePause     startDate, endDate                    vacation / sick, freezes streaks
BodyweightEntry   date, weightKg
UnlockedItem      itemId, unlockedAt, sessionId?        permanent once earned
EarnedTrophy      trophyId, earnedAt, sessionId?        permanent once earned
AvatarConfig      skin, hair, hairColor, headwear, top, topColor, bottom, bottomColor, shoes, accessory
                  (DataStore JSON; item ids, anything locked falls back to the default)
```

**Derived, not stored:** totals, per-exercise stats, personal records, streaks, streak freezes and XP are all calculated from history. Editing or deleting an old workout corrects everything automatically. **Unlocks and achievements are kept permanently**: once you earn something, an edit never takes it away.

- **Personal records** by tracking type:
  - Weight × reps: heaviest weight and best e1RM per set; most volume per workout.
  - Bodyweight: most reps per set and per workout.
  - Time: longest. Distance: farthest. Weight × distance: heaviest.
  - The first time you do an exercise sets the baseline and isn't celebrated, and ties don't count.
  - Per-set records pop NEW HIGH SCORE as you log; per-workout ones show on the finish screen.
- **Schedule rules are never edited in place** once they have past days. Removing one ends it yesterday, so past occurrences and the streaks built on them stay put. A rule that hasn't started yet is simply deleted.

### 4.6 Settings (DataStore)

Unit system, week start day (default Monday), weekly goal (default 3), reminder on/off and time, grace-day reminder on/off, sound on/off, haptics on/off, keep-screen-on during a session, seed version.

## 5. Bodyweight factors (starting estimates, to verify during the content pass)

| Exercise | Factor |
|---|---|
| Push-up | 0.65 |
| Knee push-up | 0.50 |
| Incline push-up (hands on bench) | 0.45 |
| Decline push-up (feet on bench) | 0.75 |
| Pull-up / chin-up | 1.00 |
| Dip | 1.00 |
| Bench dip (feet on floor) | 0.65 |
| Handstand push-up | 1.00 |
| Inverted row | 0.55 |
| Pistol squat | 0.90 |
| Bodyweight squat / lunge | 0.70 |
| Sit-up, crunch, leg raise, plank | `null` (reps or time only, no weight) |

Custom calisthenics exercises can set their own factor.

## 6. Exercise library content

- **Source:** the open *free-exercise-db* dataset (around 800 exercises, public domain; **confirm the license before shipping**). It provides names, muscles, equipment, mechanic and instructions. Its photos are **not** used.
- **Pipeline** (`scripts/build_seed`):
  1. Map the dataset's fields to ours.
  2. Infer `trackingType`, `discipline`, `recommendedLoad` and `bodyweightFactor` by rule.
  3. Hand-curate the top ~150 exercises.
  4. Output `assets/exercises.json`.
- **Updates:** the seed has a version number. App updates merge new or changed seed exercises by `id` and never touch custom exercises or user flags.
- **Art:** one pixel icon per primary muscle group. Animated sprites for a handful of popular exercises come later (backlog).

## 7. Features

### 7.1 Exercise library
- Search by name or alias. Filter by muscle, equipment, discipline and tracking type, plus a favorites filter.
- **Detail screen:** muscle icon, recommended load, sets and reps, instructions, and a **personal panel**:
  - Best set and estimated 1RM (labeled as an estimate)
  - Max weight and max reps
  - Total reps and total volume
  - Times performed and last performed
  - PR history, and a progress chart
- Create, edit and archive custom exercises, or duplicate a seed exercise.

### 7.2 Workouts (templates)
- Create, rename, reorder, duplicate and archive templates. Each template is an ordered list of exercises with target sets and reps.
- Each exercise is **WORKING** or **WARM-UP**. A warm-up exercise starts every workout with all its sets marked W, pre-filled from last time's warm-ups, with no suggestion chips. In the workout, ⋯ → "Mark as working sets" / "Mark all as warm-up" flips the whole exercise, and single sets can still be flipped from their number. Finishing offers to save the change to the workout.

### 7.3 Workout session (core loop)
- **Start a session from:** today's scheduled workout, any template, or an empty quick workout.
- **Each exercise card shows:**
  - "Last time: 145 × 8, 8, 8"
  - Set rows pre-filled from last time
  - A **suggestion chip** (§8). Tapping it fills the sets
- Tap a set's checkbox to complete it. If the next row is still empty, it copies the numbers you just logged, so a first-ever workout isn't three rounds of retyping. You can add or remove sets, mark warm-ups, add, swap or reorder exercises mid-session, and add notes.
- **Inputs:** everything is on the number pad. Times are separate minute and second boxes; seconds can run past 59, so 90 s and 1 min 30 s both work. Added weight on calisthenics says it's optional (blank = bodyweight).
- **Keyboard:** the last input in a set shows DONE instead of NEXT. DONE, checking a set off, or switching to another app lets go of the cursor, so the keyboard never pops back up on its own.
- Checking off the last open set of an exercise scrolls to the next exercise.
- Finishing with every set checked says **ALL SETS COMPLETED! Finish workout?**
- **No rest timer in v1.** Session duration is tracked from start and end times.
- In-progress sessions are saved continuously, so killing the app never loses a workout. You can resume from Home or Train, or delete it from Train (with a confirmation).
- **Warm-ups count toward every total** (reps, volume, time, distance). They're only left out of PR detection and overload suggestions, so a light warm-up never gets mistaken for your working weight.
- **PR detection:** working sets only. It triggers a **NEW HIGH SCORE!** pop-up when the set is completed.
- **Finish screen** ("STAGE CLEAR"):
  - Duration, sets, reps and volume
  - PRs set, XP earned and new unlocks
  - An optional "Update template with today's changes?" prompt

### 7.4 History
- Sessions listed by date. Tap one to see its detail. You can edit any value, add or remove sets, or delete the whole session (with confirmation). All derived stats recalculate.
- **Backfill:** you can log a workout you forgot to record, on a past date.
- **Integrity: trust the user.** This is a personal tracker with no leaderboards or social features, so there's nobody to cheat against. Guardrails would mostly get in the way of honest people fixing mistakes. The only rules are sanity checks:
  - Sessions can't be dated in the future.
  - Values have to be within sane bounds (no 9,999-lb curls or negative reps).
  - Revisit this if social features or leaderboards are ever added.

### 7.5 Calendar & schedule
- Month view:
  - Scheduled day: outlined marker
  - Completed day: filled marker
  - Missed day: neutral gray
- **Schedule rules:**
  - Weekly ("Mon = Push, Wed = Pull, Fri = Legs")
  - Every N days (rotating splits)
  - One-off
- **For each occurrence:** start, **move** (keeps the streak; stays within the same week), or **skip**.
- **Pause schedule** for vacation or illness. Streaks freeze while paused.

### 7.6 Streaks
- **Schedule streak (primary):** consecutive scheduled occurrences completed. An occurrence counts as completed if any workout is logged that day, even if it's not the planned template. Unscheduled workouts don't extend this streak, but they count everywhere else.
- **One-day grace period:** if you miss a scheduled occurrence, a workout logged the **next day** makes it up and the streak continues.
  - This only applies if the next day has no scheduled occurrence of its own. Otherwise that day's workout counts for that day's occurrence, and the missed one goes to a freeze.
  - Each workout makes up at most one occurrence.
- **Weekly streak (secondary):** consecutive weeks with at least *weekly goal* workouts (default 3). The current week never counts as broken until it ends.
- **Streak freezes:**
  - You earn 1 for every 4 consecutive weeks with every scheduled workout done. You can hold up to 2.
  - A freeze is applied automatically to a skip, or to a missed occurrence once its grace day has passed without a make-up workout.
- Streaks are shown as **COMBO ×N** on Home and in Stats, along with your best ever.

### 7.7 Stats
- Period picker: Week / Month / Year / Lifetime.
- Tiles: workouts, total reps, total weight lifted (calisthenics included), time trained, cardio distance, PRs set.
- Both streaks (current and best), and a workout-day heatmap.
- Sets per muscle group for the period.
- Per-exercise drill-down: top weight and e1RM over time, weekly volume, totals.

### 7.8 Reminders
- **Off by default.** You're offered them once, when you create your first schedule.
- At most **one notification per scheduled occurrence**, at a time you choose. No reminder fires if you've already logged a workout that day. Tapping it opens Home, where PRESS START starts today's workout. It's a daily inexact alarm (no exact-alarm permission), so it can land up to about an hour after the chosen time.
- **Grace-day reminder** (a separate toggle, off by default): on the grace day after a missed workout, one notification at your reminder time: "Missed Monday — train today to keep your COMBO."
- No other notification types exist, and the app never sends more than one notification a day.
- Requires `POST_NOTIFICATIONS` (Android 13+) and `RECEIVE_BOOT_COMPLETED`.

### 7.9 XP, levels & achievements (tunable)
- **XP:**
  - +100 per completed workout
  - +10 per working set
  - +50 per PR
  - +25 for finishing a scheduled occurrence
  - +100 for hitting the weekly goal
- Only working sets count. The schedule bonus goes to the first workout on a scheduled day; the weekly-goal bonus goes to the workout that reaches the goal.
- **Levels:** level *n* needs `250 × (n − 1)` more XP than level *n − 1*, so reaching level *n* takes `125 · n · (n − 1)` XP (level 2 = 250, level 10 = 11,250). The finish screen says **LEVEL UP!**
- Trophies and unlocks are checked after each finished workout and at launch, which also backfills them for older history.
- **Achievements ("TROPHIES"), 20 in all:**
  - First workout ("PLAYER 1 READY")
  - 10 / 50 / 100 / 250 / 500 workouts
  - Weekly streak 4 / 12 / 26 / 52
  - COMBO ×10 / ×25 / ×50
  - First PR, 25 PRs, 100 PRs
  - Lifetime volume milestones (100k / 1M lb)
  - 1,000 lifetime pull-ups
  - Marathon distance total

### 7.10 Avatar ("LOCKER")
- A full-body 22×34 pixel character in a front lat spread, hands on hips (on a 22×37 canvas, with headroom for spiky hair and crowns). Optional, and cosmetic only: the body never changes with progress.
- **Layers,** drawn in order: body, shoes, bottom, top, hair, accessory, headwear. Each layer owns its palette chars, so colors swap independently.
  - Skin: 8 tones, all available from the start.
  - Hair: bald, short, buzz, spiky, long, mohawk. Hair color: 10.
  - Headwear: headband, cap, beanie, crown. Hats hide hair that pokes out above them.
  - Top: tank, tee, stringer, hoodie, shirtless. Bottom: shorts, joggers. Top, bottom and shoes each take one of 12 gear colors.
  - Accessory: wristbands, lifting belt, headphones, gloves, gold chain.
- You start with a basic set. More items unlock via level or trophies (lifetime totals unlock through their trophies). Supporter Pack items would come only if the pack is added later (§10).
- In the LOCKER, tapping a locked item tries it on without saving and says how to unlock it.
- **Player name:** RENAME on the Me tab; up to 12 letters, numbers or spaces, shown on Home and Me. Blank means PLAYER 1.
- A 2-frame idle bob, plus a hop on PRs, trophies and level-ups. The avatar appears on Home, the Me tab, the locker and the finish screen.
- Sprites are drawn as pixel grids in code, so palette swaps are trivial and they stay crisp at any size. Bought asset packs (with a commercial license) are a fallback for anything more ambitious.

### 7.11 Look & sound
- A custom limited palette (~16 colors), dark-first.
- Fonts:
  - **Press Start 2P** (OFL) for headers, big numbers and celebration text
  - A readable sans for body text, inputs and lists
- Chunky 2-px borders, pixel panels and big tap targets for use mid-set.
- Arcade vocabulary in the fun spots only: PRESS START, STAGE CLEAR, NEW HIGH SCORE, LEVEL UP, COMBO, TROPHIES, LOCKER. Functional buttons use plain words.
- Chiptune sound effects for set complete, PR and level-up, synthesized in code (square waves, no audio files), plus vibration. Both can be toggled and are on by default.

### 7.12 Navigation
Bottom bar with 5 tabs:
- **Home:** today's workout, PRESS START, resume, streak, avatar
- **Train:** Workouts and Library sub-tabs
- **Calendar**
- **Stats**
- **Me:** Locker, History, Settings

## 8. Progressive overload suggestions

Suggestions are always optional. They're based on the most recent session's **working** sets for that exercise.

- **First time doing an exercise:** show its recommended sets, reps and load class only.
- **`WEIGHT_REPS`:**
  - Estimated 1RM uses the Epley formula: `e1RM = w × (1 + r/30)`, from the best set (sets above 12 reps are ignored as unreliable).
  - **If every working set hit its reps**, offer two chips:
    - **Heavier:** target reps = `r − 2` (minimum 3). Weight = `e1RM / (1 + target/30)`, shown as a range rounded down and up to the increment. *Example: 3 × 8 @ 145 → e1RM ≈ 184 → "150–155 × 6".*
    - **More reps:** same weight, `r + 1`.
  - **If reps dropped off:** suggest repeating the same weight and reps.
- **`BODYWEIGHT_REPS`:** +1–2 reps per set. At the top of the recommended range, suggest added weight or a harder variation.
- **`DURATION`:** +5–10 s.
- **`DISTANCE_DURATION`:** same distance at a faster pace, or +5–10% distance.

## 9. Backup & data

1. **Android Auto Backup (default, no setup):** the database and settings back up to your Google account through the phone's system backup. They restore on reinstall or a new phone. This is controlled by your system Backup setting, not an in-app toggle. The DB is checkpointed before backup.
2. **Manual export/import:** a JSON file through the system file picker. You can save it straight to Google Drive.
3. **Optional Cloud Save (backlog):** an in-app toggle that syncs to Google Drive's private app folder, or to Play Games saved games. Off by default. It adds Google sign-in and consent-screen setup, so it's held until there's demand.

## 10. Monetization

**Decision (2026-11-05): v1 ships free, with no in-app purchases and no merchant account.** Monetizing would put the owner's full address on the public store listing, and a mailbox costs a monthly fee that a hobby project doesn't justify yet. Revisit the Supporter Pack only if the app gets real traction. The plan below is kept for that case.

### Supporter Pack (deferred)

- One **Supporter Pack** non-consumable in-app purchase (price TBD, $0.99–$2.99).
- **Contents:**
  - Supporter-exclusive outfits
  - Alternate UI color palettes
  - An extra sound pack
  - A supporter badge
- Core tracking features are never locked.
- Play Billing handles restoring the purchase on other devices with the same Google account. Purchases must be acknowledged within 3 days, or Google refunds them automatically.
- **What the billing code has to handle:**
  - Grant the pack only when a purchase is `PURCHASED`. `PENDING` purchases (for example, cash at a store) aren't granted yet.
  - **Acknowledge** the purchase right after granting it.
  - Re-check owned purchases each time the app launches, which picks up restores, new devices and refunds or chargebacks.
  - No server is needed for this.
- **Needs:**
  - A Play Console merchant account linked to a payments profile: legal name, address, bank account for payouts, and tax info.
  - License testers for test purchases.
- **Privacy note:** Google shows the **full address** from a monetizing account's payments profile publicly on the store listing. Set up a PO box or virtual mailbox first if you don't want your home address shown, and confirm Google accepts it.

### Merchant account caveat

Turning monetization on is reported to be hard or impossible to reverse for an account. So don't create a merchant account or payments profile for v1. Set one up only if the Supporter Pack is built, and only after a PO box (annual billing, ideally with USPS Street Addressing) or virtual mailbox is in place.

## 11. Roadmap

| Phase | Scope | Done when |
|---|---|---|
| **0. Foundation** ✅ | Android Studio project, git, version catalog, theme (palette, fonts, pixel components), 5-tab nav skeleton, Room setup, sprite renderer, seed pipeline + `exercises.json` | Themed app with empty tabs installs on the owner's phone |
| **1. Core loop** ✅ | Library (browse/search/filter/detail), custom exercises, templates, session logging with pre-fill and resume, basic finish screen, history with edit/delete, settings (units, bodyweight) | The owner runs real workouts on it |
| **2. Feedback** ✅ | PR detection + NEW HIGH SCORE, overload suggestions, Stats, per-exercise stats and charts, schedule + calendar, both streaks + freezes + pause, reminders | Calendar shows the weekly plan, streaks and stats are accurate |
| **3. Fun layer** ✅ | XP/levels, achievements, avatar + locker + unlocks, all 15 muscle icons, sound effects and haptics | Workouts earn XP and unlock outfits |
| **4. Touch-ups** ✅ | From the first real workout: minute + second time fields on the numpad, DONE on a set's last field, checking a set or leaving the app clears the cursor (no surprise keyboard), auto-scroll to the next exercise, "optional" hint on added weight, "All sets completed!" finish prompt, custom player name, delete an in-progress workout from Train | The owner's next workout has no keyboard or scrolling annoyances |
| **5. Go public** ✅ | Audit the project for anything that shouldn't be public (keys, keystores, local paths, personal data, `local.properties`), tighten `.gitignore`, create the GitHub repo and push | Code is on GitHub with nothing private in it or its history |
| **6. Ship** | Onboarding, export/import, Auto Backup check, accessibility and polish pass, privacy policy, Data Safety form, store listing and screenshots, closed test (12+ testers × 14 days), production release | Live on Google Play |

**Backlog:** Supporter Pack + billing (only with traction; see §10), rest timer, animated exercise sprites, supersets, optional Cloud Save, plate calculator, home-screen widget, Health Connect export, small avatar body variations.

**2.0:** more avatar poses, and animations of the avatar working out (lifting, curling, running...) that play when you open the app. A leaderboard where players see each other's accomplishments by player name.

## 12. Open questions

1. **XP and level numbers.** Tune after a few weeks of real use.

## 13. Owner to-dos before Phase 6 store setup

- [x] ~~Set up a PO box or virtual mailbox.~~ Not needed: v1 ships free with no merchant account (§10). Don't create a payments profile during Play Console setup.
- [ ] Line up 12+ closed-test testers who can commit to 14 days in a row.
- [ ] Pay the $25 Play Console fee and complete ID verification.
- [ ] Create the upload key and `keystore.properties` (README → Release builds).
- [ ] Turn on GitHub Pages (`main`, `/docs`) so the privacy policy is live.
- [ ] Work through the release checklist in `store/play-console.md`.
- [x] Name: **Gym Bros Bro**. A trademark search found no conflicts.
- [x] Application ID: `com.gymbrosbro.app`.
