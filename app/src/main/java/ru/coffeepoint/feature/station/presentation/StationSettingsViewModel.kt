package ru.coffeepoint.feature.station.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.local.entity.EmployeeEntity
import ru.coffeepoint.core.data.local.entity.EmployeeStationAssignmentEntity
import ru.coffeepoint.core.data.remote.repository.TerminalCommandRepository
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.core.util.normalizeTime
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.syncErrorDialogMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class StationSettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val stationRepository = StationRepository(application)
    private val commandRepository = TerminalCommandRepository()
    private val db = DatabaseProvider.getDatabase(application)
    private val employeeDao = db.employeeDao()
    private val employeeStationAssignmentDao = db.employeeStationAssignmentDao()

    private var loadedStationId: Int? = null
    private var loadedEmployeeAssignmentId: Int? = null

    var station by mutableStateOf<StationDetail?>(null)
        private set

    var stationName by mutableStateOf("")
        private set

    var stationNameError by mutableStateOf(false)
        private set

    var terminalId by mutableStateOf("")
        private set

    var address by mutableStateOf("")
        private set

    var visitDays by mutableStateOf("1,2,3,4,5")
        private set

    var visitTimeFrom by mutableStateOf("09:00")
        private set

    var visitTimeTo by mutableStateOf("18:00")
        private set

    var excludeFromSupplyPlanning by mutableStateOf(false)
        private set

    var excludeFromOperatorEarnings by mutableStateOf(false)
        private set

    var cupsMax by mutableStateOf(15)
        private set

    var lidsMax by mutableStateOf(6)
        private set

    var coffeeMax by mutableStateOf(6)
        private set

    var milkMax by mutableStateOf(10)
        private set

    var chocolateMax by mutableStateOf(10)
        private set

    var toppingMax by mutableStateOf(10)
        private set

    var sugarMax by mutableStateOf(2)
        private set

    var stirrersMax by mutableStateOf(4)
        private set

    var garbageBagsMax by mutableStateOf(2)
        private set

    var employees by mutableStateOf<List<EmployeeEntity>>(emptyList())
        private set

    var selectedEmployeeId by mutableStateOf<Int?>(null)
        private set

    var commandMessage by mutableStateOf<String?>(null)
        private set

    var isCommandSending by mutableStateOf(false)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var showSyncError by mutableStateOf(false)
        private set

    var syncErrorMessage by mutableStateOf("Проверьте подключение к интернету или VPN")
        private set

    fun loadStation(
        stationId: Int
    ) {

        if (loadedStationId == stationId) return

        viewModelScope.launch {

            val detail = stationRepository.getStation(stationId)
                ?: return@launch
            val loadedEmployees = loadEmployees()
            val assignment = employeeStationAssignmentDao
                .getCurrentByStationId(stationId)

            loadedStationId = stationId
            station = detail
            employees = loadedEmployees
            loadedEmployeeAssignmentId = assignment?.employeeId
            selectedEmployeeId = assignment?.employeeId

            stationName = detail.station.name
            terminalId = detail.station.terminalId?.toString() ?: ""
            address = detail.station.address
            visitDays = detail.station.visitDays
            visitTimeFrom = normalizeTime(detail.station.visitTimeFrom)
                .ifBlank { "09:00" }
            visitTimeTo = normalizeTime(detail.station.visitTimeTo)
                .ifBlank { "18:00" }
            excludeFromSupplyPlanning =
                detail.station.excludeFromSupplyPlanning
            excludeFromOperatorEarnings =
                detail.station.excludeFromOperatorEarnings

            cupsMax = detail.settings.cupsMax
            lidsMax = detail.settings.lidsMax
            coffeeMax = detail.settings.coffeeMax
            milkMax = detail.settings.milkMax
            chocolateMax = detail.settings.chocolateMax
            toppingMax = detail.settings.toppingMax
            sugarMax = detail.settings.sugarMax
            stirrersMax = detail.settings.stirrersMax
            garbageBagsMax = detail.settings.garbageBagsMax

        }

    }

    fun onStationNameChanged(value: String) {

        stationName = value
        stationNameError = false

    }

    fun onTerminalIdChanged(value: String) {

        terminalId = value.filter { it.isDigit() }

    }

    fun onAddressChanged(value: String) {

        address = value

    }

    fun onVisitDayToggled(day: Int) {

        val days = visitDays.toDaySet().toMutableSet()

        if (days.contains(day)) {
            days.remove(day)
        } else {
            days.add(day)
        }

        visitDays = days.sorted().joinToString(",")

    }

    fun onVisitTimeFromChanged(value: String) {

        visitTimeFrom = normalizeTime(value).ifBlank { "09:00" }

    }

    fun onVisitTimeToChanged(value: String) {

        visitTimeTo = normalizeTime(value).ifBlank { "18:00" }

    }

    fun onExcludeFromSupplyPlanningChanged(value: Boolean) {

        excludeFromSupplyPlanning = value

    }

    fun onExcludeFromOperatorEarningsChanged(value: Boolean) {

        excludeFromOperatorEarnings = value

    }

    fun onEmployeeSelected(employeeId: Int) {

        selectedEmployeeId = employeeId

    }

    fun plusCups() {
        cupsMax = increment(cupsMax)
    }

    fun minusCups() {
        cupsMax = decrement(cupsMax)
    }

    fun plusLids() {
        lidsMax = increment(lidsMax)
    }

    fun minusLids() {
        lidsMax = decrement(lidsMax)
    }

    fun plusCoffee() {
        coffeeMax = increment(coffeeMax)
    }

    fun minusCoffee() {
        coffeeMax = decrement(coffeeMax)
    }

    fun plusMilk() {
        milkMax = increment(milkMax)
    }

    fun minusMilk() {
        milkMax = decrement(milkMax)
    }

    fun plusChocolate() {
        chocolateMax = increment(chocolateMax)
    }

    fun minusChocolate() {
        chocolateMax = decrement(chocolateMax)
    }

    fun plusTopping() {
        toppingMax = increment(toppingMax)
    }

    fun minusTopping() {
        toppingMax = decrement(toppingMax)
    }

    fun plusSugar() {
        sugarMax = increment(sugarMax)
    }

    fun minusSugar() {
        sugarMax = decrement(sugarMax)
    }

    fun plusStirrers() {
        stirrersMax = increment(stirrersMax)
    }

    fun minusStirrers() {
        stirrersMax = decrement(stirrersMax)
    }

    fun plusGarbageBags() {
        garbageBagsMax = increment(garbageBagsMax)
    }

    fun minusGarbageBags() {
        garbageBagsMax = decrement(garbageBagsMax)
    }

    fun saveStation(
        onSuccess: () -> Unit
    ) {

        if (isSaving) return

        stationNameError = stationName.isBlank()

        if (stationNameError) return

        val current = station ?: return

        val updated = current.copy(
            station = current.station.copy(
                name = stationName,
                terminalId = terminalId.toLongOrNull(),
                address = address,
                visitDays = visitDays,
                visitTimeFrom = visitTimeFrom,
                visitTimeTo = visitTimeTo,
                excludeFromSupplyPlanning = excludeFromSupplyPlanning,
                excludeFromOperatorEarnings = excludeFromOperatorEarnings
            ),
            settings = current.settings.copy(
                cupsMax = cupsMax,
                lidsMax = lidsMax,
                coffeeMax = coffeeMax,
                milkMax = milkMax,
                chocolateMax = chocolateMax,
                toppingMax = toppingMax,
                sugarMax = sugarMax,
                stirrersMax = stirrersMax,
                garbageBagsMax = garbageBagsMax
            )
        )

        viewModelScope.launch {

            isSaving = true
            showSyncError = false

            val saved = stationRepository.saveStation(
                detail = updated,
                syncImmediately = true
            )

            saveEmployeeAssignmentIfChanged(current.station.id)

            station = stationRepository.getStation(current.station.id)
            isSaving = false

            if (saved) {
                onSuccess()
            } else {
                syncErrorMessage = station?.syncErrorDialogMessage()
                    ?: "Проверьте подключение к интернету или VPN"
                showSyncError = true
            }

        }

    }

    fun dismissSyncError() {
        showSyncError = false
    }

    fun deleteStation(
        onSuccess: () -> Unit
    ) {

        val current = station ?: return

        viewModelScope.launch {

            stationRepository.deleteStation(current.station.id)
            onSuccess()

        }

    }

    fun rebootTerminal() {

        sendTerminalCommand(
            successMessage = "Команда перезагрузки отправлена"
        ) { terminalId ->
            commandRepository.rebootTerminal(terminalId)
        }

    }

    fun turnScreenOff() {

        sendTerminalCommand(
            successMessage = "Команда выключения экрана отправлена"
        ) { terminalId ->
            commandRepository.turnScreenOff(terminalId)
        }

    }

    fun turnScreenOn() {

        sendTerminalCommand(
            successMessage = "Команда включения экрана отправлена"
        ) { terminalId ->
            commandRepository.turnScreenOn(terminalId)
        }

    }

    fun updateRentPaidUntil(
        date: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {

        val currentStation = station ?: return

        if (!isTodayOrFuture(date)) {
            onError()
            return
        }

        viewModelScope.launch {

            stationRepository.updateRentPaidUntil(
                stationId = currentStation.station.id,
                rentPaidUntil = date
            )

            station = stationRepository.getStation(
                currentStation.station.id
            )

            onSuccess()

        }

    }

    private fun sendTerminalCommand(
        successMessage: String,
        action: suspend (Long) -> Unit
    ) {

        val terminalId = station?.station?.terminalId

        if (terminalId == null) {
            commandMessage = "У станции не указан номер терминала"
            return
        }

        viewModelScope.launch {

            isCommandSending = true
            commandMessage = "Отправляем команду..."

            try {

                action(terminalId)
                commandMessage = successMessage

            } catch (_: Exception) {

                commandMessage = "Не удалось отправить команду"

            } finally {

                isCommandSending = false

            }

        }

    }

    private suspend fun loadEmployees(): List<EmployeeEntity> {

        val currentEmployees = employeeDao.getActiveByRole(DEFAULT_EMPLOYEE_ROLE)

        if (currentEmployees.isNotEmpty()) {
            return currentEmployees
        }

        val now = currentTimestamp()
        val defaultEmployee = EmployeeEntity(
            id = DEFAULT_EMPLOYEE_ID,
            name = DEFAULT_EMPLOYEE_NAME,
            role = DEFAULT_EMPLOYEE_ROLE,
            workStartDate = DEFAULT_EMPLOYEE_WORK_START_DATE,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        employeeDao.insert(defaultEmployee)

        return listOf(defaultEmployee)

    }

    private suspend fun saveEmployeeAssignmentIfChanged(stationId: Int) {

        val employeeId = selectedEmployeeId ?: return

        if (employeeId == loadedEmployeeAssignmentId) return

        val now = currentTimestamp()
        val validFrom = currentDate()

        employeeStationAssignmentDao.closeCurrentForStation(
            stationId = stationId,
            validTo = validFrom,
            updatedAt = now
        )
        employeeStationAssignmentDao.insert(
            EmployeeStationAssignmentEntity(
                id = UUID.randomUUID().toString(),
                employeeId = employeeId,
                stationId = stationId,
                validFrom = validFrom,
                validTo = null,
                createdAt = now,
                updatedAt = now
            )
        )

        loadedEmployeeAssignmentId = employeeId

    }

    private fun currentTimestamp(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            Locale.getDefault()
        ).format(Calendar.getInstance().time)

    }

    private fun currentDate(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(Calendar.getInstance().time)

    }

    private fun isTodayOrFuture(date: String): Boolean {

        val formatter = SimpleDateFormat(
            "dd.MM.yyyy",
            Locale.getDefault()
        )

        val selected = formatter.parse(date) ?: return false

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        return !selected.before(today)

    }

    private fun increment(
        current: Int
    ): Int {

        return (current + 1).coerceAtMost(99)

    }

    private fun decrement(
        current: Int
    ): Int {

        return (current - 1).coerceAtLeast(1)

    }

}

private const val DEFAULT_EMPLOYEE_ID = 1
private const val DEFAULT_EMPLOYEE_NAME = "Максим"
private const val DEFAULT_EMPLOYEE_ROLE = "OPERATOR"
private const val DEFAULT_EMPLOYEE_WORK_START_DATE = "2026-09-13"

private fun String.toDaySet(): Set<Int> {
    return split(",")
        .mapNotNull { value -> value.trim().toIntOrNull() }
        .filter { day -> day in 1..7 }
        .toSet()
}
