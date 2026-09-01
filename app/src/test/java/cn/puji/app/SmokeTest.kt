package cn.puji.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test
    fun applicationName_isPuji() {
        assertEquals("铺记", AppIdentity.displayName)
    }
}
