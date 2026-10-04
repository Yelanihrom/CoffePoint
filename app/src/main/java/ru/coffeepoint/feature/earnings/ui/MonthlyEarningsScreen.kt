package ru.coffeepoint.feature.earnings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Refresh
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
import ru.coffeepoint.core.data.repository.MonthlyEarningsPayment
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
    var startDate by remember {
        mutableStateOf(currentMonthStartDisplayDate())
    }
    var endDate by remember {
        mutableStateOf(currentDisplayDate())
    }
    var paymentAmount by remember {
        mutableStateOf("")
    }
    var datePickerTarget by remember {
        mutableStateOf<EarningsDatePickerTarget?>(null)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPayments(
            startDate = startDate,
            endDate = endDate
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

    viewModel.accrualMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissAccrualMessage()
            },
            title = {
                Text("\u041d\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u0435")
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissAccrualMessage()
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
                EarningsDatePickerTarget.Start -> {
                    parseDisplayDateToMillis(startDate)
                }

                EarningsDatePickerTarget.End -> {
                    parseDisplayDateToMillis(endDate)
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
                            EarningsDatePickerTarget.Start -> {
                                startDate = selectedDate

                                if (isAfter(startDate, endDate)) {
                                    endDate = selectedDate
                                }
                            }

                            EarningsDatePickerTarget.End -> {
                                endDate = selectedDate

                                if (isAfter(startDate, endDate)) {
                                    startDate = selectedDate
                                }
                            }
                        }

                        viewModel.refreshPayments(
                            startDate = startDate,
                            endDate = endDate
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
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background
            ) {
                Button(
                    onClick = {
                        viewModel.refresh(
                            startDate = startDate,
                            endDate = endDate
                        )
                    },
                    enabled = !viewModel.loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(EarningsButtonHeight),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (viewModel.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = if (viewModel.loading) {
                            "\u041e\u0431\u043d\u043e\u0432\u043b\u044f\u0435\u043c...".asActionButtonText()
                        } else {
                            "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c \u0441\u0442\u0430\u0442\u0438\u0441\u0442\u0438\u043a\u0443".asActionButtonText()
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                WorkStartDateCard(
                    workStartDateText = viewModel.state.workStartDateText
                )
            }

            item {
                PeriodEarningsCard(
                    startDate = startDate,
                    endDate = endDate,
                    earningsText = viewModel.state.earningsText,
                    canAccrue = canCreatePayment,
                    accrualEnabled = !viewModel.loading &&
                        viewModel.state.periodFrom == startDate &&
                        viewModel.state.periodTo == endDate,
                    onStartClick = {
                        datePickerTarget = EarningsDatePickerTarget.Start
                    },
                    onEndClick = {
                        datePickerTarget = EarningsDatePickerTarget.End
                    },
                    onAccrueClick = {
                        viewModel.accrueEarnings(
                            startDate = startDate,
                            endDate = endDate
                        )
                    }
                )
            }

            item {
                PaymentsCard(
                    payments = viewModel.payments,
                    canCreatePayment = canCreatePayment,
                    paymentAmount = paymentAmount,
                    loading = viewModel.loading,
                    onPaymentAmountChanged = { value ->
                        paymentAmount = value.filter { char ->
                            char.isDigit()
                        }
                    },
                    onCreatePayment = {
                        val amount = paymentAmount.toIntOrNull()
                            ?: return@PaymentsCard

                        viewModel.addPayment(
                            amount = amount,
                            startDate = startDate,
                            endDate = endDate
                        )
                        paymentAmount = ""
                    }
                )
            }

            item {
                SettlementCard(
                    settlementText = viewModel.state.settlementText,
                    settlementAmount = viewModel.state.settlementAmount
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
private fun PeriodEarningsCard(
    startDate: String,
    endDate: String,
    earningsText: String,
    canAccrue: Boolean,
    accrualEnabled: Boolean,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    onAccrueClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "\u0420\u0430\u0441\u0447\u0435\u0442 \u0437\u0430 \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u044b\u0439 \u043f\u0435\u0440\u0438\u043e\u0434",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PeriodDateCard(
                    label = "\u0421",
                    date = startDate,
                    onClick = onStartClick,
                    modifier = Modifier.weight(1f)
                )

                PeriodDateCard(
                    label = "\u041f\u043e",
                    date = endDate,
                    onClick = onEndClick,
                    modifier = Modifier.weight(1f)
                )
            }

            EarningsSummaryRow(
                label = "\u0417\u0430\u0440\u0430\u0431\u043e\u0442\u0430\u043d\u043e \u0437\u0430 \u043f\u0435\u0440\u0438\u043e\u0434",
                value = earningsText,
                valueStyle = MaterialTheme.typography.headlineSmall,
                valueColor = MaterialTheme.colorScheme.primary
            )

            if (canAccrue) {
                Button(
                    onClick = onAccrueClick,
                    enabled = accrualEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(EarningsButtonHeight),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "\u041d\u0430\u0447\u0438\u0441\u043b\u0438\u0442\u044c".asActionButtonText(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
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
}

@Composable
private fun SettlementCard(
    settlementText: String,
    settlementAmount: Int
) {
    val isAdvanceDebt = settlementAmount < 0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EarningsSummaryRow(
                label = "\u0412\u0437\u0430\u0438\u043c\u043e\u0440\u0430\u0441\u0447\u0435\u0442",
                value = settlementText,
                valueStyle = MaterialTheme.typography.headlineSmall,
                valueColor = if (isAdvanceDebt) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

@Composable
private fun PaymentsCard(
    payments: List<MonthlyEarningsPayment>,
    canCreatePayment: Boolean,
    paymentAmount: String,
    loading: Boolean,
    onPaymentAmountChanged: (String) -> Unit,
    onCreatePayment: () -> Unit
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
            PaymentsSectionHeader(
                count = payments.size
            )

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
                    if (payments.isEmpty()) {
                        EmptyPaymentsMessage()
                    } else {
                        payments.forEach { payment ->
                            PaymentRow(payment = payment)
                        }
                    }
                }
            }

            if (canCreatePayment) {
                CreatePaymentRow(
                    paymentAmount = paymentAmount,
                    loading = loading,
                    onPaymentAmountChanged = onPaymentAmountChanged,
                    onCreatePayment = onCreatePayment
                )
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
private fun PaymentsSectionHeader(
    count: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "\u0412\u044b\u043f\u043b\u0430\u0442\u044b \u0437\u0430 \u043f\u0435\u0440\u0438\u043e\u0434",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "$count ${paymentCountSuffix(count)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PaymentRow(
    payment: MonthlyEarningsPayment
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = payment.dateText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = payment.comment.ifBlank { "\u0412\u044b\u043f\u043b\u0430\u0442\u0430" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = payment.amountText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyPaymentsMessage() {
    Text(
        text = "\u0417\u0430 \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u044b\u0439 \u043f\u0435\u0440\u0438\u043e\u0434 \u0432\u044b\u043f\u043b\u0430\u0442 \u043d\u0435\u0442",
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

private fun currentMonthStartDisplayDate(): String {
    return formatDisplayDate(
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.DAY_OF_MONTH, 1)
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

private fun paymentCountSuffix(count: Int): String {
    return when {
        count % 10 == 1 && count % 100 != 11 -> "\u0432\u044b\u043f\u043b\u0430\u0442\u0430"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "\u0432\u044b\u043f\u043b\u0430\u0442\u044b"
        else -> "\u0432\u044b\u043f\u043b\u0430\u0442"
    }
}

private val EarningsButtonHeight = 56.dp

private enum class EarningsDatePickerTarget {
    Start,
    End
}
