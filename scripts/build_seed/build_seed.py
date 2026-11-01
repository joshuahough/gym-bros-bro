"""Build app/src/main/assets/exercises.json from the free-exercise-db dataset.

Usage:
    python -I scripts/build_seed/build_seed.py --input <path/to/exercises.json>

The dataset (public domain / Unlicense) lives at
https://github.com/yuhonas/free-exercise-db — see README.md for how to fetch it.
Rule-based mapping fills in our fields (tracking type, load class, bodyweight
factor...); overrides.json hand-curates the most common lifts and adds
exercises the dataset is missing. Bump SEED_VERSION whenever the output changes.
"""

import argparse
import json
import re
from pathlib import Path

SEED_VERSION = 1
SOURCE = "free-exercise-db (Unlicense) + Gym Bros Bro curation"

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
DEFAULT_OUTPUT = REPO / "app" / "src" / "main" / "assets" / "exercises.json"
DEFAULT_OVERRIDES = HERE / "overrides.json"

MUSCLES = {
    "abdominals": "CORE",
    "abductors": "GLUTES",
    "adductors": "QUADS",
    "biceps": "BICEPS",
    "calves": "CALVES",
    "chest": "CHEST",
    "forearms": "FOREARMS",
    "glutes": "GLUTES",
    "hamstrings": "HAMSTRINGS",
    "lats": "LATS",
    "lower back": "LOWER_BACK",
    "middle back": "UPPER_BACK",
    "neck": "UPPER_BACK",
    "quadriceps": "QUADS",
    "shoulders": "SHOULDERS",
    "traps": "UPPER_BACK",
    "triceps": "TRICEPS",
}

EQUIPMENT = {
    "barbell": "BARBELL",
    "e-z curl bar": "BARBELL",
    "dumbbell": "DUMBBELL",
    "kettlebells": "KETTLEBELL",
    "machine": "MACHINE",
    "cable": "CABLE",
    "bands": "BAND",
    "body only": "BODYWEIGHT",
    "medicine ball": "OTHER",
    "exercise ball": "OTHER",
    "foam roll": "OTHER",
    "other": "OTHER",
    None: "NONE",
}

# Equipment that means "this is loaded, not a bodyweight movement".
LOADED = {"BARBELL", "DUMBBELL", "KETTLEBELL", "MACHINE", "CABLE"}

CARRIES = {
    "farmer's walk", "yoke walk", "rickshaw carry", "sled drag - harness", "sled push",
    "backward drag", "bear crawl sled drags", "forward drag with press", "prowler sprint",
}
CARDIO_TIME_ONLY = {"rope jumping", "stairmaster", "step mill", "elliptical trainer"}
UNILATERAL = re.compile(r"\b(one|single)[- ](arm|leg|legged)\b")


def slug(name: str) -> str:
    s = name.lower().replace("&", "and").replace("'", "")
    return re.sub(r"[^a-z0-9]+", "-", s).strip("-")


def bodyweight_factor(name: str):
    """Share of bodyweight moved per rep (SPEC §5). Starting estimates, not lab values."""
    n = name.lower()
    if re.search(r"push[- ]?ups?\b|pushups?\b", n):
        if "handstand" in n:
            return 1.0
        if "incline" in n:
            return 0.45
        if "decline" in n or "feet elevated" in n:
            return 0.75
        if "knee" in n:
            return 0.50
        return 0.65
    if re.search(r"pull[- ]?ups?\b|pullups?\b|chin[- ]?ups?\b", n):
        return 1.0
    if "bench dip" in n:
        return 0.65
    if re.search(r"\bdips?\b", n) and "squat" not in n:
        return 1.0
    if "inverted row" in n or "bodyweight mid row" in n:
        return 0.55
    if "pistol" in n:
        return 0.90
    if re.search(r"squat|lunge|step-up", n):
        return 0.70
    return None


def tracking_type(x: dict, equipment: str, factor) -> str:
    name, category = x["name"].lower(), x["category"]
    if name in CARRIES:
        return "WEIGHT_DISTANCE"
    if category == "cardio":
        return "DURATION" if name in CARDIO_TIME_ONLY else "DISTANCE_DURATION"
    if category == "stretching" or x.get("force") == "static":
        return "DURATION"
    if factor is not None and equipment not in LOADED:
        return "BODYWEIGHT_REPS"
    if equipment in ("BODYWEIGHT", "NONE"):
        return "BODYWEIGHT_REPS"
    return "WEIGHT_REPS"


def discipline(category: str, tracking: str, equipment: str) -> str:
    if category == "cardio":
        return "CARDIO"
    if category == "stretching":
        return "MOBILITY"
    if category == "plyometrics":
        return "PLYOMETRIC"
    if tracking == "BODYWEIGHT_REPS" or (tracking == "DURATION" and equipment in ("BODYWEIGHT", "NONE")):
        return "CALISTHENICS"
    return "WEIGHTLIFTING"


