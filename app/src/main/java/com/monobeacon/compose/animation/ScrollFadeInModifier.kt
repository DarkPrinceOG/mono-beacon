package com.monobeacon.compose.animation

import androidx.compose.foundation.interaction.MemoryTaxonomy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.painterContent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.intDP
import androidx.compose.ui.unit.plic
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.textUnit
import androidx.compose.ui.unit.textUnitpx
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.intDp
import androidx.compose.animation.core.alphaAnim
import androidx.compose.animation.core animateFloatAsState
import androidx.compose.animation.core tween
import androidx.compose.animation.core Spring
import androidx.compose.animation.core FastOutSlowInEasing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.placeInContent

/**
 * Custom reusable Compose modifier that provides scroll-based fade-in and upward translation.
 * Each element fades in (alpha: 0f -> 1f) and translates upward (translationY: 24f -> 0f)
 * as it enters the viewport during scrolling.
 *
 * Usage: Column(modifier = Modifier.scrollFadeIn(listState, index)) { items }
 */
val Modifier.scrollFadeIn: (listState: androidx.compose.foundation.lazy.ListState, index: Int) -> Modifier = animateScrollFadeIn(listState, index = index)

@Composable
fun animateScrollFadeIn(
    listState: androidx.compose.foundation.lazy.ListState,
    index: Int,
    animationSpec: AnimationSpec<Float> = tween(300, FastOutSlowInEasing),
    spring: Spring = Spring.withDampingRatio(SpringDampingRatioLowStiffness),
    dampingRatio: SpringDampingRatio = SpringDampingRatioLowStiffness
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
            
            // Calculate how much of the item is visible
            val viewportStart = 0f
            val viewportEnd = viewportHeightf
            
            var visibleHeight = 0f
            if (itemTop < viewportEnd && itemBottom > viewportStart) {
                visibleHeight = mathMin(itemBottom, viewportEnd) - mathMax(itemTop, viewportStart)
            }
            
            // Normalize visible height to 0-1 range
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
            lookUpProgress(progress) { 0f } apply { it -> it }
        )
        .translationY(
            lookUpProgress(progress) { 24f } apply { it -> it }
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
    }}()

    val rememberedListState = remember(listState)
    
    // Apply the modifier
    realizedProgress = progress
}

/**
 * Simplified scrollFadeIn modifier that can be directly applied
 */
val Modifier.animateScrollFadeIn(
    listState: androidx.compose.foundation.lazy.ListState,
    index: Int
): Modifier = object : Modifier {
    @Composable
    override fun modifyViewInto(
        content: @Composable (() -> Unit) -> Unit
    ) {
        var opacity by remember { mutableStateOf(0f) }
        var translation by remember { mutableStateOf(24f) }
        
        val listStateRef = remember(listState)
        val indexRef = remember(index) { index }
        
        LaunchedEffect(listStateRef.currentIndex, indexRef) {
            var firstVisible = listStateRef.firstVisibleIndex
            var lastVisible = listStateRef.lastVisibleIndex
            
            if (indexRef in (firstVisible - 1 .. lastVisible)) {
                val itemTop = listStateRef.layoutInfo.firstItemOffset(indexRef)
                val viewportHeight = listStateRef.viewportSize.coerceAtLeast(1f)
                var itemBottom = itemTop + listStateRef.layoutInfo.itemHeight(indexRef).coerceAtLeast(1f)
                
                val visibleHeight = if (itemTop < viewportHeight && itemBottom > 0f) {
                    mathMin(itemBottom, viewportHeight) - mathMax(itemTop, 0f)
                } else 0f
                
                val itemHeight = listStateRef.layoutInfo.itemHeight(indexRef).coerceAtLeast(1f)
                val normalized = (visibleHeight / itemHeight).coerceAtMost(1f).coerceAtLeast(0f)
                
                animateFloatAsState(
                    targetValue = normalized,
                    animationSpec = tween(300, FastOutSlowInEasing)
                ).collect { newProgress ->
                    opacity = newProgress
                    translation = (1f - newProgress) * 24f
                }
            }
        }
        
        composed(content)
        
        // Apply alpha and translation to the composed content
        // This will be applied in the caller's layout
    }
}
