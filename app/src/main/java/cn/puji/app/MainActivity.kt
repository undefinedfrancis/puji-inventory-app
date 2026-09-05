package cn.puji.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import cn.puji.app.ui.PujiApp
import cn.puji.app.ui.theme.PujiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as PujiApplication
        setContent {
            PujiTheme {
                PujiApp(app.products, app.inventory, app.dashboard)
            }
        }
    }
}
