package com.example.multiaccountmail.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val title: String) {
    SYSTEM("Как в системе"),
    LIGHT("Светлая"),
    DARK("Тёмная")
}

enum class Accent(val title: String, val color: Color) {
    BLUE("Синий", Color(0xFF1565C0)),
    TEAL("Бирюза", Color(0xFF00897B)),
    GREEN("Зелёный", Color(0xFF2E7D32)),
    ORANGE("Оранжевый", Color(0xFFEF6C00)),
    RED("Красный", Color(0xFFC62828)),
    PURPLE("Фиолетовый", Color(0xFF6A1B9A)),
    PINK("Розовый", Color(0xFFAD1457)),
    DYNAMIC("Цвет системы", Color(0xFF6750A4))
}

enum class UiDensity(val title: String) {
    COMPACT("Компактно"),
    COMFORTABLE("Обычно"),
    SPACIOUS("Просторно")
}

enum class FontScale(val title: String, val value: Float) {
    SMALL("Мелкий", 0.9f),
    NORMAL("Обычный", 1f),
    LARGE("Крупный", 1.15f),
    HUGE("Очень крупный", 1.3f)
}

enum class BarStyle(val title: String) {
    PILL("Пилюля"),
    BAR("Полоса")
}

/** Только оформление поверхностей: не влияет на данные или работу почты. */
enum class SurfaceStyle(val title: String) {
    MINIMAL("Минимальный"),
    SOFT("Мягкий"),
    CONTRAST("Контрастный")
}

enum class AccentStrength(val title: String, val containerAlpha: Float) {
    SOFT("Нежный", 0.14f),
    NORMAL("Обычный", 0.24f),
    VIVID("Яркий", 0.38f)
}

enum class MotionStyle(val title: String, val stiffness: Float) {
    CALM("Спокойно", 900f),
    SMOOTH("Плавно", 500f),
    LIVELY("Живо", 280f)
}

data class Appearance(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val accent: Accent = Accent.DYNAMIC,
    val density: UiDensity = UiDensity.COMFORTABLE,
    val fontScale: FontScale = FontScale.NORMAL,
    val barStyle: BarStyle = BarStyle.PILL,
    val amoled: Boolean = false,
    val roundCards: Boolean = true,
    val surfaceStyle: SurfaceStyle = SurfaceStyle.SOFT,
    val accentStrength: AccentStrength = AccentStrength.NORMAL,
    val motionStyle: MotionStyle = MotionStyle.SMOOTH,
    val showLabels: Boolean = false,
    val fabLeft: Boolean = false,
    val showAvatars: Boolean = true,
    val showSnippets: Boolean = true
)

object AppearanceStore {
    private const val PREFS = "appearance"
    private val _state = MutableStateFlow(Appearance())
    val state: StateFlow<Appearance> = _state.asStateFlow()

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _state.value = Appearance(
            mode = enumOr(p.getString("mode", null), ThemeMode.SYSTEM),
            accent = enumOr(p.getString("accent", null), Accent.DYNAMIC),
            density = enumOr(p.getString("density", null), UiDensity.COMFORTABLE),
            fontScale = enumOr(p.getString("font", null), FontScale.NORMAL),
            barStyle = enumOr(p.getString("bar", null), BarStyle.PILL),
            amoled = p.getBoolean("amoled", false),
            roundCards = p.getBoolean("round", true),
            surfaceStyle = enumOr(p.getString("surfaceStyle", null), SurfaceStyle.SOFT),
            accentStrength = enumOr(p.getString("accentStrength", null), AccentStrength.NORMAL),
            motionStyle = enumOr(p.getString("motionStyle", null), MotionStyle.SMOOTH),
            showLabels = p.getBoolean("labels", false),
            fabLeft = p.getBoolean("fabLeft", false),
            showAvatars = p.getBoolean("showAvatars", true),
            showSnippets = p.getBoolean("showSnippets", true)
        )
    }

    fun update(context: Context, appearance: Appearance) {
        _state.value = appearance
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("mode", appearance.mode.name)
            .putString("accent", appearance.accent.name)
            .putString("density", appearance.density.name)
            .putString("font", appearance.fontScale.name)
            .putString("bar", appearance.barStyle.name)
            .putBoolean("amoled", appearance.amoled)
            .putBoolean("round", appearance.roundCards)
            .putString("surfaceStyle", appearance.surfaceStyle.name)
            .putString("accentStrength", appearance.accentStrength.name)
            .putString("motionStyle", appearance.motionStyle.name)
            .remove("liquidGlass")
            .putBoolean("labels", appearance.showLabels)
            .putBoolean("fabLeft", appearance.fabLeft)
            .remove("loadingStyle")
            .putBoolean("showAvatars", appearance.showAvatars)
            .putBoolean("showSnippets", appearance.showSnippets)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T {
        if (name == null) return default
        return runCatching { enumValueOf<T>(name) }.getOrDefault(default)
    }
}

fun Color.darken(factor: Float = 0.72f): Color = Color(
    red = red * factor,
    green = green * factor,
    blue = blue * factor,
    alpha = alpha
)

fun Color.onAccent(): Color {
    val luminance = 0.299f * red + 0.587f * green + 0.114f * blue
    return if (luminance > 0.55f) Color(0xFF1A1C1E) else Color.White
}
