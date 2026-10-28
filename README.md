# Collectors Tracking App

An Android application designed to help collectors organize and track their collections with secure cloud authentication and cross-device synchronization.

## Features
- **Firebase Authentication**: Secure email/password login and registration backed by Firebase.
- **Cross-Device Sync**: User accounts and authentication sync seamlessly across multiple devices.
- **Collection Tracking**: Track collectibles, categories, and search your items easily.
- **Modern UI**: Built using Material Design components, View Binding, and Jetpack Navigation.
- **Edge-to-Edge**: Supports modern Android edge-to-edge display features.

---

## Getting Started & Authentication

### Managing Accounts
- **Developer / Admin View**: You can view, disable, or delete registered users directly from the [Firebase Console](https://console.firebase.google.com/) under **Build** -> **Authentication** -> **Users**.
- **In-App**: Users can register new accounts, log in securely, and log out via the settings menu.

### Testing on Multiple Devices
1. Install the APK (`release/app-debug.apk`) or run the app on your primary device/emulator.
2. Register a new account on Device A.
3. Log out on Device A, then log in using those same credentials on Device B (physical phone or second emulator) to verify cross-device authentication.

---

## Prerequisites
- Android Studio Meerkat (2024.3.1) or newer.
- Android SDK API Level 24 (Android 7.0) or higher.

## Installation & Running
1. Clone the repository or download the source code.
2. Open the project in Android Studio.
3. Ensure `app/google-services.json` is present in your `app/` directory.
4. Wait for Gradle to sync and click **Run** in Android Studio.

## Built With
- [Kotlin](https://kotlinlang.org/) - Programming language.
- [Firebase Auth](https://firebase.google.com/docs/auth) - Cloud authentication.
- [Jetpack Navigation](https://developer.android.com/guide/navigation) - For app navigation.
- [Material Components](https://material.io/develop/android) - UI components.
- [View Binding](https://developer.android.com/topic/libraries/view-binding) - For layout interaction.

## Project Structure
- `app/src/main/java/com/example/collectorstrackingapp/`: Contains the Kotlin source code.
- `app/src/main/res/`: Contains layout files, strings, and other resources.
- `app/build.gradle.kts`: App-level build configuration.

## Testing

Unit tests run on the JVM, so they need no emulator and run on every push
in CI:

```bash
./gradlew testDebugUnitTest                                   # run the tests
./gradlew testDebugUnitTest createDebugUnitTestCoverageReport # + JaCoCo coverage
```

Reports: `app/build/reports/tests/testDebugUnitTest/index.html` and
`app/build/reports/coverage/test/debug/index.html`. CI uploads both as the
`unit-test-reports` artifact.

| Test class | What it covers |
|---|---|
| `CollectionLogicTest` | Portfolio summary (totals, value, category breakdown) and the overview's category filter and sort orders |
| `data/CollectibleRepositoryTest` | The real Room queries against an in-memory SQLite database via [Robolectric](https://robolectric.org/): ordering, replace-on-conflict, delete, name/tag/category search, category rules, user lookup |
| `CollectibleModelTest` | Entity defaults and construction |

The data layer (`CollectibleRepository` and the Room DAOs) and
`CollectionLogic` are at **100% line coverage**. The overview's stats and
sorting used to live inside `FirstFragment`; they were pulled out into
`CollectionLogic` so they could be tested without a device.

### Defects found and fixed

| Defect | Fix | Regression tests |
|---|---|---|
| Search treated `_` and `%` as SQL `LIKE` wildcards, so searching for `_` or `%` returned **every** collectible | User input is escaped (`CollectibleRepository.escapeLikeWildcards`) and the query declares `ESCAPE ''` | `search_underscoreIsMatchedLiterally`, `search_percentIsMatchedLiterally`, `search_backslashIsMatchedLiterally`. Each was confirmed to fail with the fix reverted |
| The same category name could be added twice. `CategoryDao.insert` uses `OnConflictStrategy.IGNORE`, but with no unique index on `name`, IGNORE only applied to ID clashes | Unique index on `categories.name`, with the database version bumped to 7 so existing installs rebuild the schema instead of crashing with a Room integrity error | `insertCategory_withDuplicateName_isIgnored` |
| Wishlist items (not yet owned) counted toward "Total Items", the portfolio value and the category breakdown | `CollectionLogic.summarize` counts owned items only; wishlist items still appear in the list | `summarize_excludesWishlistItems`, `filter_keepsWishlistItemsInTheList` |

The duplicate-name check is case-sensitive, so "FunkoPop" and "funkopop" can both exist.
