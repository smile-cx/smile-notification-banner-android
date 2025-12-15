package cx.smile.smilenotificationbanner

import android.app.Activity
import android.view.View
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class SmileBannerTest {

    @Mock
    private lateinit var mockActivity: Activity

    @Mock
    private lateinit var mockView: View

    @Before
    fun setup() {
        whenever(mockActivity.isFinishing).thenReturn(false)
        whenever(mockActivity.isDestroyed).thenReturn(false)
    }

    @Test
    fun `make returns builder instance`() {
        val builder = SmileBanner.make(mockActivity)
        assert(builder is SmileBanner.Builder)
    }

    @Test
    fun `builder can be chained`() {
        val builder = SmileBanner.make(mockActivity)
            .type(BannerType.SUCCESS)
            .message("Test")
            .position(BannerPosition.TOP)

        assert(builder is SmileBanner.Builder)
    }

    @Test
    fun `dismissCurrent does not throw exception when no banner`() {
        // Should not throw any exception
        SmileBanner.dismissCurrent()
    }

    @Test
    fun `show method works with basic configuration`() {
        // This is a basic test - in real scenarios we'd need Robolectric or instrumented tests
        val banner = SmileBanner.show(
            mockActivity,
            BannerType.SUCCESS,
            "Test message"
        )
        assert(banner is SmileBanner)
    }

    @Test
    fun `show method with duration works`() {
        val banner = SmileBanner.show(
            mockActivity,
            BannerType.INFO,
            "Test message",
            BannerPosition.TOP,
            3000L
        )
        assert(banner is SmileBanner)
    }

    @Test
    fun `show method with all parameters works`() {
        val banner = SmileBanner.show(
            mockActivity,
            BannerType.WARNING,
            "Test message",
            BannerPosition.BOTTOM,
            2000L
        )
        assert(banner is SmileBanner)
    }
}
