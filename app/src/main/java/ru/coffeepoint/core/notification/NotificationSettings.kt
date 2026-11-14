package ru.coffeepoint.core.notification

import android.content.Context
import android.content.SharedPreferences
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import ru.coffeepoint.core.auth.UserRole

enum class NotificationOption(val title: String, val nodeTitle: String? = null) {
    REFILL("Станции требуют заправки"),
    CRITICAL_STOCK("Критичные остатки станций"),
    WAREHOUSE("Нужно пополнить склад"),
    MIXERS("Миксеры", "Миксеры"),
    CONTAINER("Контейнер жидких отходов", "Контейнер жидких отходов"),
    BREW_UNIT("УЗК", "Узел заваривания кофе"),
    EXHAUST("Вытяжка", "Вытяжка"),
    TANK("Расширительный бачок", "Расширительный бочок"),
    GRINDER("Кофемолка", "Кофемолка");

    val sheetField: String get() = when (this) {
        REFILL -> "refillEnabled"
        CRITICAL_STOCK -> "criticalStockEnabled"
        WAREHOUSE -> "warehouseEnabled"
        MIXERS -> "mixersEnabled"
        CONTAINER -> "liquidWasteContainerEnabled"
        BREW_UNIT -> "brewUnitEnabled"
        EXHAUST -> "exhaustEnabled"
        TANK -> "expansionTankEnabled"
        GRINDER -> "coffeeGrinderEnabled"
    }

    fun availableFor(role: UserRole): Boolean = when {
        this == REFILL -> role != UserRole.SUPPLY_OPERATOR
        else -> role != UserRole.OPERATOR
    }

    fun defaultEnabled(role: UserRole): Boolean = availableFor(role) && when (this) {
        REFILL, CRITICAL_STOCK -> true
        WAREHOUSE, MIXERS, CONTAINER -> role == UserRole.MANAGER
        else -> false
    }
}

data class NotificationSettingsRow(
    val role: UserRole,
    val values: Map<NotificationOption, Boolean>,
    val updatedAt: String
)

class NotificationSettings(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(context.applicationContext
        .getSharedPreferences("notification_settings", Context.MODE_PRIVATE))

    fun changes() = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(Unit) }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(Unit)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun isEnabled(role: UserRole?, option: NotificationOption): Boolean = synchronized(lock) {
        role != null && option.availableFor(role) &&
            preferences.getBoolean("${role.name}.${option.name}", option.defaultEnabled(role))
    }

    fun setEnabled(role: UserRole, option: NotificationOption, enabled: Boolean) {
        synchronized(lock) {
            if (option.availableFor(role) && isEnabled(role, option) != enabled) {
                check(preferences.edit().putBoolean("${role.name}.${option.name}", enabled)
                    .putBoolean(dirtyKey(role, option), true)
                    .putString("${role.name}.updatedAt", nextTimestamp(preferences.getString("${role.name}.updatedAt", null)))
                    .commit()) { "Cannot save notification settings" }
            }
        }
    }

    fun pending(): List<NotificationSettingsRow> = synchronized(lock) {
        UserRole.entries.mapNotNull { role ->
            val fields = NotificationOption.entries.filter { preferences.getBoolean(dirtyKey(role, it), false) }
                .associateWith { isEnabled(role, it) }
            if (fields.isEmpty()) null else NotificationSettingsRow(role, fields,
                checkNotNull(preferences.getString("${role.name}.updatedAt", null)))
        }
    }

    fun importRows(rows: List<NotificationSettingsRow>) = synchronized(lock) {
        val editor = preferences.edit()
        rows.forEach { row ->
            val remote = Instant.parse(row.updatedAt)
            val lastRemote = preferences.getString("${row.role.name}.remoteUpdatedAt", null)?.let(Instant::parse)
            if (lastRemote != null && remote < lastRemote) return@forEach
            row.values.forEach { (option, value) ->
                if (!preferences.getBoolean(dirtyKey(row.role, option), false)) {
                    editor.putBoolean("${row.role.name}.${option.name}", value)
                }
            }
            editor.putString("${row.role.name}.remoteUpdatedAt", row.updatedAt)
            val local = preferences.getString("${row.role.name}.updatedAt", null)?.let(Instant::parse)
            if (local == null || remote >= local) {
                editor.putString("${row.role.name}.updatedAt", nextTimestamp(row.updatedAt))
            }
        }
        check(editor.commit()) { "Cannot import notification settings" }
    }

    fun acknowledge(row: NotificationSettingsRow) = synchronized(lock) {
        if (preferences.getString("${row.role.name}.updatedAt", null) != row.updatedAt) return@synchronized
        val editor = preferences.edit().putString("${row.role.name}.remoteUpdatedAt", row.updatedAt)
        row.values.keys.forEach { editor.remove(dirtyKey(row.role, it)) }
        check(editor.commit()) { "Cannot acknowledge notification settings" }
    }

    private fun nextTimestamp(previous: String?): String {
        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val prior = previous?.let(Instant::parse)
        return (if (prior != null && prior >= now) prior.plusMillis(1) else now).toString()
    }

    private fun dirtyKey(role: UserRole, option: NotificationOption) = "${role.name}.pending.${option.name}"

    private companion object { val lock = Any() }
}
