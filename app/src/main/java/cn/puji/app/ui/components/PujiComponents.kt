package cn.puji.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PageHeader(kicker: String, title: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun ReceiptCard(modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary,
                content: @Composable () -> Unit) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
fun Metric(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Column(modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall,
            color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun StatusPill(text: String, warning: Boolean = false) {
    Surface(color = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp)) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium,
            color = if (warning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
fun EmptyState(symbol: String, title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PujiIcon(PujiSymbol.Box, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun PujiDivider() { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }

@Composable
fun ChoiceButton(text: String, selected: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled,
        shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)) {
        Text(text)
    }
}

@Composable
fun AmountPair(firstLabel: String, firstValue: String, secondLabel: String, secondValue: String, prominent: Boolean = false) {
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 300.dp || fontScale > 1.15f || firstValue.length > 11 || secondValue.length > 11
        val content: @Composable (String, String, Boolean) -> Unit = { label, value, first ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = if (prominent && first) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall)
            }
        }
        if (stacked) Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            content(firstLabel, firstValue, true); content(secondLabel, secondValue, false)
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.weight(1f)) { content(firstLabel, firstValue, true) }
            Box(Modifier.weight(1f)) { content(secondLabel, secondValue, false) }
        }
    }
}

fun documentTypeLabel(type: cn.puji.app.domain.DocumentType): String = when (type.name) {
    "INBOUND" -> "入货"
    "OUTBOUND" -> "出货"
    "OPENING" -> "期初库存"
    "SALES_RETURN" -> "销售退货"
    "PURCHASE_RETURN" -> "进货退货"
    "STOCK_GAIN" -> "盘盈"
    "STOCK_LOSS" -> "盘亏"
    "WRITE_OFF" -> "报损"
    "REVERSAL" -> "冲正"
    else -> "库存变动"
}
