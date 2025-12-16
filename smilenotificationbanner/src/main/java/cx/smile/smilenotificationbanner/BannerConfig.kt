package cx.smile.smilenotificationbanner

import android.view.View
import android.widget.ImageView
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
    val title: String? = null,
    @StringRes val titleRes: Int? = null,
    val message: String? = null,
    @StringRes val messageRes: Int? = null,
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
    val onShowAnimationComplete: (() -> Unit)? = null, // Callback when entrance animation completes
    val onDismissAnimationComplete: (() -> Unit)? = null, // Callback when dismiss animation completes
    val vibrationDuration: VibrationDuration = VibrationDuration.NONE,
    // Left side configuration (priority: leftView > leftImageUrl > leftImage > icon)
    val leftView: View? = null,
    @DrawableRes val leftImage: Int? = null,
    val leftImageUrl: String? = null,
    val leftImageCircular: Boolean = false,
    val onLoadLeftImage: ((ImageView, String, Boolean) -> Unit)? = null, // Callback for loading left image URL
    // Right side configuration (priority: rightView > rightImageUrl > rightImage)
    val rightView: View? = null,
    @DrawableRes val rightImage: Int? = null,
    val rightImageUrl: String? = null,
    val rightImageCircular: Boolean = false,
    val onLoadRightImage: ((ImageView, String, Boolean) -> Unit)? = null, // Callback for loading right image URL
    // Expandable quick reply configuration
    val expandable: Boolean = false,
    val expandableInputHint: String? = null,
    @StringRes val expandableInputHintRes: Int? = null,
    val expandableButtonText: String? = null,
    @StringRes val expandableButtonTextRes: Int? = null,
    val onExpandableSubmit: ((String) -> Unit)? = null // Callback when text is submitted
)
