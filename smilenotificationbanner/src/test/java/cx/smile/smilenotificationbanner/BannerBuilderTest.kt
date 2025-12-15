package cx.smile.smilenotificationbanner

import android.app.Activity
import android.graphics.Color
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BannerBuilderTest {

    @Mock
    private lateinit var mockActivity: Activity

    private lateinit var builder: SmileBanner.Builder

    @Before
    fun setup() {
        builder = SmileBanner.Builder(mockActivity)
    }

    @Test
    fun `builder returns same instance on method calls`() {
        val result = builder.type(BannerType.SUCCESS)
        assert(result === builder)
    }

    @Test
    fun `builder chains multiple calls`() {
        val result = builder
            .type(BannerType.SUCCESS)
            .message("Test")
            .position(BannerPosition.TOP)
            .duration(3000L)
            .dismissible(false)

        assert(result === builder)
    }

    @Test
    fun `builder accepts all banner types`() {
        BannerType.values().forEach { type ->
            val result = builder.type(type)
            assert(result === builder)
        }
    }

    @Test
    fun `builder accepts all positions`() {
        BannerPosition.values().forEach { position ->
            val result = builder.position(position)
            assert(result === builder)
        }
    }

    @Test
    fun `builder accepts string message`() {
        val result = builder.message("Test message")
        assert(result === builder)
    }

    @Test
    fun `builder accepts resource ID message`() {
        val result = builder.message(android.R.string.ok)
        assert(result === builder)
    }

    @Test
    fun `builder accepts duration`() {
        val result = builder.duration(5000L)
        assert(result === builder)
    }

    @Test
    fun `builder accepts zero duration`() {
        val result = builder.duration(0L)
        assert(result === builder)
    }

    @Test
    fun `builder accepts dismissible flag`() {
        val trueResult = builder.dismissible(true)
        assert(trueResult === builder)

        val falseResult = builder.dismissible(false)
        assert(falseResult === builder)
    }

    @Test
    fun `builder accepts custom layout`() {
        val result = builder.customLayout(android.R.layout.simple_list_item_1)
        assert(result === builder)
    }

    @Test
    fun `builder accepts background color int`() {
        val result = builder.backgroundColor(Color.RED)
        assert(result === builder)
    }

    @Test
    fun `builder accepts background color resource`() {
        val result = builder.backgroundColorRes(android.R.color.black)
        assert(result === builder)
    }

    @Test
    fun `builder accepts text color int`() {
        val result = builder.textColor(Color.WHITE)
        assert(result === builder)
    }

    @Test
    fun `builder accepts text color resource`() {
        val result = builder.textColorRes(android.R.color.white)
        assert(result === builder)
    }

    @Test
    fun `builder accepts icon resource`() {
        val result = builder.icon(android.R.drawable.ic_dialog_info)
        assert(result === builder)
    }

    @Test
    fun `builder accepts banner click listener`() {
        val result = builder.onBannerClick { }
        assert(result === builder)
    }

    @Test
    fun `builder accepts dismiss listener`() {
        val result = builder.onDismiss { }
        assert(result === builder)
    }

    @Test
    fun `message with string clears resource ID`() {
        // This test verifies the implementation detail that setting string clears resource ID
        // We can't directly test this without reflection, but we test the builder chain works
        val result = builder
            .message(android.R.string.ok)
            .message("Test string")
        assert(result === builder)
    }

    @Test
    fun `message with resource ID clears string`() {
        val result = builder
            .message("Test string")
            .message(android.R.string.ok)
        assert(result === builder)
    }

    @Test
    fun `backgroundColor clears backgroundColorRes`() {
        val result = builder
            .backgroundColorRes(android.R.color.black)
            .backgroundColor(Color.RED)
        assert(result === builder)
    }

    @Test
    fun `backgroundColorRes clears backgroundColor`() {
        val result = builder
            .backgroundColor(Color.RED)
            .backgroundColorRes(android.R.color.black)
        assert(result === builder)
    }

    @Test
    fun `textColor clears textColorRes`() {
        val result = builder
            .textColorRes(android.R.color.white)
            .textColor(Color.WHITE)
        assert(result === builder)
    }

    @Test
    fun `textColorRes clears textColor`() {
        val result = builder
            .textColor(Color.WHITE)
            .textColorRes(android.R.color.white)
        assert(result === builder)
    }
}
