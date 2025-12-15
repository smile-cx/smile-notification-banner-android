package cx.smile.smilenotificationbanner

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SmileBanner - A modern, customizable notification banner for Android
 *
 * This class provides a simple way to display in-app notification banners with various
 * styles, positions, and customization options.
 */
class SmileBanner private constructor(
    private val activity: Activity,
    private val config: BannerConfig
) {
    private var popupWindow: PopupWindow? = null
    private var bannerView: View? = null
    private var autoDismissJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        @Volatile
        private var currentBanner: SmileBanner? = null

        /**
         * Create a banner builder for chaining configuration
         * @param activity The activity to display the banner in
         * @return Builder instance for chaining
         */
        @JvmStatic
        fun make(activity: Activity): Builder {
            return Builder(activity)
        }

        /**
         * Internal method to create SmileBanner from config
         */
        private fun create(activity: Activity, config: BannerConfig): SmileBanner {
            // Dismiss any existing banner
            currentBanner?.dismiss()

            val banner = SmileBanner(activity, config)
            currentBanner = banner
            return banner
        }

        /**
         * Quick method to show a simple banner
         * @param activity The activity to display the banner in
         * @param type The banner type (SUCCESS, INFO, WARNING, ERROR)
         * @param message The message to display
         * @param position The position (TOP or BOTTOM)
         * @return SmileBanner instance
         */
        @JvmStatic
        @JvmOverloads
        fun show(
            activity: Activity,
            type: BannerType,
            message: String,
            position: BannerPosition = BannerPosition.TOP
        ): SmileBanner {
            val config = BannerConfig(
                type = type,
                message = message,
                position = position
            )
            return create(activity, config).apply { show() }
        }

        /**
         * Quick method to show a banner with auto-dismiss
         * @param activity The activity to display the banner in
         * @param type The banner type
         * @param message The message to display
         * @param position The position
         * @param duration Duration in milliseconds before auto-dismiss
         * @return SmileBanner instance
         */
        @JvmStatic
        @JvmOverloads
        fun show(
            activity: Activity,
            type: BannerType,
            message: String,
            position: BannerPosition = BannerPosition.TOP,
            duration: Long
        ): SmileBanner {
            val config = BannerConfig(
                type = type,
                message = message,
                position = position,
                duration = duration
            )
            return create(activity, config).apply { show() }
        }

        /**
         * Dismiss the currently showing banner, if any
         */
        @JvmStatic
        fun dismissCurrent() {
            currentBanner?.dismiss()
        }
    }

    /**
     * Show the banner
     */
    fun show() {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        activity.runOnUiThread {
            try {
                setupBannerView()
                showPopupWindow()
                setupAutoDismiss()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Dismiss the banner
     */
    fun dismiss() {
        autoDismissJob?.cancel()
        popupWindow?.dismiss()
        popupWindow = null
        bannerView = null
        if (currentBanner == this) {
            currentBanner = null
        }
        config.onDismiss?.invoke()
    }

    /**
     * Get the banner view for further customization
     * Call this after show() to access the view
     */
    fun getBannerView(): View? = bannerView

    private fun setupBannerView() {
        val layoutRes = config.customLayout ?: R.layout.smile_banner_default
        bannerView = LayoutInflater.from(activity).inflate(layoutRes, null)

        if (config.customLayout == null) {
            configurDefaultBanner()
        }

        // Set click listener on the banner
        config.onBannerClick?.let { clickListener ->
            bannerView?.setOnClickListener { view ->
                clickListener.invoke(view)
            }
        }

        // Setup close button
        val closeButton = bannerView?.findViewById<ImageView>(R.id.bannerClose)
        if (config.dismissible) {
            closeButton?.setOnClickListener {
                dismiss()
            }
        } else {
            closeButton?.visibility = View.GONE
        }

        // Hide close button if auto-dismiss is enabled
        if (config.duration > 0) {
            closeButton?.visibility = View.GONE
        }
    }

    private fun configurDefaultBanner() {
        val card = bannerView?.findViewById<CardView>(R.id.bannerCard)
        val icon = bannerView?.findViewById<ImageView>(R.id.bannerIcon)
        val message = bannerView?.findViewById<TextView>(R.id.bannerMessage)

        // Set message - support both string and resource ID
        when {
            config.message != null -> message?.text = config.message
            config.messageRes != null -> message?.setText(config.messageRes)
        }

        // Apply custom colors or use defaults based on type
        val backgroundColor = when {
            config.backgroundColor != null -> config.backgroundColor
            config.backgroundColorRes != null -> ContextCompat.getColor(activity, config.backgroundColorRes)
            else -> getDefaultBackgroundColor()
        }

        val textColor = when {
            config.textColor != null -> config.textColor
            config.textColorRes != null -> ContextCompat.getColor(activity, config.textColorRes)
            else -> ContextCompat.getColor(activity, R.color.smile_banner_text)
        }

        val iconRes = config.icon ?: getDefaultIcon()

        card?.setCardBackgroundColor(backgroundColor)
        message?.setTextColor(textColor)
        icon?.setImageResource(iconRes)
        icon?.setColorFilter(textColor)

        // Set tint for close button
        val closeButton = bannerView?.findViewById<ImageView>(R.id.bannerClose)
        closeButton?.setColorFilter(textColor)
    }

    @ColorInt
    private fun getDefaultBackgroundColor(): Int {
        return when (config.type) {
            BannerType.SUCCESS -> ContextCompat.getColor(activity, R.color.smile_banner_success)
            BannerType.INFO -> ContextCompat.getColor(activity, R.color.smile_banner_info)
            BannerType.WARNING -> ContextCompat.getColor(activity, R.color.smile_banner_warning)
            BannerType.ERROR -> ContextCompat.getColor(activity, R.color.smile_banner_error)
            BannerType.CUSTOM -> ContextCompat.getColor(activity, R.color.smile_banner_info)
        }
    }

    @DrawableRes
    private fun getDefaultIcon(): Int {
        return when (config.type) {
            BannerType.SUCCESS -> R.drawable.smile_banner_icon_success
            BannerType.INFO -> R.drawable.smile_banner_icon_info
            BannerType.WARNING -> R.drawable.smile_banner_icon_warning
            BannerType.ERROR -> R.drawable.smile_banner_icon_error
            BannerType.CUSTOM -> R.drawable.smile_banner_icon_info
        }
    }

    private fun showPopupWindow() {
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.WRAP_CONTENT

        popupWindow = PopupWindow(bannerView, width, height, false).apply {
            // Set animation style based on position
            animationStyle = when (config.position) {
                BannerPosition.TOP -> R.style.SmileBannerAnimationTop
                BannerPosition.BOTTOM -> R.style.SmileBannerAnimationBottom
            }

            // Get the root view of the activity
            val rootView = activity.findViewById<View>(android.R.id.content)

            // Show the popup
            rootView.post {
                try {
                    val gravity = when (config.position) {
                        BannerPosition.TOP -> Gravity.TOP
                        BannerPosition.BOTTOM -> Gravity.BOTTOM
                    }

                    // Calculate insets for Android 15+ edge-to-edge support
                    val (xOffset, yOffset) = calculateInsets(rootView)

                    showAtLocation(rootView, gravity, xOffset, yOffset)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Calculate window insets for proper positioning with edge-to-edge support
     * Ensures banners don't overlap with system bars on Android 15+
     */
    private fun calculateInsets(rootView: View): Pair<Int, Int> {
        var xOffset = 0
        var yOffset = 0

        // Use ViewCompat for backward compatibility
        val insets = ViewCompat.getRootWindowInsets(rootView)
        if (insets != null) {
            when (config.position) {
                BannerPosition.TOP -> {
                    // Account for status bar at top
                    val systemBarsInsets = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                    )
                    yOffset = systemBarsInsets.top
                }
                BannerPosition.BOTTOM -> {
                    // Account for navigation bar at bottom
                    val systemBarsInsets = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                    )
                    yOffset = systemBarsInsets.bottom
                }
            }
        }

        return Pair(xOffset, yOffset)
    }

    private fun setupAutoDismiss() {
        if (config.duration > 0) {
            autoDismissJob = scope.launch {
                delay(config.duration)
                dismiss()
            }
        }
    }

    /**
     * Builder class for fluent banner configuration
     * Allows chaining configuration methods directly on SmileBanner
     */
    class Builder(private val activity: Activity) {
        private var type: BannerType = BannerType.INFO
        private var message: String? = null
        private var messageRes: Int? = null
        private var position: BannerPosition = BannerPosition.TOP
        private var duration: Long = 0L
        private var dismissible: Boolean = true
        private var customLayout: Int? = null
        private var backgroundColor: Int? = null
        private var backgroundColorRes: Int? = null
        private var textColor: Int? = null
        private var textColorRes: Int? = null
        private var icon: Int? = null
        private var onBannerClick: ((View) -> Unit)? = null
        private var onDismiss: (() -> Unit)? = null

        /**
         * Set the banner type
         */
        fun type(type: BannerType) = apply { this.type = type }

        /**
         * Set the banner message with a string
         */
        fun message(message: String) = apply {
            this.message = message
            this.messageRes = null
        }

        /**
         * Set the banner message with a string resource ID
         */
        fun message(@androidx.annotation.StringRes messageResId: Int) = apply {
            this.messageRes = messageResId
            this.message = null
        }

        /**
         * Set the banner position
         */
        fun position(position: BannerPosition) = apply { this.position = position }

        /**
         * Set auto-dismiss duration in milliseconds (0 = no auto-dismiss)
         */
        fun duration(duration: Long) = apply { this.duration = duration }

        /**
         * Set whether the banner is dismissible via close button
         */
        fun dismissible(dismissible: Boolean) = apply { this.dismissible = dismissible }

        /**
         * Set a custom layout resource
         */
        fun customLayout(@LayoutRes layoutRes: Int) = apply { this.customLayout = layoutRes }

        /**
         * Set custom background color using a color int
         */
        fun backgroundColor(@ColorInt color: Int) = apply {
            this.backgroundColor = color
            this.backgroundColorRes = null
        }

        /**
         * Set custom background color using a color resource ID
         */
        fun backgroundColorRes(@androidx.annotation.ColorRes colorRes: Int) = apply {
            this.backgroundColorRes = colorRes
            this.backgroundColor = null
        }

        /**
         * Set custom text color using a color int
         */
        fun textColor(@ColorInt color: Int) = apply {
            this.textColor = color
            this.textColorRes = null
        }

        /**
         * Set custom text color using a color resource ID
         */
        fun textColorRes(@androidx.annotation.ColorRes colorRes: Int) = apply {
            this.textColorRes = colorRes
            this.textColor = null
        }

        /**
         * Set custom icon resource
         */
        fun icon(@DrawableRes iconRes: Int) = apply { this.icon = iconRes }

        /**
         * Set click listener for the banner
         */
        fun onBannerClick(listener: (View) -> Unit) = apply { this.onBannerClick = listener }

        /**
         * Set dismiss callback
         */
        fun onDismiss(listener: () -> Unit) = apply { this.onDismiss = listener }

        /**
         * Build and return the SmileBanner instance (does not show it yet)
         */
        fun build(): SmileBanner {
            val config = BannerConfig(
                type = type,
                message = message,
                messageRes = messageRes,
                position = position,
                duration = duration,
                dismissible = dismissible,
                customLayout = customLayout,
                backgroundColor = backgroundColor,
                backgroundColorRes = backgroundColorRes,
                textColor = textColor,
                textColorRes = textColorRes,
                icon = icon,
                onBannerClick = onBannerClick,
                onDismiss = onDismiss
            )
            return create(activity, config)
        }

        /**
         * Build and immediately show the banner
         */
        fun show(): SmileBanner {
            return build().apply { show() }
        }
    }
}
