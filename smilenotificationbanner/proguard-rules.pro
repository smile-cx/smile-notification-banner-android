# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep SmileBanner public API
-keep public class cx.smile.smilenotificationbanner.SmileBanner {
    public *;
}

-keep public class cx.smile.smilenotificationbanner.SmileBanner$Builder {
    public *;
}

-keep public class cx.smile.smilenotificationbanner.BannerType {
    *;
}

-keep public class cx.smile.smilenotificationbanner.BannerPosition {
    *;
}
