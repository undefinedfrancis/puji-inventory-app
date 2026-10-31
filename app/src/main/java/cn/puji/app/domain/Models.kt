package cn.puji.app.domain

data class Product(
    val id: Long = 0,
    val name: String,
    val barcode: String = "",
    val category: String = "未分类",
    val costPriceCents: Long = 0,
    val salePriceCents: Long = 0,
    val stockQuantity: Int = 0,
    val lowStockThreshold: Int = 5,
    val note: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val uuid: String = "",
    val stockCostCents: Long = 0,
    val version: Long = 1,
    val source: String = "local",
    val costSource: String = "actual",
) {
    val isLowStock: Boolean get() = active && stockQuantity <= lowStockThreshold
}

enum class DocumentType { INBOUND, OUTBOUND, OPENING, SALES_RETURN, PURCHASE_RETURN, STOCK_GAIN, STOCK_LOSS, WRITE_OFF, REVERSAL }

data class StockDocument(
    val id: Long,
    val type: DocumentType,
    val totalCents: Long,
    val note: String,
    val createdAt: Long,
    val lines: List<DocumentLine> = emptyList(),
    val uuid: String = "",
    val requestId: String = "",
    val originalDocumentId: Long? = null,
    val originalType: DocumentType? = null,
    val valuationDifferenceCents: Long = 0,
    val costSource: String = "actual",
    val expectedQuantity: Int? = null,
    val actualQuantity: Int? = null,
)

data class DocumentLine(
    val id: Long = 0,
    val documentId: Long = 0,
    val productId: Long,
    val productName: String,
    val barcode: String,
    val unitPriceCents: Long,
    val costPriceSnapshotCents: Long = 0,
    val quantity: Int,
    val lineCostCents: Long = Math.multiplyExact(costPriceSnapshotCents, quantity.toLong()),
    val stockCostDeltaCents: Long = 0,
    val quantityDelta: Int = 0,
    val originalLineId: Long? = null,
    val valuationDifferenceCents: Long = 0,
    val costSource: String = "actual",
    val uuid: String = "",
    val returnedQuantity: Int = 0,
    val returnedCostCents: Long = 0,
) {
    val lineTotalCents: Long get() = Math.multiplyExact(unitPriceCents, quantity.toLong())
    val costTotalCents: Long get() = lineCostCents
    val grossProfitCents: Long get() = Math.subtractExact(lineTotalCents, costTotalCents)
}

data class StockHistoryEntry(
    val id: Long,
    val documentId: Long,
    val type: DocumentType,
    val quantityDelta: Int,
    val stockAfter: Int,
    val stockCostDeltaCents: Long,
    val stockCostAfterCents: Long,
    val createdAt: Long,
    val note: String,
)

data class DashboardStats(
    val todaySalesCents: Long = 0,
    val todayItemCount: Int = 0,
    val productCount: Int = 0,
    val lowStockCount: Int = 0,
    val inventoryValueCents: Long = 0,
    val todayGrossProfitCents: Long = 0,
)
