# Play Console answers

Everything to paste or pick in Play Console for the first release. Limits are Play's.

## Store listing

**App name** (30 max, 12 used)

> Gym Bros Bro

**Short description** (80 max, 70 used)

> Retro 8-bit workout tracker. Log lifts, earn XP and level up your bro.

**Full description** (4,000 max)

> Gym Bros Bro is a workout tracker dressed up as an 8-bit arcade game. Every workout earns XP, every personal record is a NEW HIGH SCORE, and your pixel bro levels up right along with you.
>
> LOG ANY WORKOUT
> • Lifting, bodyweight moves, cardio, mobility and plyometrics
> • 880+ exercises built in, plus your own custom ones
> • Save workouts as templates and start them in one tap
> • Last time's weights and reps fill in for you, with suggestions for when to go heavier
>
> KEEP THE STREAK ALIVE
> • Plan your training days on the calendar
> • COMBO streak for hitting your planned days, plus a weekly streak
> • Pause for vacation or sick days without losing your streak
> • Optional reminders, off unless you turn them on
>
> LEVEL UP
> • Earn XP and level up with every workout
> • Unlock trophies and gear for your avatar in the Locker
> • Charts and stats for every exercise
> • Chiptune sound effects and haptics (both optional)
>
> YOUR DATA STAYS YOURS
> • No account, no ads, no tracking
> • The app doesn't even have internet permission
> • Backed up with your phone's Google backup, plus export to a file any time
>
> Pounds or kilograms, miles or kilometers, your choice.

**App category:** Health & Fitness

**Contact email:** Required, and shown publicly on the listing. Use an address you don't mind being public.

**Privacy policy URL:** `https://joshuahough.github.io/gym-bros-bro/privacy-policy` (once GitHub Pages is on; see the checklist below)

**Graphics needed:**
- App icon, 512 × 512 PNG
- Feature graphic, 1024 × 500
- At least 2 phone screenshots (Home, a workout, Calendar, Stats and the Locker show it off best)

## App content

**Privacy policy:** the URL above.

**Ads:** No, the app doesn't contain ads.

**App access:** All functionality is available without special access (no login).

**Target audience:** 13 and older. Picking under 13 would put the app under the Families policy, and the privacy policy says it isn't directed at children under 13.

**Content rating:** Fill in the questionnaire with category "Utility, productivity, communication or other" (or "Health & fitness" if listed). Answer No to violence, sexual content, language, controlled substances, gambling, user interaction, sharing location and digital purchases. Expect a rating of Everyone / PEGI 3.

**Data safety:**
- Does your app collect or share any of the required user data types? **No.**
  - Collecting means sending data off the device. The app has no internet permission, so it can't.
  - Export only writes a file where the user picks, when they ask.
- That makes the remaining security questions (encryption in transit, deletion requests) not applicable.
- Decided (Nov 2026): Android's own Auto Backup doesn't count as collection. Android does that backup and encrypts it, into the user's own Google account, and the developer can't access it. The privacy policy describes it openly.

**Health apps declaration (if asked):** Activity and fitness tracking. No medical features, no Health Connect.

**Government app / financial features / news:** No / none / no.

## Release checklist

1. [ ] Pay the $25 developer fee and finish identity verification. Don't create a payments profile (v1 is free; see SPEC §10).
2. [ ] Create the upload key and `keystore.properties` (README → Release builds).
3. [ ] Turn on GitHub Pages: repo Settings → Pages → deploy from branch `main`, folder `/docs`. Check that the privacy policy URL loads.
4. [ ] Create the app in Play Console: name Gym Bros Bro, app (not game), free.
5. [ ] Fill in App content and the Store listing from this file.
6. [ ] Build the bundle with `./gradlew bundleRelease` and upload `app/build/outputs/bundle/release/app-release.aab` to a **closed testing** track. Let Play App Signing manage the app signing key.
7. [ ] Add the 12+ testers (an email list or Google Group) and send them the opt-in link.
8. [ ] After 14 days with at least 12 testers opted in, apply for production access.
