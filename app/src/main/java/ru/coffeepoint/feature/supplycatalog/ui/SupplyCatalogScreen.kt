package ru.coffeepoint.feature.supplycatalog.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import ru.coffeepoint.shared.component.AppDialog
import ru.coffeepoint.shared.component.AppDialogButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as rotateCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ru.coffeepoint.R
import ru.coffeepoint.core.data.local.entity.SupplyCatalogEntity
import ru.coffeepoint.feature.supplycatalog.model.SupplyCatalogItemType
import ru.coffeepoint.feature.supplycatalog.presentation.SupplyCatalogViewModel
import ru.coffeepoint.shared.component.asActionButtonText

@Composable
fun SupplyCatalogScreen(
    canEdit: Boolean,
    onBack: () -> Unit,
    onEditOption: (Int) -> Unit,
    onAddOption: (String) -> Unit
) {

    val viewModel: SupplyCatalogViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.loadItems()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadItems()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (viewModel.showRefreshError) {
        AppDialog(
            onDismissRequest = {
                viewModel.dismissRefreshError()
            },
            title = {
                Text("Ошибка подключения")
            },
            text = {
                Text("Не удалось обновить каталог закупки")
            },
            confirmButton = {
                AppDialogButton("Закрыть", onClick = viewModel::dismissRefreshError)
            }
        )
    }

    Scaffold(
        topBar = {
            SupplyCatalogTopBar(
                loading = viewModel.loading,
                onBack = onBack,
                onRefresh = {
                    viewModel.refresh()
                }
            )
        }
    ) { padding ->

        val catalogGroups = remember(viewModel.items, canEdit) {
            viewModel.items
                .filter { item -> canEdit || item.isActive }
                .groupBy { item -> item.itemType }
                .toSortedMap(compareBy { itemType -> itemTypeOrder(itemType) })
                .map { (itemType, items) ->
                    SupplyCatalogGroup(
                        itemType = itemType,
                        items = items.sortedWith(
                            compareBy<SupplyCatalogEntity> { item -> item.priority }
                                .thenBy { item -> item.id }
                        )
                    )
                }
        }

        if (catalogGroups.isEmpty() && !viewModel.loading) {
            EmptyCatalog(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            )
        } else {
            var expandedItemType by remember {
                mutableStateOf<String?>(null)
            }

            val catalogRows = remember(catalogGroups) {
                catalogGroups.chunked(2)
            }

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 8.dp,
                    end = 16.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = catalogRows,
                    key = { rowGroups -> rowGroups.joinToString("-") { group -> group.itemType } }
                ) { rowGroups ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowGroups.forEach { group ->
                                SupplyCatalogTile(
                                    group = group,
                                    expanded = expandedItemType == group.itemType,
                                    onClick = {
                                        expandedItemType = if (expandedItemType == group.itemType) {
                                            null
                                        } else {
                                            group.itemType
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (rowGroups.size == 1) {
                                Spacer(
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        rowGroups
                            .firstOrNull { group -> group.itemType == expandedItemType }
                            ?.let { group ->
                                SupplyCatalogExpandedCard(
                                    group = group,
                                    canEdit = canEdit,
                                    onEditOption = onEditOption,
                                    onAddOption = onAddOption,
                                    onCollapse = {
                                        expandedItemType = null
                                    }
                                )
                            }
                    }
                }
            }
        }
    }

}

@Composable
private fun SupplyCatalogTopBar(
    loading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {

    val infiniteTransition = rememberInfiniteTransition(label = "")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад"
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Каталог закупки",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Ингредиенты и расходники",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onRefresh,
                enabled = !loading
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Обновить",
                    modifier = Modifier.rotate(
                        if (loading) rotation else 0f
                    )
                )
            }
        }
    }

}

@Composable
private fun SupplyCatalogTile(
    group: SupplyCatalogGroup,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Surface(
        modifier = modifier
            .height(92.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = if (expanded) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = itemTypeLabel(group.itemType),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SupplyCatalogItemIcon(
                    itemType = group.itemType,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(30.dp)
                )

                Text(
                    text = variantsText(group.items.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(20.dp)
                )
            }
        }
    }

}

