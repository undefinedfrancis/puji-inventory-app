package cn.puji.app.ui.manage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cn.puji.app.domain.*
import cn.puji.app.ui.components.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    busy: Boolean, feedback: Boolean, lastBackup: Long, onBack: () -> Unit,
    onBackup: () -> Unit, onRestore: () -> Unit, onImport: () -> Unit, onTemplate: () -> Unit,
    onInventory: () -> Unit, onRollback: () -> Unit, onFeedback: (Boolean) -> Unit,
    onReset: () -> Unit, onAbout: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { DetailHeader("设置", busy, onBack) }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item { ReceiptCard(Modifier.fillMaxWidth()) {
            Text("数据备份", style = MaterialTheme.typography.titleMedium)
            Text(if (lastBackup > 0) "最近备份 ${formatTime(lastBackup)}" else "尚未保存完整备份",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MenuAction("完整备份", !busy, onBackup)
            PujiDivider(); MenuAction("恢复备份", !busy, onRestore)
            PujiDivider(); MenuAction("本机数据副本", !busy, onRollback)
        } }
        item { ReceiptCard(Modifier.fillMaxWidth()) {
            Text("导入与导出", style = MaterialTheme.typography.titleMedium)
            MenuAction("导入商品", !busy, onImport)
            PujiDivider(); MenuAction("商品导入模板", !busy, onTemplate)
            PujiDivider(); MenuAction("导出库存", !busy, onInventory)
        } }
        item { ReceiptCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("扫码反馈", modifier = Modifier.weight(1f))
                Switch(checked = feedback, onCheckedChange = onFeedback, enabled = !busy)
            }
            PujiDivider(); MenuAction("关于铺记", !busy, onAbout)
        } }
        item { ReceiptCard(Modifier.fillMaxWidth()) { MenuAction("清除全部数据", !busy, onReset, danger = true) } }
    }
}

