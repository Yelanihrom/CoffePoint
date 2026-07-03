package ru.coffeepoint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.DatabaseSeeder
import ru.coffeepoint.navigation.AppNavigation
import ru.coffeepoint.ui.theme.CoffeePointTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {

            DatabaseSeeder.seed(this@MainActivity)

        }

        setContent {

            CoffeePointTheme {

                AppNavigation()

            }

        }

    }

}