package cx.smile.smilenotificationbanner

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
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
import androidx.core.view.isVisible

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
    private var originalStatusBarColor: Int? = null
    private var originalSystemUiVisibility: Int? = null
    private var isReplacingBanner = false
    private var showDelay = 0L
    private var isQueued = false // If true, this banner is in the queue and shouldn't show until dequeued

    // Cached view references for performance
    private var titleView: TextView? = null
    private var messageView: TextView? = null
    private var leftContainerView: ViewGroup? = null
    private var rightContainerView: ViewGroup? = null
    private var dragIndicatorView: View? = null

    companion object {
        // Static reference to current banner is safe because:
        // 1. It's cleared in dismiss() (line 347) preventing long-term memory leaks
        // 2. Activity is only referenced while banner is actively showing
        // 3. This singleton pattern is necessary for managing banner queue and display
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var currentBanner: SmileBanner? = null

        // Queue for pending notifications
        private val pendingQueue = mutableListOf<Pair<Activity, BannerConfig>>()

        // Maximum queue size - like WhatsApp, we only keep the most recent notification(s)
        // When queue is full, oldest is removed and newest is added (rotation)
        private const val MAX_QUEUE_SIZE = 1

        // Minimum time between banner transitions to avoid too-fast animations (milliseconds)
        private const val MIN_TRANSITION_DELAY = 200L
        private var lastBannerShowTime = 0L

        // Minimum time a banner should be displayed before showing the next one (milliseconds)
        // This prevents rapid-fire banner replacements when multiple are queued
        private const val MIN_DISPLAY_TIME = 1000L // 1 second

        /**
         * Add a banner to the queue with rotation (removes oldest if queue is full)
         * Like WhatsApp, we only keep the most recent notification(s)
         * Also schedules queue processing if not already scheduled
         */
        private fun addToQueue(activity: Activity, config: BannerConfig) {
            // If queue is at max size, remove the oldest
            if (pendingQueue.size >= MAX_QUEUE_SIZE) {
                pendingQueue.removeAt(0)
                Log.d("SmileBanner", "Queue full (max: $MAX_QUEUE_SIZE), removing oldest banner to make room")
            }

            // Check if this is the first item being added to an empty queue
            val wasEmpty = pendingQueue.isEmpty()

            pendingQueue.add(Pair(activity, config))
            Log.d("SmileBanner", "Added to queue (size: ${pendingQueue.size}/$MAX_QUEUE_SIZE)")

            // If queue was empty and now has an item, schedule processing
            // This handles the case where a banner is showing without auto-dismiss
            // and a new banner comes in later
            if (wasEmpty && currentBanner != null) {
                val currentTime = System.currentTimeMillis()
                val timeShown = currentTime - lastBannerShowTime

                // Calculate delay: time remaining until MIN_DISPLAY_TIME + transition delay
                val minDisplayRemaining = if (timeShown < MIN_DISPLAY_TIME) {
                    MIN_DISPLAY_TIME - timeShown
                } else {
                    0L
                }
                val transitionDelay = 300L
                val totalDelay = minDisplayRemaining + transitionDelay

                Log.d("SmileBanner", "Scheduling queue processing in ${totalDelay}ms (timeShown=${timeShown}ms)")

                // Schedule processing of the queue
                currentBanner?.bannerView?.postDelayed({
                    if (currentBanner != null && pendingQueue.isNotEmpty()) {
                        Log.d("SmileBanner", "Processing next banner from queue (triggered by addToQueue)")
                        processNextInQueue()
                    }
                }, totalDelay)
            }
        }

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
         * @param fromQueue If true, bypass queueing logic (used when processing from queue)
         */
        private fun create(activity: Activity, config: BannerConfig, fromQueue: Boolean = false): SmileBanner {
            // When processing from queue, skip all queueing logic
            if (!fromQueue) {
                // If current banner is expanded, queue this notification instead of dismissing
                if (currentBanner?.isExpanded == true) {
                    Log.d("SmileBanner", "Current banner is expanded, queueing new banner")
                    addToQueue(activity, config)
                    // Return a dummy banner that won't show - mark it as queued
                    val dummyBanner = SmileBanner(activity, config)
                    dummyBanner.isQueued = true
                    return dummyBanner
                }

                // If there's a current banner showing and we have pending banners in queue,
                // queue this one too to maintain order
                if (currentBanner != null && pendingQueue.isNotEmpty()) {
                    Log.d("SmileBanner", "Banner is showing and queue is not empty, queueing new banner")
                    addToQueue(activity, config)
                    // Return a dummy banner that won't show - mark it as queued
                    val dummyBanner = SmileBanner(activity, config)
                    dummyBanner.isQueued = true
                    return dummyBanner
                }

                // If there's a current banner but queue is empty, check if we should queue or replace
                if (currentBanner != null) {
                    // Calculate how long the current banner has been shown
                    val currentTime = System.currentTimeMillis()
                    val timeShown = currentTime - lastBannerShowTime

                    // If current banner hasn't been shown for minimum display time yet, queue this one
                    if (timeShown < MIN_DISPLAY_TIME) {
                        Log.d("SmileBanner", "Current banner shown for ${timeShown}ms (min: ${MIN_DISPLAY_TIME}ms), queueing new banner")
                        addToQueue(activity, config)
                        // Return a dummy banner that won't show - mark it as queued
                        val dummyBanner = SmileBanner(activity, config)
                        dummyBanner.isQueued = true
                        return dummyBanner
                    }
                    // Otherwise, allow replacement (banner has been shown long enough)
                    Log.d("SmileBanner", "Current banner shown for ${timeShown}ms, allowing replacement")
                }
            }

            // Check if we're replacing an existing banner
            val hasExistingBanner = currentBanner != null

            // Calculate time since last banner was shown
            val currentTime = System.currentTimeMillis()
            val timeSinceLastShow = currentTime - lastBannerShowTime

            // If not enough time has passed, add a small delay
            val delayNeeded = if (hasExistingBanner && timeSinceLastShow < MIN_TRANSITION_DELAY) {
                MIN_TRANSITION_DELAY - timeSinceLastShow
            } else {
                0L
            }

            // Create new banner instance first to measure its height
            val banner = SmileBanner(activity, config)
            banner.isReplacingBanner = hasExistingBanner

            // If replacing, measure new banner height and animate old banner to match
            // Pass delayNeeded so height animation starts when new banner appears
            if (hasExistingBanner) {
                val newHeight = banner.measureBannerHeight()
                currentBanner?.dismissWithFade(newHeight, delayNeeded)
            }

            // Store delay for use when showing
            if (delayNeeded > 0) {
                Log.d("SmileBanner", "Will delay banner show by ${delayNeeded}ms to avoid too-fast transition")
            }

            currentBanner = banner
            banner.showDelay = delayNeeded
            return banner
        }

        /**
         * Process the next notification in the queue
         */
        private fun processNextInQueue() {
            if (pendingQueue.isNotEmpty()) {
                val (activity, config) = pendingQueue.removeAt(0)
                Log.d("SmileBanner", "Dequeued banner from queue, ${pendingQueue.size} remaining")
                create(activity, config, fromQueue = true).show()
            }
        }

        /**
         * Quick method to show a simple banner
         * @param activity The activity to display the banner in
         * @param type The banner type (SUCCESS, INFO, WARNING, ERROR)
         * @param message The message to display
         * @return SmileBanner instance
         */
        @JvmStatic
        fun show(
            activity: Activity,
            type: BannerType,
            message: String
        ): SmileBanner {
            val config = BannerConfig(
                type = type,
                message = message
            )
            return create(activity, config).apply { show() }
        }

        /**
         * Quick method to show a banner with auto-dismiss
         * @param activity The activity to display the banner in
         * @param type The banner type
         * @param message The message to display
         * @param duration Duration in milliseconds before auto-dismiss
         * @return SmileBanner instance
         */
        @JvmStatic
        fun show(
            activity: Activity,
            type: BannerType,
            message: String,
            duration: Long
        ): SmileBanner {
            val config = BannerConfig(
                type = type,
                message = message,
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
        // If this banner is queued, don't show it - it will show when dequeued
        if (isQueued) {
            Log.d("SmileBanner", "Banner is queued, not showing until dequeued")
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        // Apply delay if needed to avoid too-fast transitions
        if (showDelay > 0) {
            scope.launch {
                delay(showDelay)
                lastBannerShowTime = System.currentTimeMillis()
                showInternal()
            }
        } else {
            lastBannerShowTime = System.currentTimeMillis()
            showInternal()
        }
    }

    private fun showInternal() {
        activity.runOnUiThread {
            try {
                setupBannerView()
                applyStatusBarColor()
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
        restoreStatusBarColor()
        popupWindow?.dismiss()
        popupWindow = null
        bannerView = null

        // Clear cached view references to prevent memory leaks
        titleView = null
        messageView = null
        leftContainerView = null
        rightContainerView = null
        dragIndicatorView = null

        val wasCurrent = currentBanner == this
        if (wasCurrent) {
            currentBanner = null
        }

        // Call developer's callbacks asynchronously so they don't block
        config.onDismiss?.let { callback ->
            scope.launch {
                try {
                    callback.invoke()
                } catch (e: Exception) {
                    Log.e("SmileBanner", "Error in developer's onDismiss callback", e)
                }
            }
        }

        config.onDismissAnimationComplete?.let { callback ->
            scope.launch {
                try {
                    callback.invoke()
                } catch (e: Exception) {
                    Log.e("SmileBanner", "Error in developer's onDismissAnimationComplete callback", e)
                }
            }
        }

        // Process next notification in queue if this was the current banner
        if (wasCurrent) {
            processNextInQueue()
        }
    }

    /**
     * Dismiss the banner with delay to let new banner cover it
     * Used internally when showing a new banner to replace this one
     * @param targetHeight Optional target height to animate to before dismissal
     * @param animationDelay Delay before starting height animation (to sync with new banner appearance)
     */
    private fun dismissWithFade(targetHeight: Int? = null, animationDelay: Long = 0L) {
        autoDismissJob?.cancel()

        // If target height provided, animate the banner to match new banner's height
        // Delay the animation to sync with when new banner actually appears
        if (targetHeight != null) {
            if (animationDelay > 0) {
                scope.launch {
                    delay(animationDelay)
                    animateHeightChange(targetHeight)
                }
            } else {
                animateHeightChange(targetHeight)
            }
        }

        // Wait for push-up animation to complete (350ms total)
        val dismissDelay = 300L

        // Delay dismissal so new banner can slide in over this one
        scope.launch {
            delay(dismissDelay)
            restoreStatusBarColor()
            // Disable animation for clean removal once covered
            popupWindow?.animationStyle = 0
            popupWindow?.dismiss()
            popupWindow = null
            bannerView = null

            // Clear cached view references to prevent memory leaks
            titleView = null
            messageView = null
            leftContainerView = null
            rightContainerView = null
            dragIndicatorView = null

            // Call developer's callbacks asynchronously so they don't block
            config.onDismiss?.let { callback ->
                scope.launch {
                    try {
                        callback.invoke()
                    } catch (e: Exception) {
                        Log.e("SmileBanner", "Error in developer's onDismiss callback", e)
                    }
                }
            }

            config.onDismissAnimationComplete?.let { callback ->
                scope.launch {
                    try {
                        callback.invoke()
                    } catch (e: Exception) {
                        Log.e("SmileBanner", "Error in developer's onDismissAnimationComplete callback", e)
                    }
                }
            }
        }
    }

    /**
     * Get the banner view for further customization
     * Call this after show() to access the view
     */
    fun getBannerView(): View? = bannerView

    /**
     * Get all content views that need to be animated
     */
    private fun getContentViews(): List<View?> {
        return listOf(titleView, messageView, leftContainerView, rightContainerView, dragIndicatorView)
    }

    /**
     * Measure the height of this banner before showing it
     * @return The measured height in pixels
     */
    private fun measureBannerHeight(): Int {
        // Inflate the banner view if not already done
        if (bannerView == null) {
            setupBannerView()
        }

        val view = bannerView ?: return 0

        // Apply system bar padding to get accurate measurement
        applySystemBarPadding()

        // Get screen width for accurate measurement
        val displayMetrics = activity.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        // Measure the view with screen width
        view.measure(
            View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        return view.measuredHeight
    }

    /**
     * Animate the banner's height to a target height
     * Only shrinks - does not expand (only animates when old banner is taller)
     * @param targetHeight The target height in pixels
     */
    private fun animateHeightChange(targetHeight: Int) {
        val card = bannerView?.findViewById<CardView>(R.id.bannerCard) ?: return
        val currentHeight = card.height

        if (currentHeight == targetHeight || currentHeight == 0) {
            return
        }

        // Only animate if shrinking (current is taller than target)
        // Don't expand old banner when new banner is taller
        if (currentHeight <= targetHeight) {
            Log.d("SmileBanner", "Skipping height animation - new banner is same size or taller ($currentHeight -> $targetHeight)")
            return
        }

        Log.d("SmileBanner", "Animating height shrink from $currentHeight to $targetHeight")

        // Animate the card height (shrinking)
        ValueAnimator.ofInt(currentHeight, targetHeight).also {
            it.duration = 300L // Match the dismissal animation duration
            it.interpolator = AccelerateDecelerateInterpolator()
            it.addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                popupWindow?.update(
                    popupWindow?.width ?: ViewGroup.LayoutParams.MATCH_PARENT,
                    value
                )
            }
            it.start()
        }
    }

    /**
     * Hide banner content (title, message, icons, drag indicator) by setting alpha to 0
     */
    private fun hideBannerContent() {
        getContentViews().forEach { it?.alpha = 0f }
    }

    /**
     * Fade in banner content (title, message, icons, drag indicator)
     * @param duration Animation duration in milliseconds
     */
    private fun fadeInBannerContent(duration: Long) {
        getContentViews().forEach { it?.animate()?.alpha(1f)?.setDuration(duration)?.start() }
    }

    private fun setupBannerView() {
        // Skip if view is already set up (e.g., for measurement)
        if (bannerView != null) {
            return
        }

        val layoutRes = config.customLayout ?: R.layout.smile_banner_default
        bannerView = LayoutInflater.from(activity).inflate(layoutRes, null)

        // Cache view references for performance
        titleView = bannerView?.findViewById(R.id.bannerTitle)
        messageView = bannerView?.findViewById(R.id.bannerMessage)
        leftContainerView = bannerView?.findViewById(R.id.bannerLeftContainer)
        rightContainerView = bannerView?.findViewById(R.id.bannerRightContainer)
        dragIndicatorView = bannerView?.findViewById(R.id.bannerDragIndicator)

        if (config.customLayout == null) {
            configureDefaultBanner()
        }

        // Setup swipe-to-dismiss gesture (also handles banner clicks)
        setupSwipeToDismiss()
    }

    /**
     * Apply padding to banner container to account for system bars and display cutout
     * This ensures content doesn't overlap with status bar or camera cutout
     * Also sets minimum height to cover action bar
     */
    private fun applySystemBarPadding() {
        val card = bannerView?.findViewById<CardView>(R.id.bannerCard) ?: return

        val rootView = activity.findViewById<View>(android.R.id.content)
        var topInset = 0

        // Try to get insets from ViewCompat
        val insets = ViewCompat.getRootWindowInsets(rootView)
        if (insets != null) {
            // Get status bar inset
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val statusBarHeight = systemBarsInsets.top

            // Get display cutout inset
            val displayCutoutInsets = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val cutoutHeight = displayCutoutInsets.top

            // Use the maximum of status bar and cutout to ensure content is below both
            topInset = maxOf(statusBarHeight, cutoutHeight)
        }

        // Fallback: if insets are not available, calculate status bar height manually
        // Using getIdentifier is discouraged, but this is a last-resort fallback for edge cases
        // where window insets are not available. Modern Android (API 21+) should provide insets.
        if (topInset == 0) {
            @Suppress("DiscouragedApi")
            val resourceId = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                topInset = activity.resources.getDimensionPixelSize(resourceId)
            }
        }

        // Only proceed if we have a valid inset
        if (topInset > 0) {
            // Remove top margin from card to fill the entire top
            val layoutParams = card.layoutParams as? ViewGroup.MarginLayoutParams
            layoutParams?.let {
                val sideMargin = it.leftMargin // Keep side margins
                it.setMargins(sideMargin, 0, sideMargin, it.bottomMargin)
                card.layoutParams = it
            }

            // Remove top corner radius for flush appearance
            card.radius = 0f

            // Set height of top spacer view to push content below status bar and cutout
            val topSpacer = bannerView?.findViewById<View>(R.id.bannerTopSpacer)
            topSpacer?.layoutParams?.height = topInset
            topSpacer?.requestLayout()

            // Calculate and set minimum height on the card to cover both status bar and action bar
            // Total coverage needed = topInset + actionBarHeight
            val actionBarHeight = getActionBarHeight()
            val totalMinHeight = topInset + actionBarHeight
            card.minimumHeight = totalMinHeight

            // After setting minimum height, calculate and apply centering margins
            card.post {
                applyCenteringMargins(topInset)
            }
        }
    }

    /**
     * Calculate and apply vertical centering margins to the content area
     * This centers the content when the banner is at minimum height
     */
    private fun applyCenteringMargins(topInset: Int) {
        val contentArea = bannerView?.findViewById<ViewGroup>(R.id.bannerContentArea) ?: return
        val dragIndicator = bannerView?.findViewById<View>(R.id.bannerDragIndicator)

        // Get the action bar height (the visible area below status bar where content should be centered)
        val actionBarHeight = getActionBarHeight()
        if (actionBarHeight == 0) return

        // Measure the content area if not yet measured
        if (contentArea.measuredHeight == 0) {
            contentArea.measure(
                View.MeasureSpec.makeMeasureSpec(contentArea.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }

        val contentHeight = contentArea.measuredHeight

        // Get drag indicator height if visible (0 if gone/invisible)
        val dragIndicatorHeight = if (dragIndicator?.visibility == View.VISIBLE) {
            val margins = (dragIndicator.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
                it.topMargin + it.bottomMargin
            } ?: 0
            dragIndicator.height + margins
        } else {
            0
        }

        // Calculate available space in the visible area (action bar height)
        // actionBarHeight = contentHeight + topMargin + bottomMargin + dragIndicatorHeight
        val availableSpace = actionBarHeight - contentHeight - dragIndicatorHeight

        // Calculate equal margins for centering (min 4dp)
        val centeringMargin = maxOf(availableSpace / 2, 4.dpToPx(activity))

        Log.d("SmileBanner", "Centering calculation: actionBarHeight=$actionBarHeight, topInset=$topInset, contentHeight=$contentHeight, availableSpace=$availableSpace, centeringMargin=$centeringMargin")

        // Apply the calculated margins
        val layoutParams = contentArea.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        layoutParams?.let {
            it.topMargin = centeringMargin
            it.bottomMargin = centeringMargin
            contentArea.layoutParams = it
        }
    }

    /**
     * Get the action bar height from theme
     */
    private fun getActionBarHeight(): Int {
        val styledAttributes = activity.theme.obtainStyledAttributes(
            intArrayOf(android.R.attr.actionBarSize)
        )
        val actionBarHeight = styledAttributes.getDimension(0, 0f).toInt()
        styledAttributes.recycle()
        return actionBarHeight
    }

    /**
     * Configure status bar appearance
     * Makes status bar transparent and adjusts icon colors based on banner brightness
     */
    @Suppress("DEPRECATION")
    private fun applyStatusBarColor() {
        val window = activity.window
        val decorView = window.decorView

        // Save original state to restore later
        originalStatusBarColor = window.statusBarColor
        originalSystemUiVisibility = decorView.systemUiVisibility

        // Make status bar transparent so banner color shows through
        window.statusBarColor = Color.TRANSPARENT

        // Get banner background color
        val backgroundColor = when {
            config.backgroundColor != null -> config.backgroundColor
            config.backgroundColorRes != null -> ContextCompat.getColor(activity, config.backgroundColorRes)
            else -> getDefaultBackgroundColor()
        }

        // Calculate if we need light or dark status bar icons
        val isLightBackground = isColorLight(backgroundColor)

        // Set status bar icon color based on background brightness
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = decorView.systemUiVisibility
            flags = if (isLightBackground) {
                // Light background -> dark icons
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                // Dark background -> light icons
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            decorView.systemUiVisibility = flags
        }
    }

    /**
     * Restore original status bar color and icon appearance
     */
    @Suppress("DEPRECATION")
    private fun restoreStatusBarColor() {
        val window = activity.window
        val decorView = window.decorView

        // Restore status bar color
        originalStatusBarColor?.let {
            window.statusBarColor = it
        }

        // Restore system UI visibility (status bar icon colors)
        originalSystemUiVisibility?.let {
            decorView.systemUiVisibility = it
        }
    }

    /**
     * Calculate if a color is light or dark
     * Used to determine appropriate status bar icon color
     */
    private fun isColorLight(color: Int): Boolean {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        // Calculate perceived brightness using standard formula
        val brightness = (red * 299 + green * 587 + blue * 114) / 1000

        return brightness > 128
    }

    private fun configureDefaultBanner() {
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
                config.onLoadLeftImage.invoke(imageView, config.leftImageUrl, config.leftImageCircular)
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
                config.onLoadRightImage.invoke(imageView, config.rightImageUrl, config.rightImageCircular)
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
        Log.d("SmileBanner", "setupExpandable called - expandable: ${config.expandable}")

        if (!config.expandable) {
            Log.d("SmileBanner", "setupExpandable: banner is not expandable, returning")
            return
        }

        val dragIndicator = bannerView?.findViewById<View>(R.id.bannerDragIndicator)
        val expandableArea = bannerView?.findViewById<ViewGroup>(R.id.bannerExpandableArea)
        val expandableContent = bannerView?.findViewById<ViewGroup>(R.id.bannerExpandableContent)
        val expandableInput = bannerView?.findViewById<EditText>(R.id.bannerExpandableInput)
        val expandableButton = bannerView?.findViewById<android.widget.Button>(R.id.bannerExpandableButton)

        Log.d("SmileBanner", "setupExpandable: dragIndicator=$dragIndicator, expandableArea=$expandableArea, expandableContent=$expandableContent")

        // Start expandable area at height 0 (completely collapsed)
        expandableArea?.let { area ->
            area.visibility = View.VISIBLE
            area.layoutParams?.height = 0
            area.requestLayout()
            Log.d("SmileBanner", "setupExpandable: set expandableArea height to 0 (collapsed)")
        }

        // Hide the expandable content initially (input + button)
        expandableContent?.visibility = View.GONE
        Log.d("SmileBanner", "setupExpandable: hidden expandableContent")

        // Show drag indicator (positioned outside expandable area, so it's visible even when area is height 0)
        dragIndicator?.visibility = View.VISIBLE
        Log.d("SmileBanner", "setupExpandable: showing drag indicator")

        // Apply text color to input and button
        expandableInput?.setTextColor(textColor)
        expandableInput?.setHintTextColor(textColor)
        expandableButton?.setTextColor(textColor)

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
            else -> expandableButton?.setText(R.string.smile_banner_send_default)
        }

        // Note: Drag-to-expand is now handled by the container's touch listener
        // The drag indicator serves as a visual affordance only

        // Setup submit action for both button and keyboard
        val submitAction = {
            val text = expandableInput?.text?.toString() ?: ""
            Log.d("SmileBanner", "Submit action triggered with text: '$text'")

            if (text.isNotBlank()) {
                Log.d("SmileBanner", "Text is not blank, invoking onExpandableSubmit callback")
                config.onExpandableSubmit?.invoke(text)
                expandableInput?.text?.clear()

                // Hide keyboard first
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(expandableInput?.windowToken, 0)

                // Animate roll-up and dismiss
                Log.d("SmileBanner", "Starting roll-up animation")
                animateRollUpAndDismiss()
            } else {
                Log.d("SmileBanner", "Text is blank, not submitting")
            }
        }

        // Setup submit button
        expandableButton?.setOnClickListener {
            submitAction()
        }

        // Setup IME action for keyboard send button
        expandableInput?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                submitAction()
                true
            } else {
                false
            }
        }
    }

    private fun collapseToZero(expandableArea: ViewGroup?, expandableContent: ViewGroup?, card: CardView?) {
        if (expandableArea == null || card == null) return

        isExpanded = false

        // Restore PopupWindow to non-focusable for swipe gestures
        popupWindow?.isFocusable = false
        popupWindow?.update()

        Log.d("SmileBanner", "collapseToZero: collapsing from ${expandableArea.height} to 0")

        // Hide keyboard if open
        val expandableInput = bannerView?.findViewById<EditText>(R.id.bannerExpandableInput)
        expandableInput?.let { input ->
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(input.windowToken, 0)
        }

        // Calculate base card height
        val baseCardHeight = card.height - expandableArea.height

        // Animate back to 0 (fully collapsed)
        val currentHeight = expandableArea.height
        ValueAnimator.ofInt(currentHeight, 0).also {
            it.duration = 200
            it.addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                val layoutParams = expandableArea.layoutParams
                layoutParams.height = value
                expandableArea.layoutParams = layoutParams

                // Update popup window size
                popupWindow?.update(
                    popupWindow?.width ?: ViewGroup.LayoutParams.MATCH_PARENT,
                    baseCardHeight + value
                )
            }
            it.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    expandableContent?.visibility = View.GONE
                    Log.d("SmileBanner", "collapseToZero: animation complete, expandable area now at height 0")
                }
            })
            it.start()
        }
    }

    @Suppress("DEPRECATION")
    private fun expandToFullScreen(expandableArea: ViewGroup?, card: CardView?) {
        if (expandableArea == null || card == null) return

        isExpanded = true

        // Cancel auto-dismiss when expanded
        autoDismissJob?.cancel()

        // Make PopupWindow focusable and set input method mode to accept keyboard input
        popupWindow?.isFocusable = true
        popupWindow?.inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
        popupWindow?.softInputMode = android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        popupWindow?.update()

        // Show expandable content
        val expandableContent = bannerView?.findViewById<ViewGroup>(R.id.bannerExpandableContent)
        expandableContent?.visibility = View.VISIBLE

        // Get screen height
        val displayMetrics = activity.resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels

        // Calculate base card height (without current expandable area)
        val baseCardHeight = card.height - expandableArea.height

        // Calculate target height: fill remaining screen space
        val cardLocation = IntArray(2)
        card.getLocationOnScreen(cardLocation)
        val cardTop = cardLocation[1]
        val targetHeight = screenHeight - cardTop - baseCardHeight

        Log.d("SmileBanner", "expandToFullScreen: screenHeight=$screenHeight, cardTop=$cardTop, baseCardHeight=$baseCardHeight, targetHeight=$targetHeight")

        // Animate expandable area to full screen
        val currentHeight = expandableArea.height
        ValueAnimator.ofInt(currentHeight, targetHeight).also {
            it.duration = 300
            it.addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                val layoutParams = expandableArea.layoutParams
                layoutParams.height = value
                expandableArea.layoutParams = layoutParams

                // Update popup window size (base card + expandable area)
                val totalHeight = baseCardHeight + value
                Log.d("SmileBanner", "expandToFullScreen animation: expandableHeight=$value, totalHeight=$totalHeight")
                popupWindow?.update(
                    popupWindow?.width ?: ViewGroup.LayoutParams.MATCH_PARENT,
                    totalHeight.coerceAtMost(screenHeight)
                )
            }
            it.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    // Focus input and show keyboard after animation
                    val expandableInput = bannerView?.findViewById<EditText>(R.id.bannerExpandableInput)

                    // Post the keyboard request to ensure view is fully attached and laid out
                    expandableInput?.post {
                        expandableInput.requestFocus()

                        // Post again with a small delay to ensure the view is "served" by IMM
                        expandableInput.postDelayed({
                            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            Log.d("SmileBanner", "Requesting keyboard for input: hasFocus=${expandableInput.hasFocus()}, isAttachedToWindow=${expandableInput.isAttachedToWindow}")
                            imm?.showSoftInput(expandableInput, InputMethodManager.SHOW_IMPLICIT)
                        }, 100)
                    }
                }
            })
            it.start()
        }
    }

    /**
     * Animate the banner rolling up and then dismiss
     * Used when the user submits text in the expandable area
     */
    private fun animateRollUpAndDismiss() {
        val card = bannerView?.findViewById<CardView>(R.id.bannerCard) ?: return
        val expandableArea = bannerView?.findViewById<ViewGroup>(R.id.bannerExpandableArea)
        val expandableContent = bannerView?.findViewById<ViewGroup>(R.id.bannerExpandableContent)

        Log.d("SmileBanner", "animateRollUpAndDismiss: card height=${card.height}, expandableArea height=${expandableArea?.height}")

        // First collapse the expandable area
        if (expandableArea != null && expandableArea.height > 0) {
            val baseCardHeight = card.height - expandableArea.height
            val currentHeight = expandableArea.height

            Log.d("SmileBanner", "animateRollUpAndDismiss: collapsing from $currentHeight to 0")

            // Animate collapse
            ValueAnimator.ofInt(currentHeight, 0).also {
                it.duration = 200
                it.addUpdateListener { animator ->
                    val value = animator.animatedValue as Int
                    expandableArea.layoutParams?.height = value
                    expandableArea.requestLayout()

                    // Update popup window size
                    popupWindow?.update(
                        popupWindow?.width ?: ViewGroup.LayoutParams.MATCH_PARENT,
                        baseCardHeight + value
                    )
                }
                it.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        expandableContent?.visibility = View.GONE
                        Log.d("SmileBanner", "animateRollUpAndDismiss: collapse complete, now sliding up")
                        // After collapse, slide the whole banner up
                        slideUpAndDismiss(card)
                    }
                })
                it.start()
            }
        } else {
            // No expandable area or already collapsed, just slide up
            Log.d("SmileBanner", "animateRollUpAndDismiss: no expandable area, sliding up directly")
            slideUpAndDismiss(card)
        }
    }

    /**
     * Slide the card up and dismiss
     */
    private fun slideUpAndDismiss(card: CardView) {
        Log.d("SmileBanner", "slideUpAndDismiss: animating card up by ${card.height} pixels")

        card.animate()
            .translationY(-card.height.toFloat())
            .alpha(0f)
            .setDuration(250)
            .withEndAction {
                Log.d("SmileBanner", "slideUpAndDismiss: animation complete, calling callback and dismissing")

                // Call developer's callback asynchronously so it doesn't block dismiss
                config.onDismissAnimationComplete?.let { callback ->
                    scope.launch {
                        try {
                            callback.invoke()
                        } catch (e: Exception) {
                            Log.e("SmileBanner", "Error in developer's onDismissAnimationComplete callback", e)
                        }
                    }
                }

                dismiss()
            }
            .start()
    }

    private sealed class DragDirection {
        object UpToDismiss : DragDirection()
        object DownToExpand : DragDirection()
    }

    /**
     * Setup swipe gestures
     * - Swipe up: dismiss banner
     * - Swipe down: expand (if expandable)
     */
    private fun setupSwipeToDismiss() {
        val card = bannerView?.findViewById<CardView>(R.id.bannerCard) ?: return
        val container = bannerView?.findViewById<ViewGroup>(R.id.bannerContainer) ?: return
        val expandableArea = bannerView?.findViewById<ViewGroup>(R.id.bannerExpandableArea)

        var initialY = 0f
        var initialX = 0f
        var initialTranslationY = 0f
        var baseCardHeight = 0
        var screenHeight = 0
        var isDragging = false
        var dragDirection: DragDirection? = null // Track committed direction
        var touchStartedOnInteractiveView = false

        container.setOnTouchListener { _, event ->
            Log.d("SmileBanner", "Container touch event: action=${event.action}, x=${event.x}, y=${event.y}")

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Capture initial touch state - these values are read in ACTION_MOVE/ACTION_UP
                    // IDE may warn "never read" but they're used in subsequent event callbacks
                    initialY = event.rawY
                    initialX = event.rawX
                    initialTranslationY = card.translationY
                    isDragging = false
                    dragDirection = null

                    // Calculate base card height for expand logic
                    expandableArea?.let {
                        baseCardHeight = card.height - it.height
                    }

                    // Get screen height
                    val displayMetrics = activity.resources.displayMetrics
                    screenHeight = displayMetrics.heightPixels

                    // Cancel auto-dismiss immediately on any touch interaction
                    if (config.duration > 0) {
                        autoDismissJob?.cancel()
                        Log.d("SmileBanner", "Auto-dismiss paused - user interaction started")
                    }

                    // Check if expandable content is visible - if so, check if touch is inside it
                    val expandableContent = bannerView?.findViewById<ViewGroup>(R.id.bannerExpandableContent)
                    var touchInsideExpandableContent = false

                    if (expandableContent?.visibility == View.VISIBLE) {
                        // Check if touch coordinates are inside expandable content bounds
                        val location = IntArray(2)
                        expandableContent.getLocationInWindow(location)
                        val containerLocation = IntArray(2)
                        container.getLocationInWindow(containerLocation)

                        val contentX = location[0] - containerLocation[0]
                        val contentY = location[1] - containerLocation[1]

                        touchInsideExpandableContent = event.x >= contentX &&
                                event.x <= contentX + expandableContent.width &&
                                event.y >= contentY &&
                                event.y <= contentY + expandableContent.height

                        Log.d("SmileBanner", "Container ACTION_DOWN: expandableContent visible, touchInside=$touchInsideExpandableContent")
                    }

                    // Check if touch started on an interactive child view
                    val touchedView = findViewAt(container, event.x, event.y)
                    touchStartedOnInteractiveView = touchInsideExpandableContent ||
                            touchedView is EditText ||
                            touchedView is android.widget.Button

                    Log.d("SmileBanner", "Container ACTION_DOWN: touchedView=$touchedView, isInteractive=$touchStartedOnInteractiveView")

                    // If touch is on interactive view, request that parent doesn't intercept
                    if (touchStartedOnInteractiveView) {
                        container.parent?.requestDisallowInterceptTouchEvent(true)
                    }

                    // Don't consume if on interactive view, let it handle the touch
                    val shouldConsume = touchStartedOnInteractiveView.not()
                    Log.d("SmileBanner", "Container ACTION_DOWN: returning $shouldConsume")
                    shouldConsume
                }
                MotionEvent.ACTION_MOVE -> {
                    if (touchStartedOnInteractiveView) {
                        Log.d("SmileBanner", "Container ACTION_MOVE: touch on interactive view, not handling")
                        return@setOnTouchListener false
                    }

                    val deltaY = event.rawY - initialY
                    val deltaX = event.rawX - initialX
                    val absDeltaY = kotlin.math.abs(deltaY)
                    val absDeltaX = kotlin.math.abs(deltaX)

                    Log.d("SmileBanner", "Container ACTION_MOVE: deltaY=$deltaY, absDeltaY=$absDeltaY, isDragging=$isDragging, direction=$dragDirection")

                    // Detect and commit to drag direction once threshold is crossed
                    if (!isDragging && absDeltaY > 20 && absDeltaY > absDeltaX) {
                        isDragging = true

                        // Determine direction and commit
                        dragDirection = when {
                            deltaY < 0 -> {
                                // Upward swipe → dismiss
                                Log.d("SmileBanner", "Container ACTION_MOVE: committed to UpToDismiss")
                                DragDirection.UpToDismiss
                            }
                            deltaY > 0 && config.expandable && !isExpanded -> {
                                // Downward swipe → expand (only if expandable and not already expanded)
                                Log.d("SmileBanner", "Container ACTION_MOVE: committed to DownToExpand")
                                DragDirection.DownToExpand
                            }
                            else -> {
                                Log.d("SmileBanner", "Container ACTION_MOVE: no valid direction")
                                null
                            }
                        }
                    }

                    // Execute action based on committed direction
                    when (dragDirection) {
                        is DragDirection.UpToDismiss -> {
                            // Dismiss: move card up
                            if (deltaY < 0) {
                                card.translationY = initialTranslationY + deltaY
                                Log.d("SmileBanner", "Container ACTION_MOVE: dismissing, translationY=${card.translationY}")
                            }
                            true
                        }
                        is DragDirection.DownToExpand -> {
                            // Expand: grow expandable area
                            expandableArea?.let { area ->
                                val currentHeight = area.height
                                val newHeight = (currentHeight + deltaY.toInt()).coerceAtLeast(0)

                                // Show content when height exceeds threshold
                                val contentShowThreshold = 60
                                val expandableContent = bannerView?.findViewById<ViewGroup>(R.id.bannerExpandableContent)
                                if (newHeight > contentShowThreshold && expandableContent?.visibility != View.VISIBLE) {
                                    expandableContent?.visibility = View.VISIBLE
                                    Log.d("SmileBanner", "Container ACTION_MOVE: showing expandable content")
                                }

                                // Update heights
                                area.layoutParams.height = newHeight
                                area.requestLayout()

                                popupWindow?.update(
                                    popupWindow?.width ?: ViewGroup.LayoutParams.MATCH_PARENT,
                                    (baseCardHeight + newHeight).coerceAtMost(screenHeight)
                                )

                                Log.d("SmileBanner", "Container ACTION_MOVE: expanding, newHeight=$newHeight")

                                // Update initialY for continuous dragging
                                initialY = event.rawY
                            }
                            true
                        }
                        else -> false
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    Log.d("SmileBanner", "Container ACTION_UP: isDragging=$isDragging, direction=$dragDirection")

                    if (touchStartedOnInteractiveView) {
                        return@setOnTouchListener false
                    }

                    if (isDragging) {
                        when (dragDirection) {
                            is DragDirection.UpToDismiss -> {
                                // Complete dismiss action
                                val deltaY = event.rawY - initialY
                                val dismissThreshold = 100

                                Log.d("SmileBanner", "Container ACTION_UP: dismiss deltaY=$deltaY, threshold=$dismissThreshold")

                                if (deltaY < -dismissThreshold) {
                                    // Animate out and dismiss
                                    Log.d("SmileBanner", "Container ACTION_UP: dismissing banner")
                                    card.animate()
                                        .translationY(-card.height.toFloat())
                                        .alpha(0f)
                                        .setDuration(200)
                                        .withEndAction {
                                            // Call developer's callback asynchronously so it doesn't block dismiss
                                            config.onDismissAnimationComplete?.let { callback ->
                                                scope.launch {
                                                    try {
                                                        callback.invoke()
                                                    } catch (e: Exception) {
                                                        Log.e("SmileBanner", "Error in developer's onDismissAnimationComplete callback", e)
                                                    }
                                                }
                                            }
                                            dismiss()
                                        }
                                        .start()
                                } else {
                                    // Animate back to original position
                                    Log.d("SmileBanner", "Container ACTION_UP: animating back")
                                    card.animate()
                                        .translationY(initialTranslationY)
                                        .setDuration(200)
                                        .withEndAction {
                                            // Restart auto-dismiss timer after animation
                                            restartAutoDismiss()
                                        }
                                        .start()
                                }
                            }
                            is DragDirection.DownToExpand -> {
                                // Complete expand action
                                expandableArea?.let { area ->
                                    val currentHeight = area.height
                                    val snapThreshold = 180

                                    Log.d("SmileBanner", "Container ACTION_UP: expand currentHeight=$currentHeight, threshold=$snapThreshold")

                                    if (currentHeight > snapThreshold) {
                                        // Snap to full screen - don't restart timer, user is expanding
                                        Log.d("SmileBanner", "Container ACTION_UP: expanding to full screen")
                                        expandToFullScreen(area, card)
                                    } else {
                                        // Collapse back to 0 - user didn't expand, restart timer
                                        Log.d("SmileBanner", "Container ACTION_UP: collapsing to 0")
                                        val expandableContent = bannerView?.findViewById<ViewGroup>(R.id.bannerExpandableContent)
                                        collapseToZero(area, expandableContent, card)
                                        // Restart timer after collapse animation completes (200ms)
                                        bannerView?.postDelayed({
                                            restartAutoDismiss()
                                        }, 200L)
                                    }
                                }
                            }
                            else -> {
                                Log.d("SmileBanner", "Container ACTION_UP: no action")
                                // Restart timer if no action taken
                                restartAutoDismiss()
                            }
                        }
                        isDragging = false
                        dragDirection = null
                        true
                    } else {
                        // Not dragging, treat as a click
                        Log.d("SmileBanner", "Container ACTION_UP: treating as click")
                        container.performClick()
                        config.onBannerClick?.invoke(container)
                        // Restart timer after click
                        restartAutoDismiss()
                        false
                    }
                }
                else -> {
                    Log.d("SmileBanner", "Container: unknown event action=${event.action}")
                    false
                }
            }
        }
    }

    /**
     * Find the view at the given coordinates
     */
    private fun findViewAt(parent: ViewGroup, x: Float, y: Float): View? {
        Log.d("SmileBanner", "findViewAt: parent=$parent, x=$x, y=$y, childCount=${parent.childCount}")

        for (i in parent.childCount - 1 downTo 0) {
            val child = parent.getChildAt(i)
            if (child.isVisible) {
                val location = IntArray(2)
                child.getLocationInWindow(location)
                val parentLocation = IntArray(2)
                parent.getLocationInWindow(parentLocation)

                val childX = location[0] - parentLocation[0]
                val childY = location[1] - parentLocation[1]

                Log.d("SmileBanner", "findViewAt: checking child[${i}]=${child::class.simpleName}, id=${child.id}, bounds=[${childX},${childY},${childX + child.width},${childY + child.height}]")

                if (x >= childX && x <= childX + child.width &&
                    y >= childY && y <= childY + child.height) {
                    Log.d("SmileBanner", "findViewAt: touch inside child[${i}]=${child::class.simpleName}")
                    if (child is ViewGroup) {
                        val nestedView = findViewAt(child, x - childX, y - childY)
                        if (nestedView != null) {
                            Log.d("SmileBanner", "findViewAt: found nested view=${nestedView::class.simpleName}, id=${nestedView.id}")
                            return nestedView
                        }
                    }
                    Log.d("SmileBanner", "findViewAt: returning child=${child::class.simpleName}, id=${child.id}")
                    return child
                }
            }
        }
        Log.d("SmileBanner", "findViewAt: no view found at coordinates")
        return null
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

        // Capture reference to this SmileBanner instance for use in closures
        val thisBanner = this

        popupWindow = PopupWindow(bannerView, width, height, false).apply {
            // Set animation style: use push-up stacking animation when replacing
            animationStyle = if (isReplacingBanner) {
                R.style.SmileBannerAnimationTopShort
            } else {
                R.style.SmileBannerAnimationTop
            }

            // Allow popup to draw outside of screen bounds (over status bar)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                isClippingEnabled = false
            }

            // Get the root view of the activity
            val rootView = activity.findViewById<View>(android.R.id.content)

            // Show the popup
            rootView.post {
                try {
                    // When replacing, hide content initially so only background is visible
                    if (isReplacingBanner) {
                        hideBannerContent()
                    }

                    // Always show at TOP
                    showAtLocation(rootView, Gravity.TOP, 0, 0)

                    // Apply system bar padding after popup is shown
                    bannerView?.post {
                        applySystemBarPadding()
                    }

                    // When replacing, fade in content after 30% of animation (105ms)
                    if (isReplacingBanner) {
                        bannerView?.postDelayed({
                            // Fade in content quickly over 150ms
                            fadeInBannerContent(150)
                        }, 105)
                    }

                    // If this banner replaced another, reset animation style after entrance animation completes
                    // Reset to normal style so later dismissal uses the proper slide-out animation
                    if (isReplacingBanner) {
                        bannerView?.postDelayed({
                            popupWindow?.let { popup ->
                                popup.animationStyle = R.style.SmileBannerAnimationTop
                                // Call update() to apply the animation style change
                                popup.update()
                            }
                        }, 350L) // Full push-up slide animation duration
                    }

                    // After entrance animation completes, trigger internal callback for queue management
                    val entranceAnimationDuration = if (isReplacingBanner) 350L else 350L // Both use same duration
                    bannerView?.postDelayed({
                        Log.d("SmileBanner", "Entrance animation complete")

                        // IMPORTANT: Schedule queue management FIRST, before calling developer's callback
                        // This ensures that even if developer's callback blocks or takes time,
                        // the queue timer is already set up and will fire at the correct time
                        if (currentBanner == thisBanner && pendingQueue.isNotEmpty()) {
                            // Calculate how long this banner has been shown
                            val currentTime = System.currentTimeMillis()
                            val timeShown = currentTime - lastBannerShowTime

                            // Calculate delay: minimum display time + 300ms transition delay
                            val minDisplayRemaining = if (timeShown < MIN_DISPLAY_TIME) {
                                MIN_DISPLAY_TIME - timeShown
                            } else {
                                0L
                            }
                            val transitionDelay = 300L // Additional delay between banners
                            val totalDelay = minDisplayRemaining + transitionDelay

                            Log.d("SmileBanner", "Scheduling next banner in queue after ${totalDelay}ms (timeShown=${timeShown}ms, minDisplayTime=${MIN_DISPLAY_TIME}ms, transitionDelay=${transitionDelay}ms)")

                            // Schedule processing of next banner
                            bannerView?.postDelayed({
                                if (currentBanner == thisBanner) {
                                    Log.d("SmileBanner", "Processing next banner from queue")
                                    processNextInQueue()
                                }
                            }, totalDelay)
                        }

                        // Call developer's callback asynchronously on main thread so it doesn't block our code
                        // Even if developer does heavy synchronous work, it won't block the library
                        config.onShowAnimationComplete?.let { callback ->
                            scope.launch {
                                try {
                                    callback.invoke()
                                } catch (e: Exception) {
                                    Log.e("SmileBanner", "Error in developer's onShowAnimationComplete callback", e)
                                }
                            }
                        }
                    }, entranceAnimationDuration)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
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
     * Restart auto-dismiss timer if banner has auto-dismiss configured
     * Used when user interaction ends without expanding or dismissing
     */
    private fun restartAutoDismiss() {
        if (config.duration > 0 && !isExpanded) {
            autoDismissJob?.cancel()
            autoDismissJob = scope.launch {
                delay(config.duration)
                dismiss()
            }
            Log.d("SmileBanner", "Auto-dismiss timer restarted")
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
        private var onShowAnimationComplete: (() -> Unit)? = null
        private var onDismissAnimationComplete: (() -> Unit)? = null
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
         * Set callback for when the show/entrance animation completes
         * This is called after the banner has fully appeared on screen
         */
        fun onShowAnimationComplete(listener: () -> Unit) = apply { this.onShowAnimationComplete = listener }

        /**
         * Set callback for when the dismiss/exit animation completes
         * This is called after the banner has fully disappeared from screen
         */
        fun onDismissAnimationComplete(listener: () -> Unit) = apply { this.onDismissAnimationComplete = listener }

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
                onShowAnimationComplete = onShowAnimationComplete,
                onDismissAnimationComplete = onDismissAnimationComplete,
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
