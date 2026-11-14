package ru.coffeepoint

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import ru.coffeepoint.feature.dashboard.component.StationCard
import ru.coffeepoint.feature.dashboard.model.DashboardMode
import ru.coffeepoint.feature.station.model.*
import ru.coffeepoint.ui.theme.CoffeePointTheme

class FullSupplyStationCardTest {
    @get:Rule val compose = createComposeRule()

    private fun show(excluded: Boolean = false) {
        val detail = StationDetail(
            CoffeeStation(id = 1, name = "Station", lastServiceDate = "", salesAfterService = 0,
                excludeFromSupplyPlanning = excluded, mixersServiceDate = "01.01.2999"),
            StationInventory(10, 10, 10, 10, 10, 10, 10, 10),
            StationSettings(cupsMax = 10, lidsMax = 10, coffeeMax = 10, milkMax = 10,
                chocolateMax = 10, toppingMax = 10)
        )
        compose.setContent {
            CoffeePointTheme {
                StationCard(detail, DashboardMode.SUPPLY, false, false, false)
            }
        }
    }

    @Test fun fullStationShowsLoadedStatusWithoutPurchaseHeading() {
        show()
        compose.onNodeWithText("Станция полностью загружена").assertIsDisplayed()
        compose.onNodeWithText("Необходимо закупить").assertDoesNotExist()
    }

    @Test fun excludedStationIsNotReportedAsFullyLoaded() {
        show(excluded = true)
        compose.onNodeWithText("Станция исключена из снабжения").assertIsDisplayed()
        compose.onNodeWithText("Станция полностью загружена").assertDoesNotExist()
    }
}
