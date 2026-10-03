package ru.coffeepoint.feature.auth.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.shared.component.asActionButtonText

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (UserRole) -> Unit
) {

    var selectedProtectedRole by remember {
        mutableStateOf<UserRole?>(null)
    }

    var managerPassword by remember {
        mutableStateOf("")
    }

    var supplyOperatorPassword by remember {
        mutableStateOf("")
    }

    var showPasswordError by remember {
        mutableStateOf(false)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Выберите роль",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Роль определяет доступные действия в приложении",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            RoleTile(
                title = "Оператор кофе-станций",
                subtitle = "Заправка и обслуживание автоматов",
                icon = Icons.Outlined.LocalCafe,
                accentColor = Color(0xFFD08A32),
                selected = false,
                onClick = {
                    onRoleSelected(UserRole.OPERATOR)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            RoleTile(
                title = "Снабженец",
                subtitle = "Поставка остатков и простое обслуживание",
                icon = Icons.Outlined.Inventory2,
                accentColor = Color(0xFF42A5F5),
                selected = selectedProtectedRole == UserRole.SUPPLY_OPERATOR,
                onClick = {
                    selectedProtectedRole = UserRole.SUPPLY_OPERATOR
                    showPasswordError = false
                }
            )

            if (selectedProtectedRole == UserRole.SUPPLY_OPERATOR) {
                PinLoginBlock(
                    value = supplyOperatorPassword,
                    onValueChange = {
                        supplyOperatorPassword = it.filter { char ->
                            char.isDigit()
                        }
                        showPasswordError = false
                    },
                    label = "PIN-код",
                    isError = showPasswordError,
                    onLogin = {
                        if (supplyOperatorPassword == SUPPLY_OPERATOR_PASSWORD) {
                            onRoleSelected(UserRole.SUPPLY_OPERATOR)
                        } else {
                            showPasswordError = true
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            RoleTile(
                title = "Управляющий",
                subtitle = "Полный доступ к настройкам и синхронизации",
                icon = Icons.Outlined.AdminPanelSettings,
                accentColor = Color(0xFF4CAF50),
                selected = selectedProtectedRole == UserRole.MANAGER,
                onClick = {
                    selectedProtectedRole = UserRole.MANAGER
                    showPasswordError = false
                }
            )

            if (selectedProtectedRole == UserRole.MANAGER) {
                PinLoginBlock(
                    value = managerPassword,
                    onValueChange = {
                        managerPassword = it.filter { char ->
                            char.isDigit()
                        }
                        showPasswordError = false
                    },
                    label = "PIN-код",
                    isError = showPasswordError,
                    onLogin = {
                        if (managerPassword == MANAGER_PASSWORD) {
                            onRoleSelected(UserRole.MANAGER)
                        } else {
                            showPasswordError = true
                        }
                    }
                )
            }

        }

    }

}

@Composable
private fun RoleTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    selected: Boolean,
    onClick: () -> Unit
) {

    val borderColor = if (selected) {
        accentColor
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(32.dp)
                )

            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

            }

        }

    }

}

@Composable
private fun PinLoginBlock(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    onLogin: () -> Unit
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(label)
        },
        isError = isError,
        supportingText = {
            if (isError) {
                Text("Неверный PIN-код")
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                onLogin()
            }
        ),
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    )

    OutlinedButton(
        onClick = onLogin,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = LOGIN_GREEN
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = LOGIN_GREEN
        )
    ) {
        Text("Войти".asActionButtonText())
    }

}

private val LOGIN_GREEN = Color(0xFF2E7D32)
private const val MANAGER_PASSWORD = "4088"
private const val SUPPLY_OPERATOR_PASSWORD = "2971"
