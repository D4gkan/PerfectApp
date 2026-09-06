<p align="center">
  <img src="icon.png" alt="Perfect App icon" width="160" />
</p>

<h1 align="center">PERFECT APP</h1>

<p align="center">
  <strong>Your personal life dashboard, built for clarity.</strong><br />
  Health, money, routines, plans, and the details that keep life moving.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/build-v1.0.2-9b111e?style=for-the-badge" alt="Build v1.0.2" />
  <img src="https://img.shields.io/badge/platform-Android-3ddc84?style=for-the-badge&logo=android&logoColor=white" alt="Platform Android" />
  <img src="https://img.shields.io/badge/license-MIT-c0c0c0?style=for-the-badge" alt="MIT License" />
</p>

## Overview

Perfect App is a premium, offline-first Android app for managing the practical parts of
everyday life in one focused place. It brings personal health, nutrition, hydration,
finances, schedules, vehicle care, subscriptions, and reminders into a single dashboard.

Everything is designed for personal use: fast local storage, no account, no login, no cloud,
and no dependency on an internet connection.

## Highlights

### Refreshed navigation and widgets

- System-aware light and dark themes with ruby red accents and neutral silver surfaces, animated tab selection, screen fades, and smooth card/progress updates
- Labeled, horizontally scrollable bottom tabs for every section, including Car, Renewals, Widgets, Alerts, Backup, and Settings
- A Widgets tab with a Today widget pinning button (or manual instructions for launchers without pinning support)
- A resizable Today widget with net worth and money at the top, tasks/reminders on the left, University in the middle and birthday countdowns on the right beneath it, followed by daily progress and quick actions
- Widgets load saved data before rendering, observe live changes, refresh after database changes, and request periodic launcher updates every 30 minutes (Android may defer these)
- Calendar widgets include recurring events and omit completed items; old repeating series no longer disappear after 500 elapsed occurrences

To add the dashboard widget, swipe the bottom tabs to **Widgets**, choose **Add Today widget**, and confirm the launcher prompt. Enabled summaries appear at every size; scroll inside the widget for overflow or expand it to see more. Configure summaries, financial privacy, and three shortcuts in Widgets. Tap individual items to open the relevant screen.

Validation: debug and minified release builds and 18 unit tests pass, including calendar recurrence, birthday and renewal currency regression tests. Emulator checks cover Home navigation from a widget launch, birthday search, University management, renewal editing and USD conversion, and TRY cash fuel expenses.

### Today widget and calendar

- Net worth and the money summary appear at the top left, with recent transactions on the right (green income, red expenses).
- Below them are three columns: tasks/reminders, University, and birthday countdowns.
- Expanded widgets show protein and carbs beneath water/calorie progress. Ruby red accents replace the previous pink widget accent.
- Enabled money and net-worth summaries appear at every widget size, with scrolling for overflow. Net worth can be enabled independently of the money summary.
- Widget settings update live. Enable Show financial amounts to reveal values.
- Calendar has searchable Birthdays and an add-birthday flow that saves all-day yearly events.
- University is a dedicated calendar view and event type for manually adding, editing and deleting lessons or exams. Existing university lessons remain compatible.
- Birthday countdown regression tests cover today, tomorrow, year rollover, and February 29.

### One calm dashboard

- Live summaries for health, diet, water, wealth, calendar events, car maintenance, and reminders
- Quick actions for the things you record most often
- Reusable premium cards, progress indicators, countdowns, and metric rows
- Search birthdays in Calendar and transactions in Wealth; the separate Search tab has been removed
- Home navigation returns reliably from other tabs and widget launches

### Health and habits

- Body measurements with weight, BMI, body fat, fat mass, and progress trends
- Segmental body composition tracking
- Activity logging and measurement history
- Daily calorie, macro, and hydration goals
- Meal entries with calories, protein, carbohydrates, and fat
- Water logging with daily progress

### Money, plans, and responsibilities

