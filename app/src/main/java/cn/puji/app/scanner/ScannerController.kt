package cn.puji.app.scanner

class ScannerController(
    private val cooldownMillis: Long = 350,
    private val onAccepted: (String) -> Unit,
) {
    private var lastBarcode = ""
    private var clearSince: Long? = null

    fun accept(raw: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val barcode = raw.trim()
        if (barcode.isEmpty()) {
            if (clearSince == null) clearSince = nowMillis
            return false
        }
        val hasLeftFrame = clearSince?.let { nowMillis - it >= cooldownMillis } ?: false
        clearSince = null
        if (barcode == lastBarcode && !hasLeftFrame) return false
        lastBarcode = barcode
        onAccepted(barcode)
        return true
    }
}

