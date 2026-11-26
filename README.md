# Weather without ads and trackers

**Free, open-source Android weather app. No ads. No trackers. No account. About 80 KB.**

[English](README.md) · [Українська](README.uk.md) · [Русский](README.ru.md)

A privacy-friendly alternative to weather apps that show more ads than forecast. Native Kotlin, Android 5.0+, no Google services, no analytics SDK, no ad SDK. The whole source is here under GPL-3.0.

**[Download the latest APK](../../releases/latest)**

## Why

Many free weather apps make money from ads and tracking libraries. This one doesn't: you get the forecast and nothing else.

| | This app | Typical ad-supported weather app |
|---|---|---|
| Ads | none | banners, video, pop-ups |
| Trackers / analytics SDKs | none | several |
| Account or sign-in | not needed | often |
| Google Play Services | not used | often |
| Source code | open (GPL-3.0) | closed |
| APK size | ~80 KB | tens of MB |

## Features

- Animated backgrounds for every weather type: sun, moon, stars, clouds, rain, snow, fog, thunderstorms
- Hourly forecast with temperature curve, 10-day forecast, rain nowcast for the next 2 hours
- Air quality (EU and US AQI, PM2.5, PM10, NO2, O3, SO2) and pollen (Europe)
- Wind with compass, humidity, pressure, UV index, visibility, cloud cover, precipitation, daylight
- Sunrise/sunset arc and moon phase
- City search, saved places, approximate location (optional)
- Units: °C / °F, km/h / m/s / mph, hPa / mmHg / inHg
- Light on old and low-end phones: fewer particles, lower frame rate, respects "remove animations"
- Updates: checks GitHub Releases at launch and offers a one-tap update (you can skip it)

## Languages

English, Українська, Русский, Deutsch, Français, Español, Italiano, Português, Polski, Türkçe, Čeština, Română, 中文, 日本語, 한국어.

Translations were prepared by the author and may contain mistakes. Corrections are welcome: edit `app/src/main/res/values-xx/strings.xml` and open a pull request.

## Privacy

Verify it yourself, the code is short:

- Permissions: `INTERNET`, `ACCESS_COARSE_LOCATION` (approximate, optional), `REQUEST_INSTALL_PACKAGES` (only for in-app updates)
- Dependencies: none besides the Kotlin standard library
- Network: HTTPS only (plain HTTP is blocked), and only to `open-meteo.com` (weather, air quality, city search) and `github.com` (update check)
- What leaves the device: the coordinates of the selected place, sent to [Open-Meteo](https://open-meteo.com). No device ID, no advertising ID, no account. Open-Meteo's own privacy policy applies to their servers.

## Install

1. Download `Weather.apk` from [Releases](../../releases/latest)
2. Open it and allow installing from your browser/file manager if Android asks
3. Next versions update from inside the app

Requires Android 5.0 or newer.

## Build

Pushing to `main` builds a signed APK with GitHub Actions and publishes it as a release (`.github/workflows/build.yml`). Locally: JDK 17, Gradle 8.7, Android SDK 34, then `gradle assembleRelease`.

## License

GPL-3.0-or-later with additional terms, see [LICENSE](LICENSE) and [NOTICE](NOTICE).
Forks must stay open source, keep the author credit and be marked as modified.

Weather data by [Open-Meteo.com](https://open-meteo.com) (CC BY 4.0).

<sub>Keywords: free weather app Android, open source weather, no ads weather app, no tracking, privacy weather forecast, FOSS, ad-free, Open-Meteo, Kotlin, lightweight weather widget alternative.</sub>
