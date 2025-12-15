package cx.smile.smilenotificationbanner

import org.junit.Assert.assertEquals
import org.junit.Test

class BannerTypeTest {

    @Test
    fun `all banner types exist`() {
        val types = BannerType.values()
        assertEquals(5, types.size)
        assert(types.contains(BannerType.SUCCESS))
        assert(types.contains(BannerType.INFO))
        assert(types.contains(BannerType.WARNING))
        assert(types.contains(BannerType.ERROR))
        assert(types.contains(BannerType.CUSTOM))
    }

    @Test
    fun `banner type names are correct`() {
        assertEquals("SUCCESS", BannerType.SUCCESS.name)
        assertEquals("INFO", BannerType.INFO.name)
        assertEquals("WARNING", BannerType.WARNING.name)
        assertEquals("ERROR", BannerType.ERROR.name)
        assertEquals("CUSTOM", BannerType.CUSTOM.name)
    }
}
