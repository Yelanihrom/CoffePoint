package ru.coffeepoint.feature.service.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.StationInventory
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.feature.station.model.next
import ru.coffeepoint.feature.station.model.previous

class ServiceStep1ViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = StationRepository(application)
    var station by mutableStateOf<StationDetail?>(null)
        private set

    var inventory by mutableStateOf<StationInventory?>(null)
        private set

    fun loadStation(stationId: Int) {

        if (station != null) return

        viewModelScope.launch {

            val result = repository.getStation(stationId) ?: return@launch

            station = result
            inventory = result.inventory.copy()

        }

    }

    fun plusCups() {

        val current = inventory ?: return
        val max = station?.settings?.cupsMax ?: return

        inventory = current.copy(
            cups = increment(
                current.cups,
                max
            )
        )

    }

    fun minusCups() {

        val current = inventory ?: return

        inventory = current.copy(
            cups = decrement(current.cups)
        )

    }

    fun plusLids() {

        val current = inventory ?: return
        val max = station?.settings?.lidsMax ?: return

        inventory = current.copy(
            lids = increment(
                current.lids,
                max
            )
        )

    }

    fun minusLids() {

        val current = inventory ?: return

        inventory = current.copy(
            lids = decrement(current.lids)
        )

    }

    fun plusCoffee() {

        val current = inventory ?: return
        val max = station?.settings?.coffeeMax ?: return

        inventory = current.copy(
            coffee = increment(
                current.coffee,
                max
            )
        )

    }

    fun minusCoffee() {

        val current = inventory ?: return

        inventory = current.copy(
            coffee = decrement(current.coffee)
        )

    }

    fun plusMilk() {

        val current = inventory ?: return
        val max = station?.settings?.milkMax ?: return

        inventory = current.copy(
            milk = increment(
                current.milk,
                max
            )
        )

    }

    fun minusMilk() {

        val current = inventory ?: return

        inventory = current.copy(
            milk = decrement(current.milk)
        )

    }

    fun plusChocolate() {

        val current = inventory ?: return
        val max = station?.settings?.chocolateMax ?: return

        inventory = current.copy(
            chocolate = increment(
                current.chocolate,
                max
            )
        )

    }

    fun minusChocolate() {

        val current = inventory ?: return

        inventory = current.copy(
            chocolate = decrement(current.chocolate)
        )

    }

    fun plusTopping() {

        val current = inventory ?: return
        val max = station?.settings?.toppingMax ?: return

        inventory = current.copy(
            topping = increment(
                current.topping,
                max
            )
        )

    }

    fun minusTopping() {

        val current = inventory ?: return

        inventory = current.copy(
            topping = decrement(current.topping)
        )

    }
    fun saveChanges() {

        val detail = station ?: return
        val currentInventory = inventory ?: return

        viewModelScope.launch {

            repository.saveStation(
                detail.copy(
                    inventory = currentInventory
                )
            )

        }

    }
    fun nextSugar() {

        inventory = inventory?.copy(
            sugar = inventory!!.sugar.next()
        )

    }

    fun nextStirrers() {

        inventory = inventory?.copy(
            stirrers = inventory!!.stirrers.next()
        )

    }

    fun previousSugar() {

        inventory = inventory?.copy(
            sugar = inventory!!.sugar.previous()
        )

    }

    fun previousStirrers() {

        inventory = inventory?.copy(
            stirrers = inventory!!.stirrers.previous()
        )

    }
    private fun increment(
        current: Int,
        max: Int
    ): Int {

        return (current + 1)
            .coerceAtMost(max)

    }

    private fun decrement(
        current: Int
    ): Int {

        return (current - 1)
            .coerceAtLeast(0)

    }

}