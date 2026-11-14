package ru.coffeepoint.feature.analytics.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import ru.coffeepoint.feature.analytics.model.ANALYTICS_TIME_ZONE
import ru.coffeepoint.shared.component.AppDatePickerDialog
import ru.coffeepoint.shared.component.AppDialogButton
import ru.coffeepoint.shared.component.DialogButtonStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnalyticsDatePicker(initial: LocalDate, onSelect: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val today = LocalDate.now(ANALYTICS_TIME_ZONE)
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate() <= today
        }
    )
    AppDatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AppDialogButton("Выбрать", enabled = picker.selectedDateMillis != null, onClick = {
                picker.selectedDateMillis?.let {
                    onSelect(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            })
        },
        dismissButton = {
            AppDialogButton("Отменить", style = DialogButtonStyle.Secondary, onClick = onDismiss)
        }
    ) { DatePicker(state = picker) }
}
