package cx.smile.smilenotificationbanner

import org.junit.Assert.assertEquals
import org.junit.Test

class BannerPositionTest {

    @Test
    fun `all banner positions exist`() {
        val positions = BannerPosition.values()
        assertEquals(2, positions.size)
        assert(positions.contains(BannerPosition.TOP))
        assert(positions.contains(BannerPosition.BOTTOM))
    }

    @Test
    fun `banner position names are correct`() {
        assertEquals("TOP", BannerPosition.TOP.name)
        assertEquals("BOTTOM", BannerPosition.BOTTOM.name)
    }
}
