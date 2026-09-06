package cn.puji.app.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import cn.puji.app.BuildConfig
import cn.puji.app.data.AppResetRepository
import cn.puji.app.data.BusinessSummaryRepository
import cn.puji.app.data.DashboardRepository
import cn.puji.app.data.InventoryRepository
import cn.puji.app.data.ProductRepository
import cn.puji.app.domain.Cart
import cn.puji.app.domain.DateRange
import cn.puji.app.domain.DocumentType
import cn.puji.app.domain.PeriodCalculator
import cn.puji.app.domain.PeriodKind
import cn.puji.app.domain.Product
import cn.puji.app.domain.ProductCategories
import cn.puji.app.domain.ProductCategoryCount
import cn.puji.app.export.CsvExporter
import cn.puji.app.scanner.ScanTransactionGate
import cn.puji.app.ui.home.HomeScreen
import cn.puji.app.ui.home.ResetDataDialog
import cn.puji.app.ui.inventory.InventoryScreen
import cn.puji.app.ui.product.ProductEditor
import cn.puji.app.ui.records.CustomExportDialog
import cn.puji.app.ui.records.LedgerScreen
import cn.puji.app.ui.records.RecordsScreen
import cn.puji.app.ui.scanner.ScannerScreen
import cn.puji.app.ui.theme.ScannerOrange
import cn.puji.app.ui.theme.TicketGreenSoft
import cn.puji.app.ui.theme.WarningSoft
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class AppTab(val label: String, val symbol: String) {
    HOME("首页", "⌂"), INVENTORY("货架", "▦"), SCANNER("扫码", "▣"), RECORDS("记录", "≡")
}

