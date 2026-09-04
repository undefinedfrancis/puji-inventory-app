package cn.puji.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {
    @Test fun parseYuan_usesIntegerCents() {
        assertEquals(350L, Money.parseYuan("3.50").getOrThrow())
        assertEquals(300L, Money.parseYuan("3").getOrThrow())
        assertEquals(305L, Money.parseYuan(" 3.05 ").getOrThrow())
    }

    @Test fun parseYuan_rejectsInvalidValues() {
        assertTrue(Money.parseYuan("").isFailure)
        assertTrue(Money.parseYuan("-1").isFailure)
        assertTrue(Money.parseYuan("3.005").isFailure)
    }

    @Test fun format_showsChineseCurrency() {
        assertEquals("¥3.50", Money.format(350))
    }
}

