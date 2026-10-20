package ru.coffeepoint.feature.earnings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.core.data.repository.MonthlyEarningsOperation
import ru.coffeepoint.feature.earnings.presentation.MonthlyEarningsViewModel
import ru.coffeepoint.shared.component.asActionButtonText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyEarningsScreen(
    canCreatePayment: Boolean,
    onBack: () -> Unit
) {

    val viewModel: MonthlyEarningsViewModel = viewModel()
    var operationsStartDate by remember {
        mutableStateOf(lastSevenDaysStartDisplayDate())
    }
    var operationsEndDate by remember {
        mutableStateOf(currentDisplayDate())
    }
    var paymentAmount by remember {
        mutableStateOf("")
    }
    var datePickerTarget by remember {
        mutableStateOf<EarningsDatePickerTarget?>(null)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshOperations(
            startDate = operationsStartDate,
            endDate = operationsEndDate
        )
    }

    if (viewModel.showConnectionError) {
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissConnectionError()
            },
            title = {
                Text("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u044f")
            },
            text = {
                Text("\u041f\u0440\u043e\u0432\u0435\u0440\u044c\u0442\u0435 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u043a \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442\u0443 \u0438 \u043e\u0442\u043a\u043b\u044e\u0447\u0438\u0442\u0435 VPN")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissConnectionError()
                    }
                ) {
                    Text("\u0417\u0430\u043a\u0440\u044b\u0442\u044c".asActionButtonText())
                }
            }
        )
    }

    if (datePickerTarget != null) {
        val target = datePickerTarget
            ?: return
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = when (target) {
                EarningsDatePickerTarget.OperationsStart -> {
                    parseDisplayDateToMillis(operationsStartDate)
                }

                EarningsDatePickerTarget.OperationsEnd -> {
                    parseDisplayDateToMillis(operationsEndDate)
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = {
                datePickerTarget = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis
                            ?.let { millis ->
                                formatDisplayDate(millis)
                            }
                            ?: return@TextButton

                        when (target) {
                            EarningsDatePickerTarget.OperationsStart -> {
                                operationsStartDate = selectedDate

                                if (isAfter(operationsStartDate, operationsEndDate)) {
                                    operationsEndDate = selectedDate
                                }
                            }

                            EarningsDatePickerTarget.OperationsEnd -> {
                                operationsEndDate = selectedDate

                                if (isAfter(operationsStartDate, operationsEndDate)) {
                                    operationsStartDate = selectedDate
                                }
                            }
                        }

                        viewModel.loadOperationsForPeriod(
                            startDate = operationsStartDate,
                            endDate = operationsEndDate
                        )
                        datePickerTarget = null
                    }
                ) {
                    Text("\u0412\u044b\u0431\u0440\u0430\u0442\u044c".asActionButtonText())
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        datePickerTarget = null
                    }
                ) {
                    Text("\u041e\u0442\u043c\u0435\u043d\u0430".asActionButtonText())
                }
            }
        ) {
            DatePicker(
                state = datePickerState
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "\u041d\u0430\u0437\u0430\u0434"
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "\u0417\u0430\u0440\u0430\u0431\u043e\u0442\u043e\u043a \u043e\u043f\u0435\u0440\u0430\u0442\u043e\u0440\u0430",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    bottom = if (canCreatePayment) 188.dp else 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    WorkStartDateCard(
                        workStartDateText = viewModel.state.workStartDateText
                    )
                }

                item {
                    OperationsCard(
                        operations = viewModel.operations,
                        startDate = operationsStartDate,
                        endDate = operationsEndDate,
                        onStartDateClick = {
                            datePickerTarget = EarningsDatePickerTarget.OperationsStart
                        },
                        onEndDateClick = {
                            datePickerTarget = EarningsDatePickerTarget.OperationsEnd
                        }
                    )
                }

            }
        }

        SettlementCard(
            settlementText = viewModel.state.settlementText,
            settlementAmount = viewModel.state.settlementAmount,
            canCreatePayment = canCreatePayment,
            paymentAmount = paymentAmount,
            loading = viewModel.loading,
            onPaymentAmountChanged = { value ->
                paymentAmount = value.filter { char ->
                    char.isDigit()
                }
            },
            onPaymentClick = {
                val amount = paymentAmount.toIntOrNull()
                    ?: return@SettlementCard

                viewModel.addPayment(
                    amount = amount,
                    startDate = operationsStartDate,
                    endDate = operationsEndDate
                )
                paymentAmount = ""
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        )

        if (viewModel.loading) {
            BlockingLoaderOverlay()
        }
    }

}

