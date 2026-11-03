package ru.coffeepoint.feature.analytics.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.feature.analytics.model.ANALYTICS_TIME_ZONE
import ru.coffeepoint.feature.analytics.model.AnalyticsPeriod
import ru.coffeepoint.feature.analytics.model.SalesAnalyticsReport
import ru.coffeepoint.feature.analytics.presentation.SalesAnalyticsViewModel
import ru.coffeepoint.shared.component.AppDatePickerDialog
import ru.coffeepoint.shared.component.AppDialogButton
import ru.coffeepoint.shared.component.AppPeriodSelector
import ru.coffeepoint.shared.component.DialogButtonStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val displayDate = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
fun SalesAnalyticsScreen(onBack: () -> Unit) {
    val model: SalesAnalyticsViewModel = viewModel()
    SalesAnalyticsContent(model.period, model.report, model.loading, model.error,
        model::selectPeriod, model::refresh, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesAnalyticsContent(
    period: AnalyticsPeriod,
    report: SalesAnalyticsReport?,
    loading: Boolean,
    error: String?,
    onPeriodChange: (AnalyticsPeriod) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    var selectingStart by remember { mutableStateOf<Boolean?>(null) }
    selectingStart?.let { start ->
        val initial = (if (start) period.start else period.end)
            ?: LocalDate.now(ANALYTICS_TIME_ZONE)
        val today = LocalDate.now(ANALYTICS_TIME_ZONE)
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate() <= today
            }
        )
        AppDatePickerDialog(
            onDismissRequest = { selectingStart = null },
            confirmButton = {
                AppDialogButton("Выбрать", enabled = picker.selectedDateMillis != null, onClick = {
                    picker.selectedDateMillis?.let { millis ->
                        val day = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onPeriodChange(if (start) period.copy(start = day) else period.copy(end = day))
                    }
                    selectingStart = null
                })
            },
            dismissButton = {
                AppDialogButton("Отменить", style = DialogButtonStyle.Secondary,
                    onClick = { selectingStart = null })
            }
        ) { DatePicker(state = picker) }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Аналитика") }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
        })
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Продажи за период", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                AppPeriodSelector(
                    startDate = period.start.format(displayDate),
                    endDate = period.end?.format(displayDate) ?: "Сейчас",
                    onStartDateClick = { selectingStart = true },
                    onEndDateClick = { selectingStart = false },
                    enabled = !loading,
                    modifier = Modifier.padding(top = 12.dp)
                )
                if (period.end != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(enabled = !loading,
                            onClick = { onPeriodChange(period.copy(end = null)) }) {
                            Text("По текущий момент")
                        }
                    }
                }
                AppDialogButton("Обновить продажи", enabled = !loading, onClick = onRefresh,
                    modifier = Modifier.padding(top = 12.dp))
            }
            if (loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(32.dp))
                    }
                }
            } else if (error != null) {
                item { Text(error, color = MaterialTheme.colorScheme.error) }
            } else if (report != null) {
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Всего продаж", modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium)
                        Text(formatCount(report.totalSales), style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("Обновлено: " + report.generatedAt.atZone(ANALYTICS_TIME_ZONE)
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (report.stations.isEmpty()) {
                    item { Text("Станций пока нет") }
                }
                items(report.stations, key = { it.stationId }) { station ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(station.name, modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge)
                        Text(station.salesCount?.let(::formatCount) ?: "Нет терминала",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (station.salesCount == null) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

private fun formatCount(value: Long): String = String.format(Locale.forLanguageTag("ru-RU"), "%,d", value)
