package cn.puji.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import cn.puji.app.ui.PujiViewModel
import cn.puji.app.ui.PujiApp
import cn.puji.app.ui.theme.PujiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val model = ViewModelProvider(this)[PujiViewModel::class.java]
        setContent {
            PujiTheme {
                PujiApp(model)
            }
        }
    }
}
