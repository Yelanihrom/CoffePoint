package ru.coffeepoint.shared.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

enum class DialogButtonStyle { Primary, Secondary, Destructive, Cancel }

@Composable
fun AppDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: DialogButtonStyle = DialogButtonStyle.Primary
) {
    val buttonModifier = modifier.fillMaxWidth().heightIn(min = 48.dp)
    val label: @Composable () -> Unit = {
        Text(
            text = text.asActionButtonText(),
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.sp),
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 14.sp),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
    val padding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
    val shape = RoundedCornerShape(8.dp)
    when (style) {
        DialogButtonStyle.Primary, DialogButtonStyle.Destructive -> Button(
            onClick = onClick, modifier = buttonModifier, enabled = enabled,
            shape = shape, contentPadding = padding,
            colors = if (style == DialogButtonStyle.Destructive) ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ) else ButtonDefaults.buttonColors()
        ) { label() }
        DialogButtonStyle.Secondary -> OutlinedButton(
            onClick = onClick, modifier = buttonModifier, enabled = enabled,
            shape = shape, contentPadding = padding
        ) { label() }
        DialogButtonStyle.Cancel -> TextButton(
            onClick = onClick, modifier = buttonModifier, enabled = enabled,
            shape = shape, contentPadding = padding
        ) { label() }
    }
}

@Composable
fun AppDialogActions(
    first: @Composable () -> Unit,
    second: (@Composable () -> Unit)? = null,
    cancel: (@Composable () -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.weight(1f).fillMaxHeight()) { first() }
            if (second != null) Box(Modifier.weight(1f).fillMaxHeight()) { second() }
        }
        cancel?.invoke()
    }
}

@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val maxBodyHeight = (LocalConfiguration.current.screenHeightDp * 0.5f).dp
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(16.dp),
        title = {
            ProvideTextStyle(MaterialTheme.typography.headlineSmall.copy(letterSpacing = 0.sp)) { title() }
        },
        text = {
            Box(Modifier.heightIn(max = maxBodyHeight).verticalScroll(rememberScrollState())) { text() }
        },
        confirmButton = {
            if (dismissButton == null) confirmButton()
            else AppDialogActions(first = dismissButton, second = confirmButton)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(16.dp),
        confirmButton = {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                AppDialogActions(first = dismissButton, second = confirmButton)
            }
        }
    ) { content() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(value: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val parts = value.split(':')
    val state = rememberTimePickerState(
        initialHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9,
        initialMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0,
        is24Hour = true
    )
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text("Время посещения") },
        text = { TimeInput(state = state) },
        confirmButton = {
            AppDialogButton("Выбрать", onClick = {
                onSave(String.format(Locale.getDefault(), "%02d:%02d", state.hour, state.minute))
            })
        },
        dismissButton = {
            AppDialogButton("Отменить", onClick = onDismiss, style = DialogButtonStyle.Secondary)
        }
    )
}
