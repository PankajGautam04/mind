package com.mind.app.core.model

/**
 * User preferences persisted locally via Jetpack DataStore.
 */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val accentPalette: AccentPalette = AccentPalette.NEURAL_INDIGO,
    val layoutMode: LayoutMode = LayoutMode.GRID,
    val animationStyle: AnimationStyle = AnimationStyle.COMPANION_WAVE,
    val animationsEnabled: Boolean = true,
    val floatingCaptureEnabled: Boolean = false,
    val captureConfirmationEnabled: Boolean = true,
    val cloudAiEnabled: Boolean = false,
    val localOnlyMode: Boolean = true,
    val autoCategorizationEnabled: Boolean = true,
    val aiLanguageTag: String = "en",
    val geminiModel: String = DEFAULT_GEMINI_MODEL,
    val customBackendUrl: String = "",
    val hasGeminiApiKey: Boolean = false,
    val assistantAction: AssistantAction = AssistantAction.ASK_EVERY_TIME,
    val onboardingCompleted: Boolean = false,
) {
    companion object {
        const val DEFAULT_GEMINI_MODEL = "gemini-3.8-flash"
    }
}

enum class AssistantAction(val displayName: String, val summary: String) {
    ASK_EVERY_TIME(
        "Ask Every Time (Assistant Menu)",
        "Show a compact picker to Save Screen, Quick Note, Record Voice, or Ask Mind",
    ),
    CAPTURE_SCREEN(
        "Instant Screen Capture",
        "Save the visible screen silently with zero extra taps",
    ),
    VOICE_MEMORY(
        "Record Voice Memory",
        "Start recording a local voice memo right away",
    ),
    QUICK_NOTE(
        "Quick Note or Link",
        "Open Mind's quick note or webpage bookmark sheet",
    ),
    OPEN_SEARCH(
        "Ask Mind (AI Search)",
        "Open Mind straight to AI search and Q&A",
    ),
}

enum class ThemeMode {
    LIGHT,
    DARK,
    FOLLOW_SYSTEM,
}

enum class AccentPalette(val displayName: String) {
    NEURAL_INDIGO("Mist Blue"),
    BIOLUMINESCENT_TEAL("Ocean Blue"),
    SKY_AZURE("Sky Azure"),
    ELECTRIC_CYAN("Electric Cyan"),
    SAGE_MEMORY("Turquoise"),
    EMERALD_MIST("Emerald"),
    JADE_MINT("Jade Mint"),
    SPRING_LIME("Spring Lime"),
    CITRUS_SOL("Solar Gold"),
    AMBER_RECALL("Warm Amber"),
    TANGERINE_GLOW("Tangerine"),
    SUNSET_CORAL("Sunset Coral"),
    CRIMSON_PULSE("Crimson"),
    CHERRY_BLOSSOM("Cherry Rose"),
    HOT_MAGENTA("Magenta"),
    ORCHID_BLOOM("Orchid"),
    ROSE_QUARTZ("Dusk Lavender"),
    ROYAL_VIOLET("Royal Violet"),
    COSMIC_PURPLE("Cosmic Purple"),
    COBALT_NOVA("Cobalt"),
    ARCTIC_ICE("Arctic Ice"),
    GRAPHITE_SLATE("Graphite"),
    TITANIUM_MIST("Titanium"),
    ROSE_GOLD("Rose Gold"),
}

enum class LayoutMode {
    GRID,
    LIST,
}

enum class AnimationStyle(val displayName: String, val summary: String) {
    COMPANION_WAVE("Wave Lens · Bottom ↑", "Chromatic glass distortion wave rising smoothly from bottom"),
    COMPANION_WAVE_TOP_DOWN("Wave Lens · Top ↓", "Chromatic glass distortion wave cascading smoothly from top"),
    SEA_WAVE("Sea Wave", "Freezes your screen as calm waves roll gently from top to bottom"),
    RIPPLE("Edge Aura", "Iridescent screen-edge glow sweeping into a floating preview pill"),
    PORTAL("Vortex Capsule", "Luminous beam condensing captured content into the Mind dock"),
    MORPH("Card Glide", "Crisp live preview glides smoothly into the memory capsule"),
    PARTICLE("Starlight Bokeh", "Layered light motes spiral gracefully toward Mind"),
    LIQUID("Aurora Ribbon", "Dual translucent ambient ribbons along screen edges"),
    MINIMAL("Crisp Edge", "Razor-thin shutter flash with animated check pill"),
}
