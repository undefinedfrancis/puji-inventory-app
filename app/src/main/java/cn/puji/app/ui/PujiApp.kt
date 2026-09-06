package cn.puji.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import cn.puji.app.data.DashboardRepository
import cn.puji.app.data.InventoryRepository
import cn.puji.app.data.ProductRepository
import cn.puji.app.domain.Cart
import cn.puji.app.domain.DocumentType
import cn.puji.app.domain.Product
import cn.puji.app.export.CsvExporter
import cn.puji.app.scanner.ScanTransactionGate
import cn.puji.app.ui.home.HomeScreen
import cn.puji.app.ui.inventory.InventoryScreen
import cn.puji.app.ui.product.ProductEditor
import cn.puji.app.ui.records.RecordsScreen
import cn.puji.app.ui.scanner.ScannerScreen
import cn.puji.app.ui.theme.Paper
import cn.puji.app.ui.theme.ScannerOrange
import cn.puji.app.ui.theme.TicketGreenSoft
import cn.puji.app.ui.theme.WarningSoft
import java.io.File

private enum class AppTab(val label: String, val symbol: String) {
    HOME("首页", "⌂"), INVENTORY("库存", "▦"), SCANNER("扫码", "▣"), RECORDS("记录", "≋")
}

@Composable
fun PujiApp(
    productRepository: ProductRepository,
    inventoryRepository: InventoryRepository,
    dashboardRepository: DashboardRepository,
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var query by remember { mutableStateOf("") }
    var products by remember { mutableStateOf(productRepository.list()) }
    var documents by remember { mutableStateOf(inventoryRepository.documents()) }
    var recordFilter by remember { mutableStateOf<DocumentType?>(null) }
    var stats by remember { mutableStateOf(dashboardRepository.stats()) }
    var mode by remember { mutableStateOf(DocumentType.OUTBOUND) }
    var cart by remember { mutableStateOf(Cart.empty(mode)) }
    var manualBarcode by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Product?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var seedBarcode by remember { mutableStateOf("") }
    var addAfterSave by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scanGate = remember { ScanTransactionGate() }
    var scannerLocked by remember { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }

    fun unlockScannerAfterCancellation() {
        scanGate.registrationCancelled()
        scannerLocked = false
    }

    fun reload() {
        products = productRepository.list(query)
        documents = inventoryRepository.documents(recordFilter)
        stats = dashboardRepository.stats()
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

    LaunchedEffect(message) {
        message?.let { snackbarHost.showSnackbar(it); message = null }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab; if (tab == AppTab.RECORDS) reload() },
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
                    lowStock = products.filter(Product::isLowStock),
                    onInbound = { openScanner(DocumentType.INBOUND) },
                    onOutbound = { openScanner(DocumentType.OUTBOUND) },
                    onInventory = { selectedTab = AppTab.INVENTORY },
                    onAbout = { showAbout = true },
                )
                AppTab.INVENTORY -> InventoryScreen(
                    products = products,
                    query = query,
                    onQueryChange = { query = it; products = productRepository.list(it) },
                    onAdd = { editing = null; seedBarcode = ""; addAfterSave = false; showEditor = true },
                    onEdit = { editing = it; seedBarcode = ""; addAfterSave = false; showEditor = true },
                    onExport = {
                        shareCsv(context, "铺记-库存.csv", "导出库存 CSV", CsvExporter.products(productRepository.list(includeInactive = true)))
                        message = "库存 CSV 已打开分享面板"
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
                AppTab.RECORDS -> RecordsScreen(
                    documents = documents,
                    filter = recordFilter,
                    onFilterChange = { recordFilter = it; documents = inventoryRepository.documents(it) },
                    onExport = {
                        shareCsv(context, "铺记-流水.csv", "导出流水 CSV", CsvExporter.documents(inventoryRepository.documents()))
                        message = "流水 CSV 已打开分享面板"
                    },
                )
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

private fun shareCsv(context: Context, fileName: String, chooserTitle: String, csv: String) {
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
