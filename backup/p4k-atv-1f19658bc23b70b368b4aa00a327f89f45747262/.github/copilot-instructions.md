# Copilot Instructions for OXOO-TV Android TV Codebase

## Project Overview
- This is an Android TV application using Java and Android SDK, with support for native code via JNI/CMake.
- Main app logic is in `app/src/main/java/com/files/codes/view/`.
- Native code is managed in `app/src/main/jni/` with CMake build scripts.

## Architecture
- **Activity/Fragment-based UI:**
  - `HomeActivity.java` is the main entry point for the home page, orchestrating navigation and fragment management.
  - Content is modularized into Fragments: `HomeFragment`, `MoviesFragment`, `TvSeriesFragment`, etc.
  - Fragments are dynamically loaded and managed via a `LinkedHashMap` in `HomeActivity`.
- **Data Layer:**
  - Data is fetched using `DataProvider` (see `com.files.codes.utils.DataProvider`).
  - Subscription status and preferences are managed via `PreferenceUtils`.
- **Native Integration:**
  - Native libraries are built using CMake (`app/src/main/jni/CMakeLists.txt`).
  - JNI code is used for performance-critical or platform-specific features.

## Build & Run
- Use Gradle for builds:
  - Windows: `gradlew.bat assembleDebug` or `gradlew.bat assembleRelease`
  - Linux/macOS: `./gradlew assembleDebug`
- Native code is built automatically via Gradle integration with CMake.
- APK output is in `app/build/outputs/apk/`.

## Testing & Debugging
- Standard Android testing tools (JUnit, Espresso) can be used if test files exist in `app/src/test/` or `app/src/androidTest/`.
- Debug via Android Studio or `adb logcat`.
- For native code, use Android Studio's native debugger.

## Conventions & Patterns
- **Fragment Management:** All content sections are Fragments, managed by `HomeActivity`.
- **Network/Data:** Data is loaded asynchronously via `Handler` and `DataProvider`.
- **Error Handling:** Network errors redirect to `ErrorActivity`.
- **Resource IDs:** Layouts and views use resource IDs defined in `res/layout` and `res/values`.
- **Native Libraries:** Add new native libraries via `add_library` in CMakeLists.txt and update Gradle if needed.

## Key Files & Directories
- `app/src/main/java/com/files/codes/view/HomeActivity.java`: Main UI logic and navigation
- `app/src/main/java/com/files/codes/view/fragments/`: All content Fragments
- `app/src/main/jni/`: Native code and CMake scripts
- `app/build.gradle`, `app/src/main/jni/CMakeLists.txt`: Build configuration

## Integration Points
- DataProvider: Handles all remote data fetching and caching
- PreferenceUtils: Manages user preferences and subscription status
- Native code: Interfaced via JNI for specific features

## Example: Adding a New Content Section
1. Create a new Fragment in `app/src/main/java/com/files/codes/view/fragments/`
2. Add it to the fragment management logic in `HomeActivity.java`
3. Update navigation and resource files as needed

---

If any section is unclear or missing, please specify what needs improvement or more detail.