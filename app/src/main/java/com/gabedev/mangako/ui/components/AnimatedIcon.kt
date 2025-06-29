package com.gabedev.mangako.ui.components

import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun AnimatedIcon(
    isSelected: Boolean,
    @DrawableRes animatedIconRes: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val drawable = remember(animatedIconRes) {
        AppCompatResources.getDrawable(context, animatedIconRes)?.mutate()
    }

    AndroidView(
        modifier = modifier.size(24.dp),
        factory = {
            ImageView(it).apply {
                setImageDrawable(drawable)
            }
        },
        update = { imageView ->
            val stateArray = if (isSelected) {
                intArrayOf(android.R.attr.state_checked)
            } else {
                intArrayOf(-android.R.attr.state_checked)
            }
            imageView.setImageState(stateArray, true)
        }
    )
}