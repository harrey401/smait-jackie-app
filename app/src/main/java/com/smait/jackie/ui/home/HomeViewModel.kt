package com.smait.jackie.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smait.jackie.data.theme.ThemeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val themeRepository: ThemeRepository,
    scope: CoroutineScope? = null
) : ViewModel() {

    private val coroutineScope: CoroutineScope by lazy { scope ?: viewModelScope }

    val eventName: StateFlow<String> by lazy {
        themeRepository.config
            .map { it.eventName }
            .stateIn(coroutineScope, SharingStarted.Eagerly, themeRepository.config.value.eventName)
    }

    val tagline: StateFlow<String> by lazy {
        themeRepository.config
            .map { it.tagline }
            .stateIn(coroutineScope, SharingStarted.Eagerly, themeRepository.config.value.tagline)
    }
}
