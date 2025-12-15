package cx.smile.smilenotificationbanner

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for SmileBanner
 * These tests run on an Android device or emulator
 */
@RunWith(AndroidJUnit4::class)
class SmileBannerInstrumentedTest {

    @Test
    fun useAppContext() {
        // Context of the app under test
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("cx.smile.smilenotificationbanner.test", appContext.packageName)
    }

    @Test
    fun bannerTypesExist() {
        val types = BannerType.values()
        assertEquals(5, types.size)
    }

    @Test
    fun bannerPositionsExist() {
        val positions = BannerPosition.values()
        assertEquals(2, positions.size)
    }
}
