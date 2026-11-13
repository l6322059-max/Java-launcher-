# ✨ Java Launcher

A fast, colorful Android home screen written in Java. No ads, no tracking, and no third-party runtime dependencies.

## Features
- Sorted app grid with a live clock and greeting
- Instant app search
- Tap an app to launch it
- Long-press an app to open its system app settings
- Back button clears search and returns to the top
- Refreshes the app list when packages change

## Requirements
- Android 8.0 (API 26) or newer
- JDK 17
- Android Gradle Plugin 8.5.2
- Gradle 8.7

## Build an APK with GitHub Actions
1. Open the **Actions** tab in this repository.
2. Select **Build APK**.
3. Tap **Run workflow** (or push a commit to `main`).
4. When the run completes, download the `JavaLauncher-debug` artifact.
5. The workflow also publishes a GitHub Release containing `JavaLauncher.apk`.

## Build locally
```bash
gradle assembleDebug --no-daemon
```
The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Set up GitHub Pages
In **Settings → Pages**, choose **Deploy from a branch**, select `main` and `/docs`, then save.

## License
Add a license file before redistributing if you intend to publish this project.
