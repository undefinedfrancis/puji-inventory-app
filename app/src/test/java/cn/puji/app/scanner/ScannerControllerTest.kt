package cn.puji.app.scanner

import org.junit.Assert.assertEquals
import org.junit.Test

class ScannerControllerTest {
    @Test fun repeatedBarcode_insideCooldown_isIgnored() {
        val emitted = mutableListOf<String>()
        val controller = ScannerController(onAccepted = emitted::add)
        controller.accept(" 6901 ", nowMillis = 1_000)
        controller.accept("6901", nowMillis = 1_500)
        controller.accept("6902", nowMillis = 1_600)
        controller.accept("6901", nowMillis = 2_300)
        assertEquals(listOf("6901", "6902", "6901"), emitted)
    }
}

