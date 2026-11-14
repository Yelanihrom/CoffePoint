package ru.coffeepoint.feature.service.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.core.data.repository.EmployeeSyncRepository
import ru.coffeepoint.core.data.repository.SupplierServiceCompletionRepository
import ru.coffeepoint.core.data.repository.LocalSupplierUnavailableException
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncManager
import ru.coffeepoint.feature.service.model.SupplyServiceChecklistItem
import ru.coffeepoint.feature.service.model.buildSupplyServiceChecklist
import ru.coffeepoint.feature.service.model.canFinishSupplyService
import ru.coffeepoint.feature.station.model.syncErrorDialogMessage
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class SupplyServiceStep2ViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repository = StationRepository(application)
    private val db = DatabaseProvider.getDatabase(application)
    private val completionRepository = SupplierServiceCompletionRepository(db)
    private val completionId = savedStateHandle.get<String>("completionId")
        ?: UUID.randomUUID().toString().also { savedStateHandle["completionId"] = it }
    private val locallyCompleted: Boolean get() = savedStateHandle["locallyCompleted"] ?: false

    var localSaveError by mutableStateOf<String?>(null)
        private set

    var checklist by mutableStateOf<List<SupplyServiceChecklistItem>>(emptyList())
        private set

    private var isLoaded = false
    private var isLoading = false

    var isSaving by mutableStateOf(false)
        private set

    var showSyncError by mutableStateOf(false)
        private set

    var syncErrorMessage by mutableStateOf("Проверьте подключение к интернету или VPN")
        private set

    fun loadStation(stationId: Int) {

        if (isLoaded || isLoading) return
        isLoading = true

        viewModelScope.launch {
            try {
                repository.getStation(stationId) ?: return@launch
                val checkedTasks = savedStateHandle.get<ArrayList<String>>("checkedTasks").orEmpty()
                checklist = buildSupplyServiceChecklist().map { item ->
                    item.copy(checklistItem = item.checklistItem.copy(
                        checked = locallyCompleted || item.task.name in checkedTasks
                    ))
                }
                isLoaded = true
            } finally {
                isLoading = false
            }
        }

    }

    fun toggleChecklist(index: Int, checked: Boolean) {
        if (isSaving || locallyCompleted) return
        checklist = checklist.toMutableList().also { items ->
            val current = items[index]
            items[index] = current.copy(
                checklistItem = current.checklistItem.copy(
                    checked = checked
                )
            )
        }
        savedStateHandle["checkedTasks"] = ArrayList(checklist.filter { it.checklistItem.checked }.map { it.task.name })
    }

    fun canFinish(): Boolean {
        return !isSaving && checklist.canFinishSupplyService()
    }

    fun finishSupplyService(
        stationId: Int,
        role: UserRole,
        onFinished: () -> Unit
    ) {

        if (isSaving || (!locallyCompleted && !canFinish())) return

        viewModelScope.launch {
            isSaving = true
            showSyncError = false
            localSaveError = null
            try {
                if (!locallyCompleted) {
                    if (role == UserRole.SUPPLY_OPERATOR &&
                        db.employeeDao().getActiveByRole(role.name).singleOrNull() == null) {
                        EmployeeSyncRepository(getApplication(), db).refreshEmployeeDataFromGoogleSheets()
                    }
                    completionRepository.completeLocally(completionId, stationId, role)
                    savedStateHandle["locallyCompleted"] = true
                }
                GoogleSheetsSyncManager().enqueueBackgroundSync(getApplication())
                val synced = repository.syncSupplyServiceNow(stationId)
                val financeResult = if (role == UserRole.SUPPLY_OPERATOR)
                    EmployeeSyncRepository(getApplication()).syncSupplierVisits() else null
                if (synced && financeResult?.synced != false) {
                    onFinished()
                } else {
                    syncErrorMessage = if (synced) "Визит и начисление сохранены на устройстве и будут отправлены автоматически"
                        else repository.getStation(stationId)?.syncErrorDialogMessage()
                        ?: "Проверьте подключение к интернету или VPN"
                    showSyncError = true
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                if (locallyCompleted) {
                    syncErrorMessage = "Не удалось отправить изменения"
                    showSyncError = true
                } else {
                    localSaveError = if (error is LocalSupplierUnavailableException)
                        "Не удалось определить снабженца" else "Не удалось сохранить обслуживание на устройстве"
                }
            } finally {
                isSaving = false
            }
        }

    }

    fun dismissSyncError() {
        showSyncError = false
    }

    fun dismissLocalSaveError() { localSaveError = null }

}
