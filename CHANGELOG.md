# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.5.0] - 2025-01-22

### Added
- **Pending Banner (Cross-Activity) Feature**: Schedule banners to be shown on the next activity
  - `SmileBanner.schedulePending(context)` - Create a builder for scheduling pending banners
  - `SmileBanner.showPendingIfAvailable(activity)` - Show pending banner if one exists
  - `SmileBanner.clearPending()` - Clear pending banner without showing
  - `.schedule()` terminal method for pending banner builders
  - Thread-safe implementation with synchronized access and volatile fields
  - One pending banner at a time (replaceable)
  - Auto-clears after showing (one-time use)
  - Full documentation with usage examples

### Enhanced
- Builder class now supports nullable activity parameter for pending mode
- Added internal `createBannerConfig()` helper method for better code reuse
- Added validation to prevent misuse of pending vs immediate banner APIs
- Sample app now includes pending banner demo with SecondActivity

### Technical Details
- Added `@Volatile pendingBanner: BannerConfig?` static field for cross-activity storage
- Added `pendingBannerLock` object for thread-safe operations
- Comprehensive test suite with 19 new unit tests covering:
  - Basic API functionality
  - Thread safety (concurrent access)
  - Edge cases and error handling
  - Full workflow testing

### Use Cases
- Show success banner after completing a transfer in one activity and returning to another
- Display notifications when an activity finishes before it can show a banner
- Seamless banner display across activity transitions

## [1.0.0] - 2025-12-15

### Added
- Initial release of SmileNotificationBanner
- Package: `cx.smile.smilenotificationbanner`
- Android 15 (API 36) support with edge-to-edge compatibility
- Support for multiple banner types (SUCCESS, INFO, WARNING, ERROR, CUSTOM)
- Flexible positioning (TOP, BOTTOM)
- Smooth slide-in/slide-out animations
- Auto-dismiss functionality with configurable duration
- Full customization support:
  - Custom colors (background and text) - supports both direct values and resource IDs
  - Custom icons
  - Custom layouts
  - Messages support both strings and string resource IDs
- Chainable builder API for fluent configuration: `SmileBanner.make(this).type(...).message(...).show()`
- Click listeners for banner and dismiss events
- Java and Kotlin compatibility with `@JvmStatic` annotations
- Lifecycle-aware implementation
- Material Design 3 styling
- Backward compatibility with Android API 21+
- Complete sample app demonstrating all features
- Comprehensive test suite:
  - 5 unit test classes with 30+ test cases
  - Instrumented tests for Android-specific functionality
  - Mockito and Robolectric support
- Comprehensive documentation with Java and Kotlin examples

### Technical Details
- Built with Kotlin 1.9.20
- Uses Kotlin Coroutines for async operations
- AndroidX dependencies with WindowInsetsCompat for edge-to-edge support
- ViewBinding support
- Internal configuration class (BannerConfig) to keep public API simple
- Automatic window insets handling for status bar and navigation bar
- Display cutout support for notched devices
- Minimum SDK: API 21 (Android 5.0)
- Target SDK: API 36 (Android 15)

### API Design
- Clean single-entry API: `SmileBanner.make(activity)` returns a builder
- Two build modes:
  - `.show()` - builds and displays immediately
  - `.build()` - returns SmileBanner instance for custom view access before showing
- Removed redundant `BannerConfig.Builder` for cleaner API
