package ru.coffeepoint.feature.service.supply.presentation

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
import ru.coffeepoint.feature.service.supply.model.*
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.syncErrorDialogMessage
import ru.coffeepoint.feature.warehouse.model.WarehouseItemType

class SupplyStockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StationRepository(application)
    private val warehouseRepository = WarehouseStockRepository(application)
    private val refillRepository = SupplierRefillRepository(application)
    private var loading = false
    var station by mutableStateOf<StationDetail?>(null)
        private set
    var refillLines by mutableStateOf<List<SupplierRefillLine>>(emptyList())
        private set
    var selectedSource by mutableStateOf(RefillSource.Warehouse)
        private set
    var warehouseQuantities by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    var isRefilling by mutableStateOf(false)
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
    val canProceed: Boolean get() = !isSaving && station != null &&
        (!isRefilling || hasSavedLocally || shortages.isEmpty())

    fun loadStation(stationId: Int) {
        if (station != null || loading) return
        loading = true
        viewModelScope.launch {
            try {
                val detail = repository.getStation(stationId) ?: return@launch
                warehouseQuantities = warehouseRepository.getQuantityByType()
                refillLines = createSupplierRefillLines(detail.inventory, detail.settings)
                station = detail
            } finally { loading = false }
        }
    }

    fun changeQuantity(type: WarehouseItemType, delta: Int) {
        if (isSaving || hasSavedLocally) return
        refillLines = refillLines.map { line ->
            if (line.type != type) line else {
                val current = if (isRefilling) line.added else line.verified
                val value = (current.toLong() + delta).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
                if (isRefilling) line.withAdded(value) else line.withVerified(value)
            }
        }
        warehouseError = null
    }

    fun selectSource(source: RefillSource) {
        if (isSaving || hasSavedLocally) return
        selectedSource = source
        warehouseError = null
        if (source == RefillSource.Warehouse) viewModelScope.launch {
            warehouseQuantities = warehouseRepository.getQuantityByType()
        }
    }

    fun backToVerification() {
        if (!isSaving && !hasSavedLocally) isRefilling = false
    }

    fun proceed(onSaved: () -> Unit) {
        if (!canProceed) return
        if (hasSavedLocally) { onSaved(); return }
        if (!isRefilling) { isRefilling = true; return }
        saveChanges(onSaved)
    }

    fun retrySave(onSaved: () -> Unit) = saveChanges(onSaved)

    private fun saveChanges(onSaved: () -> Unit) {
        if (isSaving || !isRefilling) return
        val detail = station ?: return
        isSaving = true
        showSyncError = false
        viewModelScope.launch {
            try {
                val writeOff = refillLines.warehouseWriteOff(selectedSource)
                if (!hasSavedLocally) {
                    when (val result = refillRepository.saveLocally(detail, refillLines.actualInventory(detail.inventory), writeOff)) {
                        is SupplierRefillSaveResult.Shortage -> {
                            warehouseQuantities = result.quantities
                            warehouseError = "Не хватает остатков на складе"
                            return@launch
                        }
                        SupplierRefillSaveResult.Saved -> hasSavedLocally = true
                    }
                    GoogleSheetsSyncManager().enqueueBackgroundSync(getApplication())
                }
                val stationSaved = repository.syncStationNow(detail.station.id)
                val warehouseResult = if (writeOff.isNotEmpty()) warehouseRepository.syncPendingNow() else null
                station = repository.getStation(detail.station.id) ?: detail
                if (stationSaved && warehouseResult?.synced != false) onSaved() else {
                    syncErrorMessage = if (!stationSaved) station?.syncErrorDialogMessage().orEmpty()
                        else GoogleSheetsSyncMessages.errorMessage(checkNotNull(warehouseResult).errorType)
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
