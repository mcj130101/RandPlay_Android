package com.mcj130101.randplayer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcj130101.randplayer.data.repository.SettingsRepository
import com.mcj130101.randplayer.domain.model.AppSettings
import com.mcj130101.randplayer.domain.model.AppTheme
import com.mcj130101.randplayer.domain.model.PlayerSelectionMode
import com.mcj130101.randplayer.domain.model.RandomMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setRandomMode(mode: RandomMode) {
        viewModelScope.launch {
            settingsRepository.updateRandomMode(mode)
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.updateTheme(theme)
        }
    }

    fun setAccentColor(hex: String) {
        viewModelScope.launch {
            settingsRepository.updateAccentColor(hex)
        }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoSync(enabled)
        }
    }

    fun setPlayOnShake(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePlayOnShake(enabled)
        }
    }

    fun setSyncInterval(hours: Int) {
        viewModelScope.launch {
            settingsRepository.updateSyncInterval(hours)
        }
    }

    fun setSyncWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSyncWifiOnly(enabled)
        }
    }

    fun setSyncOnMobile(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSyncOnMobile(enabled)
        }
    }

    fun setSyncWhileCharging(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSyncWhileCharging(enabled)
        }
    }

    fun setSyncBeforePlayback(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSyncBeforePlayback(enabled)
        }
    }

    fun setSmartShuffleLimit(limit: Int) {
        viewModelScope.launch {
            settingsRepository.updateSmartShuffleLimit(limit)
        }
    }

    fun setPreferredPlayer(packageName: String?) {
        viewModelScope.launch {
            settingsRepository.updatePreferredPlayer(packageName)
        }
    }

    fun setPlayerSelectionMode(mode: PlayerSelectionMode) {
        viewModelScope.launch {
            settingsRepository.updatePlayerSelectionMode(mode)
        }
    }
}
