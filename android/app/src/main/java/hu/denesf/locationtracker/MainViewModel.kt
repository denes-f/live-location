package hu.denesf.locationtracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val deviceId: String = "",
    val trackingEnabled: Boolean = false,
    val deviceIdSet: Boolean = false,   // false = first run / not set yet
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    val uiState: StateFlow<MainUiState> =
        repo.settingsFlow
            .mapToUi()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MainUiState()
            )

    private fun kotlinx.coroutines.flow.Flow<Settings>.mapToUi() =
        this.map { s ->
            MainUiState(
                deviceId = s.deviceId.orEmpty(),
                trackingEnabled = s.trackingEnabled,
                deviceIdSet = !s.deviceId.isNullOrBlank()
            )
        }

    fun updateDeviceId(newId: String) {
        viewModelScope.launch {
            repo.setDeviceId(newId.trim())
        }
    }

    fun setTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.setTrackingEnabled(enabled)
        }
    }
}