- Assets, transactions, net worth snapshots, and multi-currency values
- Local exchange-rate management
- Renewals is the single place for manual renewals and automatic subscriptions, including existing subscription records
- Edit renewal details without changing their identity, currency or linked vehicle
- Native renewal amounts remain in their saved currencies; USD equivalents and monthly totals use locally saved Wealth exchange rates
- Missing exchange rates are reported instead of relabeling foreign amounts as USD
- Calendar events with repeating occurrences and reminders
- Add, edit, remove and switch vehicles from the selector at the top left of Car
- Vehicle-specific odometer history, fuel, maintenance and renewals
- Fuel is always paid from TRY CASH; each save atomically records the fuel entry, Wealth expense and cash-balance change
- Editing a vehicle preserves its fuel/maintenance history; removing it keeps Wealth transactions and renewal reminders
- Renewals and important due-date reminders

### Android-native utility

- One responsive Today home-screen widget for events, reminders, daily progress, and money
- AlarmManager wake-ups with WorkManager-powered notification delivery and daily summaries
- Updated alarm UI for a clearer event alarm experience
- Event reminders scheduled **at event time** ring and vibrate continuously until you press **Stop alarm**, with a lock-screen alarm screen; earlier reminders remain normal notifications
- Notifications rescheduled after device reboot
- Local backup and restore support
- Adaptive launcher icon and a dark premium Material 3 theme

## Privacy by design

Perfect App is deliberately offline-first.

- No account system or login
- No backend, cloud sync, analytics, or third-party API requirement
- Data is stored locally on the Android device using Room
- Local configuration, signing keys, environment files, database files, backups, exports, build output, and local screenshots under `.tmp/` are excluded from Git
- Subscription auto-charging is only local bookkeeping; it never contacts a bank,
  card network, payment processor, or external service

## Technology

- **Platform:** Android, API 26+
- **Language:** Kotlin
- **UI:** Jetpack Compose and Material 3
- **Architecture:** MVVM with repositories, ViewModels, Coroutines, and Flow
- **Persistence:** Room Database and DataStore Preferences
- **Background work:** AlarmManager, WorkManager, and Android notifications
- **Widgets:** Jetpack Glance
- **Build:** Gradle Kotlin DSL, Android Gradle Plugin, Kotlin Symbol Processing

## Project structure

```text
app/src/main/java/com/perfectapp/
|-- data/                 Room database, entities, DAOs, repositories
|-- domain/               Calculations, calendar rules, notification workers
|-- ui/components/        Shared premium cards and dashboard components
|-- ui/navigation/        App destinations and navigation graph
|-- ui/screens/           Feature screens and ViewModels
|-- ui/theme/             Material 3 colors, typography, and theme
`-- ui/widgets/           Glance widgets and refresh handling
```

The application follows a consistent flow from entity to DAO, repository, ViewModel, and
Compose screen. This keeps business logic testable and makes each area straightforward to
extend.

## Build and run

### Requirements

- Android Studio Koala, Ladybug, or newer
- JDK 17
- Android SDK 35
- An Android device or emulator running API 26 or newer

### Steps

1. Clone or open this repository in Android Studio.
2. Allow Gradle to sync and download dependencies from Google Maven and Maven Central.
3. Select an API 26+ device or emulator.
4. Run the `app` configuration.

For event alarms scheduled at event time, allow **precise alarms** when Android prompts for
that access. Without it, Android may defer the alarm and the event is delivered as a normal
notification instead.

To build a release APK from a terminal:

```powershell
gradle :app:assembleRelease
```

The generated APK is written beneath `app/build/outputs/apk/`. Use Gradle 8.10; this repository does not include wrapper scripts. Release signing is not configured: the release APK is unsigned and needs your signing key before installation. `gradle :app:assembleDebug` builds an installable development APK.

## Version

**v1.0.2** updates the alarm UI. Android version code: **3**.

## License

Perfect App is released under the [MIT License](LICENSE).

Copyright (c) 2026 Perfect App contributors.
