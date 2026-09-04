package cn.puji.app.scanner

class ScannerController(
    private val cooldownMillis: Long = 1_200,
    private val onAccepted: (String) -> Unit,
) {
    private var lastBarcode = ""
    private var lastAcceptedAt = Long.MIN_VALUE

    fun accept(raw: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val barcode = raw.trim()
        if (barcode.isEmpty()) return false
        if (barcode == lastBarcode && nowMillis - lastAcceptedAt < cooldownMillis) return false
        lastBarcode = barcode
        lastAcceptedAt = nowMillis
        onAccepted(barcode)
        return true
    }
}

