/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Player color extraction system for generating gradients from album artwork
 * 
 * This system analyzes album artwork and extracts vibrant, dominant colors
 * to create visually appealing gradients for the music player interface.
 */
object PlayerColorExtractor {

    /**
     * Extracts colors from a palette and creates a gradient
     * 
     * @param palette The color palette extracted from album artwork
     * @param fallbackColor Fallback color to use if extraction fails
     * @return List of colors for gradient (primary, darker variant, black)
     */
    suspend fun extractGradientColors(
        palette: Palette,
        fallbackColor: Int
    ): List<Color> = withContext(Dispatchers.Default) {
        
        val primaryColor = extractDominantColor(palette, fallbackColor)
        
        // Create sophisticated gradient with 3 color points: rich top to dark bottom
        listOf(
            primaryColor, // Top: same as mini player dominant color
            primaryColor.copy(
                red = (primaryColor.red * 0.4f).coerceAtLeast(0f),
                green = (primaryColor.green * 0.4f).coerceAtLeast(0f),
                blue = (primaryColor.blue * 0.4f).coerceAtLeast(0f)
            ), // Middle: darker variant (used for Mini Player background)
            Color(0xFF0D0D0D) // Bottom: near black
        )
    }

    /**
     * Extracts a muted, blended color for the Mini Player background.
     * Matches the middle color of the full player's gradient for visual cohesion.
     */
    suspend fun extractMiniPlayerColor(
        palette: Palette,
        fallbackColor: Int
    ): Color = withContext(Dispatchers.Default) {
        val primaryColor = extractDominantColor(palette, fallbackColor)
        
        // Match the middle color logic from extractGradientColors (primary * 0.4)
        // and blend it slightly with the surface color for a premium feel
        val darkerVariant = primaryColor.copy(
            red = (primaryColor.red * 0.4f).coerceAtLeast(0f),
            green = (primaryColor.green * 0.4f).coerceAtLeast(0f),
            blue = (primaryColor.blue * 0.4f).coerceAtLeast(0f)
        )
        
        // Blend with #121212 to ensure it doesn't get too dark or too bright
        val surfaceColor = Color(0xFF121212)
        Color(
            red = (darkerVariant.red * 0.7f + surfaceColor.red * 0.3f).coerceIn(0f, 1f),
            green = (darkerVariant.green * 0.7f + surfaceColor.green * 0.3f).coerceIn(0f, 1f),
            blue = (darkerVariant.blue * 0.7f + surfaceColor.blue * 0.3f).coerceIn(0f, 1f),
            alpha = 1f
        )
    }

    /**
     * Extracts a single dominant color for the Mini Player
     * 
     * @param palette The color palette extracted from album artwork
     * @param fallbackColor Fallback color to use if extraction fails
     * @return A single vibrant color suitable for background use
     */
    suspend fun extractDominantColor(
        palette: Palette,
        fallbackColor: Int
    ): Color = withContext(Dispatchers.Default) {
        val dominant = palette.dominantSwatch?.rgb?.let { Color(it) }
            ?: Color(palette.getDominantColor(fallbackColor))
        
        // Ensure the color is not too dark for the mini player's semi-transparent look
        enhanceColorVividness(dominant, 1.2f)
    }

    /**
     * Determines if a color is vibrant enough for use in player UI
     * 
     * @param color The color to analyze
     * @return true if the color has sufficient saturation and brightness
     */
    private fun isColorVibrant(color: Color): Boolean {
        val argb = color.toArgb()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        val saturation = hsv[1] // HSV[1] is saturation
        val brightness = hsv[2] // HSV[2] is brightness
        
        // Color is vibrant if it has sufficient saturation and appropriate brightness
        // Avoid colors that are too dark or too bright
        return saturation > 0.25f && brightness > 0.2f && brightness < 0.9f
    }
    
    /**
     * Enhances color vividness by adjusting saturation and brightness
     * 
     * @param color The color to enhance
     * @param saturationFactor Factor to multiply saturation by (default 1.4)
     * @return Enhanced color with improved vividness
     */
    private fun enhanceColorVividness(color: Color, saturationFactor: Float = 1.4f): Color {
        val argb = color.toArgb()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        
        // Increase saturation for more vivid colors
        hsv[1] = (hsv[1] * saturationFactor).coerceAtMost(1.0f)
        // Adjust brightness for better visibility
        hsv[2] = (hsv[2] * 0.9f).coerceIn(0.4f, 0.85f)
        
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    /**
     * Calculates weight for color selection based on dominance and vibrancy
     * 
     * @param swatch The palette swatch to analyze
     * @return Weight value for color selection priority
     */
    private fun calculateColorWeight(swatch: Palette.Swatch?): Float {
        if (swatch == null) return 0f
        val population = swatch.population.toFloat()
        val color = Color(swatch.rgb)
        val argb = color.toArgb()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        val saturation = hsv[1]
        val brightness = hsv[2]
        
        // Give higher priority to dominance (population) while considering vibrancy
        val populationWeight = population * 2f // Double dominance weight
        val vibrancyBonus = if (saturation > 0.3f && brightness > 0.3f) 1.5f else 1f
        
        return populationWeight * vibrancyBonus * (saturation + brightness) / 2f
    }

    /**
     * Configuration constants for color extraction
     */
    object Config {
        const val MAX_COLOR_COUNT = 32
        const val BITMAP_AREA = 8000
        const val IMAGE_SIZE = 200
        
        // Color enhancement factors
        const val VIBRANT_SATURATION_THRESHOLD = 0.25f
        const val VIBRANT_BRIGHTNESS_MIN = 0.2f
        const val VIBRANT_BRIGHTNESS_MAX = 0.9f
        
        const val POPULATION_WEIGHT_MULTIPLIER = 2f
        const val VIBRANCY_THRESHOLD_SATURATION = 0.3f
        const val VIBRANCY_THRESHOLD_BRIGHTNESS = 0.3f
        const val VIBRANCY_BONUS = 1.5f
        
        const val DEFAULT_SATURATION_FACTOR = 1.4f
        const val VIBRANT_SATURATION_FACTOR = 1.3f
        const val FALLBACK_SATURATION_FACTOR = 1.1f
        
        const val BRIGHTNESS_MULTIPLIER = 0.9f
        const val BRIGHTNESS_MIN = 0.4f
        const val BRIGHTNESS_MAX = 0.85f
        
        const val DARKER_VARIANT_FACTOR = 0.6f
    }
}
