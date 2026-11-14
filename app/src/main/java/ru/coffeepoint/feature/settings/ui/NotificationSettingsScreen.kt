package ru.coffeepoint.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.notification.NotificationOption
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.feature.settings.presentation.NotificationSettingsViewModel
import ru.coffeepoint.shared.component.SyncSendingOverlay

@Composable
fun NotificationSettingsScreen(onBack: () -> Unit, model: NotificationSettingsViewModel = viewModel()) {
    NotificationSettingsContent(onBack, model.values, model::setEnabled, model.isBusy,
        model.message, model.hasPending, model::refresh)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsContent(
    onBack: () -> Unit,
    values: Map<UserRole, Map<NotificationOption, Boolean>>,
    onToggle: (UserRole, NotificationOption, Boolean) -> Unit,
    isBusy: Boolean = false,
    message: String? = null,
    hasPending: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    if (isBusy) SyncSendingOverlay()
    val roles = listOf(UserRole.MANAGER, UserRole.OPERATOR, UserRole.SUPPLY_OPERATOR)
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val role = roles[selected]
    Scaffold(topBar = {
        TopAppBar(title = { Text("Настройка уведомлений") }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
        }, actions = {
            IconButton(onClick = onRefresh, enabled = !isBusy) {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить настройки")
            }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (message != null || hasPending) {
                Text(message ?: "Ожидает отправки", Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PrimaryTabRow(selectedTabIndex = selected) {
                listOf("Управляющий", "Оператор", "Снабженец").forEachIndexed { index, title ->
                    Tab(selected = selected == index, onClick = { selected = index },
                        text = { Text(title, style = MaterialTheme.typography.labelMedium) })
                }
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                NotificationOption.entries.filter { it.availableFor(role) }.forEach { option ->
                    if (option == NotificationOption.MIXERS) {
                        Text("Обслуживание узлов", style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = (28f / 1.5f).sp, lineHeight = 26.sp),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
                    }
                    key(role, option) {
                        val enabled = values[role]?.get(option) ?: option.defaultEnabled(role)
                        Row(Modifier.fillMaxWidth().toggleable(enabled, enabled = !isBusy, role = Role.Switch) {
                            onToggle(role, option, it)
                        }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(option.title, Modifier.weight(1f).padding(end = 12.dp),
                                style = MaterialTheme.typography.bodyLarge)
                            Switch(checked = enabled, onCheckedChange = null)
                        }
                    }
                }
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}
