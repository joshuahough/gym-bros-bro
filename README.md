# Gym Bros Bro

The gym bro's bro: a retro 8-bit workout tracker for Android. See [SPEC.md](SPEC.md) for the full design and roadmap.

## Build & run

You need JDK 17+ and the Android SDK (API 36). Android Studio installs both.

```bash
./gradlew testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Debug builds install as `com.gymbrosbro.app.debug`, so they can sit alongside a future store build.

### Install on your phone

1. Turn on Developer options. Go to Settings → About phone and tap "Build number" 7 times.
2. Turn on USB debugging in Settings → System → Developer options.
3. Plug the phone in and accept the prompt. `adb devices` should list it.
4. Run the `adb install` command above, or press Run in Android Studio.

## Layout

| Path | What |
|---|---|
| `app/src/main/java/com/gymbrosbro/app/data` | Room database, DataStore settings, seed loader |
| `app/src/main/java/com/gymbrosbro/app/domain` | Pure-Kotlin models and math (units; stats and streaks later) |
| `app/src/main/java/com/gymbrosbro/app/ui` | Theme, pixel components, screens, navigation |
| `app/src/main/java/com/gymbrosbro/app/sprites` | Pixel art defined as text grids, plus the renderer |
| `app/src/main/assets/exercises.json` | Generated exercise library. Don't hand-edit it |
| `scripts/build_seed` | Pipeline that generates the library (see its README) |

## Licenses

- Exercise data: [free-exercise-db](https://github.com/yuhonas/free-exercise-db), public domain (Unlicense).
- Font: Press Start 2P, SIL Open Font License 1.1 (`app/src/main/assets/licenses/`).