@Composable
private fun SupplyCatalogExpandedCard(
    group: SupplyCatalogGroup,
    canEdit: Boolean,
    onEditOption: (Int) -> Unit,
    onAddOption: (String) -> Unit,
    onCollapse: () -> Unit
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCollapse),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = itemTypeLabel(group.itemType),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = variantsText(group.items.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Свернуть",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            group.items.forEach { item ->
                HorizontalDivider()

                SupplyCatalogOptionCard(
                    item = item,
                    canEdit = canEdit,
                    onEditOption = onEditOption
                )
            }

            if (canEdit) {
                OutlinedButton(
                    onClick = {
                        onAddOption(group.itemType)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )

                    Text(
                        text = "Добавить вариант".asActionButtonText(),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }

}

@Composable
private fun SupplyCatalogOptionCard(
    item: SupplyCatalogEntity,
    canEdit: Boolean,
    onEditOption: (Int) -> Unit
) {

    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.title.ifBlank { itemTypeLabel(item.itemType) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                PriorityBadge(priority = item.priority)

                if (canEdit) {
                    IconButton(
                        onClick = {
                            onEditOption(item.id)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Text(
                text = "${item.supplier.ifBlank { "-" }} · арт. ${item.article.ifBlank { "-" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (item.comment.isNotBlank()) {
                CatalogLine(
                    label = "Комментарий",
                    value = item.comment,
                    maxLines = 2
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CatalogPriceText(
                    value = "до ${item.maxPrice} р / ${priceUnitFor(item.itemType)}",
                    modifier = Modifier.weight(1f)
                )

                if (item.url.isNotBlank()) {
                    IconButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(item.url)
                                )
                            )
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

}

@Composable
private fun PriorityBadge(priority: Int) {

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            text = "#$priority",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }

}

@Composable
private fun SupplyCatalogOption(
    item: SupplyCatalogEntity,
    canEdit: Boolean,
    onEditOption: (Int) -> Unit
) {

    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title.ifBlank { itemTypeLabel(item.itemType) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (canEdit) {
                IconButton(
                    onClick = {
                        onEditOption(item.id)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Редактировать вариант",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        PlainCatalogText(
            value = "Поставщик: ${item.supplier.ifBlank { "—" }}"
        )

        PlainCatalogText(
            value = "Артикул: ${item.article.ifBlank { "—" }}"
        )

        PlainCatalogText(
            value = "Приоритет: ${item.priority}"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CatalogPriceText(
                value = "Цена: до ${item.maxPrice} р / ${priceUnitFor(item.itemType)}",
                modifier = Modifier.weight(1f)
            )

            if (item.url.isNotBlank()) {
                IconButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(item.url)
                            )
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Открыть в магазине",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (item.comment.isNotBlank()) {
            CatalogLine(
                label = "Комментарий",
                value = item.comment,
                maxLines = 2
            )
        }

    }

}

@Composable
private fun CatalogLine(
    label: String,
    value: String,
    maxLines: Int = 1
) {

    if (value.isBlank()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }

}

@Composable
private fun PlainCatalogText(
    value: String
) {

    if (value.isBlank()) return

    Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

}

@Composable
private fun CatalogPriceText(
    value: String,
    modifier: Modifier = Modifier
) {

    if (value.isBlank()) return

    Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )

}

@Composable
private fun EmptyCatalog(
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Каталог пуст",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Нажмите обновить, чтобы загрузить данные из Google таблицы",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

}

private data class SupplyCatalogGroup(
    val itemType: String,
    val items: List<SupplyCatalogEntity>
)

private fun itemTypeLabel(itemType: String): String {
    return SupplyCatalogItemType.labelOf(itemType)
}


private fun priceUnitFor(itemType: String): String {
    return when (itemType) {
        SupplyCatalogItemType.Cups.code,
        SupplyCatalogItemType.Lids.code,
        SupplyCatalogItemType.Sugar.code,
        SupplyCatalogItemType.Stirrers.code,
        SupplyCatalogItemType.GarbageBags.code -> "шт."
        else -> "уп."
    }
}
private fun itemTypeOrder(itemType: String): Int {
    return SupplyCatalogItemType.orderOf(itemType)
}

@Composable
private fun SupplyCatalogItemIcon(
    itemType: String,
    modifier: Modifier = Modifier
) {

    val imageResource = when (itemType) {
        SupplyCatalogItemType.Cups.code -> R.drawable.ic_supply_cups
        SupplyCatalogItemType.Lids.code -> R.drawable.ic_supply_lids
        SupplyCatalogItemType.Coffee.code -> R.drawable.ic_supply_coffee_bean
        SupplyCatalogItemType.Milk.code -> R.drawable.ic_supply_milk
        SupplyCatalogItemType.Chocolate.code -> R.drawable.ic_supply_chocolate
        SupplyCatalogItemType.Topping.code -> R.drawable.ic_supply_topping_banana
        SupplyCatalogItemType.Sugar.code -> R.drawable.ic_supply_sugar_cubes
        SupplyCatalogItemType.Stirrers.code -> R.drawable.ic_supply_stirrers
        SupplyCatalogItemType.GarbageBags.code -> R.drawable.ic_supply_garbage_bags
        else -> null
    }

    if (imageResource != null) {
        Image(
            painter = painterResource(id = imageResource),
            contentDescription = null,
            modifier = modifier
        )
        return
    }

    val color = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
    ) {
        val stroke = Stroke(
            width = 2.4f,
            cap = StrokeCap.Round
        )
        val thinStroke = Stroke(
            width = 1.7f,
            cap = StrokeCap.Round
        )
        val w = size.width
        val h = size.height

        when (itemType) {
            SupplyCatalogItemType.Cups.code -> {
                val cup = Path().apply {
                    moveTo(w * 0.26f, h * 0.22f)
                    lineTo(w * 0.74f, h * 0.22f)
                    lineTo(w * 0.66f, h * 0.82f)
                    lineTo(w * 0.34f, h * 0.82f)
                    close()
                }

                drawPath(cup, color, style = stroke)
                drawLine(
                    color = color,
                    start = Offset(w * 0.32f, h * 0.42f),
                    end = Offset(w * 0.68f, h * 0.42f),
                    strokeWidth = 1.7f,
                    cap = StrokeCap.Round
                )
            }

            SupplyCatalogItemType.Lids.code -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.18f, h * 0.40f),
                    size = Size(w * 0.64f, h * 0.28f),
                    cornerRadius = CornerRadius(w * 0.08f, h * 0.08f),
                    style = stroke
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.28f, h * 0.32f),
                    end = Offset(w * 0.72f, h * 0.32f),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.24f, h * 0.68f),
                    end = Offset(w * 0.76f, h * 0.68f),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round
                )
            }

            SupplyCatalogItemType.Sugar.code -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.20f, h * 0.45f),
                    size = Size(w * 0.24f, h * 0.24f),
                    cornerRadius = CornerRadius(3f, 3f),
                    style = stroke
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.44f, h * 0.45f),
                    size = Size(w * 0.24f, h * 0.24f),
                    cornerRadius = CornerRadius(3f, 3f),
                    style = stroke
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.32f, h * 0.22f),
                    size = Size(w * 0.24f, h * 0.24f),
                    cornerRadius = CornerRadius(3f, 3f),
                    style = stroke
                )
            }

            SupplyCatalogItemType.Stirrers.code -> {
                drawLine(
                    color = color,
                    start = Offset(w * 0.24f, h * 0.78f),
                    end = Offset(w * 0.76f, h * 0.22f),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.36f, h * 0.78f),
                    end = Offset(w * 0.84f, h * 0.30f),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )
            }

            SupplyCatalogItemType.GarbageBags.code -> {
                val bag = Path().apply {
                    moveTo(w * 0.34f, h * 0.26f)
                    lineTo(w * 0.45f, h * 0.18f)
                    lineTo(w * 0.55f, h * 0.18f)
                    lineTo(w * 0.66f, h * 0.26f)
                    lineTo(w * 0.72f, h * 0.78f)
                    lineTo(w * 0.28f, h * 0.78f)
                    close()
                }

                drawPath(bag, color, style = stroke)
                drawLine(
                    color = color,
                    start = Offset(w * 0.40f, h * 0.30f),
                    end = Offset(w * 0.60f, h * 0.30f),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )
            }

            SupplyCatalogItemType.Coffee.code -> {
                rotateCanvas(
                    degrees = -25f,
                    pivot = center
                ) {
                    drawOval(
                        color = color,
                        topLeft = Offset(w * 0.28f, h * 0.16f),
                        size = Size(w * 0.44f, h * 0.68f),
                        style = stroke
                    )
                    drawLine(
                        color = color,
                        start = Offset(w * 0.50f, h * 0.22f),
                        end = Offset(w * 0.50f, h * 0.78f),
                        strokeWidth = 1.8f,
                        cap = StrokeCap.Round
                    )
                }
            }

            SupplyCatalogItemType.Milk.code -> {
                val carton = Path().apply {
                    moveTo(w * 0.30f, h * 0.30f)
                    lineTo(w * 0.44f, h * 0.14f)
                    lineTo(w * 0.70f, h * 0.30f)
                    lineTo(w * 0.70f, h * 0.82f)
                    lineTo(w * 0.30f, h * 0.82f)
                    close()
                }

                drawPath(carton, color, style = stroke)
                drawLine(
                    color = color,
                    start = Offset(w * 0.44f, h * 0.14f),
                    end = Offset(w * 0.44f, h * 0.82f),
                    strokeWidth = 1.7f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.40f, h * 0.52f),
                    end = Offset(w * 0.60f, h * 0.52f),
                    strokeWidth = 1.7f,
                    cap = StrokeCap.Round
                )
            }

            SupplyCatalogItemType.Chocolate.code -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.22f, h * 0.22f),
                    size = Size(w * 0.56f, h * 0.60f),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = stroke
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.22f, h * 0.42f),
                    end = Offset(w * 0.78f, h * 0.42f),
                    strokeWidth = 1.6f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.22f, h * 0.62f),
                    end = Offset(w * 0.78f, h * 0.62f),
                    strokeWidth = 1.6f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.50f, h * 0.22f),
                    end = Offset(w * 0.50f, h * 0.82f),
                    strokeWidth = 1.6f,
                    cap = StrokeCap.Round
                )
            }

            SupplyCatalogItemType.Topping.code -> {
                drawArc(
                    color = color,
                    startAngle = 20f,
                    sweepAngle = 210f,
                    useCenter = false,
                    topLeft = Offset(w * 0.12f, h * 0.20f),
                    size = Size(w * 0.60f, h * 0.60f),
                    style = Stroke(
                        width = 4f,
                        cap = StrokeCap.Round
                    )
                )
                drawOval(
                    color = color,
                    topLeft = Offset(w * 0.52f, h * 0.42f),
                    size = Size(w * 0.30f, h * 0.34f),
                    style = stroke
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.64f, h * 0.38f),
                    end = Offset(w * 0.74f, h * 0.30f),
                    strokeWidth = 1.7f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = color,
                    radius = 1.8f,
                    center = Offset(w * 0.62f, h * 0.56f)
                )
                drawCircle(
                    color = color,
                    radius = 1.8f,
                    center = Offset(w * 0.72f, h * 0.62f)
                )
            }

            else -> {
                drawCircle(
                    color = color,
                    radius = w * 0.28f,
                    center = center,
                    style = thinStroke
                )
            }
        }
    }

}

private fun variantsText(count: Int): String {
    val suffix = when {
        count % 10 == 1 && count % 100 != 11 -> "вариант"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "варианта"
        else -> "вариантов"
    }

    return "$count $suffix закупки"
}
