# 🛠️ Troubleshooting & FAQ Guide

This guide helps you resolve common configuration errors, runtime issues, and setup bugs.

---

## 1. How to Fix "Failed to fetch" Chat Errors

If your chat returns a `⚠️ Failed to fetch` warning when sending text or images, check the following:

### **Case A: You are testing in a standard Web Browser (Web Version)**
*   **The Cause**: Some API providers (like NVIDIA) block direct browser requests due to CORS (Cross-Origin Resource Sharing) policies.
*   **The Fix**: Use **Google Gemini** or **OpenRouter** which allow CORS, or deploy a backend proxy. Alternatively, run the app inside the Android app package where CORS is bypassed.
*   **Ad-blockers**: Ad-blockers or privacy extensions (e.g., Brave shields, uBlock Origin) often block requests to AI domains. Disable them for the site and try again.

### **Case B: Invalid or Expired API Keys**
*   **The Cause**: If your saved API key is invalid, wrong, or has expired quota limits, the fetch call will fail.
*   **The Fix**: Go to **Menu > Settings** and re-verify your API key. If you don't have a key, delete the text completely to run in **Mock Simulation Mode**.

---

## 2. Alarms & Reminders Not Triggering (Android)

If daily notifications are not popping up:

### **Case A: Missing Permissions**
*   Go to your phone **Settings > Apps > Daywise**.
*   Ensure **Notifications** are enabled.
*   Ensure **Alarms & Reminders** permission is allowed (requires `USE_EXACT_ALARM` which is declared in our Manifest).

### **Case B: Battery Optimization & Task Killers**
*   Some aggressive Android skins (MIUI, HyperOS, OneUI) put apps to sleep, killing background alarm events.
*   Go to **App Info > Battery Saver** and set it to **"No Restrictions"**.
*   Enable **Autostart** for Daywise in the system settings.

---

## 3. Data Resets on Web Version Refresh

*   **The Question**: *"Why does the web version force me to do onboarding every time I refresh the page?"*
*   **The Answer**: This is an intentional security design! To prevent users' private API keys and log histories from being stored permanently in shared web browsers, the web build detects standard web environments and wipes `localStorage` on page reload.
*   **The Solution**: If you want persistent tracking, run the application inside the packaged **Android App** where your settings are permanently stored.
