# Mangako

Mangako is an Android application designed to help you manage and track your Manga collection. Keep track of the series you own and update the specific volumes you have acquired.

## Features

- **Collection Management**: Add and organize your manga series.
- **Volume Tracking**: Keep detailed records of which volumes you own.
- **Modern UI**: Built with Jetpack Compose and Material 3 for a smooth, modern experience.

## Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Architecture**: MVVM (implied by typical Android modern architecture)
- **Network**: [Retrofit](https://square.github.io/retrofit/) with Gson
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
- **Database**: [Room](https://developer.android.com/training/data-storage/room)
- **Preferences**: DataStore

## Prerequisites

- Android Studio Koala or newer (recommended)
- JDK 11 or higher
- Android SDK (minSdk 25, targetSdk 34)

## Setup & Building

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/mangako.git
   cd mangako
   ```

2. **Open in Android Studio:**
   - Launch Android Studio.
   - Select "Open" and navigate to the project directory.
   - Let Gradle sync complete.

3. **Build and Run:**
   - Connect an Android device or start an emulator.
   - Click the **Run** button (green play icon) in Android Studio.
   - Or build via command line:
     ```bash
     ./gradlew assembleDebug
     ```

## Configuration

### Signing (Release Builds)
The project is configured to look for keystore information in environment variables or a `keystore.properties` file in the root directory.

**Environment Variables:**
- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

**keystore.properties format:**
```properties
storeFile=/path/to/keystore.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```
*Note: This is only required for signing release builds. Debug builds will use the default Android debug keystore.*

## License

This project is licensed under the terms found in the [LICENSE](LICENSE) file.

## Developer

Developed by gab3-dev.
Check out my profile at [github.com/gab3-dev](https://github.com/gab3-dev).
