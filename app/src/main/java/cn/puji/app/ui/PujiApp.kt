package cn.puji.app.ui

import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.LocalLifecycleOwner
import cn.puji.app.BuildConfig
import cn.puji.app.domain.*
import cn.puji.app.ui.components.*
import cn.puji.app.ui.home.HomeScreen
import cn.puji.app.ui.home.ResetDataDialog
import cn.puji.app.ui.inventory.InventoryScreen
import cn.puji.app.ui.manage.*
import cn.puji.app.ui.product.ProductEditor
import cn.puji.app.ui.records.*
import cn.puji.app.ui.scanner.ScannerScreen
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun PujiApp(model: PujiViewModel) {
    val state by model.state.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var ledger by rememberSaveable { mutableStateOf(false) }
    var productId by rememberSaveable { mutableStateOf<Long?>(null) }
    var documentId by rememberSaveable { mutableStateOf<Long?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("全部") }
    var inventoryFilter by rememberSaveable { mutableStateOf("全部") }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var scanRegistration by remember { mutableStateOf(false) }
    var seedBarcode by remember { mutableStateOf("") }
    var manualBarcode by rememberSaveable { mutableStateOf("") }
    var pickProduct by remember { mutableStateOf(false) }
    var nextMode by remember { mutableStateOf<DocumentType?>(null) }
    var productAfterModeChange by remember { mutableStateOf<Product?>(null) }
    var adjustment by remember { mutableStateOf<Product?>(null) }
    var writeOff by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var about by remember { mutableStateOf(false) }
    var customExport by remember { mutableStateOf(false) }
    var fileKind by rememberSaveable { mutableStateOf("inventory") }
    var exportStart by rememberSaveable { mutableStateOf<Long?>(null) }
    var exportEnd by rememberSaveable { mutableStateOf<Long?>(null) }
    var exportLabel by rememberSaveable { mutableStateOf("") }
    var exportFilter by rememberSaveable { mutableStateOf<DocumentType?>(null) }
    var exportQuery by rememberSaveable { mutableStateOf("") }
    var priceConfirmation by remember { mutableStateOf(false) }
    var zeroConfirmation by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current

    val csvSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            val start = exportStart; val end = exportEnd
            model.saveFile(it, fileKind, if(start != null && end != null) DateRange(start, end, exportLabel) else null, exportFilter, exportQuery)
        }
    }
    val backupSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { model.saveFile(it, "backup") }
    }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { model.readFile(it, true) } }
    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { model.readFile(it, false) } }

    fun exportCsv(kind: String, range: DateRange? = null, filter: DocumentType? = null) {
        fileKind = kind; exportStart = range?.startInclusive; exportEnd = range?.endExclusive
        exportLabel = range?.label.orEmpty(); exportFilter = filter
        exportQuery = if(kind == "ledger") state.ledgerQuery else ""
        val label = filter?.let(::documentTypeLabel) ?: "全部"
        val start = range?.let { Instant.ofEpochMilli(it.startInclusive).atZone(ZoneId.systemDefault()).toLocalDate() }
        val end = range?.let { Instant.ofEpochMilli(it.endExclusive).atZone(ZoneId.systemDefault()).toLocalDate().minusDays(1) }
        val name = when(kind) { "inventory" -> "铺记-库存.csv"; "template" -> "铺记-商品导入模板.csv"; else -> "铺记-流水-$start-至-$end-$label.csv" }
        if(kind == "ledger") model.prepareLedgerExport(range, filter, exportQuery) { csvSaver.launch(name) }
        else csvSaver.launch(name)
    }

    fun openScanner(mode: DocumentType, product: Product? = null) {
        if(model.state.value.busy) return
        if (state.cart.lines.isNotEmpty() && mode != state.cart.type) {
            productAfterModeChange = product; nextMode = mode
        } else {
            if(mode != state.cart.type) model.changeMode(mode)
            if(product != null) model.add(product)
        }
        productId = null; settings = false; documentId = null; tab = 2
    }

    fun lookupBarcode(raw: String) {
        if(state.busy || showEditor || pickProduct || nextMode != null || priceConfirmation || zeroConfirmation) return
        val barcode = raw.trim()
        if(barcode.isEmpty()) return
        manualBarcode = ""
        val found = state.cart.lines.firstOrNull { it.product.id <= 0 && it.product.barcode == barcode }?.product
            ?: state.products.firstOrNull { it.barcode == barcode }
        if(found != null) {
            if(!found.active) model.notify("商品已停用，请先在商品详情恢复")
            else if(model.add(found) && state.feedback) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } else if(state.cart.type == DocumentType.OUTBOUND) model.notify("商品未建档，请先入货")
        else { seedBarcode = barcode; editing = null; scanRegistration = true; showEditor = true }
    }

    fun submitWithZeroCheck() {
        if(model.state.value.cart.lines.any { it.unitPriceCents == 0L }) zeroConfirmation = true else model.commit()
    }

    fun confirmCart() {
        val current = model.state.value
        val changed = current.cart.lines.any { line ->
            val product = current.products.firstOrNull { it.id == line.product.id }
            product != null && if(current.cart.type == DocumentType.INBOUND) product.costPriceCents != line.product.costPriceCents
                else product.salePriceCents != line.product.salePriceCents
        }
        if(changed) priceConfirmation = true else submitWithZeroCheck()
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if(event == Lifecycle.Event.ON_RESUME) model.refresh() }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var day = LocalDate.now()
            while(true) { delay(30_000); val now = LocalDate.now(); if(now != day) { day = now; model.refresh() } }
        }
    }
    LaunchedEffect(state.message) { state.message?.let {
        if(it.startsWith("备份已恢复")) { query = ""; category = "全部"; documentId = null; productId = null; ledger = false }
        snackbar.showSnackbar(it); model.clearMessage()
    } }
    LaunchedEffect(state.products) { if(ProductCategories.counts(state.products).none { it.name == category }) category = "全部" }
    BackHandler(enabled = settings || productId != null || documentId != null || ledger) {
        if (!state.busy) when { documentId != null -> documentId = null; productId != null -> productId = null; settings -> settings = false; else -> ledger = false }
    }

    val managed = settings || productId != null || documentId != null
    Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        if(!managed) NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            val labels = listOf("今日", "商品", "扫码", "经营")
            val symbols = listOf(PujiSymbol.Home, PujiSymbol.Box, PujiSymbol.Scan, PujiSymbol.Chart)
            labels.forEachIndexed { index, label -> NavigationBarItem(selected = tab == index, enabled = !state.busy,
                onClick = { tab = index; ledger = false; if(index == 1) inventoryFilter = "全部"; model.refresh() }, icon = {
                    PujiIcon(symbols[index], tint = if(tab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }, label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer)) }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if(!state.ready) {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Text(if(state.busy) "正在打开铺记" else "暂时无法读取数据")
                    if(!state.busy) TextButton(onClick = model::refresh) { Text("重试") }
                }
            } else when {
                documentId != null -> {
                    val id = documentId!!
                    var document by remember(id) { mutableStateOf<StockDocument?>(null) }
                    LaunchedEffect(id, state.busy) { if(!state.busy) document = model.document(id) }
                    document?.let { d -> DocumentDetailsScreen(d, state.busy, { documentId = null },
                        { model.returnLines(d, it) { documentId = null } }, { model.reverse(d) { documentId = null } }) }
                }
                productId != null -> {
                    val product = state.products.firstOrNull { it.id == productId }
                    var history by remember(productId) { mutableStateOf<List<StockHistoryEntry>>(emptyList()) }
                    LaunchedEffect(productId, state.busy) { if(!state.busy) history = model.history(productId!!) }
                    if(product == null) TextButton(onClick = { productId = null }) { Text("返回商品") }
                    else ProductDetailsScreen(product, history, state.busy, { productId = null },
                        { editing = product; seedBarcode = ""; scanRegistration = false; showEditor = true },
                        { openScanner(DocumentType.INBOUND, product) },
                        { openScanner(DocumentType.OUTBOUND, product) },
                        { adjustment = product; writeOff = false }, { adjustment = product; writeOff = true },
                        { model.setActive(product, !product.active) })
                }
                settings -> SettingsScreen(state.busy, state.feedback, state.lastBackup,
                    { settings = false }, { backupSaver.launch("铺记-备份-${LocalDate.now()}.puji.json") },
                    { restorePicker.launch(arrayOf("application/json", "application/octet-stream", "*/*")) },
                    { csvPicker.launch(arrayOf("text/*", "application/vnd.ms-excel", "application/octet-stream")) },
                    { exportCsv("template") }, { exportCsv("inventory") }, model::listLocalBackups, model::feedback,
                    { showReset = true }, { about = true })
                tab == 0 -> HomeScreen(state.stats, state.products.filter { it.isLowStock },
                    { openScanner(DocumentType.INBOUND) }, { openScanner(DocumentType.OUTBOUND) },
                    { inventoryFilter = "全部"; tab = 1 }, { settings = true }, { showReset = true },
                    recentDocuments = state.recentDocuments, onOpenLedger = {
                        model.ledger(anchor = LocalDate.now(), filter = null, query = "", range = PeriodCalculator.custom(LocalDate.now(), LocalDate.now(), ZoneId.systemDefault()).getOrThrow())
                        tab = 3; ledger = true
                    }, onLowStock = { inventoryFilter = "低库存"; query = ""; category = "全部"; tab = 1 }, onOpenDocument = { documentId = it.id })
                tab == 1 -> {
                    val categories = ProductCategories.counts(state.products)
                    val filtered = state.products.filter { p ->
                        (category == "全部" || ProductCategories.normalize(p.category) == category) &&
                            (query.isBlank() || listOf(p.name,p.barcode,p.category).any { it.contains(query.trim(), true) })
                    }
                    InventoryScreen(filtered, query, categories, category, { query = it }, { category = it },
                        { editing = null; seedBarcode = ""; scanRegistration = false; showEditor = true },
                        { productId = it.id }, { exportCsv("inventory") }, onManage = { productId = it.id }, initialStockFilter = inventoryFilter)
                }
                tab == 2 -> ScannerScreen(state.cart.type, showEditor || pickProduct || nextMode != null || priceConfirmation || zeroConfirmation, { mode ->
                    if(mode != state.cart.type) { if(state.cart.lines.isEmpty()) model.changeMode(mode) else nextMode = mode }
                }, state.cart, manualBarcode, { manualBarcode = it }, ::lookupBarcode,
                    { id -> model.changeCart { it.increment(id) } }, { id -> model.changeCart { it.decrement(id) } },
                    { id, price -> model.changeCart { it.updateUnitPrice(id, price) } }, ::confirmCart,
                    onPickProduct = { pickProduct = true }, busy = state.busy,
                    onNoteChange = { note -> model.changeCart { it.copy(note = note) } },
                    onRemove = { id -> model.changeCart { it.remove(id) } })
                ledger -> {
                    OutlinedTextField(state.ledgerQuery, { model.ledger(query = it) }, Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        singleLine = true, label = { Text("商品或单号") }, textStyle = MaterialTheme.typography.bodyMedium)
                    Box(Modifier.weight(1f)) { LedgerScreen(state.documents, state.ledgerRange, state.ledgerFilter,
                        !PeriodCalculator.isCurrent(PeriodKind.MONTH, state.ledgerAnchor), { ledger = false },
                        { model.ledger(anchor = state.ledgerAnchor.minusMonths(1)) },
                        { model.ledger(anchor = state.ledgerAnchor.plusMonths(1)) }, { model.ledger(filter = it) },
                        { exportCsv("ledger", state.ledgerRange, state.ledgerFilter) }, { customExport = true },
                        { documentId = it.id }) }
                    if(state.documents.size >= state.ledgerLimit) TextButton(onClick = { model.ledger(more = true) }, enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()) { Text("加载更多") }
                }
                else -> RecordsScreen(state.summary, state.periodKind, state.summaryRange,
                    !PeriodCalculator.isCurrent(state.periodKind, state.periodAnchor), { model.period(it, LocalDate.now()) },
                    { model.period(anchor = PeriodCalculator.previous(state.periodKind, state.periodAnchor)) },
                    { model.period(anchor = PeriodCalculator.next(state.periodKind, state.periodAnchor)) }, {
                        model.ledger(anchor = state.periodAnchor, range = null); ledger = true
                    })
            }
        }
    }

    if(showEditor) ProductEditor(editing, seedBarcode, if(scanRegistration) DocumentType.INBOUND else null,
        onDismiss = { if(!state.busy) showEditor = false }, onSave = { p ->
            if(scanRegistration) {
                val pending = p.copy(id = -System.nanoTime(), stockQuantity = 0)
                if(model.add(pending, p.stockQuantity)) showEditor = false
            } else model.saveProduct(p) { showEditor = false }
        }, onDeactivate = null, busy = state.busy)
    if(pickProduct) ProductPickerDialog(state.products, { pickProduct = false }) { if(model.add(it)) pickProduct = false }
    nextMode?.let { mode -> AlertDialog(onDismissRequest = { nextMode = null; productAfterModeChange = null }, title = { Text("切换登记方式？") },
        text = { Text("切换将清空当前未提交清单。") }, confirmButton = { TextButton(onClick = {
            model.changeMode(mode); productAfterModeChange?.let { model.add(it) }; nextMode = null; productAfterModeChange = null
        }) { Text("清空并切换") } }, dismissButton = { TextButton(onClick = { nextMode = null; productAfterModeChange = null }) { Text("保留清单") } }) }
    if(priceConfirmation) AlertDialog(onDismissRequest = { priceConfirmation = false }, title = { Text("商品价格已变化") },
        text = { Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
            state.cart.lines.forEach { line ->
                val p = state.products.firstOrNull { it.id == line.product.id }
                val latest = if(state.cart.type == DocumentType.INBOUND) p?.costPriceCents else p?.salePriceCents
                val baseline = if(state.cart.type == DocumentType.INBOUND) line.product.costPriceCents else line.product.salePriceCents
                if(latest != null && latest != baseline) Text("${line.product.name}：${Money.format(baseline)} → ${Money.format(latest)}；本单 ${Money.format(line.unitPriceCents)}")
            }
        } }, confirmButton = { TextButton(onClick = {
            val updated = model.changeCart { cart -> cart.copy(lines = cart.lines.map { line ->
                val p = model.state.value.products.firstOrNull { it.id == line.product.id }
                if(p == null) line else {
                    val old = if(cart.type == DocumentType.INBOUND) line.product.costPriceCents else line.product.salePriceCents
                    val latest = if(cart.type == DocumentType.INBOUND) p.costPriceCents else p.salePriceCents
                    if(old == latest) line else line.copy(product = p, unitPriceCents = latest)
                }
            }) }
            if(updated) { priceConfirmation = false; submitWithZeroCheck() }
        }) { Text("使用当前价") } }, dismissButton = { TextButton(onClick = {
            priceConfirmation = false; submitWithZeroCheck()
        }) { Text("保留本单价") } })
    if(zeroConfirmation) AlertDialog(onDismissRequest = { zeroConfirmation = false }, title = { Text("确认零单价商品") },
        text = { Text(state.cart.lines.filter { it.unitPriceCents == 0L }.joinToString("、") { it.product.name } + "将以零单价记账。") },
        confirmButton = { TextButton(onClick = { zeroConfirmation = false; model.commit() }) { Text("确认记账") } },
        dismissButton = { TextButton(onClick = { zeroConfirmation = false }) { Text("返回检查") } })
    adjustment?.let { p -> StockAdjustmentDialog(p, writeOff, state.busy, { if(!state.busy) adjustment = null }) { qty, cost, note ->
        if(writeOff) model.writeOff(p, qty, note) { adjustment = null } else model.count(p, qty, cost, note) { adjustment = null }
    } }
    if(showReset) ResetDataDialog(state.busy, { if(!state.busy) showReset = false }) {
        showReset = false; model.clearAll(); query = ""; category = "全部"; productId = null; documentId = null; settings = false; ledger = false; tab = 0
    }
    if(customExport) {
        val zone = ZoneId.systemDefault()
        CustomExportDialog(Instant.ofEpochMilli(state.ledgerRange.startInclusive).atZone(zone).toLocalDate(),
            Instant.ofEpochMilli(state.ledgerRange.endExclusive).atZone(zone).toLocalDate().minusDays(1), state.ledgerFilter,
            { customExport = false }) { start, end, type ->
            PeriodCalculator.custom(start, end, zone).fold({ exportCsv("ledger", it, type); customExport = false }, { model.notify(it.message ?: "日期无效") })
        }
    }
    state.restoreInfo?.let { info -> AlertDialog(onDismissRequest = { if(!state.busy) model.dismissFilePreview() },
        title = { Text("恢复备份") }, text = { Text("${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA).format(java.util.Date(info.createdAt))}\n${info.productCount} 个商品，${info.documentCount} 笔记录。\n将替换当前数据与未提交清单；替换前会保存本机回退副本。") },
        confirmButton = { TextButton(onClick = model::restore, enabled = !state.busy) { Text("确认替换") } },
        dismissButton = { TextButton(onClick = model::dismissFilePreview, enabled = !state.busy) { Text("取消") } }) }
    state.localBackups?.let { entries -> AlertDialog(onDismissRequest = model::dismissLocalBackups,
        title = { Text("本机数据副本") }, text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            entries.forEach { entry -> TextButton(onClick = { model.prepareRollback(entry.key) }, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()) { Text(entry.title) } }
        } }, confirmButton = { TextButton(onClick = model::dismissLocalBackups, enabled = !state.busy) { Text("关闭") } }) }
    state.importPreview?.let { preview ->
        val openingRows = preview.rows.filter { row -> row.product.stockQuantity > 0 &&
            (row.product.barcode.isBlank() || state.products.none { it.barcode == row.product.barcode }) }
        val zeroOpeningCount = openingRows.count { it.product.costPriceCents == 0L }
        var confirmZeroOpening by remember(preview) { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { if(!state.busy) model.dismissFilePreview() },
        title = { Text("导入预览") }, text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            Text("${preview.rows.size} 行商品，${preview.errors.size} 处错误")
            Text("已有商品仅更新资料，保留库存。", style = MaterialTheme.typography.bodySmall)
            preview.errors.take(30).forEach { Text("第 ${it.row} 行：${it.message}", color = MaterialTheme.colorScheme.error) }
            preview.rows.take(10).forEach { Text(it.product.name) }
            openingRows.take(10).forEach { row -> Text("${row.product.name}：期初 ${row.product.stockQuantity}，单位成本 ${Money.format(row.product.costPriceCents)}",
                style = MaterialTheme.typography.bodySmall) }
            if (zeroOpeningCount > 0) Row(Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(confirmZeroOpening, { confirmZeroOpening = it }, enabled = !state.busy)
                Text("确认 $zeroOpeningCount 个新商品按零成本登记期初库存", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f))
            }
        } }, confirmButton = { TextButton(onClick = model::importProducts,
            enabled = !state.busy && preview.errors.isEmpty() && preview.rows.isNotEmpty() && (zeroOpeningCount == 0 || confirmZeroOpening)) { Text("确认导入") } },
        dismissButton = { TextButton(onClick = model::dismissFilePreview, enabled = !state.busy) { Text("取消") } }) }
    if(about) AlertDialog(onDismissRequest = { about = false }, title = { Text("铺记 ${BuildConfig.VERSION_NAME}") },
        text = { Column { Text("离线小店经营工具")
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/opensourcepos/opensourcepos".toUri())) }) { Text("开源参考与许可证") } } },
        confirmButton = { TextButton(onClick = { about = false }) { Text("完成") } })
}
