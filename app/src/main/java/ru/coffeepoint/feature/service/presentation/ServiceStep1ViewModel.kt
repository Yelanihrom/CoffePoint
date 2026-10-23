package ru.coffeepoint.feature.service.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.core.data.repository.SupplierRefillRepository
import ru.coffeepoint.core.data.repository.SupplierRefillSaveResult
import ru.coffeepoint.core.data.repository.WarehouseStockRepository
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncManager
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncMessages
import ru.coffeepoint.feature.service.model.*
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.StationInventory
import ru.coffeepoint.feature.station.model.syncErrorDialogMessage
import ru.coffeepoint.feature.warehouse.model.WarehouseItemType

class ServiceStep1ViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StationRepository(application)
    private val warehouseRepository = WarehouseStockRepository(application)
    private val refillRepository = SupplierRefillRepository(application)
    private var loading = false
    private var supplyFlow = false

    var station by mutableStateOf<StationDetail?>(null)
        private set
    var inventory by mutableStateOf<StationInventory?>(null)
        private set
    var refillLines by mutableStateOf<List<SupplierRefillLine>>(emptyList())
        private set
    var selectedSource by mutableStateOf(RefillSource.Warehouse)
        private set
    var warehouseQuantities by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    var isReview by mutableStateOf(false)
        private set
    var hasSavedLocally by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var showSyncError by mutableStateOf(false)
        private set
    var syncErrorMessage by mutableStateOf("")
        private set
    var warehouseError by mutableStateOf<String?>(null)
        private set

    val shortages: Set<String> get() = warehouseShortages(refillLines.warehouseWriteOff(selectedSource), warehouseQuantities)
    val canProceed: Boolean get() = !isSaving && (hasSavedLocally || !supplyFlow || shortages.isEmpty())
    val displayInventory: StationInventory? get() {
        val original = inventory ?: return null
        if (!supplyFlow) return original
        return refillLines.fold(original) { result, line -> result.withQuantity(line.type, if (isReview) line.actual else line.added) }
    }

    fun loadStation(stationId: Int, isSupplyServiceFlow: Boolean = false) {
        if (station != null || loading) return
        loading = true
        viewModelScope.launch {
            try {
                val detail = repository.getStation(stationId) ?: return@launch
                supplyFlow = isSupplyServiceFlow
                if (supplyFlow) {
                    warehouseQuantities = warehouseRepository.getQuantityByType()
                    refillLines = createSupplierRefillLines(detail.inventory, detail.settings)
                }
                inventory = detail.inventory.copy()
                station = detail
            } finally { loading = false }
        }
    }

    fun changeQuantity(type: WarehouseItemType, delta: Int) {
        if (isSaving || (supplyFlow && hasSavedLocally)) return
        val original = inventory ?: return
        if (supplyFlow) {
            refillLines = refillLines.map { line ->
                if (line.type != type) line else {
                    val current = if (isReview) line.actual else line.added
                    val value = (current.toLong() + delta).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
                    if (isReview) line.withActual(value) else line.withAdded(value)
                }
            }
            warehouseError = null
        } else {
            val maximum = station?.settings?.maximum(type)?.coerceAtLeast(0) ?: return
            val upper = if (delta > 0) maximum.toLong() else Int.MAX_VALUE.toLong()
            val value = (original.quantity(type).toLong() + delta).coerceIn(0, upper).toInt()
            inventory = original.withQuantity(type, value)
        }
    }

    fun selectSource(source: RefillSource) {
        if (isSaving || hasSavedLocally) return
        selectedSource = source
        warehouseError = null
        if (source == RefillSource.Warehouse) viewModelScope.launch {
            warehouseQuantities = warehouseRepository.getQuantityByType()
        }
    }

    fun backToRefill() {
        if (!isSaving && !hasSavedLocally) isReview = false
    }

    fun proceed(syncImmediately: Boolean, onSaved: () -> Unit) {
        if (!canProceed) return
        if (supplyFlow && hasSavedLocally) { onSaved(); return }
        if (supplyFlow && !isReview) {
            isSaving = true
            viewModelScope.launch {
                try {
                    if (selectedSource == RefillSource.Warehouse) warehouseQuantities = warehouseRepository.getQuantityByType()
                    if (shortages.isEmpty()) { isReview = true; warehouseError = null }
                    else warehouseError = "Не хватает остатков на складе"
                } finally { isSaving = false }
            }
        } else saveChanges(syncImmediately, onSaved)
    }

    fun retrySave(syncImmediately: Boolean = false, onSaved: () -> Unit = {}) = saveChanges(syncImmediately, onSaved)

    private fun saveChanges(syncImmediately: Boolean, onSaved: () -> Unit) {
        if (isSaving) return
        val detail = station ?: return
        val original = inventory ?: return
        isSaving = true
        showSyncError = false
        viewModelScope.launch {
            try {
                val stationSaved: Boolean
                var warehouseSaved = true
                var warehouseMessage = ""
                if (supplyFlow) {
                    val writeOff = refillLines.warehouseWriteOff(selectedSource)
                    if (!hasSavedLocally) {
                        when (val result = refillRepository.saveLocally(detail, refillLines.actualInventory(original), writeOff)) {
                            is SupplierRefillSaveResult.Shortage -> {
                                warehouseQuantities = result.quantities
                                warehouseError = "Не хватает остатков на складе"
                                isReview = false
                                return@launch
                            }
                            SupplierRefillSaveResult.Saved -> hasSavedLocally = true
                        }
                        GoogleSheetsSyncManager().enqueueBackgroundSync(getApplication())
                    }
                    stationSaved = repository.syncStationNow(detail.station.id)
                    if (writeOff.isNotEmpty()) {
                        val result = warehouseRepository.syncPendingNow()
                        warehouseSaved = result.synced
                        warehouseMessage = GoogleSheetsSyncMessages.errorMessage(result.errorType)
                    }
                } else {
                    stationSaved = repository.saveStation(detail.copy(inventory = original), syncImmediately = syncImmediately)
                    hasSavedLocally = true
                }
                station = repository.getStation(detail.station.id) ?: detail
                if (stationSaved && warehouseSaved) onSaved()
                else {
                    syncErrorMessage = if (!stationSaved) station?.syncErrorDialogMessage().orEmpty() else warehouseMessage
                    showSyncError = true
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                syncErrorMessage = if (hasSavedLocally) "Не удалось отправить изменения. Повторите синхронизацию."
                    else "Не удалось сохранить изменения. Попробуйте ещё раз."
                showSyncError = true
            } finally { isSaving = false }
        }
    }

    fun dismissSyncError() { showSyncError = false }
}
