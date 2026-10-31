package cn.puji.app.ui.records

import android.app.DatePickerDialog
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.puji.app.domain.DocumentType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CustomExportDialog(
    initialStart: LocalDate,
    initialEnd: LocalDate,
    initialType: DocumentType?,
    onDismiss: () -> Unit,
    onExport: (LocalDate, LocalDate, DocumentType?) -> Unit,
) {
    val context = LocalContext.current
    var start by remember { mutableStateOf(initialStart) }
    var end by remember { mutableStateOf(initialEnd) }
    var type by remember { mutableStateOf(initialType) }
    val valid = !start.isAfter(end)

    fun pick(current: LocalDate, update: (LocalDate) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, day -> update(LocalDate.of(year, month + 1, day)) },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth,
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义时间导出") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pick(start) { start = it } }, modifier = Modifier.weight(1f)) {
                        Text("开始\n${start.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
                    }
                    OutlinedButton(onClick = { pick(end) { end = it } }, modifier = Modifier.weight(1f)) {
                        Text("结束\n${end.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { TypeButton("全部", type == null, Modifier) { type = null } }
                    items(DocumentType.values().toList()) { current ->
                        TypeButton(cn.puji.app.ui.components.documentTypeLabel(current), type == current, Modifier) { type = current }
                    }
                }
                if (!valid) Text("开始日期不能晚于结束日期", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = {
            Button(onClick = { onExport(start, end, type) }, enabled = valid) { Text("导出") }
        },
    )
}

@Composable
private fun TypeButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    cn.puji.app.ui.components.ChoiceButton(text, selected, modifier, onClick = onClick)
}
