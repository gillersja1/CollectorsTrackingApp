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
