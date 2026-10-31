package cn.puji.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class PujiSymbol { Home, Box, Scan, Chart, Settings, Plus, Minus, Search, ChevronRight, ChevronLeft, Inbound, Outbound, List, Keyboard, Close }

@Composable
fun PujiIcon(symbol: PujiSymbol, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onSurface,
             description: String? = null) {
    Canvas(modifier.size(24.dp).then(if (description == null) Modifier else Modifier.semantics { contentDescription = description })) {
        val u = size.width / 24f
        val stroke = Stroke(1.6f * u, cap = StrokeCap.Round)
        fun line(x: Float, y: Float, xx: Float, yy: Float) = drawLine(tint, Offset(x*u,y*u),Offset(xx*u,yy*u),strokeWidth=1.6f*u,cap=StrokeCap.Round)
        fun path(vararg p: Float) { val a=Path(); a.moveTo(p[0]*u,p[1]*u); for(i in 2 until p.size step 2) a.lineTo(p[i]*u,p[i+1]*u); drawPath(a,tint,style=stroke) }
        when(symbol) {
            PujiSymbol.Home -> { path(3f,11f,12f,3f,21f,11f); path(5f,10f,5f,21f,10f,21f,10f,15f,14f,15f,14f,21f,19f,21f,19f,10f) }
            PujiSymbol.Box -> { drawRoundRect(tint,Offset(5*u,6*u),Size(14*u,15*u),CornerRadius(2*u),style=stroke); path(9f,7f,9f,4f,15f,4f,15f,7f); line(9f,11f,9f,11.2f); line(15f,11f,15f,11.2f) }
            PujiSymbol.Scan -> { path(4f,9f,4f,4f,9f,4f);path(15f,4f,20f,4f,20f,9f);path(4f,15f,4f,20f,9f,20f);path(15f,20f,20f,20f,20f,15f);line(3f,12f,21f,12f) }
            PujiSymbol.Chart -> { line(5f,21f,5f,13f);line(12f,21f,12f,4f);line(19f,21f,19f,9f) }
            PujiSymbol.Settings -> { drawCircle(tint,4*u,Offset(12*u,12*u),style=stroke); for(i in 0..7) { val a=i*Math.PI/4; line((12+8*kotlin.math.cos(a)).toFloat(),(12+8*kotlin.math.sin(a)).toFloat(),(12+10*kotlin.math.cos(a)).toFloat(),(12+10*kotlin.math.sin(a)).toFloat()) };drawCircle(tint,8*u,Offset(12*u,12*u),style=stroke) }
            PujiSymbol.Plus -> {line(5f,12f,19f,12f);line(12f,5f,12f,19f)}
            PujiSymbol.Minus -> line(5f,12f,19f,12f)
            PujiSymbol.Search -> {drawCircle(tint,7*u,Offset(10*u,10*u),style=stroke);line(15f,15f,21f,21f)}
            PujiSymbol.ChevronRight -> path(9f,5f,16f,12f,9f,19f)
            PujiSymbol.ChevronLeft -> path(15f,5f,8f,12f,15f,19f)
            PujiSymbol.Inbound, PujiSymbol.Outbound -> {path(4f,12f,4f,20f,20f,20f,20f,12f); if(symbol==PujiSymbol.Inbound){line(12f,3f,12f,15f);path(7f,10f,12f,15f,17f,10f)}else{line(12f,15f,12f,3f);path(7f,8f,12f,3f,17f,8f)}}
            PujiSymbol.List -> for(y in listOf(5f,12f,19f)){line(3f,y,4f,y);line(8f,y,21f,y)}
            PujiSymbol.Keyboard -> {drawRoundRect(tint,Offset(2*u,5*u),Size(20*u,14*u),CornerRadius(2*u),style=stroke);for(x in listOf(6f,10f,14f,18f))line(x,9f,x,9.2f);line(6f,14f,18f,14f)}
            PujiSymbol.Close -> {line(6f,6f,18f,18f);line(18f,6f,6f,18f)}
        }
    }
}
