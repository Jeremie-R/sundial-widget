# Google Play listing

Copy for the Play Console entry, alongside the assets in this folder.

## App details

| Field | Value |
|---|---|
| App name | Sundial |
| Default language | English (United Kingdom) |
| Application ID | `com.roberrini.sundial` |
| Category | Personalisation |
| Tags | Widgets, Clock, Weather & time |
| Contact email | *(your address — required)* |
| Privacy policy URL | *(publish [privacy-policy.md](privacy-policy.md) and link it — required, see below)* |

## Short description

*80 characters maximum; this one is 74.*

```
A minimalist sun-path clock widget. Sunrise, sunset and the moon, offline.
```

## Full description

*4000 characters maximum.*

```
Sundial replaces the clock face with the sky.

A 24-hour dial runs from midnight at the bottom to noon at the top. An arc marks the hours between sunrise and sunset, a thin bar carries on to astronomical dawn and dusk, and the night is left blank. A small sun travels along it through the day, so a glance tells you how much daylight is left.

The times are the real ones for where you are, worked out on your phone.

WIDGET
• A 2x2 widget that resizes, with the time and date in the middle
• Colours taken from your wallpaper on Android 12 and newer, light and dark
• Five styles, from soft and rounded to high contrast
• Ten background shapes, or none at all
• Tap it to open the app, your alarms, your timers, or any app you choose

IN THE APP
• Today's sunrise and sunset, how long the day lasts, and the civil, nautical and astronomical twilights
• The moon: its phase drawn as it really looks tonight, how much of it is lit, and when it rises and sets
• Shortcuts to your clock app's alarms and timers

QUIET BY DESIGN
• No internet permission, so nothing can leave your phone
• No accounts, no adverts, no tracking, no analytics
• The sun and moon are computed on the device with standard astronomical methods (NOAA for the sun, Meeus for the moon)
• The widget redraws every 15 minutes with an inexact alarm that never wakes your phone

LOCATION
Sunrise and sunset depend on where you are, so the app asks for approximate location once. If you would rather not share it, pick a city from the built-in list instead and the app will never ask again. Your location stays on the device.

Sundial is free and open source: https://github.com/Jeremie-R/sundial-widget
```

## Assets in this folder

| File | Play field | Spec |
|---|---|---|
| `icon-512.png` | App icon | 512×512, 32-bit PNG |
| `feature-graphic.png` | Feature graphic | 1024×500, no alpha |
| `screenshots/01…06.png` | Phone screenshots | 1080×1920 each (2–8 required) |

A raw phone capture is 1280×2856, which is 2.23:1 — Play rejects anything where the long
side is more than twice the short side, so each screenshot is composed onto a 1080×1920
canvas. `make_play_assets.py` (in the session scratchpad) rebuilds them from fresh captures.

## Before you can publish

1. **Privacy policy URL is required**, because the app requests location. Publish
   `privacy-policy.md` somewhere public — GitHub Pages on this repo is enough — and paste
   the URL into the Play Console.
2. **Data safety form**: declare *no data collected and no data shared*. Location is used
   on-device only and never leaves the phone.
3. **Permissions declaration**: `ACCESS_COARSE_LOCATION` is approximate location, used to
   compute sunrise, sunset and moon times. `RECEIVE_BOOT_COMPLETED` re-arms the widget's
   update alarm after a restart. Neither needs a sensitive-permission form.
4. **Upload an App Bundle, not an APK.** Play requires `.aab` for new apps:
   `./gradlew :app:bundleRelease` produces `app/build/outputs/bundle/release/app-release.aab`,
   signed with the same key. The APK in the repo stays for direct installs.
5. **Keep the signing key safe.** `keystore/sundial-release.jks` is gitignored; losing it
   means never being able to update the listing. Consider enrolling in Play App Signing.
6. **Content rating questionnaire** and a target audience (this app has no user content,
   no ads and no purchases).
