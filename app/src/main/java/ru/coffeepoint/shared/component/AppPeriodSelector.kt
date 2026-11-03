package ru.coffeepoint.shared.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AppPeriodSelector(
    startDate: String,
    endDate: String,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val minFieldWidth = (50 + 100 * LocalDensity.current.fontScale).dp
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth >= minFieldWidth * 2 + 10.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppPeriodDateField("С", startDate, onStartDateClick, Modifier.weight(1f), enabled)
                AppPeriodDateField("По", endDate, onEndDateClick, Modifier.weight(1f), enabled)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppPeriodDateField("С", startDate, onStartDateClick, Modifier.fillMaxWidth(), enabled)
                AppPeriodDateField("По", endDate, onEndDateClick, Modifier.fillMaxWidth(), enabled)
            }
        }
    }
}

@Composable
fun AppPeriodDateField(
    label: String,
    date: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier.heightIn(min = 48.dp).semantics { role = Role.Button },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        enabled = enabled,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label $date",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.38f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
