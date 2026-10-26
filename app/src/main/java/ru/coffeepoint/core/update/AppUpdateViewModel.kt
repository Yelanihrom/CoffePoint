package ru.coffeepoint.core.update

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.coffeepoint.BuildConfig
import java.io.File

data class AppUpdateUiState(
    val update: UpdateManifest? = null,
    val downloading: Boolean = false,
    val progress: Int = 0,
    val file: File? = null,
    val error: String? = null,
    val checking: Boolean = false,
    val checkMessage: String? = null,
    val checkFailed: Boolean = false
)

class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {
    var state by mutableStateOf(AppUpdateUiState())
        private set
    private val remote = AppUpdateRemoteSource()
    private val preferences = application.getSharedPreferences("app_updates", 0)
    private var started = false
    private var job: Job? = null
    private var generation = 0
    private var autoInstall = false

    fun checkOnLaunch() {
        if (started || BuildConfig.IS_TEST_ENVIRONMENT) return
        started = true
        check(manual = false)
    }

    fun checkManually() {
        if (BuildConfig.IS_TEST_ENVIRONMENT) {
            state = state.copy(checkMessage = "Проверка обновлений доступна в рабочей версии", checkFailed = false)
            return
        }
        check(manual = true)
    }

    private fun check(manual: Boolean) {
        if (state.checking || state.downloading) return
        state = state.copy(checking = true, checkMessage = null, checkFailed = false)
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                if (!manual && now < preferences.getLong("postponedUntil", 0)) return@launch
                val installed = withContext(Dispatchers.IO) { AppUpdatePlatform.installed(getApplication()) }
                val cached = preferences.getString("manifest", null)?.let {
                    runCatching { remote.validator.parse(it, installed) }.getOrNull()
                }
                if (!manual && cached != null) { state = AppUpdateUiState(update = cached); return@launch }
                if (!UpdateCheckPolicy.shouldCheck(now, preferences.getLong("success", 0), preferences.getLong("attempt", 0), manual)) return@launch
                preferences.edit().putLong("attempt", now).apply()
                val update = remote.check(installed)
                preferences.edit().putLong("success", now).putString("manifest", update?.toJson()).apply {
                    if (manual) remove("postponedUntil")
                }.apply()
                state = AppUpdateUiState(
                    update = update,
                    checkMessage = if (manual && update == null) "Доступных обновлений нет" else null
                )
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                Log.w("AppUpdate", "Update check unavailable: ${error.javaClass.simpleName}")
                if (manual) state = state.copy(checkMessage = "Не удалось проверить обновление. Повторите позже", checkFailed = true)
            } finally { state = state.copy(checking = false) }
        }
    }

    fun download() {
        val update = state.update ?: return
        if (state.downloading || state.checking) return
        val current = ++generation
        state = state.copy(downloading = true, error = null, progress = 0)
        job = viewModelScope.launch {
            var downloaded: File? = null
            try {
                val directory = File(getApplication<Application>().cacheDir, "updates")
                withContext(Dispatchers.IO) {
                    directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 7 * UpdateCheckPolicy.DAY }?.forEach { it.delete() }
                }
                downloaded = remote.download(update, directory) { percent ->
                    viewModelScope.launch { if (generation == current) state = state.copy(progress = percent) }
                }
                withContext(Dispatchers.IO) { AppUpdatePlatform.verify(getApplication(), downloaded, update) }
                autoInstall = true
                state = state.copy(downloading = false, file = downloaded)
            } catch (error: CancellationException) { downloaded?.delete(); throw error }
            catch (error: Exception) {
                downloaded?.delete()
                if (generation == current) state = state.copy(downloading = false, error = if (error is InvalidUpdateException)
                    "Не удалось проверить APK. Установка отменена." else "Не удалось скачать обновление. Проверьте подключение и повторите.")
            }
        }
    }

    fun consumeAutoInstall(): Boolean = autoInstall.also { autoInstall = false }

    fun installationError() { state = state.copy(error = "Не удалось открыть установщик. Разрешите установку из этого источника и повторите.") }

    fun dismiss() {
        generation++
        remote.cancelDownload()
        job?.cancel()
        preferences.edit().putLong("postponedUntil", System.currentTimeMillis() + UpdateCheckPolicy.DAY).apply()
        state = AppUpdateUiState()
    }

    override fun onCleared() { remote.cancelDownload(); super.onCleared() }
}
