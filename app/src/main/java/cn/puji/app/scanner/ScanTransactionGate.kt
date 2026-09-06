package cn.puji.app.scanner

class ScanTransactionGate {
    var isLocked: Boolean = false
        private set

    fun accept(raw: String): String? {
        val barcode = raw.trim()
        if (barcode.isEmpty() || isLocked) return null
        isLocked = true
        return barcode
    }

    fun canStartNewTransaction(): Boolean = !isLocked

    fun transactionSucceeded() {
        isLocked = false
    }

    fun registrationCancelled() {
        isLocked = false
    }
}
