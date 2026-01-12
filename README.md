# Daily Discipline Task Manager 📋✅

Android Kotlin app: **Add daily tasks → Check completion → Get reminder notifications until done**.

[![Live APK](https://img.shields.io/badge/Download-APK-green?style=for-the-badge)](https://github.com/RJSLabbert/Daily_Discipline_Tracker/releases/download/v1.0.0/app-debug.apk)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-orange?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API_26+-3DDC84?logo=android&logoColor=white)](https://developer.android.com)

## 🎯 Demo
![App Demo](demo.gif) *(Record emulator > Upload GIF/APK screenshot.)*

## ✨ Features
- ➕ **Add/Remove Tasks**: Simple task management.
- ☑️ **Checkbox Completion**: Visual feedback (green highlight).
- 🔔 **Smart Notifications**: 5PM-11PM every 30 mins if incomplete.
- 🔄 **Daily Auto-Reset**: Tasks uncheck at midnight.
- 💾 **Persistent Storage**: SharedPreferences saves data.
- 📱 **Permissions**: Notifications/Alarms handled.

## 🛠️ Tech Stack
- Kotlin 1.9+
- Android SDK 34 / Min 26
- AlarmManager + NotificationManager
- SharedPreferences

## 🚀 Quick Setup
1. Clone:
   git clone https://github.com/RJSLabbert/daily-discipline-android.git
   
2. Open in Android Studio
3. Build & Run on device/emulator
4. Grant notification permission when prompted

## 📅 Notification Schedule
| Time | Status |
|------|--------|
| 5:00 PM | First reminder |
| 5:30 PM → 11:00 PM | Every 30 mins |
| *Only if tasks incomplete* | |

## 🔧 Troubleshooting (Exact Error Logs + Fixes)

<details>
<summary>Click to Expand All Logs</summary>

### 1. No Notifications Appearing
**Issue**: App runs but no notifications at scheduled times.

**Fix**:
- Settings > Apps > Daily Discipline > Permissions > Enable Notifications.
- Android 12+: Settings > Apps > Daily Discipline > Alarms & Reminders > Allow.
- Disable battery optimization for app.

### 2. Tasks Not Saving
**Issue**: Tasks disappear after closing app.

**Fix**:
- Check storage permissions.
- Clear app data: Settings > Apps > Daily Discipline > Storage > Clear Data.
- Reinstall app.

### 3. Build Errors - Unresolved References
**Exact Log**: Unresolved reference: 'gson' or similar.

**Fix**:
- File > Sync Project with Gradle Files.
- Build > Clean Project > Rebuild Project.
- Check build.gradle.kts dependencies.

### 4. Permission Denied on Android 13+
**Exact Log**: Permission denied for POST_NOTIFICATIONS

**Fix**:
- Add to Manifest: uses-permission android:name="android.permission.POST_NOTIFICATIONS"
- Request permission at runtime (already in code).
- Manually enable in device settings.

</details>

## 📄 License
MIT License - See [LICENSE](LICENSE)

## 👤 Author
**RJS Labbert**
- GitHub: [@RJSLabbert](https://github.com/RJSLabbert)

---
