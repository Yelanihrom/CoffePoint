package ru.coffeepoint.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ru.coffeepoint.R
import ru.coffeepoint.core.auth.RolePreferences
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.status.attentionIssues
import ru.coffeepoint.feature.dashboard.model.isCritical
import ru.coffeepoint.feature.dashboard.model.supplyItems
import ru.coffeepoint.feature.dashboard.model.supplyAttentionTechServiceDashboardItems
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.warehouse.model.WarehouseStockItem

class StationAttentionNotification(
    private val context: Context
) {

    fun showRefillAttentionIfNeeded(
        stations: List<StationDetail>
    ) {

        if (!canShowForRefillRole()) return

        val lines = stations.mapNotNull { detail ->

            val issues = detail.attentionIssues()
                .filter { issue ->
                    issue.affectsStationStatus
                }

            if (issues.isEmpty()) return@mapNotNull null

            "${detail.station.name}: ${
                issues.joinToString(", ") { issue ->
                    issue.description
                }
            }"

        }

        if (lines.isEmpty()) return

        show(
            notificationId = REFILL_NOTIFICATION_ID,
            title = "Станции требуют заправки",
            text = lines.joinToString("\n")
        )

    }

    fun showSupplyAttentionIfNeeded(
        stations: List<StationDetail>
    ) {

        if (!canShowForSupplyRole()) return

        val lines = stations.mapNotNull { detail ->
            val criticalSupplyItems = detail.supplyItems()
                .filter { item -> item.isCritical }

            val techServiceItems = detail.supplyAttentionTechServiceDashboardItems(
                RolePreferences(context).getSelectedRole()
            )

            if (criticalSupplyItems.isEmpty() && techServiceItems.isEmpty()) {
                return@mapNotNull null
            }

            val parts = buildList {
                if (criticalSupplyItems.isNotEmpty()) {
                    add(
                        "критично ${
                            criticalSupplyItems.joinToString(", ") { item ->
                                "${item.title} ${item.value}"
                            }
                        }"
                    )
                }

                if (techServiceItems.isNotEmpty()) {
                    add(
                        "обслужить ${
                            techServiceItems.joinToString(", ") { item ->
                                item.title
                            }
                        }"
                    )
                }
            }

            "${detail.station.name}: ${parts.joinToString("; ")}"
        }

        if (lines.isEmpty()) return

        show(
            notificationId = SUPPLY_NOTIFICATION_ID,
            title = "Требуется снабжение",
            text = lines.joinToString("\n")
        )

    }

    fun showWarehouseStockAttentionIfNeeded(
        items: List<WarehouseStockItem>
    ) {

        if (!canShowForSupplyRole()) return

        val lines = items
            .filter { item ->
                item.minQuantity > 0 &&
                    item.maxQuantity > 0 &&
                    item.quantity < item.minQuantity
            }
            .map { item ->
                val orderQuantity = (item.maxQuantity - item.quantity)
                    .coerceAtLeast(0)

                "${item.title} $orderQuantity"
            }

        if (lines.isEmpty()) return

        show(
            notificationId = WAREHOUSE_STOCK_NOTIFICATION_ID,
            title = "Нужно пополнить склад",
            text = lines.joinToString(", ")
        )

    }

    private fun show(
        notificationId: Int,
        title: String,
        text: String
    ) {

        if (!canShowNotifications()) return

        createChannel()

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text.lineSequence().firstOrNull() ?: title)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            notificationId,
            notification
        )

    }

    private fun createChannel() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = context.getSystemService(
            NotificationManager::class.java
        )

        manager.createNotificationChannel(channel)

    }

    private fun canShowNotifications(): Boolean {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    }

    private fun canShowForRefillRole(): Boolean {

        return when (RolePreferences(context).getSelectedRole()) {
            UserRole.MANAGER,
            UserRole.OPERATOR -> true

            UserRole.SUPPLY_OPERATOR,
            null -> false
        }

    }

    private fun canShowForSupplyRole(): Boolean {

        return when (RolePreferences(context).getSelectedRole()) {
            UserRole.MANAGER,
            UserRole.SUPPLY_OPERATOR -> true

            UserRole.OPERATOR,
            null -> false
        }

    }

    private companion object {

        const val CHANNEL_ID = "sales_refresh"
        const val CHANNEL_NAME = "Обновление данных"
        const val REFILL_NOTIFICATION_ID = 1001
        const val SUPPLY_NOTIFICATION_ID = 1002
        const val WAREHOUSE_STOCK_NOTIFICATION_ID = 1003

    }

}
