package cn.puji.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HoldConfirmationGateTest {
    @Test fun completesOnlyAfterTwoSecondsAndOnlyOncePerPress() {
        val gate = HoldConfirmationGate(holdMillis = 2_000)

        gate.press(1_000)
        assertEquals(0.5f, gate.progress(2_000))
        assertFalse(gate.tryComplete(2_999))
        assertTrue(gate.tryComplete(3_000))
        assertFalse(gate.tryComplete(4_000))
    }

    @Test fun releasingEarlyCancelsTheHold() {
        val gate = HoldConfirmationGate(holdMillis = 2_000)

        gate.press(1_000)
        gate.release()

        assertEquals(0f, gate.progress(4_000))
        assertFalse(gate.tryComplete(4_000))
    }

    @Test fun aNewPressCanCompleteAfterRelease() {
        val gate = HoldConfirmationGate(holdMillis = 2_000)
        gate.press(0)
        assertTrue(gate.tryComplete(2_000))
        gate.release()

        gate.press(5_000)

        assertTrue(gate.tryComplete(7_000))
    }
}
