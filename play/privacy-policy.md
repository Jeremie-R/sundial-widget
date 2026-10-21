# Privacy policy for Sundial

*Last updated: 21 October 2026*

Sundial is an Android home screen widget that shows the path of the sun and the moon for
the place you are in. This policy explains what it does with your data. The short version
is that it collects nothing and sends nothing anywhere.

## What Sundial collects

**Nothing.** There are no accounts, no analytics, no advertising, no crash reporting and
no tracking of any kind.

## What stays on your device

Sundial asks for **approximate location** so it can work out when the sun and moon rise
and set where you are. If you prefer not to grant it, you can pick a city from the
built-in list instead, and the app will use that.

Either way, the coordinates are saved only in the app's own private storage on your phone,
and are used only to compute those times on the device. They are never transmitted,
shared or sold. Uninstalling the app deletes them.

Your appearance settings — style, background shape, colours, theme, and what each button
opens — are stored the same way.

## No network access

Sundial does not request the `INTERNET` permission. The Android system therefore prevents
the app from making any network connection at all, so none of your data can leave your
phone even in principle. Sunrise, sunset, twilight and moon times are calculated on the
device using published astronomical methods, not fetched from a server.

## Permissions used

| Permission | Why |
|---|---|
| `ACCESS_COARSE_LOCATION` | To compute sunrise, sunset, twilight and moon times for your area. Optional — a city can be chosen by hand instead. |
| `RECEIVE_BOOT_COMPLETED` | To restart the widget's update schedule after the phone restarts. |

## Children

Sundial does not collect data from anyone, including children.

## Changes

If this policy ever changes, the updated version will be published at this address and the
date above will change.

## Contact

Questions about this policy can be raised as an issue at
<https://github.com/Jeremie-R/sundial-widget/issues>.
