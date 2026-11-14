package ru.coffeepoint.feature.analytics.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.core.data.local.entity.ServiceVisitHistoryRow
import ru.coffeepoint.feature.analytics.model.ANALYTICS_TIME_ZONE
import ru.coffeepoint.feature.analytics.model.AnalyticsPeriod
import ru.coffeepoint.feature.analytics.presentation.ServiceVisitHistoryViewModel
import ru.coffeepoint.shared.component.AppPeriodSelector
import ru.coffeepoint.shared.component.SyncSendingOverlay
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ServiceVisitHistoryScreen(onBack: () -> Unit) {
    val model: ServiceVisitHistoryViewModel = viewModel()
    ServiceVisitHistoryContent(model.visits, model.loading, model.error, model::refresh, onBack,
        model.period, model::selectPeriod)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceVisitHistoryContent(
    visits: List<ServiceVisitHistoryRow>, loading: Boolean, error: String?,
    onRefresh: () -> Unit, onBack: () -> Unit,
    period: AnalyticsPeriod = AnalyticsPeriod.currentMonth(),
    onPeriodChange: (AnalyticsPeriod) -> Unit = {}
) {
    if (loading) SyncSendingOverlay()
    var selectingStart by remember { mutableStateOf<Boolean?>(null) }
    val validPeriod = period.isValid(LocalDate.now(ANALYTICS_TIME_ZONE))
    selectingStart?.let { start ->
        AnalyticsDatePicker(
            initial = (if (start) period.start else period.end) ?: LocalDate.now(ANALYTICS_TIME_ZONE),
            onSelect = { day -> onPeriodChange(if (start) period.copy(start = day) else period.copy(end = day)) },
            onDismiss = { selectingStart = null }
        )
    }
    Scaffold(topBar = {
        TopAppBar(title = {
            Text("История обслуживаний", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
        }, actions = {
            IconButton(onClick = onRefresh, enabled = !loading) {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить историю")
            }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                AppPeriodSelector(
                    startDate = period.start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    endDate = period.end?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "Сейчас",
                    onStartDateClick = { selectingStart = true },
                    onEndDateClick = { selectingStart = false },
                    enabled = !loading,
                    modifier = Modifier.padding(16.dp)
                )
                if (!validPeriod) Text("Проверьте период: дата «По» не должна быть раньше даты «С»",
                    color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            }
            if (error != null) item {
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp))
            }
            if (visits.isEmpty() && !loading && validPeriod) item {
                Text("За этот период обслуживаний нет", modifier = Modifier.padding(16.dp))
            }
            items(visits, key = { it.id }) { visit ->
                ListItem(headlineContent = {
                    Text(visit.stationName?.takeIf { it.isNotBlank() }
                        ?: visit.stationAddress?.takeIf { it.isNotBlank() } ?: "Станция №${visit.stationId}",
                        fontWeight = FontWeight.SemiBold)
                }, supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        visit.stationAddress?.takeIf { it.isNotBlank() && it != visit.stationName }?.let { Text(it) }
                        Text(visit.employeeName?.takeIf { it.isNotBlank() } ?: "Сотрудник №${visit.employeeId}")
                        Text(runCatching {
                            Instant.parse(visit.completedAt).atZone(ANALYTICS_TIME_ZONE)
                                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                        }.getOrDefault(visit.completedAt))
                        if (!visit.isSynced) Text(
                            if (visit.syncError) "Не отправлено" else "Ожидает отправки",
                            color = if (visit.syncError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
