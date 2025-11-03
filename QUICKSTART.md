# ⚡ JARVIS Launcher - Quick Start Guide

Get up and running with JARVIS Launcher in **5 minutes**!

---

## 🎯 Prerequisites

✅ Android Studio Hedgehog (2023.1.1) or newer  
✅ Android device or emulator running **Android 10+** (API 29+)  
✅ USB Debugging enabled (for physical device)

---

## 📥 Step 1: Open the Project

1. Launch **Android Studio**
2. Click **File** → **Open**
3. Navigate to the `JarvisLauncher` folder
4. Click **OK**
5. Wait for Gradle sync to complete (~2-3 minutes)

---

## 🔧 Step 2: Sync Gradle

If Gradle doesn't auto-sync:

1. Click **File** → **Sync Project with Gradle Files**
2. Wait for dependencies to download
3. Ensure no errors in the **Build** tab

---

## 🚀 Step 3: Run the App

### Option A: Using Android Studio

1. Click the **Run** button (green ▶️) or press `Shift+F10`
2. Select your device/emulator
3. Click **OK**
4. Wait for build and installation (~1-2 minutes first time)

### Option B: Using Command Line

```bash
# Windows
gradlew.bat installDebug

# Linux/Mac
./gradlew installDebug
```

---

## 🏠 Step 4: Set as Default Launcher

1. Press the **Home** button on your device
2. You'll see a launcher picker dialog
3. Select **JARVIS Launcher**
4. Tap **Always** (not "Just once")
5. You're now using JARVIS! 🎉

---

## 🎤 Step 5: Grant Permissions

When you first tap the mic button, you'll be prompted:

1. **Allow** "Record Audio" permission
    - Required for voice commands
    - Can be denied if you only want text input

2. (Optional) **Allow** "Location" permission
    - Only needed for context awareness features
    - Not required for basic launcher functionality

---

## ✨ Step 6: Try It Out!

### Test the Launcher

- **Tap** any app icon to launch it
- **Long press** an app to see its info page

### Test the AI Assistant

1. Tap the **microphone FAB** (bottom-right corner)
2. Try these commands:
   ```
   "Open Chrome"
   "Launch YouTube"
   "Hello JARVIS"
   "Turn on flashlight"
   ```

### Test Voice Input

1. In the chat, tap the **mic icon** (left of send button)
2. Speak your command
3. Tap **send** after recognition completes

---

## 🛠️ Troubleshooting

### Build Errors

**Problem**: Gradle sync fails  
**Solution**:

- File → Invalidate Caches → Invalidate and Restart
- Ensure you have JDK 17 installed

**Problem**: Compose compiler errors  
**Solution**:

- Check that `kotlinCompilerExtensionVersion = "1.5.3"`
- Verify Kotlin version matches

### Runtime Errors

**Problem**: App crashes on launch  
**Solution**:

- Check Logcat for stack trace
- Verify minSdk is 29 in `build.gradle.kts`

**Problem**: Voice input doesn't work  
**Solution**:

- Grant "Record Audio" permission in Settings
- Check device has Google Speech Services installed

**Problem**: Apps don't appear in grid  
**Solution**:

- Grant "Query All Packages" permission (should auto-grant)
- Check Logcat for PackageManager errors

---

## 📱 Testing Checklist

After installation, verify:

- [ ] App grid displays with all installed apps
- [ ] Clock shows current time (updates every second)
- [ ] Tapping an app launches it
- [ ] Long-pressing an app opens its settings
- [ ] Floating mic button appears in bottom-right
- [ ] Tapping mic button opens chat sheet
- [ ] Text input works and sends messages
- [ ] AI responds to "Hello JARVIS"
- [ ] AI responds to "Open Chrome" (or any app)
- [ ] Voice button requests microphone permission
- [ ] AI mode toggle switches between Local/Cloud

---

## 🔄 Resetting to Default Launcher

To switch back to your original launcher:

1. Open **Settings** → **Apps**
2. Tap the **⚙️ gear icon** → **Default apps**
3. Tap **Home app**
4. Select your original launcher (e.g., Pixel Launcher)

Or:

1. Open **Settings**
2. Search for "default home"
3. Select a different launcher

---

## 🎨 Customization (Advanced)

### Change Theme Colors

Edit `app/src/main/java/com/jarvis/launcher/ui/theme/Theme.kt`:

```kotlin
primary = Color(0xFF6366F1),  // Your color here
```

### Add More AI Commands

Edit `app/src/main/java/com/jarvis/launcher/ai/engine/AiEngine.kt`:

```kotlin
when {
    lowerCommand.contains("your phrase") -> {
        return AiResponse("Your response")
    }
}
```

### Change App Grid Columns

Edit `app/src/main/java/com/jarvis/launcher/ui/launcher/AppGrid.kt`:

```kotlin
columns = GridCells.Fixed(4),  // Change to 3, 5, etc.
```

---

## 📚 Next Steps

1. **Read the README.md** for full feature documentation
2. **Check PROJECT_SUMMARY.md** for architecture overview
3. **Review TODO comments** in code for extension ideas
4. **Start with Day 2 tasks** (see README)

---

## 🆘 Getting Help

### Documentation

- **README.md** - Complete feature guide
- **PROJECT_SUMMARY.md** - Architecture & metrics
- **Code Comments** - Every file is heavily documented

### Debugging

- **Logcat** - View all app logs (filter by "JARVIS" or package name)
- **Compose Inspector** - Debug UI hierarchy in Android Studio

### Common Issues

1. **App not appearing in launcher picker**
    - Check AndroidManifest.xml has HOME intent filter
    - Reinstall the app

2. **AI doesn't recognize app names**
    - Make sure app is installed and has launcher intent
    - Try full app name (e.g., "Google Chrome" not "Chrome")

3. **Voice recognition fails immediately**
    - Check internet connection (Google services required)
    - Try restarting the app

---

## ⚡ Pro Tips

1. **Fast Reload**: Use `Ctrl+Shift+F10` (Windows) or `Cmd+Shift+R` (Mac)
2. **Compose Preview**: Add `@Preview` to composables for live preview
3. **Logcat Filters**: Use `tag:VoiceService OR tag:AiEngine` to filter logs
4. **Break on Exceptions**: Enable in Debug settings for easier troubleshooting

---

## 🎉 You're Ready!

You now have a fully functional JARVIS AI launcher running on your device!

**What to try next:**

- Experiment with voice commands
- Customize the theme
- Add new AI command patterns
- Integrate with your smart home

---

**Happy Coding!** 🚀

> "The best way to predict the future is to invent it." - Alan Kay
