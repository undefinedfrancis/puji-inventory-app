package cn.puji.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.puji.app.ui.theme.Hairline
import cn.puji.app.ui.theme.InkMuted
import cn.puji.app.ui.theme.ScannerOrange
import cn.puji.app.ui.theme.TicketGreen
import cn.puji.app.ui.theme.TicketGreenSoft
import cn.puji.app.ui.theme.WarningSoft

@Composable
fun PageHeader(kicker: String, title: String, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            Text(kicker.uppercase(), color = TicketGreen, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
            Text(title, style = MaterialTheme.typography.headlineMedium)
        }
        action?.invoke()
    }
}

@Composable
fun ReceiptCard(
    modifier: Modifier = Modifier,
    accent: Color = TicketGreen,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Hairline),
        shadowElevation = 1.dp,
    ) {
        Column {
            Box(Modifier.fillMaxWidth().background(accent).padding(vertical = 3.dp))
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        }
    }
}

@Composable
fun Metric(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Surface(
        modifier = modifier,
        color = if (highlight) WarningSoft else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (highlight) ScannerOrange.copy(alpha = .3f) else Hairline),
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = InkMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                value,
                fontSize = when {
                    value.length > 13 -> 15.sp
                    value.length > 10 -> 18.sp
                    else -> 22.sp
                },
                maxLines = 1,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
fun StatusPill(text: String, warning: Boolean = false) {
    Surface(color = if (warning) WarningSoft else TicketGreenSoft, shape = RoundedCornerShape(999.dp)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = if (warning) ScannerOrange else TicketGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
fun EmptyState(symbol: String, title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(symbol, fontSize = 38.sp)
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
