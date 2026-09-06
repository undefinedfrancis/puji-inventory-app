package cn.puji.app.domain

class HoldConfirmationGate(
    private val holdMillis: Long = 2_000,
) {
    private var pressedAt: Long? = null
    private var completed = false

    init {
        require(holdMillis > 0) { "长按时间必须大于 0" }
    }

    fun press(nowMillis: Long) {
        pressedAt = nowMillis
        completed = false
    }

    fun release() {
        pressedAt = null
        completed = false
    }

    fun progress(nowMillis: Long): Float {
        val start = pressedAt ?: return 0f
        return ((nowMillis - start).coerceAtLeast(0).toFloat() / holdMillis).coerceIn(0f, 1f)
    }

    fun tryComplete(nowMillis: Long): Boolean {
        if (pressedAt == null || completed || progress(nowMillis) < 1f) return false
        completed = true
        return true
    }
}
