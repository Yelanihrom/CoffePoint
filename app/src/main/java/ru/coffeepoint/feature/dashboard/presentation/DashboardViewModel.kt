package ru.coffeepoint.feature.dashboard.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.local.entity.SupplyCatalogEntity
import ru.coffeepoint.core.data.preferences.GoogleSheetsImportPreferences
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.core.data.repository.SupplyCatalogRepository
import ru.coffeepoint.core.data.repository.WarehouseStockRepository
import ru.coffeepoint.core.sync.google.GoogleSheetsImportResult
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.warehouse.model.WarehouseStockItem

class DashboardViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = StationRepository(application)
    private val googleSheetsImportPreferences = GoogleSheetsImportPreferences(application)
    private val warehouseStockRepository = WarehouseStockRepository(application)
    private val supplyCatalogRepository = SupplyCatalogRepository(application)

    var stations by mutableStateOf<List<StationDetail>>(emptyList())
        private set

    var warehouseStock by mutableStateOf<List<WarehouseStockItem>>(emptyList())
        private set

    var supplyCatalogItems by mutableStateOf<List<SupplyCatalogEntity>>(emptyList())
        private set

    var showGoogleSheetsSyncError by mutableStateOf(false)
        private set

    var showImportSuccess by mutableStateOf(false)
        private set

    var isRefreshingData by mutableStateOf(false)
        private set

    var lastGoogleSheetsImportText by mutableStateOf<String?>(null)
        private set

    fun loadStations() {

        viewModelScope.launch {

            stations = repository.getStations()
            warehouseStock = warehouseStockRepository.getItems()
            supplyCatalogItems = supplyCatalogRepository.getItems()
            lastGoogleSheetsImportText =
                googleSheetsImportPreferences.getLastImportText()

            val imported = repository.importFromGoogleSheets()

            if (imported) {
                stations = repository.getStations()
                warehouseStock = warehouseStockRepository.getItems()
                supplyCatalogItems = supplyCatalogRepository.getItems()
                lastGoogleSheetsImportText =
                    googleSheetsImportPreferences.getLastImportText()
            }

        }

    }

    fun refreshData() {

        if (isRefreshingData) return

        viewModelScope.launch {

            isRefreshingData = true
            showGoogleSheetsSyncError = false
            showImportSuccess = false

            try {

                when (importFromGoogleSheetsWithPendingSync()) {

                    GoogleSheetsImportResult.Success -> {
                        stations = repository.getStations()
                        warehouseStock = warehouseStockRepository.getItems()
                        supplyCatalogItems = supplyCatalogRepository.getItems()
                        lastGoogleSheetsImportText =
                            googleSheetsImportPreferences.getLastImportText()
                        showImportSuccess = true
                    }

                    GoogleSheetsImportResult.SkippedPendingChanges,
                    GoogleSheetsImportResult.Empty,
                    is GoogleSheetsImportResult.Error -> {
                        showGoogleSheetsSyncError = true
                        return@launch
                    }

                }

            } finally {
                isRefreshingData = false
            }

        }

    }

    private suspend fun importFromGoogleSheetsWithPendingSync(): GoogleSheetsImportResult {

        val firstResult = repository.importFromGoogleSheetsResult()

        if (firstResult != GoogleSheetsImportResult.SkippedPendingChanges) {
            return firstResult
        }

        repository.syncPendingGoogleSheets()
        warehouseStockRepository.syncPendingGoogleSheets()

        repository.resolveConflictBlocksForImport()

        return repository.importFromGoogleSheetsResult()

    }

    fun dismissGoogleSheetsSyncError() {

        showGoogleSheetsSyncError = false

    }

    fun dismissImportSuccess() {

        showImportSuccess = false

    }

}