@Composable
fun ProductDetailsScreen(
    product: Product, history: List<StockHistoryEntry>, busy: Boolean, onBack: () -> Unit,
    onEdit: () -> Unit, onInbound: () -> Unit, onOutbound: () -> Unit,
    onCount: () -> Unit, onWriteOff: () -> Unit, onToggleActive: () -> Unit,
) {
    var confirmDeactivate by remember(product.id) { mutableStateOf(false) }
    LaunchedEffect(product.active) { confirmDeactivate = false }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { DetailHeader("商品详情", busy, onBack) }
        item { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(product.name, style = MaterialTheme.typography.headlineSmall)
            Text(product.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!product.active) StatusPill("已停用")
        } }
        item { ReceiptCard(Modifier.fillMaxWidth()) {
            AmountPair("现有库存", "${product.stockQuantity}", "库存总值", Money.format(product.stockCostCents))
            PujiDivider()
            DetailValue("售价", Money.format(product.salePriceCents))
            DetailValue("建议进价", Money.format(product.costPriceCents))
            DetailValue("低库存预警", "${product.lowStockThreshold}")
            DetailValue("条码", product.barcode.ifBlank { "无条码" })
            Text("成本来源：${costSourceLabel(product.costSource)}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (product.note.isNotBlank()) Text(product.note, style = MaterialTheme.typography.bodySmall)
        } }
        item { ReceiptCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton("入货", !busy && product.active, onInbound, Modifier.weight(1f))
                ActionButton("出货", !busy && product.active, onOutbound, Modifier.weight(1f))
            }
            MenuAction("编辑资料", !busy, onEdit)
            PujiDivider(); MenuAction("盘点", !busy && product.active, onCount)
            PujiDivider(); MenuAction("报损", !busy && product.active, onWriteOff)
            PujiDivider(); MenuAction(if (product.active) "停用商品" else "恢复商品", !busy,
                { if (product.active) confirmDeactivate = true else onToggleActive() }, danger = product.active)
        } }
        item { Text("库存变动", style = MaterialTheme.typography.titleMedium) }
        if (history.isEmpty()) item { EmptyState("", "暂无库存变动", "") }
        items(history, key = { it.id }) { entry ->
            ReceiptCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(documentTypeLabel(entry.type), style = MaterialTheme.typography.titleMedium)
                        Text(formatTime(entry.createdAt), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (entry.quantityDelta > 0) "+${entry.quantityDelta}" else "${entry.quantityDelta}",
                            style = MaterialTheme.typography.titleMedium)
                        Text("库存 ${entry.stockAfter}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("单据 #${entry.documentId}", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                DetailValue("成本变动", Money.format(entry.stockCostDeltaCents))
                DetailValue("变动后总成本", Money.format(entry.stockCostAfterCents))
                if (entry.note.isNotBlank()) Text(entry.note, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (confirmDeactivate) AlertDialog(
        onDismissRequest = { if (!busy) confirmDeactivate = false }, title = { Text("停用商品") },
        text = { Text("当前库存 ${product.stockQuantity}。停用后不能入货或出货，库存与历史记录保留。") },
        dismissButton = { TextButton(onClick = { confirmDeactivate = false }, enabled = !busy) { Text("取消") } },
        confirmButton = { TextButton(onClick = onToggleActive, enabled = !busy) { Text(if (busy) "处理中" else "确认停用") } },
    )
}

@Composable
fun StockAdjustmentDialog(
    product: Product, writeOff: Boolean, busy: Boolean, onDismiss: () -> Unit,
    onConfirm: (Int, Long, String) -> Unit,
) {
    val unitCost = remember(product.id, product.version) {
        if (product.stockQuantity > 0) BigDecimal.valueOf(product.stockCostCents)
            .divide(BigDecimal.valueOf(product.stockQuantity.toLong()), 0, RoundingMode.HALF_UP).longValueExact()
        else product.costPriceCents
    }
    var quantity by remember(product.id, writeOff) { mutableStateOf(if (writeOff) "" else product.stockQuantity.toString()) }
    var cost by remember(product.id, writeOff) { mutableStateOf(BigDecimal.valueOf(unitCost, 2).toPlainString()) }
    var reason by remember(product.id, writeOff) { mutableStateOf("") }
    var confirmZero by remember(product.id, writeOff) { mutableStateOf(false) }
    var submitted by remember(product.id, writeOff) { mutableStateOf(false) }
    val parsedQuantity = quantity.toIntOrNull()
    val parsedCost = Money.parseYuan(cost).getOrNull()
    val quantityError = when {
        parsedQuantity == null -> "请输入有效整数"
        writeOff && parsedQuantity <= 0 -> "报损数量必须大于 0"
        !writeOff && parsedQuantity < 0 -> "实点库存不能为负数"
        else -> null
    }
    val addsStock = !writeOff && parsedQuantity != null && parsedQuantity > product.stockQuantity
    val costError = if (parsedCost == null) "请输入有效金额，最多两位小数" else null
    val reasonError = if (writeOff && reason.isBlank()) "请填写报损原因" else null
    val zeroError = addsStock && parsedCost == 0L && !confirmZero
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().imePadding().heightIn(max = 680.dp).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (writeOff) "报损" else "盘点", style = MaterialTheme.typography.headlineMedium)
                Text(product.name)
                Text("系统库存 ${product.stockQuantity}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth(), label = { Text(if (writeOff) "报损数量" else "实点库存") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = submitted && quantityError != null,
                    supportingText = if (submitted && quantityError != null) { { Text(quantityError) } } else null)
                if (!writeOff && parsedQuantity != null && quantityError == null) {
                    val delta = parsedQuantity.toLong() - product.stockQuantity.toLong()
                    Text("差额 ${if (delta > 0) "+" else ""}$delta", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(value = cost, onValueChange = { cost = it; confirmZero = false }, enabled = !busy && !writeOff,
                    modifier = Modifier.fillMaxWidth(), label = { Text(if (writeOff) "当前单位成本（元）" else "盘盈单位成本（元）") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = submitted && costError != null,
                    supportingText = { Text(if (submitted && costError != null) costError else
                        if (product.stockQuantity > 0) "默认当前平均成本，显示到分" else "默认建议进价") })
                if (!writeOff) Text("单位成本用于增加的库存；减少库存按当前成本核算。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (addsStock && parsedCost == 0L) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = confirmZero, onCheckedChange = { confirmZero = it }, enabled = !busy)
                        Text("确认按零成本增加库存", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    }
                    if (submitted && zeroError) Text("请确认零成本，或填写单位成本", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(value = reason, onValueChange = { reason = it }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (writeOff) "报损原因" else "盘点原因（选填）") }, minLines = 2, maxLines = 4,
                    isError = submitted && reasonError != null,
                    supportingText = if (submitted && reasonError != null) { { Text(reasonError) } } else null)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("取消") }
                    ActionButton(if (busy) "提交中" else if (writeOff) "确认报损" else "完成盘点", !busy, {
                        submitted = true
                        if (quantityError == null && costError == null && reasonError == null && !zeroError) {
                            onConfirm(requireNotNull(parsedQuantity), requireNotNull(parsedCost), reason.trim())
                        }
                    }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun DocumentDetailsScreen(
    document: StockDocument, busy: Boolean, onBack: () -> Unit,
    onReturn: (Map<Long, Int>) -> Unit, onReverse: () -> Unit,
) {
    val quantities = remember(document.id) { mutableStateMapOf<Long, String>() }
    var submitted by remember(document.id) { mutableStateOf(false) }
    var reverseConfirmation by remember(document.id) { mutableStateOf(false) }
    val canReturn = document.type == DocumentType.INBOUND || document.type == DocumentType.OUTBOUND
    val eligibleLines = document.lines.filter { it.quantity > it.returnedQuantity }
    val errors = eligibleLines.mapNotNull { line ->
        val value = (quantities[line.id] ?: "0").toIntOrNull()
        val maximum = (line.quantity - line.returnedQuantity).coerceAtLeast(0)
        if (value == null || value < 0 || value > maximum) line.id to "请输入 0–$maximum 的整数" else null
    }.toMap()
    val selected = eligibleLines.mapNotNull { line ->
        (quantities[line.id] ?: "0").toIntOrNull()?.takeIf { it > 0 }?.let { line.id to it }
    }.toMap()
    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { DetailHeader("单据详情", busy, onBack) }
        item { ReceiptCard(Modifier.fillMaxWidth()) {
            Text(documentTypeLabel(document.type), style = MaterialTheme.typography.titleMedium)
            Text(Money.format(document.totalCents), style = MaterialTheme.typography.headlineSmall)
            DetailValue("单号", "#${document.id}")
            DetailValue("时间", formatTime(document.createdAt))
            document.originalDocumentId?.let { original ->
                DetailValue("关联原单", "#${original}${document.originalType?.let { " · ${documentTypeLabel(it)}" }.orEmpty()}")
            }
            if (document.expectedQuantity != null) DetailValue("系统数量", document.expectedQuantity.toString())
            if (document.actualQuantity != null) DetailValue("实点数量", document.actualQuantity.toString())
            DetailValue("成本来源", costSourceLabel(document.costSource))
            if (document.valuationDifferenceCents != 0L) DetailValue("库存估值差额", Money.format(document.valuationDifferenceCents))
            if (document.note.isNotBlank()) Text(document.note, style = MaterialTheme.typography.bodySmall)
        } }
        item { Text("商品明细", style = MaterialTheme.typography.titleMedium) }
        items(document.lines, key = { it.id }) { line ->
            ReceiptCard(Modifier.fillMaxWidth()) {
                Text(line.productName, style = MaterialTheme.typography.titleMedium)
                DetailValue("数量与单价", "${line.quantity} × ${Money.format(line.unitPriceCents)}")
                DetailValue("成交金额", Money.format(line.lineTotalCents))
                DetailValue("成本", Money.format(line.lineCostCents))
                DetailValue("成本来源", costSourceLabel(line.costSource))
                if (line.originalLineId != null) DetailValue("关联明细", "#${line.originalLineId}")
                if (canReturn) DetailValue("已退数量", "${line.returnedQuantity}")
                if (canReturn && line.quantity > line.returnedQuantity) {
                    val maximum = (line.quantity - line.returnedQuantity).coerceAtLeast(0)
                    OutlinedTextField(value = quantities[line.id] ?: "0", onValueChange = { quantities[line.id] = it },
                        enabled = !busy, modifier = Modifier.fillMaxWidth(), label = { Text("退货数量") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = submitted && errors.containsKey(line.id),
                        supportingText = { Text(if (submitted && errors.containsKey(line.id)) errors.getValue(line.id) else "最多可退 $maximum") })
                }
            }
        }
        if (canReturn && eligibleLines.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (submitted && selected.isEmpty() && errors.isEmpty()) Text("请至少选择一件退货商品", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
                Text("按原单成交价格退货。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ActionButton(if (busy) "提交中" else "确认退货", !busy, {
                    submitted = true
                    if (errors.isEmpty() && selected.isNotEmpty()) onReturn(selected)
                }, Modifier.fillMaxWidth())
            }
        }
        if (canReturn && eligibleLines.isEmpty()) item { Text("本单已无可退数量", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (document.type != DocumentType.REVERSAL) item {
            OutlinedButton(onClick = { reverseConfirmation = true }, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("冲正此单") }
        }
    }
    if (reverseConfirmation) AlertDialog(onDismissRequest = { if (!busy) reverseConfirmation = false },
        title = { Text("确认冲正") }, text = { Text("将为单据 #${document.id} 生成关联冲正记录，原单保留。库存与业务金额将相应冲回。") },
        dismissButton = { TextButton(onClick = { reverseConfirmation = false }, enabled = !busy) { Text("取消") } },
        confirmButton = { TextButton(onClick = onReverse, enabled = !busy) { Text(if (busy) "提交中" else "确认冲正") } })
}

@Composable
fun ProductPickerDialog(products: List<Product>, onDismiss: () -> Unit, onSelect: (Product) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = products.filter { it.active && (query.isBlank() || it.name.contains(query, ignoreCase = true)
        || it.barcode.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().imePadding().heightIn(max = 640.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PageHeader("", "选商品") { IconButton(onClick = onDismiss) { PujiIcon(PujiSymbol.Close, description = "关闭选货") } }
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, label = { Text("搜索名称、条码或分类") },
                    leadingIcon = { PujiIcon(PujiSymbol.Search, tint = MaterialTheme.colorScheme.onSurfaceVariant) })
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    if (results.isEmpty()) item { EmptyState("", "没有找到商品", "试试其他关键词") }
                    items(results, key = { it.id }) { product ->
                        Column {
                            Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable { onSelect(product) }.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(product.name)
                                    Text(product.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(Money.format(product.salePriceCents), style = MaterialTheme.typography.titleMedium)
                                    Text("库存 ${product.stockQuantity}", style = MaterialTheme.typography.bodySmall,
                                        color = if (product.isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            PujiDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(title: String, busy: Boolean, onBack: () -> Unit) {
    PageHeader("", title) { TextButton(onClick = onBack, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("返回") } }
}

@Composable
private fun MenuAction(title: String, enabled: Boolean, onClick: () -> Unit, danger: Boolean = false) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, modifier = Modifier.weight(1f), color = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
            danger -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        })
        PujiIcon(PujiSymbol.ChevronRight, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else .38f))
    }
}

@Composable
private fun DetailValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
    }
}

@Composable
private fun ActionButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) { Text(text) }
}

private fun formatTime(time: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(time))

private fun costSourceLabel(source: String): String = when (source) {
    "actual" -> "实际成本"
    "historical_estimate" -> "历史估算"
    "historical_unspecified" -> "历史口径，来源未记录"
    "migration", "migration_opening", "migration_estimate" -> "升级期初估值"
    else -> "来源未记录"
}
