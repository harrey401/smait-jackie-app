package com.gow.eng192lab

import com.gow.eng192lab.data.model.CardConfig
import com.gow.eng192lab.data.model.ScheduleItem
import com.gow.eng192lab.data.model.SpeakerInfo
import com.gow.eng192lab.data.model.SponsorConfig
import com.gow.eng192lab.data.model.ThemeConfig
import com.gow.eng192lab.data.theme.ThemeRepository
import com.gow.eng192lab.ui.eventinfo.EventInfoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for EventInfoViewModel.
 *
 * Verifies that EventInfoViewModel correctly maps ThemeRepository.config to
 * schedule, speakers, eventName, tagline, and sponsors StateFlows.
 */
@ExperimentalCoroutinesApi
class EventInfoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var mockThemeRepository: ThemeRepository
    private lateinit var configFlow: MutableStateFlow<ThemeConfig>

    private val wie2026Config = ThemeConfig(
        eventName = "WiE 2026",
        tagline = "Women in Engineering Conference",
        sponsors = listOf(
            SponsorConfig(name = "Google", logoAsset = "google_logo.png"),
            SponsorConfig(name = "Apple", logoAsset = "apple_logo.png"),
            SponsorConfig(name = "Amazon", logoAsset = "amazon_logo.png")
        ),
        cards = listOf(
            CardConfig(label = "Ask Me Anything", action = "navigate:chat", icon = "chat")
        ),
        schedule = listOf(
            ScheduleItem(time = "9:00 AM", title = "Keynote", speaker = "Dr. Smith",
                location = "Main Hall", track = "Opening"),
            ScheduleItem(time = "10:30 AM", title = "Panel: Women in AI", speaker = "Multiple",
                location = "Room 101", track = "AI"),
            ScheduleItem(time = "1:00 PM", title = "Networking Lunch", speaker = "",
                location = "Cafeteria", track = ""),
            ScheduleItem(time = "2:00 PM", title = "Workshop: ML Basics", speaker = "Dr. Patel",
                location = "Lab 1", track = "Workshop"),
            ScheduleItem(time = "4:00 PM", title = "Closing Ceremony", speaker = "Dean Johnson",
                location = "Main Hall", track = "Closing"),
            ScheduleItem(time = "5:00 PM", title = "Networking Reception", speaker = "",
                location = "Lobby", track = "")
        ),
        speakers = listOf(
            SpeakerInfo(name = "Dr. Jane Smith", title = "Professor of CS",
                bio = "Expert in AI research with 20 years experience.",
                photoAsset = "smith.png"),
            SpeakerInfo(name = "Dr. Priya Patel", title = "ML Engineer",
                bio = "Machine learning practitioner at a top tech company.",
                photoAsset = "patel.png"),
            SpeakerInfo(name = "Dean Sarah Johnson", title = "Dean of Engineering",
                bio = "Leading engineering education transformation.",
                photoAsset = "johnson.png")
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        configFlow = MutableStateFlow(wie2026Config)
        mockThemeRepository = mock()
        whenever(mockThemeRepository.config).thenReturn(configFlow)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): EventInfoViewModel =
        EventInfoViewModel(mockThemeRepository, testScope)

    // Test 1: EventInfoViewModel.schedule returns schedule items from ThemeConfig
    @Test
    fun `schedule returns schedule items from ThemeConfig`() = testScope.runTest {
        val viewModel = createViewModel()
        val schedule = viewModel.schedule.first()

        assertEquals(6, schedule.size)
        assertEquals("9:00 AM", schedule[0].time)
        assertEquals("Keynote", schedule[0].title)
        assertEquals("Dr. Smith", schedule[0].speaker)
        assertEquals("Main Hall", schedule[0].location)
        assertEquals("Opening", schedule[0].track)
    }

    // Test 2: EventInfoViewModel.speakers returns speakers from ThemeConfig
    @Test
    fun `speakers returns speakers from ThemeConfig`() = testScope.runTest {
        val viewModel = createViewModel()
        val speakers = viewModel.speakers.first()

        assertEquals(3, speakers.size)
        assertEquals("Dr. Jane Smith", speakers[0].name)
        assertEquals("Professor of CS", speakers[0].title)
        assertEquals("Dr. Priya Patel", speakers[1].name)
        assertEquals("Dean Sarah Johnson", speakers[2].name)
    }

    // Test 3: EventInfoViewModel.eventName returns ThemeConfig.eventName
    @Test
    fun `eventName returns ThemeConfig eventName`() = testScope.runTest {
        val viewModel = createViewModel()
        val eventName = viewModel.eventName.first()

        assertEquals("WiE 2026", eventName)
    }

    // Test 4: Empty schedule list results in empty state (no crash)
    @Test
    fun `empty schedule list returns empty list without crash`() = testScope.runTest {
        val emptyConfig = wie2026Config.copy(schedule = emptyList())
        configFlow.value = emptyConfig

        val viewModel = createViewModel()
        val schedule = viewModel.schedule.first()

        assertTrue(schedule.isEmpty())
    }

    @Test
    fun `empty speakers list returns empty list without crash`() = testScope.runTest {
        val emptyConfig = wie2026Config.copy(speakers = emptyList())
        configFlow.value = emptyConfig

        val viewModel = createViewModel()
        val speakers = viewModel.speakers.first()

        assertTrue(speakers.isEmpty())
    }

    @Test
    fun `tagline returns ThemeConfig tagline`() = testScope.runTest {
        val viewModel = createViewModel()
        val tagline = viewModel.tagline.first()

        assertEquals("Women in Engineering Conference", tagline)
    }

    @Test
    fun `sponsors returns ThemeConfig sponsors`() = testScope.runTest {
        val viewModel = createViewModel()
        val sponsors = viewModel.sponsors.first()

        assertEquals(3, sponsors.size)
        assertEquals("Google", sponsors[0].name)
        assertEquals("Apple", sponsors[1].name)
        assertEquals("Amazon", sponsors[2].name)
    }

    @Test
    fun `schedule updates when ThemeConfig changes`() = testScope.runTest {
        val viewModel = createViewModel()

        // Initial: 6 items
        assertEquals(6, viewModel.schedule.first().size)

        // Update to new config with 1 item
        val newConfig = wie2026Config.copy(schedule = listOf(
            ScheduleItem(time = "3:00 PM", title = "Single Event")
        ))
        configFlow.value = newConfig

        val updatedSchedule = viewModel.schedule.first()
        assertEquals(1, updatedSchedule.size)
        assertEquals("Single Event", updatedSchedule[0].title)
    }
}
