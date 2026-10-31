package cn.puji.app.scanner

import org.junit.Assert.*
import org.junit.Test

class ContinuousScannerTest {
    @Test fun stationaryBarcodeNeverRepeatsEvenAfterCooldown() {
        val emitted = mutableListOf<String>()
        val scanner = ScannerController(onAccepted = emitted::add)
        scanner.accept("A", 1000)
        scanner.accept("A", 5000)
        scanner.accept("A", 20000)
        assertEquals(listOf("A"), emitted)
    }

    @Test fun barcodeCanReturnAfterAnObservedClearFrame() {
        val emitted = mutableListOf<String>()
        val scanner = ScannerController(cooldownMillis = 300, onAccepted = emitted::add)
        scanner.accept("A", 1000)
        scanner.accept("", 1100)
        scanner.accept("A", 1200)
        assertEquals(listOf("A"), emitted)
        scanner.accept("", 1300)
        scanner.accept("", 1700)
        scanner.accept("A", 1800)
        assertEquals(listOf("A", "A"), emitted)
    }

    @Test fun differentProductsCanBeScannedImmediately() {
        val emitted = mutableListOf<String>()
        val scanner = ScannerController(onAccepted = emitted::add)
        scanner.accept("A", 1000)
        scanner.accept("B", 1010)
        scanner.accept("A", 1020)
        assertEquals(listOf("A", "B", "A"), emitted)
    }
}
