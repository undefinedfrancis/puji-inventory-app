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
) {
    val isLowStock: Boolean get() = active && stockQuantity <= lowStockThreshold
}

enum class DocumentType { INBOUND, OUTBOUND }

data class StockDocument(
    val id: Long,
    val type: DocumentType,
    val totalCents: Long,
    val note: String,
    val createdAt: Long,
    val lines: List<DocumentLine> = emptyList(),
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
) {
    val lineTotalCents: Long get() = unitPriceCents * quantity
    val costTotalCents: Long get() = costPriceSnapshotCents * quantity
    val grossProfitCents: Long get() = lineTotalCents - costTotalCents
}

data class DashboardStats(
    val todaySalesCents: Long = 0,
    val todayItemCount: Int = 0,
    val productCount: Int = 0,
    val lowStockCount: Int = 0,
    val inventoryValueCents: Long = 0,
)
