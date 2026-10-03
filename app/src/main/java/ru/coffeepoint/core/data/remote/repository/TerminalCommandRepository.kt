package ru.coffeepoint.core.data.remote.repository

import ru.coffeepoint.core.data.remote.VendistaCredentials
import ru.coffeepoint.core.data.remote.dto.TerminalCommandRequestDto
import ru.coffeepoint.core.data.remote.network.ApiProvider

class TerminalCommandRepository {

    private val api = ApiProvider.terminalCommandApi

    suspend fun rebootTerminal(
        terminalId: Long
    ) {

        sendCommand(
            terminalId = terminalId,
            commandId = REBOOT_COMMAND_ID
        )

    }

    suspend fun turnScreenOff(
        terminalId: Long
    ) {

        sendCommand(
            terminalId = terminalId,
            commandId = SCREEN_MODE_COMMAND_ID,
            parameter1 = SCREEN_OFF_AND_IGNORE_TOUCH
        )

    }

    suspend fun turnScreenOn(
        terminalId: Long
    ) {

        sendCommand(
            terminalId = terminalId,
            commandId = SCREEN_MODE_COMMAND_ID,
            parameter1 = SCREEN_ON
        )

    }

    private suspend fun sendCommand(
        terminalId: Long,
        commandId: Int,
        parameter1: Int? = null
    ) {

        api.sendCommand(
            terminalId = terminalId,
            token = VendistaCredentials.TOKEN,
            command = TerminalCommandRequestDto(
                commandId = commandId,
                parameter1 = parameter1
            )
        )

    }

    private companion object {

        const val REBOOT_COMMAND_ID = 2
        const val SCREEN_MODE_COMMAND_ID = 15
        const val SCREEN_ON = 0
        const val SCREEN_OFF_AND_IGNORE_TOUCH = 1

    }

}
