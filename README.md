# M-Hike Android

[![Android CI](https://github.com/quyncgcs230483/mhike-android/actions/workflows/android.yml/badge.svg)](https://github.com/quyncgcs230483/mhike-android/actions/workflows/android.yml)

Native Android coursework project for planning hikes, recording GPS routes, and keeping field observations. This repository is a cleaned and maintained portfolio edition of the original coursework.

## Features

- Create, review, update, search, and delete hike records
- Store hike, observation, and route data locally with SQLite
- Search locations and choose coordinates on OpenStreetMap
- Record a route in a foreground location service and render it as a map polyline
- Add timestamped observations with a camera thumbnail and durable in-app audio recording
- Show optional current or forecast weather through OpenWeather
- Preserve existing local data through additive database migrations

## Tech stack

- Java 11 and Android Views
- SQLite through `SQLiteOpenHelper`
- Material Components and RecyclerView
- Google Play Services Fused Location Provider
- osmdroid/OpenStreetMap
- Volley and Glide
- JUnit 4

## Project structure

```text
app/src/main/java/com/quyncgcs230483/mhike/
├── *Activity.java         Screens and user flows
├── DatabaseHelper.java   Schema, migrations, and local persistence
├── TrackingService.java  Foreground GPS route recording
├── *Adapter.java         RecyclerView presentation
└── HikeFilter.java       Pure, unit-tested search logic
```

## Run locally

Requirements: Android Studio, JDK 17, and Android SDK 36.

1. Clone this repository and open it in Android Studio.
2. Let Android Studio create `local.properties` with your SDK path.
3. Optional: copy the weather setting from `local.properties.example` and add your own restricted OpenWeather key:

   ```properties
   OPEN_WEATHER_API_KEY=your_key_here
   ```

   Weather cards stay hidden when no key is configured. Never commit `local.properties`.

4. Run the `app` configuration on an Android 7.0+ device or emulator.

Command-line checks:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## Data and permissions

- Hikes, observations, and route points remain in the app's local SQLite database.
- Audio files remain in app-internal storage and are removed with their observation where possible.
- Camera is optional. Location is required only for GPS selection and route recording. Microphone is required only for audio notes.
- Enabling weather sends selected coordinates to OpenWeather. Map use sends tile requests to OpenStreetMap infrastructure.

## Engineering notes

- Database upgrades are additive; upgrades no longer drop user tables.
- Observation and route rows are deleted with their parent hike.
- API configuration is injected at build time and excluded from version control.
- CI runs unit tests, Android Lint, and a debug build on every push and pull request.

## Current scope

M-Hike is a coursework portfolio application, not a production navigation or safety tool. Route accuracy depends on device GPS, and weather data depends on the configured third-party service.
