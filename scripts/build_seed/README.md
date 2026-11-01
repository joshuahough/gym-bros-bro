# Exercise seed pipeline

Builds `app/src/main/assets/exercises.json`, the library bundled with the app.

## Source

[free-exercise-db](https://github.com/yuhonas/free-exercise-db). This is public domain under the Unlicense. Only names, muscles, equipment, mechanic and instructions are used. Its photos are not used.

## Rebuild

```bash
mkdir -p /tmp/free-exercise-db
curl -sSfL -o /tmp/free-exercise-db/exercises.json https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json
python -I scripts/build_seed/build_seed.py --input /tmp/free-exercise-db/exercises.json
```

The seed was first built from the dataset fetched on 2026-10-31 (876 exercises).

## Editing

- **Fix or curate an exercise:** add a patch in `overrides.json`, keyed by our id. The id is the slug of the dataset name, for example `barbell-bench-press-medium-grip`.
- **Add an exercise the dataset lacks:** add it under `additions` in `overrides.json`, with every field filled in.
- **Change rules:** edit `build_seed.py`. This covers tracking type, load class, bodyweight factors and recommended sets and reps.

After any change, bump `SEED_VERSION` in `build_seed.py`. The app re-merges the library when it sees a newer version, and it keeps users' favorites, hidden flags and notes. **Never change an existing id.** Workout history references ids.
