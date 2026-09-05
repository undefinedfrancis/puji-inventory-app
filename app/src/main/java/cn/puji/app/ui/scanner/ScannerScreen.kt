package cn.puji.app.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.puji.app.domain.Cart
import cn.puji.app.domain.CartLine
import cn.puji.app.domain.DocumentType
import cn.puji.app.domain.Money
import cn.puji.app.ui.components.EmptyState
import cn.puji.app.ui.components.PageHeader
import cn.puji.app.ui.components.ReceiptCard
import cn.puji.app.ui.theme.ScannerOrange

@Composable
fun ScannerScreen(
    mode: DocumentType,
    onModeChange: (DocumentType) -> Unit,
    cart: Cart,
    manualBarcode: String,
    onManualBarcodeChange: (String) -> Unit,
    onLookup: (String) -> Unit,
    onIncrement: (Long) -> Unit,
    onDecrement: (Long) -> Unit,
    onPriceChange: (Long, Long) -> Unit,
    onConfirm: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        PageHeader("Scanner / 扫码", if (mode == DocumentType.OUTBOUND) "快速出货" else "入货登记")
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeButton("出货", mode == DocumentType.OUTBOUND, Modifier.weight(1f)) { onModeChange(DocumentType.OUTBOUND) }
            ModeButton("入货", mode == DocumentType.INBOUND, Modifier.weight(1f)) { onModeChange(DocumentType.INBOUND) }
        }
        Spacer(Modifier.height(12.dp))
        CameraScanner(onBarcode = onLookup, modifier = Modifier.fillMaxWidth().height(190.dp))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = manualBarcode,
            onValueChange = onManualBarcodeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("手动输入条码") },
            singleLine = true,
            trailingIcon = { Text("↵", fontSize = 22.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onLookup(manualBarcode) }),
        )
        Spacer(Modifier.height(10.dp))
        if (cart.lines.isEmpty()) {
            EmptyState("▥", "等待第一件商品", "扫描条码，或输入条码后按回车")
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cart.lines, key = { it.product.id }) { line ->
                    CartRow(line, mode, onIncrement, onDecrement, onPriceChange)
                }
            }
            ReceiptCard(accent = if (mode == DocumentType.OUTBOUND) ScannerOrange else MaterialTheme.colorScheme.primary) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("${cart.totalQuantity} 件", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (mode == DocumentType.OUTBOUND) "本次出货" else "本次入货", fontWeight = FontWeight.Bold)
                    }
                    Text(Money.format(cart.totalCents), fontFamily = FontFamily.Monospace, fontSize = 27.sp, fontWeight = FontWeight.Black)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (mode == DocumentType.OUTBOUND) ScannerOrange else MaterialTheme.colorScheme.primary),
                ) { Text(if (mode == DocumentType.OUTBOUND) "确认出货并扣库存" else "确认入货并加库存") }
            }
        }
        Spacer(Modifier.height(88.dp))
    }
}

@Composable
private fun ModeButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick, modifier = modifier) { Text(text) }
    else OutlinedButton(onClick = onClick, modifier = modifier) { Text(text) }
}

@Composable
private fun CartRow(
    line: CartLine,
    mode: DocumentType,
    onIncrement: (Long) -> Unit,
    onDecrement: (Long) -> Unit,
    onPriceChange: (Long, Long) -> Unit,
) {
    ReceiptCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(line.product.name, fontWeight = FontWeight.Black)
                Text(
                    if (mode == DocumentType.OUTBOUND) "库存 ${line.product.stockQuantity}" else "入库后 ${line.product.stockQuantity + line.quantity}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
            Text(Money.format(line.totalCents), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onDecrement(line.product.id) }) { Text("−") }
            Text("${line.quantity}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
            OutlinedButton(onClick = { onIncrement(line.product.id) }) { Text("＋") }
            if (mode == DocumentType.INBOUND) {
                OutlinedTextField(
                    value = "%.2f".format(line.unitPriceCents / 100.0),
                    onValueChange = { Money.parseYuan(it).getOrNull()?.let { cents -> onPriceChange(line.product.id, cents) } },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("进价") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }
    }
}
