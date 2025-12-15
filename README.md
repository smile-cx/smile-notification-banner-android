# SmileNotificationBanner

[![JitPack](https://jitpack.io/v/smile-cx/smile-notification-banner-android.svg)](https://jitpack.io/#smile-cx/smile-notification-banner-android)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-purple.svg)](https://kotlinlang.org)

A modern, customizable in-app notification banner library for Android, built with Kotlin and compatible with Java.

## Features

- **Multiple Banner Types**: Success, Info, Warning, Error, and Custom
- **Flexible Positioning**: Display banners at the top or bottom of the screen
- **Enhanced Layout**: Support for title + message, left/right images or custom views (NEW in v2.1)
- **Expandable Quick Reply**: Drag-to-expand input field for inline responses (NEW in v2.1)
- **Smooth Animations**: Beautiful slide-in/slide-out animations with intelligent singleton management
- **Haptic Feedback**: Optional vibration support with configurable duration
- **Auto-Dismiss**: Configure banners to automatically dismiss after a specified duration
- **Fully Customizable**: Customize colors, icons, layouts, images, and more
- **Click Listeners**: Handle banner and dismiss events
- **Smart Singleton**: Automatically manages multiple rapid notifications with clean transitions
- **Modern Architecture**: Built with Kotlin Coroutines and AndroidX
- **Android 15 Ready**: Full edge-to-edge support with automatic window insets handling
- **Display Cutout Support**: Works perfectly on notched/holed devices
- **Java Compatible**: Full interoperability with Java projects
- **Lifecycle Aware**: Properly handles activity lifecycle
- **Backward Compatible**: Supports Android API 21+ (Android 5.0 Lollipop)

## Requirements

- **Minimum SDK**: API 21 (Android 5.0 Lollipop)
- **Target SDK**: API 36 (Android 15)
- **Language**: Kotlin 1.9.20+ or Java 11+

## Installation

### Step 1: Add JitPack Repository

Add the JitPack repository to your root `settings.gradle.kts` (or `settings.gradle`):

**Kotlin DSL (settings.gradle.kts):**
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**Groovy (settings.gradle):**
```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

Or in your root `build.gradle` / `build.gradle.kts`:

**Kotlin DSL:**
```kotlin
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**Groovy:**
```groovy
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2: Add the Dependency

Add the library to your module's `build.gradle.kts` (or `build.gradle`):

**Kotlin DSL (build.gradle.kts):**
```kotlin
dependencies {
    implementation("com.github.smile-cx:smile-notification-banner-android:2.1.0")
}
```

**Groovy (build.gradle):**
```groovy
dependencies {
    implementation 'com.github.smile-cx:smile-notification-banner-android:2.1.0'
}
```

### Step 3: Sync Project

Sync your project with Gradle files and start using SmileBanner!

## Quick Start

### Simple Usage

Show a banner with just one line:

**Kotlin:**
```kotlin
import cx.smile.smilenotificationbanner.SmileBanner
import cx.smile.smilenotificationbanner.BannerType

SmileBanner.show(this, BannerType.SUCCESS, "Operation completed successfully!")
```

**Java:**
```java
import cx.smile.smilenotificationbanner.SmileBanner;
import cx.smile.smilenotificationbanner.BannerType;

SmileBanner.show(this, BannerType.SUCCESS, "Operation completed successfully!");
```

### Banner Types

The library provides four pre-styled banner types:

```kotlin
import cx.smile.smilenotificationbanner.*

// Success banner (green)
SmileBanner.show(this, BannerType.SUCCESS, "Success message", BannerPosition.TOP)

// Info banner (blue)
SmileBanner.show(this, BannerType.INFO, "Info message", BannerPosition.TOP)

// Warning banner (orange)
SmileBanner.show(this, BannerType.WARNING, "Warning message", BannerPosition.TOP)

// Error banner (red)
SmileBanner.show(this, BannerType.ERROR, "Error message", BannerPosition.TOP)
```

### Positioning

Display banners at the top or bottom:

```kotlin
// Show at top
SmileBanner.show(this, BannerType.INFO, "Top banner", BannerPosition.TOP)

// Show at bottom
SmileBanner.show(this, BannerType.INFO, "Bottom banner", BannerPosition.BOTTOM)
```

### Auto-Dismiss

Configure banners to automatically dismiss:

```kotlin
// Auto-dismiss after 3 seconds (3000 milliseconds)
SmileBanner.show(
    this,
    BannerType.INFO,
    "This will auto-dismiss",
    BannerPosition.TOP,
    3000L
)
```

## Advanced Usage

### Chainable Builder API

Configure banners using the fluent builder pattern:

**Kotlin:**
```kotlin
SmileBanner.make(this)
    .type(BannerType.CUSTOM)
    .message("Custom styled banner")
    .position(BannerPosition.TOP)
    .duration(5000L)
    .backgroundColor(Color.parseColor("#9C27B0"))
    .textColor(Color.WHITE)
    .dismissible(true)
    .show()
```

**Java:**
```java
SmileBanner.make(this)
    .type(BannerType.CUSTOM)
    .message("Custom styled banner")
    .position(BannerPosition.TOP)
    .duration(5000L)
    .backgroundColor(Color.parseColor("#9C27B0"))
    .textColor(Color.WHITE)
    .dismissible(true)
    .show();
```

### Vibration Feedback (v2.0)

Add haptic feedback to banners for better user experience:

```kotlin
import cx.smile.smilenotificationbanner.VibrationDuration

// Short vibration (50ms) - default when calling vibrate()
SmileBanner.make(this)
    .type(BannerType.INFO)
    .message("New message received")
    .vibrate()
    .show()

// Medium vibration (100ms)
SmileBanner.make(this)
    .type(BannerType.INFO)
    .message("Important notification")
    .vibrate(VibrationDuration.MEDIUM)
    .show()

// Long vibration (200ms)
SmileBanner.make(this)
    .type(BannerType.WARNING)
    .message("Critical alert")
    .vibrate(VibrationDuration.LONG)
    .show()
```

**Note:** The VIBRATE permission is automatically included in the library manifest and will be merged into your app's manifest. No manual permission declaration is required.

### Smart Singleton Management (v2.0)

When multiple banners are shown in quick succession (e.g., rapid in-app notifications), the library automatically:

1. Dismisses the current banner **instantly** (without exit animation)
2. Shows the new banner with its entrance animation
3. Provides clean, non-overlapping transitions

This is perfect for chat notifications, real-time updates, or any scenario with rapid successive notifications:

```kotlin
// Simulate multiple chat messages arriving
SmileBanner.make(this)
    .message("New message from Alice")
    .vibrate()
    .show()

// 300ms later, another message arrives
SmileBanner.make(this)
    .message("New message from Bob")
    .vibrate()
    .show()
// The banner from Alice disappears instantly, Bob's message shows smoothly
```

### Enhanced Layout with Title (v2.1)

Create richer notification banners with separate title and message:

```kotlin
SmileBanner.make(this)
    .type(BannerType.INFO)
    .title("Important Notification")
    .message("This banner has both a bold title and a message")
    .position(BannerPosition.TOP)
    .duration(3000L)
    .show()
```

### Left and Right Images/Views (v2.1)

Add images or custom views to the left or right side of banners:

```kotlin
// Banner with left image from drawable
SmileBanner.make(this)
    .title("New Achievement!")
    .message("You've unlocked a new badge")
    .leftImage(R.drawable.ic_star)
    .duration(3000L)
    .show()

// Banner with right image
SmileBanner.make(this)
    .title("Update Available")
    .message("Version 2.0 is ready")
    .rightImage(R.drawable.ic_download)
    .show()

// Banner with custom views
val avatarView = layoutInflater.inflate(R.layout.custom_avatar, null)
// Configure avatarView...

SmileBanner.make(this)
    .title("New Message")
    .message("Alice sent you a message")
    .leftView(avatarView)
    .show()
```

**Loading images from URLs**: The library supports image loading via callback:

```kotlin
SmileBanner.make(this)
    .title("New Message")
    .message("From Alice")
    .leftImageUrl("https://example.com/avatar.jpg", circular = true) { imageView, url, circular ->
        // Use your preferred image loading library
        Glide.with(this)
            .load(url)
            .apply(if (circular) RequestOptions.circleCropTransform() else RequestOptions())
            .into(imageView)
    }
    .show()
```

### Expandable Quick Reply (v2.1)

Add an expandable input field for quick responses - perfect for chat notifications:

```kotlin
SmileBanner.make(this)
    .type(BannerType.INFO)
    .title("New message from Alice")
    .message("Hey! How are you doing?")
    .leftImage(R.drawable.ic_person)
    .expandable(true)
    .expandableInputHint("Type your reply...")
    .expandableButtonText("Send")
    .onExpandableSubmit { text ->
        // Handle the submitted text
        sendReply(text)
        Toast.makeText(this, "Reply sent: $text", Toast.LENGTH_SHORT).show()
    }
    .duration(15000L) // Longer duration for interaction
    .show()
```

**How it works:**
1. The banner shows with a drag indicator at the bottom
2. User drags down on the indicator to reveal the input field
3. User types their reply and taps the send button
4. Your callback receives the text
5. Input is cleared and banner optionally collapses

### Click Listeners

Handle banner clicks and dismiss events:

```kotlin
SmileBanner.make(this)
    .type(BannerType.INFO)
    .message("Tap for more info")
    .position(BannerPosition.TOP)
    .onBannerClick { view ->
        // Handle banner click
        Toast.makeText(this, "Banner clicked!", Toast.LENGTH_SHORT).show()
        SmileBanner.dismissCurrent()
    }
    .onDismiss {
        // Handle dismiss event
        Log.d("SmileBanner", "Banner dismissed")
    }
    .show()
```

### Using Resource IDs

The library supports both direct values and resource IDs for strings and colors:

**String Resources:**
```kotlin
// Using string resource ID
SmileBanner.make(this)
    .type(BannerType.SUCCESS)
    .message(R.string.my_success_message) // Resource ID
    .show()

// Or using string directly
SmileBanner.make(this)
    .type(BannerType.SUCCESS)
    .message("Success message") // Direct string
    .show()
```

**Color Resources:**
```kotlin
// Using color resource ID
SmileBanner.make(this)
    .type(BannerType.CUSTOM)
    .message("Custom colors")
    .backgroundColorRes(R.color.my_banner_background) // Resource ID
    .textColorRes(R.color.my_banner_text) // Resource ID
    .show()

// Or using color int directly
SmileBanner.make(this)
    .type(BannerType.CUSTOM)
    .message("Custom colors")
    .backgroundColor(Color.parseColor("#9C27B0")) // Direct color
    .textColor(Color.WHITE) // Direct color
    .show()
```

### Custom Icons

Use your own icons:

```kotlin
SmileBanner.make(this)
    .type(BannerType.CUSTOM)
    .message("Custom icon banner")
    .icon(R.drawable.my_custom_icon)
    .show()
```

### Custom Layouts

Create completely custom banner layouts:

1. Create your custom layout XML:

```xml
<!-- res/layout/my_custom_banner.xml -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="16dp"
    android:background="#FF5722">

    <TextView
        android:id="@+id/customMessage"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textColor="@android:color/white" />
</LinearLayout>
```

2. Use it in your code:

```kotlin
// Build the banner first
val banner = SmileBanner.make(this)
    .type(BannerType.CUSTOM)
    .message("") // Message not used with custom layout
    .customLayout(R.layout.my_custom_banner)
    .build() // Use build() instead of show() to get the banner instance

// Access and customize your custom views before showing
banner.getBannerView()?.findViewById<TextView>(R.id.customMessage)?.apply {
    text = "This is my custom banner!"
}

// Now show it
banner.show()
```

### Programmatic Dismiss

Dismiss the currently showing banner:

```kotlin
SmileBanner.dismissCurrent()
```

### Configuration Options

| Method | Parameter Type | Description |
|--------|----------------|-------------|
| `type()` | `BannerType` | Banner type (SUCCESS, INFO, WARNING, ERROR, CUSTOM) |
| `title()` | `String` or `@StringRes Int` | Title text (bold, optional). **NEW in v2.1** |
| `message()` | `String` or `@StringRes Int` | Message to display (supports both string and resource ID) |
| `position()` | `BannerPosition` | Banner position (TOP or BOTTOM) |
| `duration()` | `Long` | Auto-dismiss duration in ms (0 = no auto-dismiss) |
| `dismissible()` | `Boolean` | Show/hide close button (default: true) |
| `vibrate()` | `VibrationDuration` (optional) | Enable vibration feedback (SHORT/MEDIUM/LONG). If called without parameter, uses SHORT (50ms) |
| `leftView()` | `View` | Custom view for left side. **NEW in v2.1** |
| `leftImage()` | `@DrawableRes Int` | Left side image from drawable resource. **NEW in v2.1** |
| `leftImageUrl()` | `String, Boolean, Callback` | Left side image from URL with loading callback. **NEW in v2.1** |
| `leftImageCircular()` | `Boolean` | Make left image circular. **NEW in v2.1** |
| `rightView()` | `View` | Custom view for right side. **NEW in v2.1** |
| `rightImage()` | `@DrawableRes Int` | Right side image from drawable resource. **NEW in v2.1** |
| `rightImageUrl()` | `String, Boolean, Callback` | Right side image from URL with loading callback. **NEW in v2.1** |
| `rightImageCircular()` | `Boolean` | Make right image circular. **NEW in v2.1** |
| `expandable()` | `Boolean` | Enable drag-to-expand input field. **NEW in v2.1** |
| `expandableInputHint()` | `String` or `@StringRes Int` | Hint text for expandable input. **NEW in v2.1** |
| `expandableButtonText()` | `String` or `@StringRes Int` | Button text for expandable submit. **NEW in v2.1** |
| `onExpandableSubmit()` | `(String) -> Unit` | Callback when expandable text is submitted. **NEW in v2.1** |
| `customLayout()` | `@LayoutRes Int` | Custom layout resource ID |
| `backgroundColor()` | `@ColorInt Int` | Custom background color (direct color value) |
| `backgroundColorRes()` | `@ColorRes Int` | Custom background color (color resource ID) |
| `textColor()` | `@ColorInt Int` | Custom text color (direct color value) |
| `textColorRes()` | `@ColorRes Int` | Custom text color (color resource ID) |
| `icon()` | `@DrawableRes Int` | Custom icon resource ID |
| `onBannerClick()` | `(View) -> Unit` | Click listener for banner |
| `onDismiss()` | `() -> Unit` | Callback when banner is dismissed |
| `show()` | - | Build and immediately display the banner |
| `build()` | - | Build the banner without showing (for custom view access) |

## Sample App

The project includes a complete sample app in the `sample` module that demonstrates all library features:

- All banner types (Success, Info, Warning, Error)
- Top and bottom positioning
- Auto-dismiss functionality
- Custom colors and styling
- Click listeners and callbacks
- Vibration feedback with different durations
- Rapid banner display (singleton behavior demo)
- **Banners with title and message** (NEW in v2.1)
- **Left and right images/views** (NEW in v2.1)
- **Expandable quick reply with input field** (NEW in v2.1)

### Running the Sample App

```bash
# Install on connected device/emulator
./gradlew :sample:installDebug

# Or open the project in Android Studio and run the 'sample' module
```

The sample app provides interactive buttons to test each feature.

## Testing

The library includes comprehensive unit and instrumented tests:

### Running Tests

```bash
# Run all tests
./run-tests.sh

# Or run individually:
./gradlew :smilenotificationbanner:test                    # Unit tests
./gradlew :smilenotificationbanner:connectedAndroidTest   # Instrumented tests
```

### Test Coverage

- **Unit Tests**: Test business logic, builder pattern, configuration
  - `BannerTypeTest`: Enum tests
  - `BannerPositionTest`: Position enum tests
  - `BannerBuilderTest`: Builder pattern and chaining
  - `BannerConfigTest`: Configuration data class
  - `SmileBannerTest`: Core functionality

- **Instrumented Tests**: Test Android-specific functionality
  - `SmileBannerInstrumentedTest`: Real device tests

Test reports are generated in:
- `smilenotificationbanner/build/reports/tests/`
- `smilenotificationbanner/build/reports/androidTests/`

## Architecture

SmileNotificationBanner is built with modern Android development practices:

- **Kotlin**: Written in Kotlin with full Java interoperability
- **AndroidX**: Uses modern AndroidX libraries
- **Material Design**: Follows Material Design 3 guidelines
- **Coroutines**: Uses Kotlin Coroutines for asynchronous operations
- **ViewBinding**: Supports ViewBinding for type-safe view access
- **Lifecycle Aware**: Properly handles activity lifecycle to prevent crashes

## Default Styles

The library comes with pre-styled banner types:

| Type | Background Color | Icon |
|------|------------------|------|
| SUCCESS | Green (#4CAF50) | Check mark |
| INFO | Blue (#2196F3) | Info circle |
| WARNING | Orange (#FF9800) | Warning triangle |
| ERROR | Red (#F44336) | Error circle |

All colors and icons are customizable.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

```
MIT License

Copyright (c) 2025 SmileNotificationBanner Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Acknowledgments

Inspired by the concept of [NotificationBanner](https://github.com/shasin89/NotificationBanner) by Shasindran Poonudurai, but completely reimplemented with modern Android practices.

## Support

If you find this library helpful, please star the repository!

For issues, feature requests, or questions, please open an issue on GitHub.
