package com.gow.eng192lab.ui.eventinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gow.eng192lab.data.model.ScheduleItem
import com.gow.eng192lab.data.model.SpeakerInfo
import com.gow.eng192lab.data.model.SponsorConfig
import com.gow.eng192lab.data.theme.ThemeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Event Info screen.
 *
 * Exposes [StateFlow]s derived from [ThemeRepository.config] for schedule items,
 * speakers, event name, tagline, and sponsors.
 *
 * @param themeRepository  Repository holding the active [com.gow.eng192lab.data.model.ThemeConfig].
 * @param scope            CoroutineScope for StateFlow sharing. Defaults to [viewModelScope].
 *                         Inject a [kotlinx.coroutines.test.TestScope] in unit tests.
 */
class EventInfoViewModel(
    private val themeRepository: ThemeRepository,
    scope: CoroutineScope? = null
) : ViewModel() {

    private val coroutineScope: CoroutineScope by lazy { scope ?: viewModelScope }

    /** Schedule items from the active theme configuration. */
    val schedule: StateFlow<List<ScheduleItem>> by lazy {
        themeRepository.config
            .map { it.schedule }
            .stateIn(
                coroutineScope,
                SharingStarted.Eagerly,
                themeRepository.config.value.schedule
            )
    }

    /** Speaker profiles from the active theme configuration. */
    val speakers: StateFlow<List<SpeakerInfo>> by lazy {
        themeRepository.config
            .map { it.speakers }
            .stateIn(
                coroutineScope,
                SharingStarted.Eagerly,
                themeRepository.config.value.speakers
            )
    }

    /** Event name from the active theme configuration. */
    val eventName: StateFlow<String> by lazy {
        themeRepository.config
            .map { it.eventName }
            .stateIn(
                coroutineScope,
                SharingStarted.Eagerly,
                themeRepository.config.value.eventName
            )
    }

    /** Tagline from the active theme configuration. */
    val tagline: StateFlow<String> by lazy {
        themeRepository.config
            .map { it.tagline }
            .stateIn(
                coroutineScope,
                SharingStarted.Eagerly,
                themeRepository.config.value.tagline
            )
    }

    /** Sponsors list from the active theme configuration (for [com.gow.eng192lab.ui.common.SponsorBar]). */
    val sponsors: StateFlow<List<SponsorConfig>> by lazy {
        themeRepository.config
            .map { it.sponsors }
            .stateIn(
                coroutineScope,
                SharingStarted.Eagerly,
                themeRepository.config.value.sponsors
            )
    }
}
