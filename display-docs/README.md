# Daywise — AI Health & Fitness Tracker

Daywise is a premium, privacy-first health, nutrition, and fitness tracking application. It is designed to run **completely standalone and offline** on Android (via a hybrid WebView shell) and as a responsive web app. 

Instead of routing user logs through a centralized backend, Daywise executes natural language food and exercise parsing directly in the client using free-tier LLMs (Google Gemini & OpenRouter). 

---

## 🚀 Key Features

*   **Natural Language Chat Logging**: Simply tell the app what you ate (e.g., *"3 boiled eggs and a toast"*) or your workout (e.g., *"ran for 30 minutes"*). The local AI parses it instantly into structured calorie and macro metrics.
*   **Offline Standalone Architecture**: The Android app bundles all web assets directly inside the package assets, bypassing the need for a web host or dev server.
*   **AI Daily Diet Planner**: Generates personalized, calorie-aligned daily meal plans matching your biological profile and macro targets.
*   **AI Chef Recipes**: Recommends healthy recipes on-demand based on ingredients currently in your pantry.
*   **Weekly Summary & Metrics**: A beautiful dashboard tracking weekly budgets, macro splits, and historical progress.
*   **Weight Tracker**: Interactive visual line chart (rendered via custom Compose Canvas in the native app) plotting weight entries against your goal weights.
*   **Local Alarms & Reminders**: Native Android alarms scheduled and updated directly from the frontend to keep your tracking routine on track.

---

## 🛠️ Technology Stack

### **Android Application (Shell & Native Bindings)**
*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Material 3)
*   **Database**: Room DB (for local structured food, workout, weight, and chat storage)
*   **Preferences**: DataStore Preferences (for persistent settings)
*   **Core WebView Components**: Android WebView, `WebViewAssetLoader` (for secure local asset loading), and custom `@JavascriptInterface` bridges for camera, gallery, and system alarm triggers.

### **Frontend Web App (Chat UI & AI Orchestration)**
*   **Core**: HTML5, Vanilla CSS3 (curated forest green & warm tan palette), Vanilla JavaScript (ES6 Modules)
*   **Build Tool**: Vite (bundling HTML, CSS, and JS into a single-file `index.html` payload using `vite-plugin-singlefile` for offline loading)
*   **Icon Library**: Lucide Icons
*   **Telemetry**: Vercel Speed Insights (conditionally loaded in web deployments)

---

## 🔒 Privacy & Key Security

Daywise places absolute control in the hands of the user:
1.  **Direct API Routing**: Your API keys (Gemini, OpenRouter) are saved locally on your device and sent directly to the AI endpoints. They are never transmitted to any third-party backend.
2.  **Web Version Autosetup Reset**: To prevent keys from being exposed on public browser deployments (e.g., Vercel), the web version detects if it is running outside the Android app, clearing `localStorage` and resetting settings on every page reload or refresh.
