package cn.puji.app.ui.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cn.puji.app.domain.Money
import cn.puji.app.domain.DocumentType
import cn.puji.app.domain.Product

@Composable
fun ProductEditor(
    product: Product?,
    seedBarcode: String = "",
    registrationMode: DocumentType? = null,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit,
    onDeactivate: ((Product) -> Unit)? = null,
) {
    var form by remember(product, seedBarcode, registrationMode) {
        mutableStateOf(
            ProductForm(
                name = product?.name.orEmpty(),
                barcode = product?.barcode?.ifBlank { seedBarcode } ?: seedBarcode,
                category = product?.category.orEmpty(),
                costPrice = product?.let { "%.2f".format(it.costPriceCents / 100.0) } ?: "0.00",
                salePrice = product?.let { "%.2f".format(it.salePriceCents / 100.0) } ?: "0.00",
                stock = product?.stockQuantity?.toString()
                    ?: if (registrationMode == DocumentType.INBOUND) "1" else "0",
                threshold = product?.lowStockThreshold?.toString() ?: "5",
                note = product?.note.orEmpty(),
            ),
        )
    }
    var errors by remember { mutableStateOf<Map<ProductField, String>>(emptyMap()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 720.dp).verticalScroll(rememberScrollState()).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(if (product == null) "新商品入册" else "编辑商品", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (product == null && registrationMode == DocumentType.INBOUND) {
                        "完善商品资料后，本次入货会直接记入流水"
                    } else "条码、价格和库存都可以稍后修改",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Field("商品名称", form.name, { form = form.copy(name = it) }, error = errors[ProductField.NAME])
                Field("条码（可留空）", form.barcode, { form = form.copy(barcode = it) })
                Field("分类", form.category, { form = form.copy(category = it) })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field("进货价", form.costPrice, { form = form.copy(costPrice = it) }, Modifier.weight(1f), errors[ProductField.COST_PRICE], true)
                    Field("售价", form.salePrice, { form = form.copy(salePrice = it) }, Modifier.weight(1f), errors[ProductField.SALE_PRICE], true)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field(
                        if (product == null && registrationMode == DocumentType.INBOUND) "本次入货数量" else "现有库存",
                        form.stock,
                        { form = form.copy(stock = it) },
                        Modifier.weight(1f),
                        errors[ProductField.STOCK],
                        true,
                    )
                    Field("低库存预警", form.threshold, { form = form.copy(threshold = it) }, Modifier.weight(1f), errors[ProductField.THRESHOLD], true)
                }
                Field("备注", form.note, { form = form.copy(note = it) })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
                    Button(
                        onClick = {
                            val checked = ProductFormValidator.validate(
                                form,
                                minimumStock = if (product == null && registrationMode == DocumentType.INBOUND) 1 else 0,
                            )
                            errors = checked
                            if (checked.isEmpty()) {
                                onSave(
                                    (product ?: Product(name = form.name)).copy(
                                        name = form.name.trim(),
                                        barcode = form.barcode.trim(),
                                        category = form.category.trim().ifBlank { "未分类" },
                                        costPriceCents = Money.parseYuan(form.costPrice).getOrThrow(),
                                        salePriceCents = Money.parseYuan(form.salePrice).getOrThrow(),
                                        stockQuantity = form.stock.toInt(),
                                        lowStockThreshold = form.threshold.toInt(),
                                        note = form.note.trim(),
                                    ),
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            if (product == null && registrationMode == DocumentType.INBOUND) "确认入货" else "保存商品",
                        )
                    }
                }
                if (product != null && onDeactivate != null) {
                    OutlinedButton(onClick = { onDeactivate(product) }, modifier = Modifier.fillMaxWidth()) { Text("停用此商品") }
                }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    numeric: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = label != "备注",
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
    )
}