@Composable
fun PujiApp(
    productRepository: ProductRepository,
    inventoryRepository: InventoryRepository,
    dashboardRepository: DashboardRepository,
    businessSummaryRepository: BusinessSummaryRepository,
    appResetRepository: AppResetRepository,
) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now() }
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }
    var allProducts by remember { mutableStateOf(productRepository.list()) }
    var products by remember { mutableStateOf(allProducts) }
    var categoryCounts by remember { mutableStateOf(ProductCategories.counts(allProducts)) }
    var stats by remember { mutableStateOf(dashboardRepository.stats()) }

    var periodKind by remember { mutableStateOf(PeriodKind.MONTH) }
    var periodAnchor by remember { mutableStateOf(today) }
    var summaryRange by remember { mutableStateOf(PeriodCalculator.range(periodKind, periodAnchor, zone)) }
    var summary by remember { mutableStateOf(businessSummaryRepository.summary(summaryRange, periodKind, zone)) }
    var ledgerAnchor by remember { mutableStateOf(today) }
    var ledgerRange by remember { mutableStateOf(PeriodCalculator.range(PeriodKind.MONTH, ledgerAnchor, zone)) }
    var recordFilter by remember { mutableStateOf<DocumentType?>(null) }
    var documents by remember { mutableStateOf(inventoryRepository.documents(recordFilter, ledgerRange)) }
    var showingLedger by remember { mutableStateOf(false) }

    var mode by remember { mutableStateOf(DocumentType.OUTBOUND) }
    var cart by remember { mutableStateOf(Cart.empty(mode)) }
    var manualBarcode by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Product?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var seedBarcode by remember { mutableStateOf("") }
    var addAfterSave by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var resetWorking by remember { mutableStateOf(false) }
    var showCustomExport by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scanGate = remember { ScanTransactionGate() }
    var scannerLocked by remember { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }

    fun refreshProducts() {
        val fresh = productRepository.list()
        val counts = ProductCategories.counts(fresh)
        val selected = selectedCategory.takeIf { choice -> counts.any { it.name == choice } } ?: "全部"
        allProducts = fresh
        categoryCounts = counts
        selectedCategory = selected
        products = productRepository.list(query = query, category = selected)
    }

    fun refreshReporting() {
        summary = businessSummaryRepository.summary(summaryRange, periodKind, zone)
        documents = inventoryRepository.documents(recordFilter, ledgerRange)
    }

    fun reload() {
        refreshProducts()
        refreshReporting()
        stats = dashboardRepository.stats()
    }

    fun updateSummaryPeriod(kind: PeriodKind, anchor: LocalDate) {
        periodKind = kind
        periodAnchor = anchor
        summaryRange = PeriodCalculator.range(kind, anchor, zone)
        summary = businessSummaryRepository.summary(summaryRange, kind, zone)
    }

    fun updateLedgerMonth(anchor: LocalDate) {
        ledgerAnchor = anchor
        ledgerRange = PeriodCalculator.range(PeriodKind.MONTH, anchor, zone)
        documents = inventoryRepository.documents(recordFilter, ledgerRange)
    }

    fun unlockScannerAfterCancellation() {
        scanGate.registrationCancelled()
        scannerLocked = false
    }

    fun openScanner(target: DocumentType) {
        if (!scanGate.canStartNewTransaction()) {
            selectedTab = AppTab.SCANNER
            message = "请先确认或取消当前商品，再开始下一笔交易"
            return
        }
        mode = target
        cart = Cart.empty(target)
        selectedTab = AppTab.SCANNER
    }

    fun lookupBarcode(raw: String) {
        val barcode = scanGate.accept(raw) ?: return
        scannerLocked = true
        manualBarcode = ""
        val found = productRepository.findByBarcode(barcode)
        if (found == null) {
            editing = null
            seedBarcode = barcode
            addAfterSave = true
            showEditor = true
            message = "未找到条码，请先为它建立商品"
        } else {
            cart = cart.add(found)
        }
    }

    fun exportDocuments(range: DateRange, type: DocumentType?) {
        val exportDocuments = inventoryRepository.documents(type, range)
        if (exportDocuments.isEmpty()) {
            message = "所选时间没有流水"
            return
        }
        val start = range.startInclusive.toLocalDate(zone)
        val end = range.endExclusive.toLocalDate(zone).minusDays(1)
        val typeLabel = when (type) {
            DocumentType.OUTBOUND -> "出货"
            DocumentType.INBOUND -> "入货"
            null -> "全部"
        }
        val fileName = "铺记-流水-${start.format(DateTimeFormatter.ISO_LOCAL_DATE)}至${end.format(DateTimeFormatter.ISO_LOCAL_DATE)}-$typeLabel.csv"
        shareCsv(context, fileName, "导出流水 CSV", CsvExporter.documents(exportDocuments)).fold(
            onSuccess = { message = "流水 CSV 已打开分享面板" },
            onFailure = { message = "无法打开分享面板，请检查可用应用" },
        )
    }

    LaunchedEffect(message) {
        message?.let { snackbarHost.showSnackbar(it); message = null }
    }

    BackHandler(enabled = selectedTab == AppTab.RECORDS && showingLedger) { showingLedger = false }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            if (tab == AppTab.RECORDS) {
                                showingLedger = false
                                reload()
                            }
                        },
                        icon = {
                            Text(
                                tab.symbol,
                                fontSize = if (tab == AppTab.SCANNER) 25.sp else 20.sp,
                                color = if (tab == AppTab.SCANNER) ScannerOrange else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Black,
                            )
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = if (tab == AppTab.SCANNER) WarningSoft else TicketGreenSoft,
                            selectedTextColor = if (tab == AppTab.SCANNER) ScannerOrange else MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                AppTab.HOME -> HomeScreen(
                    stats = stats,
                    lowStock = allProducts.filter(Product::isLowStock),
                    onInbound = { openScanner(DocumentType.INBOUND) },
                    onOutbound = { openScanner(DocumentType.OUTBOUND) },
                    onInventory = { selectedTab = AppTab.INVENTORY },
                    onAbout = { showAbout = true },
                    onReset = { showReset = true },
                )
                AppTab.INVENTORY -> InventoryScreen(
                    products = products,
                    query = query,
                    categories = categoryCounts,
                    selectedCategory = selectedCategory,
                    onQueryChange = {
                        query = it
                        products = productRepository.list(query = it, category = selectedCategory)
                    },
                    onCategoryChange = {
                        selectedCategory = it
                        products = productRepository.list(query = query, category = it)
                    },
                    onAdd = { editing = null; seedBarcode = ""; addAfterSave = false; showEditor = true },
                    onEdit = { editing = it; seedBarcode = ""; addAfterSave = false; showEditor = true },
                    onExport = {
                        shareCsv(
                            context,
                            "铺记-库存.csv",
                            "导出库存 CSV",
                            CsvExporter.products(productRepository.list(includeInactive = true)),
                        ).fold(
                            onSuccess = { message = "库存 CSV 已打开分享面板" },
                            onFailure = { message = "无法打开分享面板，请检查可用应用" },
                        )
                    },
                )
                AppTab.SCANNER -> ScannerScreen(
                    mode = mode,
                    scannerLocked = scannerLocked,
                    onModeChange = {
                        if (!scannerLocked && it != mode) {
                            mode = it
                            cart = Cart.empty(it)
                        }
                    },
                    cart = cart,
                    manualBarcode = manualBarcode,
                    onManualBarcodeChange = { manualBarcode = it },
                    onLookup = ::lookupBarcode,
                    onIncrement = { id -> cart = cart.increment(id) },
                    onDecrement = { id ->
                        cart = cart.decrement(id)
                        if (cart.lines.isEmpty()) unlockScannerAfterCancellation()
                    },
                    onPriceChange = { id, cents -> cart = cart.updateUnitPrice(id, cents) },
                    onConfirm = {
                        inventoryRepository.commit(cart).fold(
                            onSuccess = {
                                message = if (mode == DocumentType.OUTBOUND) "出货完成，库存已扣减" else "入货完成，库存已增加"
                                cart = Cart.empty(mode)
                                scanGate.transactionSucceeded()
                                scannerLocked = false
                                reload()
                            },
                            onFailure = { message = it.message ?: "操作失败，请重试" },
                        )
                    },
                )
                AppTab.RECORDS -> if (showingLedger) {
                    LedgerScreen(
                        documents = documents,
                        range = ledgerRange,
                        filter = recordFilter,
                        canGoNext = !PeriodCalculator.isCurrent(PeriodKind.MONTH, ledgerAnchor),
                        onBack = { showingLedger = false },
                        onPrevious = { updateLedgerMonth(PeriodCalculator.previous(PeriodKind.MONTH, ledgerAnchor)) },
                        onNext = { updateLedgerMonth(PeriodCalculator.next(PeriodKind.MONTH, ledgerAnchor)) },
                        onFilterChange = {
                            recordFilter = it
                            documents = inventoryRepository.documents(it, ledgerRange)
                        },
                        onExportMonth = { exportDocuments(ledgerRange, recordFilter) },
                        onCustomExport = { showCustomExport = true },
                    )
                } else {
                    RecordsScreen(
                        summary = summary,
                        periodKind = periodKind,
                        range = summaryRange,
                        canGoNext = !PeriodCalculator.isCurrent(periodKind, periodAnchor),
                        onPeriodKindChange = { updateSummaryPeriod(it, LocalDate.now()) },
                        onPrevious = { updateSummaryPeriod(periodKind, PeriodCalculator.previous(periodKind, periodAnchor)) },
                        onNext = { updateSummaryPeriod(periodKind, PeriodCalculator.next(periodKind, periodAnchor)) },
                        onOpenLedger = {
                            showingLedger = true
                            documents = inventoryRepository.documents(recordFilter, ledgerRange)
                        },
                    )
                }
            }
        }
    }

    if (showEditor) {
        ProductEditor(
            product = editing,
            seedBarcode = seedBarcode,
            onDismiss = {
                showEditor = false
                if (addAfterSave) unlockScannerAfterCancellation()
                addAfterSave = false
            },
            onSave = { product ->
                productRepository.save(product).fold(
                    onSuccess = { id ->
                        val saved = productRepository.find(id)
                        if (addAfterSave && saved != null) cart = cart.add(saved)
                        showEditor = false
                        addAfterSave = false
                        message = "商品已保存"
                        reload()
                    },
                    onFailure = { message = it.message ?: "保存失败" },
                )
            },
            onDeactivate = editing?.let { current ->
                { product ->
                    productRepository.deactivate(product.id)
                    showEditor = false
                    message = "${current.name} 已停用"
                    reload()
                }
            },
        )
    }

    if (showReset) {
        ResetDataDialog(
            working = resetWorking,
            onDismiss = { showReset = false },
            onConfirm = {
                resetWorking = true
                appResetRepository.clearAll().fold(
                    onSuccess = {
                        query = ""
                        selectedCategory = "全部"
                        allProducts = emptyList()
                        products = emptyList()
                        categoryCounts = listOf(ProductCategoryCount("全部", 0))
                        stats = dashboardRepository.stats()
                        cart = Cart.empty(mode)
                        scanGate.transactionSucceeded()
                        scannerLocked = false
                        recordFilter = null
                        showingLedger = false
                        updateSummaryPeriod(PeriodKind.MONTH, LocalDate.now())
                        updateLedgerMonth(LocalDate.now())
                        selectedTab = AppTab.HOME
                        resetWorking = false
                        showReset = false
                        message = "数据已重置"
                    },
                    onFailure = {
                        resetWorking = false
                        message = "重置失败，原数据已保留"
                    },
                )
            },
        )
    }

    if (showCustomExport) {
        CustomExportDialog(
            initialStart = ledgerRange.startInclusive.toLocalDate(zone),
            initialEnd = ledgerRange.endExclusive.toLocalDate(zone).minusDays(1),
            initialType = recordFilter,
            onDismiss = { showCustomExport = false },
            onExport = { start, end, type ->
                PeriodCalculator.custom(start, end, zone).fold(
                    onSuccess = {
                        showCustomExport = false
                        exportDocuments(it, type)
                    },
                    onFailure = { message = it.message ?: "日期范围无效" },
                )
            },
        )
    }

    if (showAbout) AboutDialog(onDismiss = { showAbout = false }, onOpenSource = {
        context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/opensourcepos/opensourcepos".toUri()))
    })
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit, onOpenSource: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("铺记 · v${BuildConfig.VERSION_NAME}") },
        text = {
            Column {
                Text("为小卖部设计的离线进出货工具。数据只保存在本机。")
                Text("\n业务模型参考 Open Source POS；本应用为原生 Android 重构。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onOpenSource) { Text("查看 Open Source POS 与许可证") }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("知道了") } },
    )
}

private fun shareCsv(context: Context, fileName: String, chooserTitle: String, csv: String): Result<Unit> = runCatching {
    val directory = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(directory, fileName).apply { writeText(csv, Charsets.UTF_8) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

private fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
