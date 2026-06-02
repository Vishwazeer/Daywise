# Daywise — AI Health & Fitness Tracker

Daywise is an intelligent conversational health logging and fitness tracking companion. Instead of manually searching database entries, you can simply message the AI chatbot to instantly log your food consumption and exercise routines.

---

## ✨ Features

- **Health Tracker Chat**: Chat-based logging that parses natural text inputs into structured nutrition/exercise logs.
- **AI Daily Diet Planner**: Personalized daily meal plans tailored to achieve your metabolic weight goals and target calorie budget.
- **AI Chef Recipes**: Cookbook-style recipe ideas crafted on-demand using exactly the ingredients in your pantry.
- **Weight Tracker**: Interactive progress chart to log daily weight entries and trace achievements against target weight lines.
- **Daily Targets & Reminders**: Configure custom calorie macro-splits and schedule daily logs notifications.
- **Premium User Experience**: Harmonies green/tan palette, high-contrast inputs, smooth page transitions, and interactive nutrition loader animations with fun facts.

---

## 🛠️ Tech Stack

### Web client (`/web`):
- HTML5, Vanilla CSS
- JavaScript (ES6+)
- Vite (Dev Server & Bundler)

### Android wrapper (`/app`):
- Kotlin & Jetpack Compose
- Room Database (Local Logging)
- DataStore Preferences (Configuration Settings)
- Google AI Client SDK (Direct Gemini Integration)

---

## 🚀 Getting Started

### Run Web Preview:
1. Navigate to web directory:
   ```bash
   cd web
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Launch live HMR dev server:
   ```bash
   npm run dev
   ```

### Build Android App:
1. Open root folder in Android Studio.
2. Compile and install debug build:
   ```bash
   ./gradlew installDebug
   ```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
