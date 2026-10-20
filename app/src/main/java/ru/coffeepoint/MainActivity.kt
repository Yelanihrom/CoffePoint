package ru.coffeepoint

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ru.coffeepoint.core.sync.google.worker.GoogleSheetsSyncScheduler
import ru.coffeepoint.core.worker.StationAttentionScheduler
import ru.coffeepoint.navigation.AppNavigation
import ru.coffeepoint.ui.theme.CoffeePointTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        StationAttentionScheduler.schedule(this)
        GoogleSheetsSyncScheduler.schedule(this)

        setContent {

            CoffeePointTheme {

                AppNavigation()

            }

        }

    }

    private fun requestNotificationPermission() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val permission = Manifest.permission.POST_NOTIFICATIONS

        if (
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(permission),
            NOTIFICATION_PERMISSION_REQUEST_CODE
        )

    }

    private companion object {

        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 100

    }

}
