# FullScreen Browsers

**FullScreen Browsers** is an Android application designed to create and manage standalone, customizable sub-apps that open websites in full-screen web views. Each sub-app functions like a dedicated app and can be launched directly from your Android home screen as a shortcut.

---

## Key Features

### 1. Dedicated Sub-Apps with Home Screen Shortcuts
- **Home Screen Pinning:** Pin any created sub-app to your Android home screen as an independent shortcut with custom icons and labels.
- **Multitasking / Recent Tasks Integration:** Every sub-app runs in its own document task window, creating separate entries in the Android Recent Tasks switcher (`documentLaunchMode="intoExisting"`).

### 2. Sub-App List Management
- **Main Launcher Screen:** View all created sub-apps in a clean list format.
- **Item Options Menu (`⋮`):**
  - **Modify:** Edit existing sub-app properties and behaviors.
  - **Copy:** Instantly duplicate a sub-app configuration.
  - **Add to Home Screen:** Request home-screen shortcut pinning.
  - **Delete:** Remove the sub-app.

### 3. Display & Fullscreen Customization
Customizable per sub-app in the Create / Edit screen:
- **Title & URL:** Define the launcher name and target website URL.
- **Icon Fetcher:** Provide an icon URL or automatically fetch web favicons via host domain detection.
- **Hide Navigation Bar:** Toggle system navigation bar visibility.
- **Hide Status Bar:** Toggle system status bar visibility.
- **Punch-Hole Cutout Padding:** Specify custom top (portrait) or left (landscape) padding in `dp` to safely avoid device display cutouts (punch-hole camera workaround for Android 15+).

### 4. Custom Interaction & Gesture Fixes
- **Volume Key JavaScript Injection:** Map `Volume Up` and `Volume Down` button key presses to evaluate custom JavaScript scripts on the loaded web page.
- **Double-Click Touch Fix:** Optional touch listener (`OnTouchListenerFixDoubleClick`) to filter out accidental duplicate touches occurring at the exact same screen coordinates within a configurable threshold (default `150ms`).
- **Sticky Back Navigation:** Pressing the device back button will navigate back through web history; if no web history remains, it reloads the initial sub-app URL instead of closing or exiting the activity.

### 5. Export & Import Configuration
- **Full Configuration Backup:** Backup and restore all sub-app configurations using Android's Storage Access Framework (SAF).
- **Save / Load Buttons:** Save the complete sub-app list to a `.json` file or import from a `.json` backup file.

---

## Project Structure

```
FullScreenBrowsers/
├── app/
│   └── src/main/java/com/orbitglitch/fullscreenbrowsers/
│       ├── data/
│       │   ├── SubApp.kt          # Data model for sub-app configuration
│       │   └── AppRepository.kt   # Preferences persistence & JSON import/export
│       ├── MainActivity.kt        # Main list UI, Save/Load list actions & FAB
│       ├── EditSubAppActivity.kt  # Shared Create/Edit screen with display & touch toggles
│       ├── SubAppActivity.kt      # WebView activity handling full-screen, insets & JS injection
│       └── BrowserShortcutActivity.kt # Home screen shortcut launcher trampoline
└── README.md
```

---

## Building the Project

Open the project in Android Studio or build using Gradle:

```bash
./gradlew assembleDebug
```