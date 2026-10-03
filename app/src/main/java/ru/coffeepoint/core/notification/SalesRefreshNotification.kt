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
import ru.coffeepoint.feature.station.model.StationDetail

class SalesRefreshNotification(
    private val context: Context
) {

    fun showAttentionIfNeeded(
        stations: List<StationDetail>
    ) {

        if (!canShowForCurrentRole()) return

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
            title = "Станции требуют заправки",
            text = lines.joinToString("\n")
        )

    }

    private fun show(
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
            NOTIFICATION_ID,
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

    private fun canShowForCurrentRole(): Boolean {

        return when (RolePreferences(context).getSelectedRole()) {
            UserRole.MANAGER,
            UserRole.OPERATOR -> true

            UserRole.SUPPLY_OPERATOR,
            null -> false
        }

    }

    private companion object {

        const val CHANNEL_ID = "sales_refresh"
        const val CHANNEL_NAME = "Обновление продаж"
        const val NOTIFICATION_ID = 1001

    }

}
