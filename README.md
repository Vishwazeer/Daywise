# Daywise — AI Health & Fitness Tracker

Daywise is a premium, privacy-first health, nutrition, and fitness tracking application designed to run **completely standalone and offline** on Android (via a hybrid WebView shell) and as a responsive web application. 

Instead of routing user logs through a centralized backend, Daywise executes natural language food and exercise parsing directly in the client using free-tier LLMs (Google Gemini & OpenRouter).

---

## 📖 Document Navigation

To learn more about specific aspects of Daywise, refer to these guides:
*   [📸 App Studio Tour (UI Screenshots)](./studio_tour.md) — Visual walkthrough of all app screens.
*   [🏗️ System Architecture & Data Flow](./architecture.md) — Technical details of the web-native hybrid bridge.
*   [🛠️ Troubleshooting & FAQ Guide](./troubleshooting.md) — Solving CORS fetch issues, alarm notifications, and web reset behaviors.
*   [🔑 Gemini API Key Setup](./how_to_get_gemini_key.md) — Step-by-step guide to generating your free Google AI Studio key.
*   [🔑 OpenRouter API Key Setup](./how_to_get_openrouter_key.md) — Setup instructions for fallback open-weight models.

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

## 📘 Comprehensive User Manual

### **1. Onboarding & Personalized Setup**
When you launch Daywise for the first time, you are guided through a 7-step onboarding quiz to define your baseline metrics:
1.  **Welcome**: Introduction to the tracking workflow.
2.  **Biological Profile**: Input your Biological Gender, Age, Height (cm), and Current Weight (kg). These are used to calculate your **Basal Metabolic Rate (BMR)** using the Mifflin-St Jeor equation.
3.  **Primary Goal**: Choose between **Lose Weight**, **Maintain Weight**, or **Gain Weight**.
4.  **Goal Aggression**: Select your preferred weekly adjustment rate:
    *   *Relaxed*: 0.25 kg/week
    *   *Moderate*: 0.50 kg/week
    *   *Aggressive*: 0.75 kg/week
5.  **Target Weight**: Set your goal weight. Daywise calculates your current BMI and estimated timeline to reach your target.
6.  **Nutrient Plan Review**: Review your recommended daily calorie target and macro targets (default: 25% Protein, 50% Carbs, 25% Fat).
7.  **API Key Configuration**: Paste your Google Gemini or OpenRouter key. If you don't have one, click **"Continue without key"** to run the app in offline mock simulation mode.

---

### **2. Chat-Based Food & Exercise Logging**
Logging is conversational. Type your logs into the bottom chat capsule and hit send.

#### **Food Logging Examples**
*   *"I had 3 scrambled eggs and 1 glass of milk for breakfast"*
*   *"A cup of white cooked rice, 150g grilled chicken breast, and garden salad"*
*   **Photo Logging**: Tap the **Image Icon** in the chat bar to capture or upload a picture of your food. The AI identifies the items, estimates weights, and calculates nutrition values.

#### **Workout Logging Examples**
*   *"Jogged for 45 minutes"*
*   *"Did 30 minutes of resistance training"*

#### **Editing & Deleting Logs**
*   If the AI estimates require adjustment, tap the **Edit (Pen) Icon** on any log card to edit item names, serving sizes, and calories/macros directly.
*   Tap the **Trash Icon** to delete any entry.
*   Tap the **Bookmark Icon** to save a meal for quick 1-tap logging in the future.

---

### **3. AI Daily Diet Planner**
*   Access the **AI Daily Diet Planner** from the sidebar menu.
*   Select your preferred **Cuisine Type** (e.g. *Indian, Mediterranean, Italian*) and **Cooking Style** (e.g. *Simple home cooking, Quick meals*).
*   Click **"Generate Diet Plan"**. The AI creates a structured menu matching your exact daily calorie budget.
*   You can expand each meal row to read recipes and click **"Log Item"** to automatically add it to your daily food logs.

---

### **4. AI Chef (Pantry Cooking)**
*   Access the **AI Chef** from the sidebar menu.
*   Type in the ingredients you currently have (e.g., *chicken, garlic, onions, yogurt*).
*   Specify your maximum calorie target for the meal, minimum protein target (g), and flavor profile (e.g., *Spicy, Savory*).
*   Click **"Cook Recipe"** to generate a step-by-step custom recipe with estimated macros.

---

### **5. Weight Tracker**
*   Open the **Weight Tracker** from the sidebar menu.
*   Input your daily weight measurements using the **+ button**.
*   The interactive line chart displays your progress over time (Week, Month, Year, All Time views) relative to your green dashed target weight line.
*   You can review and delete previous weigh-ins in the scrollable log history below the chart.

---

### **6. Alarm Reminders**
*   Go to **Menu > Reminders**.
*   Toggle morning (9:00 AM), afternoon (1:00 PM), and evening (7:00 PM) reminders.
*   Tap on the time string to launch a time-picker to customize notification delivery schedules.
*   Add custom alerts with the **"Add Custom Reminder"** button.

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

---

## 👥 Creator & Contributors

*   **Vishwazeer** (Vishwajeet Pisal Deshmukh) — Creator, Lead Developer, and Maintainer.
    *   GitHub: [Vishwazeer](https://github.com/Vishwazeer)
    *   Email: [vishwajeetpisaldeshmukh17@gmail.com](mailto:vishwajeetpisaldeshmukh17@gmail.com)

---

## 📄 License
This project is licensed under the [MIT License](./LICENSE).
