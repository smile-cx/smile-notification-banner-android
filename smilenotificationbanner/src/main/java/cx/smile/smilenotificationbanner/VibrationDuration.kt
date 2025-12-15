package cx.smile.smilenotificationbanner

enum class VibrationDuration(val milliseconds: Long) {
    SHORT(50),
    MEDIUM(100),
    LONG(200),
    NONE(0)
}
