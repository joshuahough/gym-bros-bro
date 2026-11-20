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

## Release builds

Release builds are shrunk with R8 and install as `com.gymbrosbro.app`, alongside the debug build.

### One-time: create the upload key

Play uses this key to confirm uploads come from you. Play App Signing holds the key that actually signs the app on users' phones.

1. From the project root, run the command below and choose your own passwords when it asks:

   ```bash
   keytool -genkeypair -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```

2. Create `keystore.properties` in the project root:

   ```properties
   storeFile=upload-keystore.jks
   storePassword=<your store password>
   keyAlias=upload
   keyPassword=<your key password>
   ```

3. Back up `upload-keystore.jks` and both passwords somewhere safe, such as a password manager. If you lose them, Play support can reset the upload key, but it takes days.

Both files are git-ignored. Without `keystore.properties`, release builds are signed with the debug key. That's fine for testing on a phone, and Play rejects debug-signed uploads, so one can't ship by accident.

### Build

```bash
./gradlew bundleRelease
```

The bundle to upload is `app/build/outputs/bundle/release/app-release.aab`. Store listing text, Data safety answers and the release checklist are in [store/play-console.md](store/play-console.md). The privacy policy is [docs/privacy-policy.md](docs/privacy-policy.md), served by GitHub Pages.

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
