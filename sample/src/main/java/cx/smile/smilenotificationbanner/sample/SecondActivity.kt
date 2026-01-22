package cx.smile.smilenotificationbanner.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import cx.smile.smilenotificationbanner.SmileBanner

/**
 * SecondActivity demonstrates the pending banner feature.
 * When navigated to from MainActivity (after scheduling a pending banner),
 * this activity will automatically show the pending banner in onResume().
 */
class SecondActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        findViewById<MaterialButton>(R.id.btnBackToMain).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Show pending banner if one was scheduled from previous activity
        SmileBanner.showPendingIfAvailable(this)
    }
}
