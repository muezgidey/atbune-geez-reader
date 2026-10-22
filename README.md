# መልክዐ ኣቡነ ቴዎድሮስ — Ge’ez reader

An offline Android reader built with Kotlin and Jetpack Compose. It includes a one-page reader, page picker (source pages 0–20), Ge’ez text search, local bookmarks, reading progress, light/dark themes, and settings for typeface, text size, line spacing, and text color. The parchment background is a subtle Brana-inspired motif.

## Text and font status

The bundled `book_draft.json` is copied byte-for-byte from the supplied starter draft (SHA-256 `B6C94A5B009B40FB45D7408C00A59A710213ADBA875BB68DBE77EB4C8F7BDEF7`). Its source marks every page for manual comparison. It is not an approved liturgical transcription. Before distributing the app, compare pages 0–20 with the supplied `ATBUNE.pdf` (and the source DOCX if available), checking Ethiopic characters, spelling, line breaks, punctuation, and uncertain readings. The PDF is deliberately not bundled in the app.

No custom font file is included: there is no verified license and full-book glyph coverage available. The typeface setting switches between Android's system family and serif fallback; actual Ethiopic glyph coverage depends on the device font.

The app requests no network permission and uses no account, ads, analytics, or backend. Preferences, reading position, and bookmarks stay on-device.

## Build without Android Studio

The included GitHub Actions workflow installs Java 17, Android SDK API 36/build tools, and Gradle 8.13 on GitHub's hosted runner. It runs the JVM unit tests and assembles a debug APK. The APK is uploaded as the `atbune-debug-apk` workflow artifact for 30 days.

### Upload the project to GitHub from Windows

1. Open the project folder `outputs\atbune-geez-reader` from this repository/workspace. It must be the folder containing `settings.gradle.kts`, `app`, and `.github`.
2. Create an empty repository at [github.com/new](https://github.com/new). Choose a repository name and visibility. Leave README/license initialization unchecked so GitHub creates an empty repository.
3. In PowerShell, change to the project directory, then set the URL below to the HTTPS URL shown by GitHub and run:

   ```powershell
   cd "D:\AI Agents\cfd-simulation-engineer\outputs\atbune-geez-reader"
   git init
   git add .
   git commit -m "Add offline Ge'ez reader Android app"
   git branch -M main
   git remote add origin https://github.com/YOUR-USERNAME/YOUR-REPOSITORY.git
   git push -u origin main
   ```

   Git for Windows is required for these commands. Sign in if GitHub prompts you. The commit includes the hidden `.github/workflows/build-apk.yml` file.

### Run the workflow and download the APK

1. In the GitHub repository, open **Actions** and select **Build Android debug APK**. The first push starts it automatically. You can also choose **Run workflow** and then **Run workflow** to start it manually.
2. Open the run and wait for the **Unit tests and debug APK** job to finish successfully.
3. On the run summary page, find **Artifacts**, select **atbune-debug-apk**, and download the ZIP. Extract it to obtain `app-debug.apk`.

If the job fails, open the failed step's log. A failed test or SDK/dependency resolution error means no downloadable APK is produced by that run.

### Install on an Android phone

1. Transfer `app-debug.apk` to the phone (USB, a private cloud drive, or another method you trust).
2. Open the APK in the phone's Files app. If Android asks, allow that app to install unknown apps in Android Settings, then return and install. You can turn that permission off after installation.
3. Launch **መልክዐ ኣቡነ ቴዎድሮስ** from the app drawer.

This is a debug APK intended for testing. Keep the original draft private until its text has been manually verified.

## Project details

- Minimum Android: API 26 (Android 8.0); compile/target SDK: API 36.
- Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin 2.2.20.
- Offline book asset and DataStore-backed preferences.
- Workflow: `.github/workflows/build-apk.yml`.
