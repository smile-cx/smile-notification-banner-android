# Testing Guide

This document provides information about testing SmileNotificationBanner.

## Test Structure

### Unit Tests (`src/test/`)
Located in `smilenotificationbanner/src/test/java/cx/smile/smilenotificationbanner/`

- **BannerTypeTest.kt**: Tests for the `BannerType` enum
- **BannerPositionTest.kt**: Tests for the `BannerPosition` enum
- **BannerBuilderTest.kt**: Tests for the builder pattern and method chaining
- **BannerConfigTest.kt**: Tests for the internal configuration data class
- **SmileBannerTest.kt**: Tests for core SmileBanner functionality

### Instrumented Tests (`src/androidTest/`)
Located in `smilenotificationbanner/src/androidTest/java/cx/smile/smilenotificationbanner/`

- **SmileBannerInstrumentedTest.kt**: Tests that require Android framework

## Running Tests

### Quick Start

Run all tests with the provided script:

```bash
./run-tests.sh
```

### Individual Test Commands

```bash
# Run unit tests only
./gradlew :smilenotificationbanner:test

# Run instrumented tests (requires device/emulator)
./gradlew :smilenotificationbanner:connectedAndroidTest

# Run specific test class
./gradlew :smilenotificationbanner:testDebugUnitTest --tests "cx.smile.smilenotificationbanner.BannerBuilderTest"

# Run with coverage
./gradlew :smilenotificationbanner:testDebugUnitTestCoverage
```

### Using Android Studio

1. Open the project in Android Studio
2. Navigate to the test file
3. Click the green arrow next to the test class or method
4. View results in the Run panel

## Test Reports

After running tests, reports are generated at:

- **Unit Test Reports**: `smilenotificationbanner/build/reports/tests/testDebugUnitTest/index.html`
- **Android Test Reports**: `smilenotificationbanner/build/reports/androidTests/connected/index.html`
- **Coverage Reports**: `smilenotificationbanner/build/reports/coverage/`

## Test Dependencies

The library uses:

- **JUnit 4**: Core testing framework
- **Mockito**: Mocking framework
- **Mockito-Kotlin**: Kotlin extensions for Mockito
- **Robolectric**: Android framework simulation for unit tests
- **AndroidX Test**: Instrumented testing

## Writing New Tests

### Unit Test Example

```kotlin
package cx.smile.smilenotificationbanner

import org.junit.Assert.assertEquals
import org.junit.Test

class MyFeatureTest {

    @Test
    fun `test description in backticks`() {
        // Arrange
        val expected = "value"

        // Act
        val actual = someFunction()

        // Assert
        assertEquals(expected, actual)
    }
}
```

### Instrumented Test Example

```kotlin
package cx.smile.smilenotificationbanner

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyInstrumentedTest {

    @Test
    fun useAppContext() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Test with real Android context
    }
}
```

## Continuous Integration

The tests can be easily integrated into CI/CD pipelines:

```yaml
# Example GitHub Actions
- name: Run Unit Tests
  run: ./gradlew :smilenotificationbanner:test

- name: Run Instrumented Tests
  run: ./gradlew :smilenotificationbanner:connectedAndroidTest
```

## Test Coverage Goals

- Aim for 80%+ code coverage
- All public API methods should be tested
- Edge cases and error conditions should be covered
- Builder pattern methods should verify chaining

## Troubleshooting

### Tests Not Running?

1. Ensure Android SDK is installed
2. Check Java version (Java 11 required)
3. Sync Gradle: `./gradlew clean build`

### Instrumented Tests Failing?

1. Ensure a device/emulator is connected: `adb devices`
2. Check device API level (minimum API 21)
3. Grant necessary permissions if required

### Build Errors?

```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

## Contributing Tests

When contributing, please:

1. Write tests for new features
2. Ensure existing tests pass
3. Maintain or improve code coverage
4. Follow the existing test structure and naming conventions
5. Use descriptive test names with backticks: `` `test description` ``
