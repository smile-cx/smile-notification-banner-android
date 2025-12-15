package cx.smile.smilenotificationbanner

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
    private var isExpanded = false

    companion object {
        @Volatile
        private var currentBanner: SmileBanner? = null

        // Queue for pending notifications when current banner is expanded
        private val pendingQueue = mutableListOf<Pair<Activity, BannerConfig>>()

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
            // If current banner is expanded, queue this notification instead of dismissing
            if (currentBanner?.isExpanded == true) {
                pendingQueue.add(Pair(activity, config))
                // Return a dummy banner that won't show
                return SmileBanner(activity, config)
            }

            // Dismiss any existing banner instantly (without animation)
            currentBanner?.dismissInstant()

            val banner = SmileBanner(activity, config)
            currentBanner = banner
            return banner
        }

        /**
         * Process the next notification in the queue
         */
        private fun processNextInQueue() {
            if (pendingQueue.isNotEmpty()) {
                val (activity, config) = pendingQueue.removeAt(0)
                create(activity, config).show()
            }
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
                performVibration()
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
        val wasCurrent = currentBanner == this
        if (wasCurrent) {
            currentBanner = null
        }
        config.onDismiss?.invoke()

        // Process next notification in queue if this was the current banner
        if (wasCurrent) {
            processNextInQueue()
        }
    }

    /**
     * Dismiss the banner instantly without animation
     * Used internally when showing a new banner
     */
    private fun dismissInstant() {
        autoDismissJob?.cancel()
        // Disable animation before dismissing
        popupWindow?.animationStyle = 0
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

        // Setup swipe-to-dismiss gesture
        setupSwipeToDismiss()
    }

    private fun configurDefaultBanner() {
        val card = bannerView?.findViewById<CardView>(R.id.bannerCard)
        val title = bannerView?.findViewById<TextView>(R.id.bannerTitle)
        val message = bannerView?.findViewById<TextView>(R.id.bannerMessage)
        val leftContainer = bannerView?.findViewById<ViewGroup>(R.id.bannerLeftContainer)
        val rightContainer = bannerView?.findViewById<ViewGroup>(R.id.bannerRightContainer)
        val defaultIcon = bannerView?.findViewById<ImageView>(R.id.bannerIcon)

        // Set title - support both string and resource ID
        when {
            config.title != null -> {
                title?.text = config.title
                title?.visibility = View.VISIBLE
            }
            config.titleRes != null -> {
                title?.setText(config.titleRes)
                title?.visibility = View.VISIBLE
            }
            else -> title?.visibility = View.GONE
        }

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

        card?.setCardBackgroundColor(backgroundColor)
        title?.setTextColor(textColor)
        message?.setTextColor(textColor)

        // Setup left side (priority: leftView > leftImageUrl > leftImage > icon)
        setupLeftSide(leftContainer, defaultIcon, textColor)

        // Setup right side (priority: rightView > rightImageUrl > rightImage)
        setupRightSide(rightContainer)

        // Set tint for close button
        val closeButton = bannerView?.findViewById<ImageView>(R.id.bannerClose)
        closeButton?.setColorFilter(textColor)

        // Setup expandable feature if enabled
        setupExpandable(textColor)
    }

    private fun setupLeftSide(container: ViewGroup?, defaultIcon: ImageView?, textColor: Int) {
        when {
            config.leftView != null -> {
                // Custom view has highest priority
                container?.visibility = View.VISIBLE
                container?.removeAllViews()
                container?.addView(config.leftView)
                defaultIcon?.visibility = View.GONE
            }
            config.leftImageUrl != null && config.onLoadLeftImage != null -> {
                // URL with callback
                container?.visibility = View.VISIBLE
                val imageView = ImageView(activity).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                container?.removeAllViews()
                container?.addView(imageView)
                defaultIcon?.visibility = View.GONE

                // Call the callback to load the image
                config.onLoadLeftImage?.invoke(imageView, config.leftImageUrl, config.leftImageCircular)
            }
            config.leftImage != null -> {
                // Drawable resource
                container?.visibility = View.VISIBLE
                defaultIcon?.visibility = View.VISIBLE
                defaultIcon?.setImageResource(config.leftImage)
                defaultIcon?.setColorFilter(textColor)
            }
            else -> {
                // Use default icon
                container?.visibility = View.VISIBLE
                defaultIcon?.visibility = View.VISIBLE
                val iconRes = config.icon ?: getDefaultIcon()
                defaultIcon?.setImageResource(iconRes)
                defaultIcon?.setColorFilter(textColor)
            }
        }
    }

    private fun setupRightSide(container: ViewGroup?) {
        when {
            config.rightView != null -> {
                // Custom view has highest priority
                container?.visibility = View.VISIBLE
                container?.removeAllViews()
                container?.addView(config.rightView)
            }
            config.rightImageUrl != null && config.onLoadRightImage != null -> {
                // URL with callback
                container?.visibility = View.VISIBLE
                val imageView = ImageView(activity).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        48.dpToPx(activity),
                        48.dpToPx(activity)
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                container?.removeAllViews()
                container?.addView(imageView)

                // Call the callback to load the image
                config.onLoadRightImage?.invoke(imageView, config.rightImageUrl, config.rightImageCircular)
            }
            config.rightImage != null -> {
                // Drawable resource
                container?.visibility = View.VISIBLE
                val imageView = ImageView(activity).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        48.dpToPx(activity),
                        48.dpToPx(activity)
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setImageResource(config.rightImage)
                }
                container?.removeAllViews()
                container?.addView(imageView)
            }
            else -> {
                // Hide right container if nothing to show
                container?.visibility = View.GONE
            }
        }
    }

    // Extension function to convert dp to pixels
    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun setupExpandable(textColor: Int) {
        if (!config.expandable) {
            return
        }

        val dragIndicator = bannerView?.findViewById<View>(R.id.bannerDragIndicator)
        val expandableContent = bannerView?.findViewById<ViewGroup>(R.id.bannerExpandableContent)
        val expandableInput = bannerView?.findViewById<android.widget.EditText>(R.id.bannerExpandableInput)
        val expandableButton = bannerView?.findViewById<android.widget.Button>(R.id.bannerExpandableButton)

        // Show drag indicator
        dragIndicator?.visibility = View.VISIBLE

        // Set hint text
        when {
            config.expandableInputHint != null -> expandableInput?.hint = config.expandableInputHint
            config.expandableInputHintRes != null -> expandableInput?.setHint(config.expandableInputHintRes)
            else -> expandableInput?.hint = "Type a reply..."
        }

        // Set button text
        when {
            config.expandableButtonText != null -> expandableButton?.text = config.expandableButtonText
            config.expandableButtonTextRes != null -> expandableButton?.setText(config.expandableButtonTextRes)
            else -> expandableButton?.text = "Send"
        }

        // Setup drag-to-expand
        setupDragToExpand(dragIndicator, expandableContent)

        // Setup submit button
        expandableButton?.setOnClickListener {
            val text = expandableInput?.text?.toString() ?: ""
            if (text.isNotBlank()) {
                config.onExpandableSubmit?.invoke(text)
                expandableInput?.text?.clear()
                // Collapse and dismiss to process queue
                collapseExpandable(expandableContent)
                dismiss()
            }
        }
    }

    private fun setupDragToExpand(dragIndicator: View?, expandableContent: ViewGroup?) {
        if (dragIndicator == null || expandableContent == null) return

        var initialY = 0f
        var isExpanded = false

        dragIndicator.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initialY = event.rawY
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - initialY
                    if (deltaY > 50 && !isExpanded) {
                        // Dragged down enough, expand
                        expandExpandable(expandableContent)
                        isExpanded = true
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    true
                }
                else -> false
            }
        }
    }

    private fun expandExpandable(expandableContent: ViewGroup?) {
        isExpanded = true
        expandableContent?.visibility = View.VISIBLE
        expandableContent?.alpha = 0f
        expandableContent?.animate()
            ?.alpha(1f)
            ?.setDuration(200)
            ?.start()
    }

    private fun collapseExpandable(expandableContent: ViewGroup?) {
        isExpanded = false
        expandableContent?.animate()
            ?.alpha(0f)
            ?.setDuration(200)
            ?.withEndAction {
                expandableContent.visibility = View.GONE
            }
            ?.start()
    }

    /**
     * Setup swipe-to-dismiss gesture
     * For TOP position: swipe up to dismiss
     * For BOTTOM position: swipe down to dismiss
     */
    private fun setupSwipeToDismiss() {
        val card = bannerView?.findViewById<CardView>(R.id.bannerCard) ?: return
        val container = bannerView?.findViewById<ViewGroup>(R.id.bannerContainer) ?: return
        var initialY = 0f
        var initialTranslationY = 0f
        var isDragging = false

        container.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initialY = event.rawY
                    initialTranslationY = card.translationY
                    isDragging = false
                    false // Allow other touch listeners to handle the event
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - initialY
                    val absDeltaY = kotlin.math.abs(deltaY)

                    // Start dragging if moved more than 10 pixels
                    if (absDeltaY > 10) {
                        isDragging = true
                    }

                    if (isDragging) {
                        when (config.position) {
                            BannerPosition.TOP -> {
                                // Allow only upward swipes (negative deltaY)
                                if (deltaY < 0) {
                                    card.translationY = initialTranslationY + deltaY
                                    true // Consume the event
                                } else false
                            }
                            BannerPosition.BOTTOM -> {
                                // Allow only downward swipes (positive deltaY)
                                if (deltaY > 0) {
                                    card.translationY = initialTranslationY + deltaY
                                    true // Consume the event
                                } else false
                            }
                        }
                    } else false
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        val deltaY = event.rawY - initialY
                        val threshold = 100 // pixels to trigger dismiss

                        val shouldDismiss = when (config.position) {
                            BannerPosition.TOP -> deltaY < -threshold
                            BannerPosition.BOTTOM -> deltaY > threshold
                        }

                        if (shouldDismiss) {
                            // Animate out and dismiss
                            val targetY = when (config.position) {
                                BannerPosition.TOP -> -card.height.toFloat()
                                BannerPosition.BOTTOM -> card.height.toFloat()
                            }
                            card.animate()
                                .translationY(targetY)
                                .alpha(0f)
                                .setDuration(200)
                                .withEndAction {
                                    dismiss()
                                }
                                .start()
                        } else {
                            // Animate back to original position
                            card.animate()
                                .translationY(initialTranslationY)
                                .setDuration(200)
                                .start()
                        }
                        true
                    } else false
                }
                else -> false
            }
        }
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
     * Perform vibration if enabled in config
     * Note: The VIBRATE permission should be declared in the app's AndroidManifest.xml
     */
    @SuppressLint("MissingPermission")
    private fun performVibration() {
        if (config.vibrationDuration == VibrationDuration.NONE) {
            return
        }

        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                activity.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(
                        VibrationEffect.createOneShot(
                            config.vibrationDuration.milliseconds,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(config.vibrationDuration.milliseconds)
                }
            }
        } catch (e: Exception) {
            // Silently fail if vibration is not available or permission is missing
            e.printStackTrace()
        }
    }

    /**
     * Builder class for fluent banner configuration
     * Allows chaining configuration methods directly on SmileBanner
     */
    class Builder(private val activity: Activity) {
        private var type: BannerType = BannerType.INFO
        private var title: String? = null
        private var titleRes: Int? = null
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
        private var vibrationDuration: VibrationDuration = VibrationDuration.NONE
        // Left side configuration
        private var leftView: View? = null
        private var leftImage: Int? = null
        private var leftImageUrl: String? = null
        private var leftImageCircular: Boolean = false
        private var onLoadLeftImage: ((ImageView, String, Boolean) -> Unit)? = null
        // Right side configuration
        private var rightView: View? = null
        private var rightImage: Int? = null
        private var rightImageUrl: String? = null
        private var rightImageCircular: Boolean = false
        private var onLoadRightImage: ((ImageView, String, Boolean) -> Unit)? = null
        // Expandable configuration
        private var expandable: Boolean = false
        private var expandableInputHint: String? = null
        private var expandableInputHintRes: Int? = null
        private var expandableButtonText: String? = null
        private var expandableButtonTextRes: Int? = null
        private var onExpandableSubmit: ((String) -> Unit)? = null

        /**
         * Set the banner type
         */
        fun type(type: BannerType) = apply { this.type = type }

        /**
         * Set the banner title with a string
         */
        fun title(title: String) = apply {
            this.title = title
            this.titleRes = null
        }

        /**
         * Set the banner title with a string resource ID
         */
        fun title(@androidx.annotation.StringRes titleResId: Int) = apply {
            this.titleRes = titleResId
            this.title = null
        }

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
         * Set a custom view for the left side (highest priority)
         * This overrides leftImage and leftImageUrl
         */
        fun leftView(view: View) = apply { this.leftView = view }

        /**
         * Set left image from drawable resource
         */
        fun leftImage(@DrawableRes imageRes: Int) = apply { this.leftImage = imageRes }

        /**
         * Set left image from URL with callback for loading
         * @param url The image URL
         * @param circular Whether to apply circular transformation
         * @param onLoad Callback to handle image loading (imageView, url, circular) -> Unit
         */
        fun leftImageUrl(url: String, circular: Boolean = false, onLoad: (ImageView, String, Boolean) -> Unit) = apply {
            this.leftImageUrl = url
            this.leftImageCircular = circular
            this.onLoadLeftImage = onLoad
        }

        /**
         * Set whether left image should be circular
         */
        fun leftImageCircular(circular: Boolean) = apply { this.leftImageCircular = circular }

        /**
         * Set a custom view for the right side (highest priority)
         * This overrides rightImage and rightImageUrl
         */
        fun rightView(view: View) = apply { this.rightView = view }

        /**
         * Set right image from drawable resource
         */
        fun rightImage(@DrawableRes imageRes: Int) = apply { this.rightImage = imageRes }

        /**
         * Set right image from URL with callback for loading
         * @param url The image URL
         * @param circular Whether to apply circular transformation
         * @param onLoad Callback to handle image loading (imageView, url, circular) -> Unit
         */
        fun rightImageUrl(url: String, circular: Boolean = false, onLoad: (ImageView, String, Boolean) -> Unit) = apply {
            this.rightImageUrl = url
            this.rightImageCircular = circular
            this.onLoadRightImage = onLoad
        }

        /**
         * Set whether right image should be circular
         */
        fun rightImageCircular(circular: Boolean) = apply { this.rightImageCircular = circular }

        /**
         * Enable expandable quick reply feature
         * When enabled, the banner can be expanded by dragging down to reveal an input field
         */
        fun expandable(enabled: Boolean = true) = apply { this.expandable = enabled }

        /**
         * Set hint text for the expandable input field
         */
        fun expandableInputHint(hint: String) = apply {
            this.expandableInputHint = hint
            this.expandableInputHintRes = null
        }

        /**
         * Set hint text for the expandable input field using a string resource
         */
        fun expandableInputHint(@androidx.annotation.StringRes hintRes: Int) = apply {
            this.expandableInputHintRes = hintRes
            this.expandableInputHint = null
        }

        /**
         * Set button text for the expandable submit button
         */
        fun expandableButtonText(text: String) = apply {
            this.expandableButtonText = text
            this.expandableButtonTextRes = null
        }

        /**
         * Set button text for the expandable submit button using a string resource
         */
        fun expandableButtonText(@androidx.annotation.StringRes textRes: Int) = apply {
            this.expandableButtonTextRes = textRes
            this.expandableButtonText = null
        }

        /**
         * Set callback for when expandable text is submitted
         * @param listener Callback receiving the submitted text
         */
        fun onExpandableSubmit(listener: (String) -> Unit) = apply { this.onExpandableSubmit = listener }

        /**
         * Enable vibration when banner is shown
         * @param duration The vibration duration (SHORT, MEDIUM, or LONG)
         */
        fun vibrate(duration: VibrationDuration) = apply { this.vibrationDuration = duration }

        /**
         * Enable vibration with SHORT duration when banner is shown
         */
        fun vibrate() = apply { this.vibrationDuration = VibrationDuration.SHORT }

        /**
         * Build and return the SmileBanner instance (does not show it yet)
         */
        fun build(): SmileBanner {
            val config = BannerConfig(
                type = type,
                title = title,
                titleRes = titleRes,
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
                onDismiss = onDismiss,
                vibrationDuration = vibrationDuration,
                leftView = leftView,
                leftImage = leftImage,
                leftImageUrl = leftImageUrl,
                leftImageCircular = leftImageCircular,
                onLoadLeftImage = onLoadLeftImage,
                rightView = rightView,
                rightImage = rightImage,
                rightImageUrl = rightImageUrl,
                rightImageCircular = rightImageCircular,
                onLoadRightImage = onLoadRightImage,
                expandable = expandable,
                expandableInputHint = expandableInputHint,
                expandableInputHintRes = expandableInputHintRes,
                expandableButtonText = expandableButtonText,
                expandableButtonTextRes = expandableButtonTextRes,
                onExpandableSubmit = onExpandableSubmit
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
