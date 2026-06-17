# 🏗️ System Architecture & Data Flow

This document details the system design, components, and data flow of the Daywise Health Tracker application.

---

## 1. High-Level Component Diagram

Daywise uses a hybrid web-native wrapper architecture. The app runs a single-page JavaScript application inside a native Android WebView container, binding device features (like notifications, SQLite storage, and camera) via secure JavaScript bridges.

```
       +-------------------------------------------------------------+
       |                         USER DEVICES                        |
       +-------------------------------------------------------------+
                                      |
                                      v
       +-------------------------------------------------------------+
       |                     ANDROID WEBVIEW SHELL                   |
       |  - Loads: local assets (file:///android_asset/index.html)    |
       |  - Security: Bypasses CORS via allowUniversalAccess         |
       +-------------------------------------------------------------+
               |                                             ^
               | (JSBridge calls)                            | (evaluateJavascript)
               v                                             |
       +-----------------------------+               +-----------------------------+
       |   NATIVE ANDROID BINDINGS   |               |    WEB UI FRONTEND (SPA)    |
       | - Camera/Gallery Access     |               | - CSS3 Theme & UI Layout    |
       | - AlarmManager Scheduler    |               | - State Machine & Data Sync |
       | - FileProvider system       |               | - Vercel Speed Telemetry    |
       +-----------------------------+               +-----------------------------+
               |                                             |
               | (Room Queries)                              | (Direct HTTPS POST)
               v                                             v
       +-----------------------------+               +-----------------------------+
       |       LOCAL SQLITE DB       |               |        EXTERNAL LLMs        |
       | - Room Database             |               | - Google Gemini API         |
       | - Local Datastore Prefs     |               | - OpenRouter Fallback API   |
       +-----------------------------+               +-----------------------------+
```

---

## 2. Dynamic Input Parsing Flow

When a user types a food item or workout entry in the chat, the application runs the parsing flow:

```
               [ User sends: "I had 2 boiled eggs" ]
                                 |
                                 v
                     [ Check state.apiKey ]
                       /               \
            (Key is Empty)            (Key exists)
                   /                       \
                  v                         v
        [ Mock Simulation ]       [ Send to Selected Provider ]
        - Run local regex         - Gemini or OpenRouter POST
        - scale calories          - High speed, 0.1 temp
                  \                         /
                   \                       /
                    v                     v
               [ Receive raw structured response ]
                                 |
                                 v
                  [ Run cleanAndParseJSON() ]
                                 |
                                 v
            [ Sync state.foodEntries / state.exerciseEntries ]
                                 |
                                 v
               [ persist settings to localStorage ]
                                 |
                                 v
                   [ trigger mountApp() repaint ]
```

---

## 3. WebView Javascript Bridge Interface

The communication between the native Kotlin wrapper (`MainActivity.kt`) and the frontend app (`main.js`) is managed through the `AndroidBridge` interface:

*   **`window.DaywiseAndroid.launchCamera()`**: Triggers the native Android camera intent via the FileProvider system. When the photo is taken, the bitmap is compressed, base64-encoded, and injected back into the WebView via `window.onCameraImageReady(base64)`.
*   **`window.DaywiseAndroid.updateAlarm(id, label, enabled, timeStr, message)`**: Interfaces with the Android `AlarmManager` to schedule, update, or cancel daily reminders using system notifications.
