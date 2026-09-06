package cn.puji.app.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanTransactionGateTest {
    @Test fun acceptedBarcode_locksUntilTransactionSucceeds() {
        val gate = ScanTransactionGate()

        assertEquals("6901", gate.accept(" 6901 "))
        assertTrue(gate.isLocked)
        assertNull(gate.accept("6902"))

        gate.transactionSucceeded()

        assertFalse(gate.isLocked)
        assertEquals("6902", gate.accept("6902"))
    }

    @Test fun cancellingUnknownProduct_unlocksScanner() {
        val gate = ScanTransactionGate()
        gate.accept("6901")

        gate.registrationCancelled()

        assertFalse(gate.isLocked)
        assertEquals("6902", gate.accept("6902"))
    }

    @Test fun blankBarcode_doesNotLockScanner() {
        val gate = ScanTransactionGate()

        assertNull(gate.accept("  "))
        assertFalse(gate.isLocked)
    }

    @Test fun newTransactionCannotStartWhileScannedItemIsPending() {
        val gate = ScanTransactionGate()
        gate.accept("6901")

        assertFalse(gate.canStartNewTransaction())

        gate.transactionSucceeded()
        assertTrue(gate.canStartNewTransaction())
    }
}
