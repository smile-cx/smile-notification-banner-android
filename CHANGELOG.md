# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-12-15

### Added
- Initial release of SmileNotificationBanner
- Package: `cx.smile.smilenotificationbanner`
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
- AndroidX dependencies
- ViewBinding support
- Internal configuration class (BannerConfig) to keep public API simple
- Minimum SDK: API 21 (Android 5.0)
- Target SDK: API 34 (Android 14)

### API Design
- Clean single-entry API: `SmileBanner.make(activity)` returns a builder
- Two build modes:
  - `.show()` - builds and displays immediately
  - `.build()` - returns SmileBanner instance for custom view access before showing
- Removed redundant `BannerConfig.Builder` for cleaner API
