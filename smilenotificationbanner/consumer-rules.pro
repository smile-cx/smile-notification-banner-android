# Consumer ProGuard rules for SmileNotificationBanner

# Keep public API classes and their public members
-keep public class cx.smile.smilenotificationbanner.SmileBanner {
    public *;
}

-keep public class cx.smile.smilenotificationbanner.SmileBanner$Builder {
    public *;
}

-keep public enum cx.smile.smilenotificationbanner.BannerType {
    *;
}

-keep public enum cx.smile.smilenotificationbanner.BannerPosition {
    *;
}
