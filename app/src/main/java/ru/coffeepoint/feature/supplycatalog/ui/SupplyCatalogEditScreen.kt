package ru.coffeepoint.feature.supplycatalog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import ru.coffeepoint.shared.component.AppDialog
import ru.coffeepoint.shared.component.AppDialogButton
import ru.coffeepoint.shared.component.DialogButtonStyle
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.feature.supplycatalog.model.SupplyCatalogItemType
import ru.coffeepoint.feature.supplycatalog.presentation.SupplyCatalogEditViewModel
import ru.coffeepoint.shared.component.BottomActionBar
import ru.coffeepoint.shared.component.SyncSendingErrorDialog
import ru.coffeepoint.shared.component.SyncSendingOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplyCatalogEditScreen(
    itemId: Int,
    initialItemType: String,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {

    val viewModel: SupplyCatalogEditViewModel = viewModel()

    val isCreateMode = itemId == 0

    viewModel.loadItem(
        itemId = itemId,
        initialItemType = initialItemType
    )

    if (viewModel.isSending) {
        SyncSendingOverlay()
    }

    if (viewModel.showSyncError) {
        SyncSendingErrorDialog(
            message = viewModel.syncErrorMessage,
            onDismiss = viewModel::dismissSyncError,
            onRetry = {
                viewModel.retry(onSaved)
            }
        )
    }

    if (viewModel.showDeleteConfirm) {
        AppDialog(
            onDismissRequest = viewModel::dismissDeleteConfirm,
            title = {
                Text("Удалить вариант?")
            },
            text = {
                Text("Вариант закупки будет удален из каталога.")
            },
            confirmButton = {
                AppDialogButton(
                    text = "Удалить", style = DialogButtonStyle.Destructive,
                    onClick = {
                        viewModel.delete(onSaved)
                    }
                )
            },
            dismissButton = {
                AppDialogButton(
                    text = "Отменить", style = DialogButtonStyle.Secondary,
                    onClick = viewModel::dismissDeleteConfirm
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isCreateMode) {
                            "Новый вариант"
                        } else {
                            "Редактировать вариант"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    if (!isCreateMode) {
                        IconButton(
                            onClick = viewModel::requestDelete
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить вариант",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomActionBar(
                text = if (isCreateMode) {
                    "Добавить вариант"
                } else {
                    "Сохранить изменения"
                },
                onClick = {
                    viewModel.save(onSaved)
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = itemTypeLabel(viewModel.itemType),
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = "Вариант закупки",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = viewModel.title,
                onValueChange = viewModel::onTitleChanged,
                label = {
                    Text("Название")
                },
                isError = viewModel.titleError,
                supportingText = {
                    if (viewModel.titleError) {
                        Text("Введите название")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.supplier,
                onValueChange = viewModel::onSupplierChanged,
                label = {
                    Text("Поставщик")
                },
                isError = viewModel.supplierError,
                supportingText = {
                    if (viewModel.supplierError) {
                        Text("Введите поставщика")
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.article,
                onValueChange = viewModel::onArticleChanged,
                label = {
                    Text("Артикул")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.url,
                onValueChange = viewModel::onUrlChanged,
                label = {
                    Text("Ссылка")
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.maxPrice,
                onValueChange = viewModel::onMaxPriceChanged,
                label = {
                    Text("Предельная цена")
                },
                isError = viewModel.maxPriceError,
                supportingText = {
                    if (viewModel.maxPriceError) {
                        Text("Введите цену")
                    }
                },
                suffix = {
                    Text("р")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.comment,
                onValueChange = viewModel::onCommentChanged,
                label = {
                    Text("Комментарий")
                },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Параметры",
                        style = MaterialTheme.typography.titleMedium
                    )

                    SupplyCatalogItemTypeField(
                        selectedItemType = SupplyCatalogItemType.fromCode(
                            viewModel.itemType
                        ),
                        onItemTypeChanged = viewModel::onItemTypeChanged
                    )

                    OutlinedTextField(
                        value = viewModel.priority,
                        onValueChange = viewModel::onPriorityChanged,
                        label = {
                            Text("Приоритет")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Активен",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Text(
                                text = "Показывать вариант в каталоге",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = viewModel.isActive,
                            onCheckedChange = viewModel::onIsActiveChanged
                        )
                    }
                }
            }
        }
    }

}

private fun itemTypeLabel(itemType: String): String {
    return SupplyCatalogItemType.labelOf(itemType)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplyCatalogItemTypeField(
    selectedItemType: SupplyCatalogItemType,
    onItemTypeChanged: (SupplyCatalogItemType) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        OutlinedTextField(
            value = selectedItemType.label,
            onValueChange = {},
            readOnly = true,
            label = {
                Text("Тип")
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            modifier = Modifier
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true
                )
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            SupplyCatalogItemType.entries.forEach { itemType ->
                DropdownMenuItem(
                    text = {
                        Text(itemType.label)
                    },
                    onClick = {
                        onItemTypeChanged(itemType)
                        expanded = false
                    }
                )
            }
        }
    }

}
