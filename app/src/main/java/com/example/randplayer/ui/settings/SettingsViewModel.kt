package com.example.randplayer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randplayer.data.repository.SettingsRepository
import com.example.randplayer.domain.model.AppSettings
import com.example.randplayer.domain.model.AppTheme
import com.example.randplayer.domain.model.RandomMode
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

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoSync(enabled)
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
}
