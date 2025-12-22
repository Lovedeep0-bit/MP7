package com.lsj.mp7.ui.extensions

import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun Modifier.offsetByProgress(progress: Float, totalWidth: Dp): Modifier {
    val density = LocalDensity.current
    return this.offset { 
        IntOffset(
            x = with(density) { (progress * totalWidth.toPx()).roundToInt() },
            y = 0
        )
    }
}

@Composable
fun Modifier.offsetByProgressPx(progress: Float, totalWidthPx: Int): Modifier {
    return this.offset { 
        IntOffset(
            x = (progress * totalWidthPx).roundToInt(),
            y = 0
        )
    }
}
