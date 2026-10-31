package cn.puji.app.ui.scanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cn.puji.app.domain.Cart
import cn.puji.app.domain.CartLine
import cn.puji.app.domain.DocumentType
import cn.puji.app.domain.Money
import cn.puji.app.ui.components.*
import java.util.Locale

@Composable
fun ScannerScreen(
    mode: DocumentType, scannerLocked: Boolean, onModeChange: (DocumentType) -> Unit, cart: Cart,
    manualBarcode: String, onManualBarcodeChange: (String) -> Unit, onLookup: (String) -> Unit,
    onIncrement: (Long) -> Unit, onDecrement: (Long) -> Unit, onPriceChange: (Long, Long) -> Unit,
    onConfirm: () -> Unit, onPickProduct: () -> Unit = {}, busy: Boolean = false,
    onNoteChange: ((String) -> Unit)? = null, onRemove: ((Long) -> Unit)? = null,
) {
    var showManual by remember { mutableStateOf(false) }
    var editingPriceId by remember { mutableStateOf<Long?>(null) }
    val available = !busy && !scannerLocked && editingPriceId == null
    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { PageHeader("", "扫码") }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceButton("入货", mode == DocumentType.INBOUND, Modifier.weight(1f), available) { onModeChange(DocumentType.INBOUND) }
                ChoiceButton("出货", mode == DocumentType.OUTBOUND, Modifier.weight(1f), available) { onModeChange(DocumentType.OUTBOUND) }
            } }
            item { CameraScanner(onBarcode = onLookup, enabled = !scannerLocked && available && !showManual && editingPriceId == null,
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp))) }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showManual = !showManual }, enabled = available, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    PujiIcon(PujiSymbol.Keyboard, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp)); Text("手动输入")
                }
                TextButton(onClick = onPickProduct, enabled = available, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    PujiIcon(PujiSymbol.List, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp)); Text("选商品")
                }
            } }
            if (showManual || manualBarcode.isNotBlank()) item {
                OutlinedTextField(value = manualBarcode, onValueChange = onManualBarcodeChange,
                    modifier = Modifier.fillMaxWidth(), label = { Text("条码") }, enabled = available, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (manualBarcode.isNotBlank() && available) onLookup(manualBarcode) }),
                    trailingIcon = { TextButton(onClick = { onLookup(manualBarcode) }, enabled = manualBarcode.isNotBlank() && available) { Text("添加") } })
            }
            if (cart.lines.isEmpty()) item { EmptyState("", "待添加商品", "") }
            items(cart.lines, key = { it.product.id }) { line ->
                CartRow(line, mode, available, onIncrement, onDecrement, onPriceChange, onRemove,
                    editingPrice = editingPriceId == line.product.id,
                    onEditingPriceChange = { editingPriceId = if (it) line.product.id else null })
            }
            if (cart.lines.isNotEmpty() && onNoteChange != null) item {
                OutlinedTextField(value = cart.note, onValueChange = onNoteChange, enabled = available,
                    modifier = Modifier.fillMaxWidth(), label = { Text("备注") }, maxLines = 3)
            }
        }
        if (cart.lines.isNotEmpty()) Surface(color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val total = Money.format(cart.totalCents)
                val largeText = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.15f
                if (total.length > 12 || largeText) {
                    Text("${cart.totalQuantity} 件 · 合计", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(total, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.End))
                } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${cart.totalQuantity} 件 · 合计", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(total, style = MaterialTheme.typography.headlineSmall)
                }
                Button(onClick = onConfirm, enabled = available, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
                    if (busy) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
                    Text(if (busy) "提交中" else if (mode == DocumentType.OUTBOUND) "确认出货" else "确认入货")
                }
            }
        }
    }
}

@Composable
private fun CartRow(line: CartLine, mode: DocumentType, enabled: Boolean, onIncrement: (Long) -> Unit,
                    onDecrement: (Long) -> Unit, onPriceChange: (Long, Long) -> Unit, onRemove: ((Long) -> Unit)?,
                    editingPrice: Boolean, onEditingPriceChange: (Boolean) -> Unit) {
    var price by remember(line.unitPriceCents) { mutableStateOf(java.math.BigDecimal.valueOf(line.unitPriceCents, 2).toPlainString()) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(line.product.name)
                Text("库存 ${line.product.stockQuantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(Money.format(line.totalCents), style = MaterialTheme.typography.titleMedium)
            if (onRemove != null) IconButton(onClick = { onRemove(line.product.id) }, enabled = enabled) {
                PujiIcon(PujiSymbol.Close, description = "移除${line.product.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onEditingPriceChange(true) }, enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                contentPadding = PaddingValues(0.dp)) { Text("单价 ${Money.format(line.unitPriceCents)}", modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall) }
            IconButton(onClick = { onDecrement(line.product.id) }, enabled = enabled) { PujiIcon(PujiSymbol.Minus, description = "减少${line.product.name}数量") }
            Text("${line.quantity}", modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = { onIncrement(line.product.id) }, enabled = enabled) { PujiIcon(PujiSymbol.Plus, description = "增加${line.product.name}数量") }
        }
        PujiDivider()
    }
    if (editingPrice) {
        val parsed = Money.parseYuan(price).getOrNull()
        AlertDialog(onDismissRequest = { onEditingPriceChange(false) }, title = { Text(if (mode == DocumentType.INBOUND) "入货单价" else "出货单价") },
            text = { OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("单价（元）") },
                singleLine = true, isError = parsed == null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) },
            dismissButton = { TextButton(onClick = { onEditingPriceChange(false) }) { Text("取消") } },
            confirmButton = { TextButton(onClick = { parsed?.let { onPriceChange(line.product.id, it) }; onEditingPriceChange(false) }, enabled = parsed != null) { Text("保存") } })
    }
}