@Composable
private fun BlockingLoaderOverlay() {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.36f)
        ) {}

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )

                Text(
                    text = "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430...",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun WorkStartDateCard(
    workStartDateText: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        EarningsSummaryRow(
            label = "\u0414\u0430\u0442\u0430 \u043d\u0430\u0447\u0430\u043b\u0430 \u0440\u0430\u0431\u043e\u0442\u044b",
            value = workStartDateText,
            valueStyle = MaterialTheme.typography.bodyLarge,
            valueColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun PeriodDateCard(
    label: String,
    date: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label $date",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )

            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettlementCard(
    settlementText: String,
    settlementAmount: Int,
    canCreatePayment: Boolean,
    paymentAmount: String,
    loading: Boolean,
    onPaymentAmountChanged: (String) -> Unit,
    onPaymentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAdvanceDebt = settlementAmount < 0
    val settlementColor = if (isAdvanceDebt) {
        MaterialTheme.colorScheme.error
    } else {
        Color(0xFF30C96B)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "\u0412\u0437\u0430\u0438\u043c\u043e\u0440\u0430\u0441\u0447\u0435\u0442",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAdvanceDebt) {
                        "\u0412\u044b\u0434\u0430\u043d\u043e \u0430\u0432\u0430\u043d\u0441\u043e\u043c"
                    } else {
                        "\u041a \u0432\u044b\u043f\u043b\u0430\u0442\u0435 \u043e\u043f\u0435\u0440\u0430\u0442\u043e\u0440\u0443"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = settlementText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = settlementColor,
                    fontWeight = FontWeight.Bold
                )
            }

            if (canCreatePayment) {
                CreatePaymentRow(
                    paymentAmount = paymentAmount,
                    loading = loading,
                    onPaymentAmountChanged = onPaymentAmountChanged,
                    onCreatePayment = onPaymentClick
                )
            }

        }
    }
}

@Composable
private fun OperationsCard(
    operations: List<MonthlyEarningsOperation>,
    startDate: String,
    endDate: String,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OperationsSectionHeader(
                count = operations.size
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PeriodDateCard(
                    label = "\u0421",
                    date = startDate,
                    onClick = onStartDateClick,
                    modifier = Modifier.weight(1f)
                )

                PeriodDateCard(
                    label = "\u041f\u043e",
                    date = endDate,
                    onClick = onEndDateClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (operations.isEmpty()) {
                        EmptyOperationsMessage()
                    } else {
                        operations.forEach { operation ->
                            OperationRow(operation = operation)
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun EarningsSummaryRow(
    label: String,
    value: String,
    valueStyle: TextStyle,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = valueStyle,
            color = valueColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OperationsSectionHeader(
    count: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "\u041d\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u044f \u0438 \u0432\u044b\u043f\u043b\u0430\u0442\u044b \u0437\u0430 \u043f\u0435\u0440\u0438\u043e\u0434",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "$count ${operationCountSuffix(count)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OperationRow(
    operation: MonthlyEarningsOperation
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = operation.dateText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = operation.amountText,
            style = MaterialTheme.typography.titleMedium,
            color = if (operation.isEarning) {
                Color(0xFF30C96B)
            } else {
                MaterialTheme.colorScheme.error
            },
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyOperationsMessage() {
    Text(
        text = "\u0417\u0430 \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u044b\u0439 \u043f\u0435\u0440\u0438\u043e\u0434 \u043e\u043f\u0435\u0440\u0430\u0446\u0438\u0439 \u043d\u0435\u0442",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun CreatePaymentRow(
    paymentAmount: String,
    loading: Boolean,
    onPaymentAmountChanged: (String) -> Unit,
    onCreatePayment: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = paymentAmount,
            onValueChange = onPaymentAmountChanged,
            placeholder = {
                Text("\u0421\u0443\u043c\u043c\u0430")
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            textStyle = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier
                .weight(1f)
                .height(EarningsButtonHeight)
        )

        Button(
            onClick = onCreatePayment,
            enabled = paymentAmount.isNotBlank() && !loading,
            modifier = Modifier
                .weight(1f)
                .height(EarningsButtonHeight),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "\u0412\u044b\u043f\u043b\u0430\u0442\u0438\u0442\u044c".asActionButtonText(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun lastSevenDaysStartDisplayDate(): String {
    return formatDisplayDate(
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            add(Calendar.DAY_OF_MONTH, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    )
}

private fun currentDisplayDate(): String {
    return formatDisplayDate(
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    )
}

private fun parseDisplayDateToMillis(
    date: String
): Long? {
    return displayDateFormatter().parse(date)?.time
}

private fun isAfter(
    firstDate: String,
    secondDate: String
): Boolean {
    val firstMillis = parseDisplayDateToMillis(firstDate)
        ?: return false
    val secondMillis = parseDisplayDateToMillis(secondDate)
        ?: return false

    return firstMillis > secondMillis
}

private fun formatDisplayDate(
    millis: Long
): String {
    return displayDateFormatter().format(Date(millis))
}

private fun displayDateFormatter(): SimpleDateFormat {
    return SimpleDateFormat(
        "dd.MM.yyyy",
        Locale.getDefault()
    ).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }
}

private fun operationCountSuffix(count: Int): String {
    return when {
        count % 10 == 1 && count % 100 != 11 -> "\u043e\u043f\u0435\u0440\u0430\u0446\u0438\u044f"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "\u043e\u043f\u0435\u0440\u0430\u0446\u0438\u0438"
        else -> "\u043e\u043f\u0435\u0440\u0430\u0446\u0438\u0439"
    }
}

private val EarningsButtonHeight = 56.dp

private enum class EarningsDatePickerTarget {
    OperationsStart,
    OperationsEnd
}
