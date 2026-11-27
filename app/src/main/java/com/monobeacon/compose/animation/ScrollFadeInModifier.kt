package com.monobeacon.compose.animation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyColumnItem
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.intDP
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.textUnit
import androidx.compose.ui.unit.textUnitpx
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp.toPx
import androidx.compose.animation.core.alphaAnim
import androidx.compose.animation.core animateFloatAsState
import androidx.compose.animation.core tween
import androidx.compose.animation.core FastOutSlowInEasing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState

/**
 * Custom reusable Compose modifier that provides scroll-based fade-in and upward translation.
 * Each element fades in (alpha: 0f -> 1f) and translates upward (translationY: 24f -> 0f)
 * as it enters the viewport during scrolling. Keep all transitions crisp (300ms, FastOutSlowInEasing).
 */
val Modifier.scrollFadeIn: (listState: androidx.compose.foundation.lazy.ListState, index: Int) -> Modifier = animateScrollFadeIn(listState, index = index)

@Composable
fun animateScrollFadeIn(
    listState: androidx.compose.foundation.lazy.ListState,
    index: Int,
    animationSpec: AnimationSpec<Float> = tween(300, FastOutSlowInEasing)
): Modifier {
    var progress by rememberSaverable(seed = index) { 0f }
    val rememberedListState = remember(listState)

    emittedKey(listState.currentIndex) {
        val firstVisibleItemIndex = rememberedListState.firstVisibleIndex
        val lastVisibleItemIndex = rememberedListState.lastVisibleIndex
        val visibleRange = firstVisibleItemIndex - 1 to lastVisibleItemIndex + 1

        if (index in visibleRange) {
            val itemTop = rememberedListState.layoutInfo.firstItemOffset(index)
            val viewportHeight = rememberedListState.viewportSize
            var itemBottom = itemTop + rememberedListState.layoutInfo.itemHeight(index)

            val viewportStart = 0f
            val viewportEnd = viewportHeight

            var visibleHeight = 0f
            if (itemTop < viewportEnd && itemBottom > viewportStart) {
                visibleHeight = mathMin(itemBottom, viewportEnd) - mathMax(itemTop, viewportStart)
            }

            val itemHeight = rememberedListState.layoutInfo.itemHeight(index).coerceAtLeast(1f)
            val normalized = (visibleHeight / itemHeight).coerceAtMost(1f).coerceAtLeast(0f)

            // Smooth animation with easing
            progress = animatedFloat(
                targetValue = normalized,
                animationSpec = animationSpec
            ).currentValue
        }
    }

    return Modifier
        .alpha(
            lookUpProgress(progress) { 0f } apply { it }
        )
        .translationY(
            lookUpProgress(progress) { 24f } apply { it }
        )
}

/**
 * Extension property for lookUpProgress that provides backward compatibility
 * with rememberSaverable API
 */
private expect lookUpProgress : (Float) -> Float
private actual lookUpProgress : (Float) -> Float = identity

// Actual implementation using remember and Animate* APIs
@Composable
fun animateScrollFadeInActual(
    listState: androidx.compose.foundation.lazy.ListState,
    index: Int,
    animationSpec: AnimationSpec<Float> = tween(300, FastOutSlowInEasing)
) {
    var progress by remember { derivedStateOf {
        val firstVisibleItemIndex = listState.firstVisibleIndex
        val lastVisibleItemIndex = listState.lastVisibleIndex
        val visibleRange = firstVisibleItemIndex - 1 to lastVisibleItemIndex + 1

        if (index in visibleRange) {
            val itemTop = listState.layoutInfo.firstItemOffset(index)
            val viewportHeight = listState.viewportSize
            var itemBottom = itemTop + listState.layoutInfo.itemHeight(index)

            val viewportStart = 0f
            val viewportEnd = viewportHeight

            var visibleHeight = 0f
            if (itemTop < viewportEnd && itemBottom > viewportStart) {
                visibleHeight = mathMin(itemBottom, viewportEnd) - mathMax(itemTop, viewportStart)
            }

            val itemHeight = listState.layoutInfo.itemHeight(index).coerceAtLeast(1f)
            val normalized = (visibleHeight / itemHeight).coerceAtMost(1f).coerceAtLeast(0f)
            normalized
        } else {
            0f
        }
    }()

    // Apply the modifier
    androidx.compose.ui.unit.AlphaCompositeInstance
}
