package cn.puji.app.domain

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object Money {
    fun parseYuan(text: String): Result<Long> = runCatching {
        require(text.isNotBlank()) { "金额不能为空" }
        val value = text.trim().toBigDecimal().setScale(2, RoundingMode.UNNECESSARY)
        require(value.signum() >= 0) { "金额不能为负数" }
        value.movePointRight(2).longValueExact()
    }

    fun format(cents: Long): String = NumberFormat.getCurrencyInstance(Locale.CHINA).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(java.math.BigDecimal.valueOf(cents, 2)).replace("CN¥", "¥")
}

