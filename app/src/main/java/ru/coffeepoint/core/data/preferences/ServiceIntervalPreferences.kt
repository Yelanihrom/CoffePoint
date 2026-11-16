package ru.coffeepoint.core.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import ru.coffeepoint.feature.station.model.ServiceIntervals
import ru.coffeepoint.feature.station.model.TechServiceNode

class ServiceIntervalPreferences(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(context.applicationContext.getSharedPreferences("service_intervals", Context.MODE_PRIVATE))

    fun read() = ServiceIntervals(TechServiceNode.entries.associateWith { node ->
        preferences.getInt(node.name, node.defaultIntervalDays).takeIf { it > 0 } ?: node.defaultIntervalDays
    })

    fun save(intervals: ServiceIntervals) {
        check(preferences.edit().apply {
            TechServiceNode.entries.forEach { putInt(it.name, intervals[it]) }
        }.commit()) { "Unable to save service intervals" }
    }

    fun observe() = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()
}
