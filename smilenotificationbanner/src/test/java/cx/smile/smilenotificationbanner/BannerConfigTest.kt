package cx.smile.smilenotificationbanner

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BannerConfigTest {

    @Test
    fun `config with string message`() {
        val config = BannerConfig(
            type = BannerType.SUCCESS,
            message = "Test message"
        )
        assertEquals("Test message", config.message)
        assertNull(config.messageRes)
    }

    @Test
    fun `config with message resource`() {
        val config = BannerConfig(
            type = BannerType.INFO,
            messageRes = android.R.string.ok
        )
        assertEquals(android.R.string.ok, config.messageRes)
        assertNull(config.message)
    }

    @Test
    fun `config with default values`() {
        val config = BannerConfig(
            type = BannerType.INFO,
            message = "Test"
        )
        assertEquals(BannerPosition.TOP, config.position)
        assertEquals(0L, config.duration)
        assertEquals(true, config.dismissible)
        assertNull(config.customLayout)
        assertNull(config.backgroundColor)
        assertNull(config.textColor)
        assertNull(config.icon)
        assertNull(config.onBannerClick)
        assertNull(config.onDismiss)
        assertEquals(VibrationDuration.NONE, config.vibrationDuration)
    }

    @Test
    fun `config with custom values`() {
        val clickListener: (android.view.View) -> Unit = {}
        val dismissListener: () -> Unit = {}

        val config = BannerConfig(
            type = BannerType.CUSTOM,
            message = "Test",
            position = BannerPosition.BOTTOM,
            duration = 5000L,
            dismissible = false,
            customLayout = android.R.layout.simple_list_item_1,
            backgroundColor = Color.RED,
            textColor = Color.WHITE,
            icon = android.R.drawable.ic_dialog_info,
            onBannerClick = clickListener,
            onDismiss = dismissListener
        )

        assertEquals(BannerType.CUSTOM, config.type)
        assertEquals("Test", config.message)
        assertEquals(BannerPosition.BOTTOM, config.position)
        assertEquals(5000L, config.duration)
        assertEquals(false, config.dismissible)
        assertEquals(android.R.layout.simple_list_item_1, config.customLayout)
        assertEquals(Color.RED, config.backgroundColor)
        assertEquals(Color.WHITE, config.textColor)
        assertEquals(android.R.drawable.ic_dialog_info, config.icon)
        assertEquals(clickListener, config.onBannerClick)
        assertEquals(dismissListener, config.onDismiss)
    }

    @Test
    fun `config with color resources`() {
        val config = BannerConfig(
            type = BannerType.CUSTOM,
            message = "Test",
            backgroundColorRes = android.R.color.black,
            textColorRes = android.R.color.white
        )

        assertNull(config.backgroundColor)
        assertEquals(android.R.color.black, config.backgroundColorRes)
        assertNull(config.textColor)
        assertEquals(android.R.color.white, config.textColorRes)
    }

    @Test
    fun `config data class copy works`() {
        val original = BannerConfig(
            type = BannerType.SUCCESS,
            message = "Original"
        )

        val copy = original.copy(message = "Modified")

        assertEquals("Modified", copy.message)
        assertEquals(BannerType.SUCCESS, copy.type)
    }

    @Test
    fun `config data class equality`() {
        val config1 = BannerConfig(
            type = BannerType.INFO,
            message = "Test"
        )

        val config2 = BannerConfig(
            type = BannerType.INFO,
            message = "Test"
        )

        assertEquals(config1, config2)
        assertEquals(config1.hashCode(), config2.hashCode())
    }

    @Test
    fun `config with vibration`() {
        val config = BannerConfig(
            type = BannerType.INFO,
            message = "Test",
            vibrationDuration = VibrationDuration.SHORT
        )

        assertEquals(VibrationDuration.SHORT, config.vibrationDuration)
    }

    @Test
    fun `config with all vibration durations`() {
        VibrationDuration.values().forEach { duration ->
            val config = BannerConfig(
                type = BannerType.INFO,
                message = "Test",
                vibrationDuration = duration
            )
            assertEquals(duration, config.vibrationDuration)
        }
    }
}
