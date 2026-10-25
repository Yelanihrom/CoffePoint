package ru.coffeepoint.core.sync.google

import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import ru.coffeepoint.BuildConfig
import ru.coffeepoint.core.data.remote.dto.TerminalCommandRequestDto
import ru.coffeepoint.core.data.remote.network.ApiProvider

class TestEnvironmentSafetyTest {
    @Test fun `test installation cannot replace the production app`() {
        assumeTrue(BuildConfig.IS_TEST_ENVIRONMENT)
        assertEquals("ru.coffeepoint.ci", BuildConfig.APPLICATION_ID)
        assertTrue(BuildConfig.VERSION_NAME.endsWith("-ci"))
    }

    @Test fun `CI sync uses only explicit test settings`() {
        assumeTrue(BuildConfig.IS_TEST_ENVIRONMENT)
        assertEquals(BuildConfig.TEST_SHEETS_WEB_APP_URL, GoogleSheetsSyncConfig.WEB_APP_URL)
        assertEquals(BuildConfig.TEST_SHEETS_SYNC_TOKEN, GoogleSheetsSyncConfig.SYNC_TOKEN)
        assertFalse(GoogleSheetsSyncConfig.isEnabled("", "test-token"))
        assertFalse(GoogleSheetsSyncConfig.isEnabled("https://example.test/exec", ""))
    }

    @Test fun `CI refuses the production Sheets endpoint`() {
        assumeTrue(BuildConfig.IS_TEST_ENVIRONMENT)
        assertFalse(GoogleSheetsSyncConfig.isEnabled(
            GoogleSheetsSyncConfig.PRODUCTION_WEB_APP_URL,
            "test-token"
        ))
    }

    @Test fun `CI blocks terminal commands before network transport`() {
        assumeTrue(BuildConfig.IS_TEST_ENVIRONMENT)
        val error = assertThrows(IOException::class.java) {
            runBlocking {
                ApiProvider.terminalCommandApi.sendCommand(
                    terminalId = 0,
                    token = "test-token",
                    command = TerminalCommandRequestDto(commandId = 2)
                )
            }
        }
        assertTrue(error.message.orEmpty().contains("disabled in the test environment"))
    }
}
