package cx.smile.smilenotificationbanner.sample

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import cx.smile.smilenotificationbanner.BannerType
import cx.smile.smilenotificationbanner.SmileBanner
import cx.smile.smilenotificationbanner.VibrationDuration

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupBannerTypeButtons()
        setupDurationButtons()
        setupCustomizationButtons()
        setupVibrationButtons()
        setupSingletonButtons()
        setupEnhancedLayoutButtons()
        setupExpandableButtons()
        setupDebugButtons()
    }

    private fun setupBannerTypeButtons() {
        findViewById<MaterialButton>(R.id.btnSuccess).setOnClickListener {
            SmileBanner.show(
                this,
                BannerType.SUCCESS,
                getString(R.string.banner_success_msg)
            )
        }

        findViewById<MaterialButton>(R.id.btnInfo).setOnClickListener {
            SmileBanner.show(
                this,
                BannerType.INFO,
                getString(R.string.banner_info_msg)
            )
        }

        findViewById<MaterialButton>(R.id.btnWarning).setOnClickListener {
            SmileBanner.make(this)
                .type(BannerType.WARNING)
                .title(getString(R.string.banner_warning_title))
                .message(getString(R.string.banner_warning_msg))
                .show()
        }

        findViewById<MaterialButton>(R.id.btnError).setOnClickListener {
            SmileBanner.make(this)
                .type(BannerType.ERROR)
                .title(getString(R.string.banner_error_title))
                .message(getString(R.string.banner_error_msg))
                .show()
        }
    }

    private fun setupDurationButtons() {
        findViewById<MaterialButton>(R.id.btnAutoDismiss).setOnClickListener {
            SmileBanner.show(
                this,
                BannerType.INFO,
                getString(R.string.banner_auto_msg),
                3000L // 3 seconds
            )
        }
    }

    private fun setupCustomizationButtons() {
        // Example 1: Using resource IDs for colors and strings
        findViewById<MaterialButton>(R.id.btnCustomColors).setOnClickListener {
            SmileBanner.make(this)
                .type(BannerType.CUSTOM)
                .message(R.string.banner_custom_msg) // Using string resource ID
                .backgroundColor(Color.parseColor("#9C27B0")) // Purple
                .textColor(Color.WHITE)
                .show()
        }

        // Example 2: Using direct color values with click listener
        findViewById<MaterialButton>(R.id.btnWithClick).setOnClickListener {
            SmileBanner.make(this)
                .type(BannerType.INFO)
                .message(getString(R.string.banner_click_msg)) // Using string directly
                .onBannerClick { _ ->
                    Toast.makeText(
                        this,
                        "Banner clicked!",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Optionally dismiss the banner
                    SmileBanner.dismissCurrent()
                }
                .onDismiss {
                    // Called when banner is dismissed
                }
                .show()
        }
    }

    private fun setupVibrationButtons() {
        // Short vibration (50ms)
        findViewById<MaterialButton>(R.id.btnVibrateShort).setOnClickListener {
            SmileBanner.make(this)
                .type(BannerType.INFO)
                .message(getString(R.string.banner_vibrate_msg))
                .vibrate(VibrationDuration.SHORT)
                .duration(2000L)
                .show()
        }

        // Medium vibration (100ms)
        findViewById<MaterialButton>(R.id.btnVibrateMedium).setOnClickListener {
            SmileBanner.make(this)
                .type(BannerType.INFO)
                .message(getString(R.string.banner_vibrate_msg))
                .vibrate(VibrationDuration.MEDIUM)
                .duration(2000L)
                .show()
        }

        // Long vibration (200ms)
        findViewById<MaterialButton>(R.id.btnVibrateLong).setOnClickListener {
            SmileBanner.make(this)
                .type(BannerType.INFO)
                .message(getString(R.string.banner_vibrate_msg))
                .vibrate(VibrationDuration.LONG)
                .duration(2000L)
                .show()
        }
    }

    private fun setupSingletonButtons() {
        // Test rapid banners to demonstrate singleton behavior and height animations
        // Library manages timing internally - no manual delays needed
        findViewById<MaterialButton>(R.id.btnRapidBanners).setOnClickListener {
            // Show 10 different banner types with varying heights
            // 1. Short message only
            SmileBanner.make(this)
                .type(BannerType.INFO)
                .message("New message")
                .duration(3000L)
                .show()

            // 2. Title + message (taller)
            SmileBanner.make(this)
                .type(BannerType.WARNING)
                .title("Warning")
                .message("This is a warning message with both title and content")
                .duration(3000L)
                .show()

            // 3. Short message again (should shrink)
            SmileBanner.make(this)
                .type(BannerType.SUCCESS)
                .message("Done!")
                .duration(3000L)
                .show()

            // 4. Longer message (taller)
            SmileBanner.make(this)
                .type(BannerType.ERROR)
                .title("Error")
                .message("An error occurred while processing your request. Please try again later.")
                .duration(3000L)
                .show()

            // 5. Medium message
            SmileBanner.make(this)
                .type(BannerType.INFO)
                .title("New Message")
                .message("You have a new message from Alice")
                .duration(3000L)
                .show()

            // 6. Expandable banner (tallest)
            SmileBanner.make(this)
                .type(BannerType.INFO)
                .title("Message from Bob")
                .message("Hey! How are you?")
                .expandable(true)
                .expandableInputHint("Reply...")
                .expandableButtonText("Send")
                .onExpandableSubmit { text ->
                    Toast.makeText(this, "Replied: $text", Toast.LENGTH_SHORT).show()
                }
                .duration(5000L)
                .show()

            // 7. Short message (should shrink from expandable)
            SmileBanner.make(this)
                .type(BannerType.SUCCESS)
                .message("Message sent!")
                .duration(3000L)
                .show()

            // 8. Very long message (wraps multiple lines)
            SmileBanner.make(this)
                .type(BannerType.WARNING)
                .title("Storage Almost Full")
                .message("Your device storage is almost full. Please delete some files to free up space and continue using the app normally.")
                .duration(3000L)
                .show()

            // 9. Title only
            SmileBanner.make(this)
                .type(BannerType.INFO)
                .title("Notification")
                .duration(3000L)
                .show()

            // 10. Final banner with title and message
            SmileBanner.make(this)
                .type(BannerType.SUCCESS)
                .title("All Done!")
                .message("You've seen all the different banner variations")
                .duration(3000L)
                .show()
        }
    }

    private fun setupEnhancedLayoutButtons() {
        // Banner with title
        findViewById<MaterialButton>(R.id.btnWithTitle).setOnClickListener {
            SmileBanner.make(this)
                .type(BannerType.INFO)
                .title(getString(R.string.banner_title_example))
                .message(getString(R.string.banner_subtitle_example))
                .duration(3000L)
                .show()
        }

        // Banner with left image (using drawable)
        findViewById<MaterialButton>(R.id.btnWithLeftImage).setOnClickListener {
            SmileBanner.make(this)
                .type(BannerType.SUCCESS)
                .title("New Achievement!")
                .message("You've unlocked a new badge")
                .leftImage(android.R.drawable.star_big_on)
                .duration(3000L)
                .show()
        }

        // Banner with right image
        findViewById<MaterialButton>(R.id.btnWithRightImage).setOnClickListener {
            SmileBanner.make(this)
                .type(BannerType.WARNING)
                .title("Update Available")
                .message("Version 2.0 is ready to install")
                .rightImage(android.R.drawable.ic_popup_sync)
                .duration(3000L)
                .show()
        }

        // Banner with custom view (circular badge)
        findViewById<MaterialButton>(R.id.btnWithCustomView).setOnClickListener {
            // Create a custom badge view
            val badgeView = layoutInflater.inflate(
                android.R.layout.simple_list_item_1,
                null
            ).apply {
                findViewById<android.widget.TextView>(android.R.id.text1)?.apply {
                    text = "3"
                    textSize = 16f
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.RED)
                    gravity = android.view.Gravity.CENTER
                    layoutParams = android.view.ViewGroup.LayoutParams(60, 60)
                }
            }

            SmileBanner.make(this)
                .type(BannerType.INFO)
                .title("New Messages")
                .message("You have 3 unread messages")
                .rightView(badgeView)
                .duration(3000L)
                .show()
        }
    }

    private fun setupExpandableButtons() {
        findViewById<MaterialButton>(R.id.btnExpandableBanner).setOnClickListener {
            SmileBanner.make(this)
                .type(BannerType.INFO)
                .title(getString(R.string.banner_expandable_title))
                .message(getString(R.string.banner_expandable_message))
                .leftImage(android.R.drawable.ic_dialog_email)
                .expandable(true)
                .expandableInputHint("Type your reply...")
                .expandableButtonText("Send")
                .onExpandableSubmit { text ->
                    android.widget.Toast.makeText(
                        this,
                        "You replied: $text",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                .duration(3000L) // Auto-dismiss after 3 seconds
                .show()
        }
    }

    private fun setupDebugButtons() {
        // Debug button with colored sections and long text
        findViewById<MaterialButton>(R.id.btnDebugLayout)?.setOnClickListener {
            SmileBanner.make(this)
                .type(BannerType.SUCCESS)
                .title("Debug Layout Test Banner")
                .message("This is a very long message that demonstrates the layout behavior with multiple lines of text. The red background shows the container, green shows the left icon area, blue shows the content area, and yellow shows the right area.")
                .duration(10000L)
                .show()
        }
    }
}
