HelsyRPG Android wrapper for v5.20 Player Experience

Build requirements:
- JDK 17+
- Android SDK platform 35 + build-tools
- Gradle 8.9
- Internet access to Google/Maven/Chaquopy repositories during first build

Build:
  gradle assembleDebug

APK:
  app/build/outputs/apk/debug/app-debug.apk

Architecture:
- Java WebView UI shell
- Chaquopy 17.0.0 / Python 3.13
- Original HelsyRPG runtime extracted into app-private writable storage
- HTTP server bound only to 127.0.0.1:8765
- helsy.db, config.json, saves/, backups/, arts/ are preserved across wrapper updates
