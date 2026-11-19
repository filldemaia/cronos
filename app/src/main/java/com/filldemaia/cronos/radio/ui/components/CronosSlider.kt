package com.filldemaia.cronos.radio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.filldemaia.cronos.radio.ui.theme.RadioOrange
import com.filldemaia.cronos.radio.ui.theme.RadioSurface
import com.filldemaia.cronos.radio.ui.theme.RadioSurfaceHighest

/**
 * Lliscant uniforme i prim de Cronos: una guia fina de 2 dp amb un botó
 * petit, compartit pel rellotge i les mides dels widgets. Els passos
 * segueixen sent discrets, però sense marques gruixudes que embrutin la guia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CronosSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val active = RadioOrange
    val inactive = RadioSurfaceHighest
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = SliderDefaults.colors(
            thumbColor = active,
            activeTrackColor = active,
            inactiveTrackColor = inactive,
            activeTickColor = active,
            inactiveTickColor = inactive,
        ),
        thumb = {
            Box(
                Modifier
                    .size(14.dp)
                    .shadow(4.dp, CircleShape, clip = false)
                    .background(active, CircleShape)
                    .border(2.dp, RadioSurface, CircleShape)
            )
        },
        track = { sliderState ->
            val fraction = sliderState.coercedValueAsFraction.coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .drawBehind {
                        val radius = size.height / 2f
                        drawRoundRect(
                            color = inactive,
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(radius, radius),
                        )
                        drawRoundRect(
                            color = active,
                            size = Size(size.width * fraction, size.height),
                            cornerRadius = CornerRadius(radius, radius),
                        )
                    }
            )
        }
    )
}
