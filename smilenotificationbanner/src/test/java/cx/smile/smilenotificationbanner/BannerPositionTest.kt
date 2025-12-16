package cx.smile.smilenotificationbanner

import org.junit.Assert.assertEquals
import org.junit.Test

@Suppress("DEPRECATION")
class BannerPositionTest {

    @Test
    fun `banner position TOP exists`() {
        val positions = BannerPosition.values()
        assertEquals(1, positions.size)
        assert(positions.contains(BannerPosition.TOP))
    }

    @Test
    fun `banner position name is correct`() {
        assertEquals("TOP", BannerPosition.TOP.name)
    }
}
