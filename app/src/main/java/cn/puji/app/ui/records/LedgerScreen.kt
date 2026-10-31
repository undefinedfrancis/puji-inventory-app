package cn.puji.app.ui.records

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.puji.app.domain.DateRange
import cn.puji.app.domain.DocumentType
import cn.puji.app.domain.Money
import cn.puji.app.domain.StockDocument
import cn.puji.app.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LedgerScreen(documents: List<StockDocument>, range: DateRange, filter: DocumentType?, canGoNext: Boolean,
    onBack: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, onFilterChange: (DocumentType?) -> Unit,
    onExportMonth: () -> Unit, onCustomExport: () -> Unit, onOpenDocument: ((StockDocument) -> Unit)? = null) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeader("", "流水") { TextButton(onClick = onBack) { Text("返回") } } }
        item { PeriodNavigator(range.label, canGoNext, onPrevious, onNext) }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            item { ChoiceButton("全部", filter == null) { onFilterChange(null) } }
            items(DocumentType.values().toList()) { type -> ChoiceButton(documentTypeLabel(type), filter == type) { onFilterChange(type) } }
        } }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${documents.size} 笔", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onExportMonth, enabled = documents.isNotEmpty()) { Text("导出本月") }
            TextButton(onClick = onCustomExport) { Text("自定义导出") }
        } }
        if(documents.isEmpty()) item { EmptyState("", "暂无流水", "切换月份或交易类型查看") }
        items(documents, key = { it.id }) { document -> DocumentCard(document, onOpenDocument) }
    }
}

@Composable
private fun DocumentCard(document: StockDocument, onOpenDocument: ((StockDocument) -> Unit)?) {
    ReceiptCard(Modifier.fillMaxWidth().then(if(onOpenDocument != null) Modifier.clickable { onOpenDocument(document) } else Modifier)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(documentTypeLabel(document.type), style = MaterialTheme.typography.titleMedium)
                Text(SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(document.createdAt)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(Money.format(document.totalCents), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
            if(onOpenDocument != null) PujiIcon(PujiSymbol.ChevronRight, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("#${document.id.toString().padStart(5, '0')}", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        document.lines.forEach { line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(line.productName, modifier = Modifier.weight(1f))
                Text("${line.quantity} × ${Money.format(line.unitPriceCents)}", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
            }
        }
        if(document.note.isNotBlank()) Text(document.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
