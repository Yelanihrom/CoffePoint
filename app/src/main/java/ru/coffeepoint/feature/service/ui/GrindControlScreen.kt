package ru.coffeepoint.feature.service.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.R
import ru.coffeepoint.feature.service.presentation.ServiceStep1ViewModel
import ru.coffeepoint.shared.component.BottomActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrindControlScreen(
    stationId: Int,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val stationViewModel: ServiceStep1ViewModel = viewModel()
    stationViewModel.loadStation(stationId)
    val station = stationViewModel.station ?: return
    var completed by remember(stationId) { mutableStateOf(false) }
    val showKeypadHelp = remember { mutableStateOf(false) }

    if (showKeypadHelp.value) {
        AlertDialog(
            onDismissRequest = { showKeypadHelp.value = false },
            title = { Text("Кнопки аппарата") },
            text = {
                Image(
                    painter = painterResource(R.drawable.grind_control_keypad),
                    contentDescription = "Схема кнопок аппарата",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                )
            },
            confirmButton = {
                TextButton(onClick = { showKeypadHelp.value = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = station.station.name,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Шаг 2 из 3",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showKeypadHelp.value = true }) {
                        Icon(
                            imageVector = Icons.Filled.HelpOutline,
                            contentDescription = "Подсказка по кнопкам аппарата"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomActionBar(
                text = "К обслуживанию узлов",
                enabled = completed,
                onClick = onContinue
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Контроль помола",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NumberedInstruction("1", "Кнопка на обратной стороне дверцы")
                    NumberedInstruction("2", "Меню аппарата")
                    NumberedInstruction("3", "Конфигурация кофейной группы")
                    NumberedInstruction("4", "Опция контроля помола")
                    NumberedInstruction(
                        "5",
                        "Средняя скорость пос. смачивания",
                        singleLine = true
                    )
                    NumberedInstruction("6", "Проверьте значение параметра")
                    GrindRangeScale(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Информация",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Регулируется в меню \"Ручная настройка помола\"",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    val uriHandler = LocalUriHandler.current
                    Text(
                        text = "Расширенная инструкция",
                        modifier = Modifier
                            .padding(start = 32.dp)
                            .clickable {
                                uriHandler.openUri(
                                    "http://jofemar-configuration.ru/nastroqka-pomola-cherez-sm-i-proverka-skorosti-smachivaniya"
                                )
                            },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDecoration = TextDecoration.Underline
                        )
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { completed = !completed }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = completed,
                        onCheckedChange = { completed = it }
                    )
                    Text(
                        text = "Проверку выполнил и при необходимости отрегулировал помол",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun GrindRangeScale(modifier: Modifier = Modifier) {
    val red = Color(0xFFE53935)
    val orange = Color(0xFFFF8A00)
    val yellow = Color(0xFFFFD54F)
    val green = Color(0xFF43C477)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
        ) {
            val y = size.height / 2f
            val strokeWidth = 9.dp.toPx()
            val x4 = size.width * 0.4f
            val x6 = size.width * 0.6f

            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(red, orange, yellow),
                    startX = 0f,
                    endX = x4
                ),
                start = Offset(0f, y),
                end = Offset(x4, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = green,
                start = Offset(x4, y),
                end = Offset(x6, y),
                strokeWidth = strokeWidth
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(yellow, orange, red),
                    startX = x6,
                    endX = size.width
                ),
                start = Offset(x6, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            listOf(x4, x6).forEach { x ->
                drawLine(
                    color = Color.White,
                    start = Offset(x, y - 11.dp.toPx()),
                    end = Offset(x, y + 11.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "0",
                modifier = Modifier.align(Alignment.TopStart),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "4",
                modifier = Modifier.offset(x = maxWidth * 0.4f - 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "6",
                modifier = Modifier.offset(x = maxWidth * 0.6f - 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "10",
                modifier = Modifier.align(Alignment.TopEnd),
                style = MaterialTheme.typography.labelSmall
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Сделать крупнее\nна 1 пункт",
                modifier = Modifier.weight(4f),
                style = MaterialTheme.typography.labelSmall,
                color = red
            )
            Text(
                text = "Отлично",
                modifier = Modifier.weight(2f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = green,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Сделать мельче\nна 2 пункта",
                modifier = Modifier.weight(4f),
                style = MaterialTheme.typography.labelSmall,
                color = red,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun NumberedInstruction(
    number: String,
    text: String,
    singleLine: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = number,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = if (singleLine) MaterialTheme.typography.bodyMedium
            else MaterialTheme.typography.bodyLarge,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            softWrap = !singleLine,
            overflow = if (singleLine) TextOverflow.Ellipsis else TextOverflow.Clip
        )
    }
}
