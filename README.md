# Daily Discipline Task Manager 📋✅

Android Kotlin app: **Add daily tasks → Check completion → Get customizable reminder notifications**.

[![Live APK](https://img.shields.io/badge/Download-APK-green?style=for-the-badge)](https://github.com/RJSLabbert/Daily_Discipline_Tracker/releases/download/v2.0.0/app-debug.apk)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-orange?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API_26+-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Version](https://img.shields.io/badge/Version-2.0.0-blue?style=flat-square)](https://github.com/RJSLabbert/Daily_Discipline_Tracker/releases)

## 🎯 Screenshots

<p float="left">
  <img src="screenshots/home.png" width="200" alt="Home Screen" />
  <img src="screenshots/tasks.png" width="200" alt="Tasks Screen" />
  <img src="screenshots/settings.png" width="200" alt="Settings Screen" />
</p>

## ✨ Features

### 🏠 Home Screen
- 📊 Daily progress overview
- 🔘 Quick access to Tasks and Settings
- 🔔 Notification status display

### 📝 Task Management
- ➕ **Add/Remove Tasks**: Simple task management
- ☑️ **Checkbox Completion**: Visual feedback (green highlight)
- 🔄 **Reset All**: Uncheck all tasks with one tap

### ⚙️ Customizable Settings
- 🕐 **Start Time**: Set when reminders begin
- 🕐 **End Time**: Set when reminders stop
- ⏱️ **Frequency**: Choose reminder interval
  - Every 15 minutes
  - Every 30 minutes
  - Every 45 minutes
  - Every 1 hour
  - Every 2 hours
- 🔔 **Toggle**: Enable/disable notifications

### 🔔 Smart Notifications
- Only triggers if tasks are incomplete
- Respects your custom schedule
- Auto-resets tasks daily

## 🛠️ Tech Stack
- Kotlin 1.9+
- Android SDK 34 / Min 26
- AlarmManager + NotificationManager
- SharedPreferences
- Multi-Activity Architecture

## 🚀 Quick Setup
1. Clone: git clone https://github.com/RJSLabbert/Daily_Discipline_Tracker.git
2. Open in Android Studio
3. Build & Run on device/emulator
4. Grant notification permission when prompted

## 📁 Project Structure
app/src/main/
├── java/com/example/dailydiscipline/
│ ├── MainActivity.kt (Home screen)
│ ├── TasksActivity.kt (Task management)
│ ├── SettingsActivity.kt (Settings)
│ ├── NotificationReceiver.kt (Handles notifications)
│ └── BootReceiver.kt (Boot handling)
├── res/layout/
│ ├── activity_main.xml (Home layout)
│ ├── activity_tasks.xml (Tasks layout)
│ └── activity_settings.xml (Settings layout)
└── AndroidManifest.xml

## 🔧 Troubleshooting

<details>
<summary>Click to Expand</summary>

### 1. No Notifications Appearing
**Fix**:
- Settings > Apps > Daily Discipline > Permissions > Enable Notifications
- Android 12+: Enable Alarms & Reminders permission
- Disable battery optimization for app

### 2. Tasks Not Saving
**Fix**:
- Clear app data: Settings > Apps > Daily Discipline > Storage > Clear Data
- Reinstall app

### 3. Settings Not Applying
**Fix**:
- Make sure to tap "Save Settings" button
- Check that End Time is after Start Time

### 4. Permission Denied on Android 13+
**Fix**:
- Manually enable notifications in device settings

</details>

## 📋 Version History

| Version | Changes |
|---------|---------|
| v2.0.0 | Home screen, Settings, Custom notifications |
| v1.0.0 | Initial release, Basic task management |

## 📄 License
MIT License - See [LICENSE](LICENSE)

## 👤 Author
**RJS Labbert**
- GitHub: [@RJSLabbert](https://github.com/RJSLabbert)

---