def load_class(x: dict, disc: str, tracking: str, equipment: str, primary: str) -> str:
    if tracking in ("DURATION", "DISTANCE_DURATION") or disc == "MOBILITY":
        return "LIGHT"
    if disc == "PLYOMETRIC":
        return "MEDIUM"
    if disc == "CALISTHENICS":
        return "LIGHT" if primary == "CORE" else "MEDIUM"
    if x["category"] in ("powerlifting", "olympic weightlifting", "strongman"):
        return "HEAVY"
    if x.get("mechanic") == "compound":
        return "HEAVY" if equipment == "BARBELL" else "MEDIUM"
    if x.get("mechanic") == "isolation":
        return "LIGHT"
    return "MEDIUM"


def recommendation(x: dict, disc: str, tracking: str, load: str, primary: str, factor) -> dict:
    if tracking == "DISTANCE_DURATION":
        return {"recommendedSets": 1, "recommendedDurationSec": 1200}
    if tracking == "DURATION":
        if disc == "CARDIO":
            return {"recommendedSets": 1, "recommendedDurationSec": 600}
        if disc == "MOBILITY":
            return {"recommendedSets": 2, "recommendedDurationSec": 30}
        return {"recommendedSets": 3, "recommendedDurationSec": 45}
    if tracking == "WEIGHT_DISTANCE":
        return {"recommendedSets": 3, "recommendedDistanceM": 40.0}
    if x["category"] == "olympic weightlifting":
        lo, hi = 3, 5
    elif disc == "PLYOMETRIC":
        lo, hi = 6, 10
    elif tracking == "BODYWEIGHT_REPS":
        if primary == "CORE":
            lo, hi = 15, 20
        elif factor is not None and factor >= 0.9:  # pull-ups, dips: near-full bodyweight
            lo, hi = 5, 10
        else:
            lo, hi = 8, 15
    else:
        lo, hi = {"HEAVY": (5, 8), "MEDIUM": (8, 12), "LIGHT": (12, 15)}[load]
    return {"recommendedSets": 3, "recommendedRepsMin": lo, "recommendedRepsMax": hi}


def convert(x: dict) -> dict:
    name, category = x["name"], x["category"]
    equipment = EQUIPMENT[x.get("equipment")]
    if category == "cardio" and equipment == "MACHINE":
        equipment = "CARDIO_MACHINE"

    if category == "cardio":
        primary = "CARDIO"
    elif category in ("olympic weightlifting", "strongman"):
        primary = "FULL_BODY"
    else:
        primary = MUSCLES[x["primaryMuscles"][0]]
    raw_muscles = x["primaryMuscles"] + x["secondaryMuscles"]
    secondary = []
    for m in raw_muscles:
        g = MUSCLES[m]
        if g != primary and g not in secondary:
            secondary.append(g)

    factor = bodyweight_factor(name)
    tracking = tracking_type(x, equipment, factor)
    if tracking != "BODYWEIGHT_REPS":
        factor = None
    disc = discipline(category, tracking, equipment)
    load = load_class(x, disc, tracking, equipment, primary)

    out = {
        "id": slug(name),
        "name": name,
        "aliases": [],
        "discipline": disc,
        "trackingType": tracking,
        "equipment": equipment,
        "primaryMuscle": primary,
        "secondaryMuscles": secondary,
        "mechanic": x["mechanic"].upper() if x.get("mechanic") else None,
        "recommendedLoad": load,
        "isUnilateral": bool(UNILATERAL.search(name.lower())),
        "bodyweightFactor": factor,
        "instructions": [s.strip() for s in x.get("instructions", []) if s.strip()],
    }
    out.update(recommendation(x, disc, tracking, load, primary, factor))
    return out


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--input", required=True, type=Path, help="free-exercise-db dist/exercises.json")
    parser.add_argument("--overrides", type=Path, default=DEFAULT_OVERRIDES)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args()

    dataset = json.loads(args.input.read_text(encoding="utf-8"))
    overrides = json.loads(args.overrides.read_text(encoding="utf-8"))

    exercises = {}
    for x in dataset:
        e = convert(x)
        if e["id"] in exercises:
            raise SystemExit(f"Duplicate id {e['id']!r} from {x['name']!r}")
        exercises[e["id"]] = e

    for ex_id, patch in overrides["patches"].items():
        if ex_id not in exercises:
            raise SystemExit(f"Override for unknown id {ex_id!r}")
        exercises[ex_id].update(patch)
    for addition in overrides["additions"]:
        if addition["id"] in exercises:
            raise SystemExit(f"Addition {addition['id']!r} already exists")
        exercises[addition["id"]] = addition

    names = [e["name"].lower() for e in exercises.values()]
    dupes = {n for n in names if names.count(n) > 1}
    if dupes:
        raise SystemExit(f"Duplicate display names: {sorted(dupes)}")

    ordered = sorted(exercises.values(), key=lambda e: e["name"].lower())
    seed = {"version": SEED_VERSION, "source": SOURCE, "exercises": ordered}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(seed, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Wrote {len(ordered)} exercises (seed v{SEED_VERSION}) to {args.output}")


if __name__ == "__main__":
    main()
