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

enum class LoadingStyle(val title: String) {
    CIRCLE("Круг"),
    LINE("Полоса"),
    DOTS("Точки")
}

data class Appearance(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val accent: Accent = Accent.DYNAMIC,
    val density: UiDensity = UiDensity.COMFORTABLE,
    val fontScale: FontScale = FontScale.NORMAL,
    val barStyle: BarStyle = BarStyle.PILL,
    val amoled: Boolean = false,
    val roundCards: Boolean = true,
    val liquidGlass: Boolean = false,
    val showLabels: Boolean = false,
    val fabLeft: Boolean = false,
    val loadingStyle: LoadingStyle = LoadingStyle.CIRCLE,
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
            liquidGlass = p.getBoolean("liquidGlass", false),
            showLabels = p.getBoolean("labels", false),
            fabLeft = p.getBoolean("fabLeft", false),
            loadingStyle = enumOr(p.getString("loadingStyle", null), LoadingStyle.CIRCLE),
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
            .putBoolean("liquidGlass", appearance.liquidGlass)
            .putBoolean("labels", appearance.showLabels)
            .putBoolean("fabLeft", appearance.fabLeft)
            .putString("loadingStyle", appearance.loadingStyle.name)
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
