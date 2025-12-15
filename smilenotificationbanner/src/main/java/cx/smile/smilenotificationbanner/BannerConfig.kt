package cx.smile.smilenotificationbanner

import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes

/**
 * Internal configuration class for SmileBanner
 * Users should use SmileBanner.make(activity) builder API instead
 */
internal data class BannerConfig(
    val type: BannerType,
    val message: String? = null,
    @StringRes val messageRes: Int? = null,
    val position: BannerPosition = BannerPosition.TOP,
    val duration: Long = 0L, // 0 means no auto-dismiss
    val dismissible: Boolean = true,
    @LayoutRes val customLayout: Int? = null,
    @ColorInt val backgroundColor: Int? = null,
    @ColorRes val backgroundColorRes: Int? = null,
    @ColorInt val textColor: Int? = null,
    @ColorRes val textColorRes: Int? = null,
    @DrawableRes val icon: Int? = null,
    val onBannerClick: ((View) -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null,
    val vibrationDuration: VibrationDuration = VibrationDuration.NONE
)
