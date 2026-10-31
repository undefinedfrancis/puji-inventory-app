package cn.puji.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.puji.app.domain.HoldConfirmationGate

private val DangerRed = Color(0xFFB3261E)

@Composable
fun ResetDataDialog(
    working: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text("将永久清除全部数据") },
        text = {
            Column {
                Text("此操作不可撤销，将同时清除：")
                Text("\n• 全部商品资料和库存\n• 全部交易与库存调整记录\n• 未提交清单和本机回退副本")
                Text("\n可先在设置中保存完整备份。已导出的文件不受影响。", style = MaterialTheme.typography.bodySmall)
                Text(
                    "\n请按住下方红色按钮 2 秒。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !working) { Text("取消") }
        },
        confirmButton = {
            HoldToResetButton(enabled = !working, working = working, onComplete = onConfirm)
        },
    )
}

@Composable
private fun HoldToResetButton(
    enabled: Boolean,
    working: Boolean,
    onComplete: () -> Unit,
) {
    val gate = remember { HoldConfirmationGate(2_000) }
    var holding by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(holding) {
        while (holding) {
            withFrameMillis { now ->
                progress = gate.progress(now)
                if (gate.tryComplete(now)) {
                    holding = false
                    onComplete()
                }
            }
        }
        if (!working) progress = 0f
    }

    Surface(
        modifier = Modifier
            .height(52.dp)
            .semantics { role = Role.Button }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    gate.press(down.uptimeMillis)
                    holding = true
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed } && holding)
                    holding = false
                    gate.release()
                }
            },
        color = if (enabled) DangerRed else DangerRed.copy(alpha = .45f),
        contentColor = Color.White,
        shape = RoundedCornerShape(999.dp),
    ) {
        Box(Modifier.padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
            if (working) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.height(22.dp))
            } else {
                Text(if (holding) "继续按住… ${(progress * 100).toInt()}%" else "按住 2 秒永久清除", fontWeight = FontWeight.Medium)
            }
            if (holding) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .align(Alignment.BottomStart)
                        .background(Color.White.copy(alpha = .9f)),
                )
            }
        }
    }
}
