#!/bin/bash

echo "Running SmileNotificationBanner Tests"
echo "======================================"
echo ""

echo "1. Running unit tests..."
./gradlew :smilenotificationbanner:test

echo ""
echo "2. Running instrumented tests (requires connected device/emulator)..."
./gradlew :smilenotificationbanner:connectedAndroidTest

echo ""
echo "3. Generating test coverage report..."
./gradlew :smilenotificationbanner:testDebugUnitTest --info

echo ""
echo "Tests completed! Check the reports:"
echo "  - Unit tests: smilenotificationbanner/build/reports/tests/testDebugUnitTest/index.html"
echo "  - Android tests: smilenotificationbanner/build/reports/androidTests/connected/index.html"
