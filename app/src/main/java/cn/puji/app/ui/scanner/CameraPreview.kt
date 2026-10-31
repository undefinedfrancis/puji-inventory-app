package cn.puji.app.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import cn.puji.app.scanner.BarcodeAnalyzer
import cn.puji.app.scanner.ScannerController
import java.util.concurrent.Executors

@Composable
fun CameraScanner(onBarcode: (String) -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val currentBarcode by rememberUpdatedState(onBarcode)
    val currentEnabled by rememberUpdatedState(enabled)
    val controller = remember { ScannerController { if (currentEnabled) currentBarcode(it) } }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    Box(modifier = modifier.background(Color.Black, RoundedCornerShape(22.dp))) {
        if (granted && enabled) CameraContent(controller)
        else Column(Modifier.align(Alignment.Center).padding(16.dp)) {
            Text(if(!enabled) "扫描已暂停" else "相机未开启", color = Color.White, style = MaterialTheme.typography.bodyMedium)
            if(!granted && enabled) TextButton(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("允许相机", color = Color.White) }
        }
        Box(
            Modifier.fillMaxSize().padding(horizontal = 44.dp, vertical = 48.dp)
                .border(1.dp, Color.White.copy(alpha = .6f), RoundedCornerShape(10.dp)),
        )
        Text(
            if (enabled) "将条码放入框内" else "扫描已暂停",
            modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun CameraContent(controller: ScannerController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember { BarcodeAnalyzer { controller.accept(it) } }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(lifecycleOwner) {
        var disposed = false
        var boundPreview: Preview? = null
        var boundAnalysis: ImageAnalysis? = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if(disposed) return@addListener
            runCatching {
                provider = future.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also { it.setAnalyzer(executor, analyzer) }
                boundPreview = preview
                boundAnalysis = analysis
                provider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            listOfNotNull(boundPreview, boundAnalysis).takeIf { it.isNotEmpty() }?.let { provider?.unbind(*it.toTypedArray()) }
            analyzer.close()
            executor.shutdown()
        }
    }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}
