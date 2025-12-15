package cx.smile.smilenotificationbanner.sample

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import cx.smile.smilenotificationbanner.BannerPosition
import cx.smile.smilenotificationbanner.BannerType
import cx.smile.smilenotificationbanner.SmileBanner

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupBannerTypeButtons()
        setupPositionButtons()
        setupDurationButtons()
        setupCustomizationButtons()
    }

    private fun setupBannerTypeButtons() {
        findViewById<MaterialButton>(R.id.btnSuccess).setOnClickListener {
            SmileBanner.show(
                this,
                BannerType.SUCCESS,
                getString(R.string.banner_success_msg),
                BannerPosition.TOP
            )
        }

        findViewById<MaterialButton>(R.id.btnInfo).setOnClickListener {
            SmileBanner.show(
                this,
                BannerType.INFO,
                getString(R.string.banner_info_msg),
                BannerPosition.TOP
            )
        }

        findViewById<MaterialButton>(R.id.btnWarning).setOnClickListener {
            SmileBanner.show(
                this,
                BannerType.WARNING,
                getString(R.string.banner_warning_msg),
                BannerPosition.TOP
            )
        }

        findViewById<MaterialButton>(R.id.btnError).setOnClickListener {
            SmileBanner.show(
                this,
                BannerType.ERROR,
                getString(R.string.banner_error_msg),
                BannerPosition.TOP
            )
        }
    }

    private fun setupPositionButtons() {
        findViewById<MaterialButton>(R.id.btnTop).setOnClickListener {
            SmileBanner.show(
                this,
                BannerType.INFO,
                getString(R.string.banner_top_msg),
                BannerPosition.TOP
            )
        }

        findViewById<MaterialButton>(R.id.btnBottom).setOnClickListener {
            SmileBanner.show(
                this,
                BannerType.INFO,
                getString(R.string.banner_bottom_msg),
                BannerPosition.BOTTOM
            )
        }
    }

    private fun setupDurationButtons() {
        findViewById<MaterialButton>(R.id.btnAutoDismiss).setOnClickListener {
            SmileBanner.show(
                this,
                BannerType.INFO,
                getString(R.string.banner_auto_msg),
                BannerPosition.TOP,
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
                .position(BannerPosition.TOP)
                .backgroundColor(Color.parseColor("#9C27B0")) // Purple
                .textColor(Color.WHITE)
                .show()
        }

        // Example 2: Using direct color values with click listener
        findViewById<MaterialButton>(R.id.btnWithClick).setOnClickListener {
            SmileBanner.make(this)
                .type(BannerType.INFO)
                .message(getString(R.string.banner_click_msg)) // Using string directly
                .position(BannerPosition.TOP)
                .onBannerClick { view ->
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
}
