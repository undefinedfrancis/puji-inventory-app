package cn.puji.app.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import cn.puji.app.PujiApplication
import cn.puji.app.backup.BackupInfo
import cn.puji.app.data.*
import cn.puji.app.domain.*
import cn.puji.app.export.CsvExporter
import cn.puji.app.importer.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class LocalBackup(val key: String, val title: String, val modifiedAt: Long)

data class ShopState(
    val ready: Boolean = false, val busy: Boolean = false, val message: String? = null,
    val products: List<Product> = emptyList(), val stats: DashboardStats = DashboardStats(),
    val summary: BusinessSummary = BusinessSummary(), val documents: List<StockDocument> = emptyList(),
    val recentDocuments: List<StockDocument> = emptyList(),
    val cart: Cart = Cart.empty(DocumentType.OUTBOUND), val requestId: String = UUID.randomUUID().toString(),
    val periodKind: PeriodKind = PeriodKind.MONTH, val periodAnchor: LocalDate = LocalDate.now(),
    val ledgerAnchor: LocalDate = LocalDate.now(), val ledgerFilter: DocumentType? = null,
    val ledgerQuery: String = "", val ledgerLimit: Int = 50,
    val ledgerOverride: DateRange? = null,
    val importPreview: CsvImportPreview? = null, val restoreInfo: BackupInfo? = null,
    val feedback: Boolean = true, val lastBackup: Long = 0,
    val localBackups: List<LocalBackup>? = null,
) {
    val summaryRange: DateRange get() = PeriodCalculator.range(periodKind, periodAnchor, ZoneId.systemDefault())
    val ledgerRange: DateRange get() = ledgerOverride ?: PeriodCalculator.range(PeriodKind.MONTH, ledgerAnchor, ZoneId.systemDefault())
}

class PujiViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PujiApplication
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val preferences = app.getSharedPreferences("puji_settings", Context.MODE_PRIVATE)
    private val drafts = DraftRepository(app)
    private val backups = BackupRepository(app.database, preferences)
    private val mutable = MutableStateFlow(ShopState(feedback = preferences.getBoolean("scanner_feedback", true), lastBackup = preferences.getLong("last_backup", 0)))
    val state = mutable.asStateFlow()
    private var pendingRestore: String? = null
    private var generation = 0L
    private var refreshPending = false
    private var automaticAttempted = false

    init {
        operation {
            val draft = runCatching { drafts.load() }.getOrElse {
                val message = "未能恢复清单，请核对流水"
                clearDraftSafely(message); notify(message); null
            }
            if (draft != null && app.inventory.documentForRequest(draft.requestId) != null) {
                clearDraftSafely("已入账，清单清理未完成")
            } else if (draft != null) {
                val valid = draft.cart.lines.all { line -> line.product.id <= 0 ||
                    app.products.find(line.product.id)?.uuid == line.product.uuid }
                if(!valid) {
                    val message = "商品标识已变化，旧清单未恢复"
                    clearDraftSafely(message); notify(message); return@operation
                }
                val lines = draft.cart.lines.map { line ->
                    val current = if (line.product.id > 0) app.products.find(line.product.id) else line.product
                    if (current == null) line else line.copy(product = line.product.copy(stockQuantity = current.stockQuantity, active = current.active))
                }
                mutable.update { it.copy(cart = draft.cart.copy(lines = lines), requestId = draft.requestId,
                    message = "已恢复未提交清单，请核对价格与库存") }
            }
        }
    }

    override fun onCleared() { scope.cancel() }
    fun clearMessage() { mutable.update { it.copy(message = null) } }
    fun notify(message: String) { mutable.update { it.copy(message = message) } }

    private fun clearDraftSafely(messageOnFailure: String) {
        try { drafts.clear() }
        catch (error: Exception) {
            if (error is CancellationException) throw error
            notify(messageOnFailure)
        }
    }

    private suspend fun loadSnapshot() {
        val request = ++generation
        val s = mutable.value
        val values = withContext(Dispatchers.IO) {
            val products = app.products.list(includeInactive = true)
            val stats = app.dashboard.stats()
            val summary = app.businessSummary.summary(s.summaryRange, s.periodKind, ZoneId.systemDefault())
            val documents = app.inventory.documents(s.ledgerFilter, s.ledgerRange, s.ledgerQuery, s.ledgerLimit)
            val recent = app.inventory.documents(limit = 4)
            Snapshot(products, stats, summary, documents, recent)
        }
        if (request == generation) mutable.update { it.copy(ready = true, products = values.products, stats = values.stats,
            summary = values.summary, documents = values.documents, recentDocuments = values.recent) }
    }

    private data class Snapshot(val products: List<Product>, val stats: DashboardStats, val summary: BusinessSummary,
        val documents: List<StockDocument>, val recent: List<StockDocument>)

    fun refresh() {
        if (mutable.value.busy) { refreshPending = true; return }
        scope.launch { runCatching { loadSnapshot() }.onFailure { notify(it.message ?: "读取失败，请重试") } }
    }

    fun operation(action: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true) }
        scope.launch {
            try {
                withContext(Dispatchers.IO) { action() }
                loadSnapshot()
                if(!automaticAttempted) {
                    automaticAttempted = true
                    withContext(Dispatchers.IO) { runCatching { automaticBackup() }.onFailure { notify("本机自动副本未保存，请另存完整备份") } }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                notify(error.message ?: "操作失败，请重试")
            } finally {
                mutable.update { it.copy(busy = false) }
                if(refreshPending) { refreshPending = false; refresh() }
            }
        }
    }

    fun changeCart(transform: (Cart) -> Cart): Boolean {
        if (mutable.value.busy) return false
        return runCatching {
            val cart = transform(mutable.value.cart)
            require(cart.lines.size <= 500) { "一单最多 500 种商品" }
            cart.totalCents; cart.totalQuantity
            val requestId = UUID.randomUUID().toString()
            drafts.save(cart, requestId)
            mutable.update { it.copy(cart = cart, requestId = requestId) }
        }.onFailure { notify(it.message ?: "数量或金额超出范围") }.isSuccess
    }

    fun add(product: Product, quantity: Int = 1) = changeCart { cart -> cart.add(product, quantity) }
    fun changeMode(type: DocumentType) = changeCart { Cart.empty(type) }
    fun commit() {
        val s = mutable.value
        operation {
            // The disk snapshot must identify this transaction even if the process exits
            // after SQLite commits and before the old draft can be removed.
            drafts.save(s.cart, s.requestId, durable = true)
            app.inventory.commit(s.cart, s.requestId).getOrThrow()
            mutable.update { it.copy(cart = Cart.empty(s.cart.type), requestId = UUID.randomUUID().toString(), message = "已完成${if(s.cart.type == DocumentType.INBOUND) "入货" else "出货"}") }
            clearDraftSafely("已入账，清单清理未完成")
        }
    }

    fun saveProduct(product: Product, onSaved: () -> Unit = {}) = operation {
        app.products.save(product).getOrThrow(); notify("商品已保存"); withContext(Dispatchers.Main) { onSaved() }
    }
    fun setActive(product: Product, active: Boolean) = operation {
        app.products.setActive(product.id, active).getOrThrow(); notify(if(active) "商品已恢复" else "商品已停用")
    }
    fun count(product: Product, actual: Int, cost: Long, note: String, onSaved: () -> Unit = {}) = operation {
        app.inventory.count(product.id, actual, product.stockQuantity, cost, note).getOrThrow(); notify("盘点已记录"); withContext(Dispatchers.Main) { onSaved() }
    }
    fun writeOff(product: Product, quantity: Int, note: String, onSaved: () -> Unit = {}) = operation {
        app.inventory.writeOff(product.id, quantity, note).getOrThrow(); notify("报损已记录"); withContext(Dispatchers.Main) { onSaved() }
    }
    fun returnLines(document: StockDocument, quantities: Map<Long, Int>, onSaved: () -> Unit = {}) {
        val request = UUID.randomUUID().toString()
        operation { app.inventory.returnLines(document.id, quantities, request).getOrThrow(); notify("退货已记录"); withContext(Dispatchers.Main) { onSaved() } }
    }
    fun reverse(document: StockDocument, onSaved: () -> Unit = {}) {
        val request = UUID.randomUUID().toString()
        operation { app.inventory.reverse(document.id, request).getOrThrow(); notify("已生成冲正记录"); withContext(Dispatchers.Main) { onSaved() } }
    }
    suspend fun history(productId: Long): List<StockHistoryEntry> = try {
        withContext(Dispatchers.IO) { app.inventory.stockHistory(productId) }
    } catch(error: Exception) { if(error is CancellationException) throw error; notify(error.message ?: "无法读取库存变动"); emptyList() }
    suspend fun document(id: Long): StockDocument? = try {
        withContext(Dispatchers.IO) { app.inventory.findDocument(id) }
    } catch(error: Exception) { if(error is CancellationException) throw error; notify(error.message ?: "无法读取单据"); null }

    fun period(kind: PeriodKind = mutable.value.periodKind, anchor: LocalDate = mutable.value.periodAnchor) {
        mutable.update { it.copy(periodKind = kind, periodAnchor = anchor) }; refresh()
    }
    fun ledger(anchor: LocalDate = mutable.value.ledgerAnchor, filter: DocumentType? = mutable.value.ledgerFilter,
        query: String = mutable.value.ledgerQuery, more: Boolean = false,
        range: DateRange? = if(anchor == mutable.value.ledgerAnchor) mutable.value.ledgerOverride else null) {
        mutable.update { it.copy(ledgerAnchor = anchor, ledgerFilter = filter, ledgerQuery = query, ledgerOverride = range,
            ledgerLimit = if(more) it.ledgerLimit + 50 else 50) }; refresh()
    }

    fun feedback(enabled: Boolean) {
        preferences.edit().putBoolean("scanner_feedback", enabled).apply()
        mutable.update { it.copy(feedback = enabled) }
    }

    fun prepareLedgerExport(range: DateRange?, filter: DocumentType?, query: String, onReady: () -> Unit) = operation {
        require(app.inventory.documents(filter, range, query, limit = 1).isNotEmpty()) { "所选范围没有流水" }
        withContext(Dispatchers.Main) { onReady() }
    }

    fun saveFile(uri: Uri, kind: String, range: DateRange? = null, filter: DocumentType? = null, query: String = "") = operation {
        val content = when(kind) {
            "backup" -> backups.exportJson()
            "template" -> ProductCsvImporter.template()
            "inventory" -> CsvExporter.products(app.products.list(includeInactive = true))
            else -> {
                val documents = app.inventory.documents(filter, range, query)
                require(documents.isNotEmpty()) { "所选范围没有流水" }
                CsvExporter.documents(documents)
            }
        }
        val stream = app.contentResolver.openOutputStream(uri, "wt") ?: error("无法保存文件")
        stream.use { it.write(content.toByteArray(Charsets.UTF_8)); it.flush() }
        if(kind == "backup") {
            val time = System.currentTimeMillis()
            preferences.edit().putLong("last_backup", time).apply()
            mutable.update { it.copy(lastBackup = time) }
        }
        notify("文件已保存")
    }

    fun readFile(uri: Uri, backup: Boolean) = operation {
        val limit = if(backup) 32 * 1024 * 1024 else 5 * 1024 * 1024
        val output = ByteArrayOutputStream()
        (app.contentResolver.openInputStream(uri) ?: error("无法读取文件")).use { input ->
            val buffer = ByteArray(8192)
            while (true) { val read = input.read(buffer); if(read < 0) break
                require(output.size().toLong() + read <= limit) { "文件过大" }; output.write(buffer, 0, read) }
        }
        val content = output.toString("UTF-8")
        if(backup) {
            val info = backups.inspect(content)
            pendingRestore = content
            mutable.update { it.copy(restoreInfo = info) }
        } else mutable.update { it.copy(importPreview = ProductCsvImporter.preview(content)) }
    }
    fun dismissFilePreview() { pendingRestore = null; mutable.update { it.copy(restoreInfo = null, importPreview = null) } }
    private fun localBackupFiles(): List<File> = (File(app.filesDir, "backups").listFiles()?.filter { it.name.endsWith(".puji.json") }.orEmpty() +
        listOf(File(app.filesDir, "before-restore.puji.json"))).filter { it.isFile }.sortedByDescending { it.lastModified() }
    private fun automaticBackup() {
        if(mutable.value.products.isEmpty()) return
        val directory = File(app.filesDir, "backups").apply { check(isDirectory || mkdirs()) }
        val file = File(directory, "${LocalDate.now()}.puji.json")
        if(!file.exists()) {
            writeLocalBackup(file, backups.exportJson())
        }
        directory.listFiles()?.filter { it.name.endsWith(".puji.json") }?.sortedByDescending { it.name }?.drop(3)?.forEach { it.delete() }
    }
    private fun writeLocalBackup(file: File, content: String) {
        val atomic = android.util.AtomicFile(file)
        val stream = atomic.startWrite()
        try { stream.write(content.toByteArray(Charsets.UTF_8)); atomic.finishWrite(stream) }
        catch(error: Exception) { atomic.failWrite(stream); throw error }
    }
    fun listLocalBackups() = operation {
        val entries = localBackupFiles().map { file -> LocalBackup(file.name,
            if(file.name == "before-restore.puji.json") "恢复前副本" else "自动副本 ${file.name.removeSuffix(".puji.json")}", file.lastModified()) }
        require(entries.isNotEmpty()) { "还没有可恢复的本机副本" }
        mutable.update { it.copy(localBackups = entries) }
    }
    fun dismissLocalBackups() { mutable.update { it.copy(localBackups = null) } }
    fun prepareRollback(key: String) = operation {
        val file = localBackupFiles().firstOrNull { it.name == key } ?: error("副本不存在")
        require(file.length() <= 32L * 1024 * 1024) { "副本过大" }
        val content = file.readText(Charsets.UTF_8)
        val info = backups.inspect(content)
        pendingRestore = content; mutable.update { it.copy(restoreInfo = info, localBackups = null) }
    }
    fun restore() = operation {
        val content = pendingRestore ?: error("请重新选择备份")
        val rollback = File(app.filesDir, "before-restore.puji.json")
        writeLocalBackup(rollback, backups.exportJson())
        drafts.beginReplacement()
        try { backups.restoreJson(content).getOrThrow() }
        catch(error: Exception) { drafts.cancelReplacement(); throw error }
        pendingRestore = null
        mutable.update { it.copy(cart = Cart.empty(DocumentType.OUTBOUND), requestId = UUID.randomUUID().toString(),
            ledgerFilter = null, ledgerQuery = "", ledgerOverride = null, ledgerAnchor = LocalDate.now(), periodAnchor = LocalDate.now(), restoreInfo = null, importPreview = null,
            feedback = preferences.getBoolean("scanner_feedback", true), message = "备份已恢复") }
        clearDraftSafely("备份已恢复，清单清理未完成")
    }
    fun importProducts() {
        val preview = mutable.value.importPreview ?: return
        operation { val result = ProductCsvImporter.importRows(app.products, preview).getOrThrow()
            mutable.update { it.copy(importPreview = null, message = "新增 ${result.created} 项，更新 ${result.updated} 项") } }
    }
    fun clearAll() = operation {
        drafts.beginReplacement()
        try {
            listOf("before-reset.puji.json", "before-restore.puji.json").forEach { name ->
                val file = File(app.filesDir, name)
                if(file.exists()) require(file.delete()) { "无法清除本机回退副本" }
            }
            val directory = File(app.filesDir, "backups")
            if(directory.exists()) require(directory.deleteRecursively()) { "无法清除本机自动副本" }
            app.appReset.clearAll().getOrThrow()
        }
        catch(error: Exception) { drafts.cancelReplacement(); throw error }
        mutable.update { it.copy(cart = Cart.empty(DocumentType.OUTBOUND), requestId = UUID.randomUUID().toString(),
            ledgerFilter = null, ledgerQuery = "", ledgerOverride = null, ledgerAnchor = LocalDate.now(), periodKind = PeriodKind.MONTH,
            periodAnchor = LocalDate.now(), message = "数据已清除") }
        clearDraftSafely("数据已清除，清单清理未完成")
    }
}
