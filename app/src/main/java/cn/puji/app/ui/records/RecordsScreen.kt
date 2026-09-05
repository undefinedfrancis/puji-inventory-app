package cn.puji.app.ui.records

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.puji.app.domain.DocumentType
import cn.puji.app.domain.Money
import cn.puji.app.domain.StockDocument
import cn.puji.app.ui.components.EmptyState
import cn.puji.app.ui.components.PageHeader
import cn.puji.app.ui.components.ReceiptCard
import cn.puji.app.ui.components.StatusPill
import cn.puji.app.ui.theme.ScannerOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordsScreen(
    documents: List<StockDocument>,
    filter: DocumentType?,
    onFilterChange: (DocumentType?) -> Unit,
    onExport: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        PageHeader("Ledger / 流水", "每笔都有据") {
            OutlinedButton(onClick = onExport) { Text("导出") }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterButton("全部", filter == null, Modifier.weight(1f)) { onFilterChange(null) }
            FilterButton("出货", filter == DocumentType.OUTBOUND, Modifier.weight(1f)) { onFilterChange(DocumentType.OUTBOUND) }
            FilterButton("入货", filter == DocumentType.INBOUND, Modifier.weight(1f)) { onFilterChange(DocumentType.INBOUND) }
        }
        Spacer(Modifier.height(14.dp))
        if (documents.isEmpty()) {
            EmptyState("≋", "还没有流水", "完成第一笔入货或出货后，这里会留下记录")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(documents, key = { it.id }) { document -> DocumentCard(document) }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun FilterButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick, modifier = modifier) { Text(text) }
    else OutlinedButton(onClick = onClick, modifier = modifier) { Text(text) }
}

@Composable
private fun DocumentCard(document: StockDocument) {
    val outbound = document.type == DocumentType.OUTBOUND
    ReceiptCard(accent = if (outbound) ScannerOrange else MaterialTheme.colorScheme.primary) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                StatusPill(if (outbound) "出货单" else "入货单", outbound)
                Text("#${document.id.toString().padStart(5, '0')}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Column {
                Text(Money.format(document.totalCents), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(
                    SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(document.createdAt)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
        document.lines.forEach { line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(line.productName, modifier = Modifier.weight(1f))
                Text("${line.quantity} × ${Money.format(line.unitPriceCents)}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
        if (document.note.isNotBlank()) Text(document.note, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}
