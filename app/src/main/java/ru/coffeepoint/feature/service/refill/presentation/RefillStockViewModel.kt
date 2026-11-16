package ru.coffeepoint.feature.service.refill.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.feature.service.model.*
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.StationInventory
import ru.coffeepoint.feature.station.model.syncErrorDialogMessage
import ru.coffeepoint.feature.warehouse.model.WarehouseItemType

class RefillStockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StationRepository(application)
    private var loading = false
    var station by mutableStateOf<StationDetail?>(null)
        private set
    var inventory by mutableStateOf<StationInventory?>(null)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var hasSavedLocally by mutableStateOf(false)
        private set
    var showSyncError by mutableStateOf(false)
        private set
    var syncErrorMessage by mutableStateOf("")
        private set

    fun loadStation(stationId: Int) {
        if (station != null || loading) return
        loading = true
        viewModelScope.launch {
            try {
                station = repository.getStation(stationId)
                inventory = station?.inventory?.copy()
            } finally { loading = false }
        }
    }

    fun changeQuantity(type: WarehouseItemType, delta: Int) {
        if (isSaving) return
        val current = inventory ?: return
        val maximum = station?.settings?.maximum(type)?.coerceAtLeast(0) ?: return
        val upper = if (delta > 0) maximum.toLong() else Int.MAX_VALUE.toLong()
        inventory = current.withQuantity(type,
            (current.quantity(type).toLong() + delta).coerceIn(0, upper).toInt())
    }

    fun save(onSaved: () -> Unit) {
        if (isSaving) return
        val detail = station ?: return
        val current = inventory ?: return
        isSaving = true
        showSyncError = false
        viewModelScope.launch {
            try {
                val synced = repository.saveStation(detail.copy(inventory = current), syncImmediately = false)
                hasSavedLocally = true
                station = repository.getStation(detail.station.id) ?: detail
                if (synced) onSaved() else {
                    syncErrorMessage = station?.syncErrorDialogMessage().orEmpty()
                    showSyncError = true
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                syncErrorMessage = "Не удалось сохранить изменения. Попробуйте ещё раз."
                showSyncError = true
            } finally { isSaving = false }
        }
    }

    fun dismissSyncError() { showSyncError = false }
}
