# 🤝 Contributing to Daywise

Thank you for your interest in contributing to Daywise! Follow this guide to set up your environment and submit contributions.

---

## 1. Local Development Setup

To run and edit the code locally, ensure you have the following prerequisites installed:
*   [Node.js](https://nodejs.org/) (v18 or higher)
*   [Android Studio](https://developer.android.com/studio) (for native app modifications)

### **Step A: Set up Web Frontend**
1. Navigate to the `web` folder:
   ```bash
   cd web
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the local Vite development server:
   ```bash
   npm run dev
   ```
4. Open your browser and go to `http://localhost:6272` to preview.

### **Step B: Build Assets for Android**
Before compiling the Android APK, you must package the frontend single-file bundle:
1. Inside the `web` folder, build the production assets:
   ```bash
   npm run build
   ```
   *Note: This generates a standalone index.html inside `web/dist/`.*
2. Copy this file into the Android assets folder:
   *   **On Windows (PowerShell)**:
       ```powershell
       Copy-Item -Path ./dist/index.html -Destination ../app/src/main/assets/index.html -Force
       ```
   *   **On macOS / Linux**:
       ```bash
       cp ./dist/index.html ../app/src/main/assets/index.html
       ```

### **Step C: Compile and Run Android App**
1. Open the root folder in **Android Studio**.
2. Sync the project with Gradle files.
3. Plug in your Android device via USB (with USB Debugging enabled).
4. Run `app` or run the Gradle task in the terminal:
   ```bash
   ./gradlew installDebug
   ```

---

## 2. Coding Guidelines

*   **HTML & CSS**: Keep layout files semantic. Utilize vanilla CSS custom properties (variables) defined in `web/src/style.css` for all theme and spacing tokens.
*   **Javascript**: Use modern ESM syntax (ES6 modules). Organize any new API providers in separated helper files inside `web/src/`.
*   **Kotlin (Android)**: Follow Jetpack Compose best practices. Maintain database models cleanly inside the Room Entity definitions.
