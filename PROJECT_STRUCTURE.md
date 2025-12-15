# Project Structure

## Overview

SmileNotificationBanner is organized into two main modules:
- `smilenotificationbanner`: The library module
- `sample`: Demo application

## Directory Structure

```
SmileNotificationBanner/
├── smilenotificationbanner/              # Library module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/cx/smile/smilenotificationbanner/
│   │   │   │   ├── SmileBanner.kt       # Main banner class
│   │   │   │   ├── BannerConfig.kt      # Internal configuration
│   │   │   │   ├── BannerType.kt        # Banner type enum
│   │   │   │   └── BannerPosition.kt    # Position enum
│   │   │   ├── res/
│   │   │   │   ├── layout/              # Default banner layout
│   │   │   │   ├── drawable/            # Icons (success, info, warning, error, close)
│   │   │   │   ├── anim/                # Slide animations (top/bottom)
│   │   │   │   └── values/              # Colors, strings, dimens, styles
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                        # Unit tests
│   │   │   └── java/cx/smile/smilenotificationbanner/
│   │   │       ├── BannerTypeTest.kt
│   │   │       ├── BannerPositionTest.kt
│   │   │       ├── BannerBuilderTest.kt
│   │   │       ├── BannerConfigTest.kt
│   │   │       └── SmileBannerTest.kt
│   │   └── androidTest/                 # Instrumented tests
│   │       └── java/cx/smile/smilenotificationbanner/
│   │           └── SmileBannerInstrumentedTest.kt
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── consumer-rules.pro
│
├── sample/                               # Sample application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/cx/smile/smilenotificationbanner/sample/
│   │   │   │   └── MainActivity.kt      # Demo activity
│   │   │   ├── res/
│   │   │   │   ├── layout/              # activity_main.xml
│   │   │   │   ├── values/              # strings, colors, themes
│   │   │   │   └── drawable/            # launcher icons
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│
├── gradle/                               # Gradle wrapper
├── build.gradle.kts                      # Root build file
├── settings.gradle.kts                   # Module configuration
├── gradle.properties                     # Project properties
├── gradlew                               # Gradle wrapper script (Unix)
├── gradlew.bat                           # Gradle wrapper script (Windows)
├── run-tests.sh                          # Test runner script
│
├── LICENSE                               # MIT License
├── README.md                             # Main documentation
├── CHANGELOG.md                          # Version history
├── CONTRIBUTING.md                       # Contribution guidelines
├── TESTING.md                            # Testing guide
├── PROJECT_STRUCTURE.md                  # This file
├── .gitignore                            # Git ignore rules
└── local.properties.template             # Template for local config
```

## Key Files

### Library Module

**Core Files:**
- `SmileBanner.kt`: Main API entry point with builder pattern
- `BannerConfig.kt`: Internal configuration data class
- `BannerType.kt`: Enum for SUCCESS, INFO, WARNING, ERROR, CUSTOM
- `BannerPosition.kt`: Enum for TOP, BOTTOM

**Resources:**
- `res/layout/smile_banner_default.xml`: Default banner layout with CardView
- `res/drawable/smile_banner_icon_*.xml`: Vector icons for each banner type
- `res/drawable/smile_banner_close.xml`: Close button icon
- `res/anim/smile_banner_slide_*.xml`: Slide animations
- `res/values/colors.xml`: Default color palette
- `res/values/strings.xml`: Default strings
- `res/values/dimens.xml`: Dimensions and sizing
- `res/values/styles.xml`: Animation styles

**Build Files:**
- `build.gradle.kts`: Library configuration
- `proguard-rules.pro`: ProGuard rules for library
- `consumer-rules.pro`: Rules for library consumers

### Sample App

**Files:**
- `MainActivity.kt`: Demonstrates all library features
- `res/layout/activity_main.xml`: UI with buttons for each demo
- `res/values/strings.xml`: All demo strings
- `AndroidManifest.xml`: App configuration

### Documentation

- `README.md`: Main documentation with usage examples
- `TESTING.md`: Comprehensive testing guide
- `CHANGELOG.md`: Version history and changes
- `CONTRIBUTING.md`: Contribution guidelines
- `PROJECT_STRUCTURE.md`: This file

### Configuration

- `settings.gradle.kts`: Module includes configuration
- `build.gradle.kts` (root): Top-level build configuration
- `gradle.properties`: Project-wide properties
- `.gitignore`: Files to ignore in version control

## Package Structure

**Library Package:** `cx.smile.smilenotificationbanner`
- SmileBanner (main class)
- SmileBanner.Builder (fluent builder)
- BannerType (enum)
- BannerPosition (enum)
- BannerConfig (internal)

**Sample Package:** `cx.smile.smilenotificationbanner.sample`
- MainActivity

## Test Structure

**Unit Tests (JUnit + Mockito):**
- Test business logic without Android framework
- Mock Activity and View dependencies
- Test builder pattern and configuration
- Located in: `smilenotificationbanner/src/test/`

**Instrumented Tests (AndroidX Test):**
- Test with real Android framework
- Require connected device or emulator
- Located in: `smilenotificationbanner/src/androidTest/`

## Build Outputs

When you build the project:

**Library:**
- AAR file: `smilenotificationbanner/build/outputs/aar/`
- Test reports: `smilenotificationbanner/build/reports/tests/`
- Android test reports: `smilenotificationbanner/build/reports/androidTests/`

**Sample:**
- APK: `sample/build/outputs/apk/`

## Dependencies

### Library Dependencies
- AndroidX Core, AppCompat, Material, ConstraintLayout, CardView
- Kotlin Coroutines
- Test frameworks (JUnit, Mockito, Robolectric, AndroidX Test)

### Sample Dependencies
- The library module (`:smilenotificationbanner`)
- Material Components (for demo UI)

## Version Information

- **Package**: `cx.smile.smilenotificationbanner`
- **Min SDK**: 21 (Android 5.0 Lollipop)
- **Target SDK**: 36 (Android 15)
- **Kotlin**: 1.9.20
- **Gradle**: 8.2
- **License**: MIT

## Getting Started

1. **Clone the repository**
2. **Sync with Gradle**: `./gradlew build`
3. **Run tests**: `./run-tests.sh`
4. **Run sample app**: `./gradlew :sample:installDebug`

## For Contributors

See `CONTRIBUTING.md` for:
- Code style guidelines
- Pull request process
- Testing requirements
- Documentation standards
