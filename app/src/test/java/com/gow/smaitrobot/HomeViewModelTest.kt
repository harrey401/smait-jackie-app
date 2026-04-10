package com.gow.eng192lab

import com.gow.eng192lab.data.model.CardConfig
import com.gow.eng192lab.data.model.ScheduleItem
import com.gow.eng192lab.data.model.SpeakerInfo
import com.gow.eng192lab.data.model.SponsorConfig
import com.gow.eng192lab.data.model.ThemeConfig
import com.gow.eng192lab.data.theme.ThemeRepository
import com.gow.eng192lab.ui.home.CardAction
import com.gow.eng192lab.ui.home.HomeViewModel
import com.gow.eng192lab.navigation.Screen
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
 * Unit tests for HomeViewModel.
 *
 * Verifies that HomeViewModel correctly maps ThemeRepository.config to
 * cards, eventName, tagline, and sponsors StateFlows, and that
 * parseCardAction() returns the correct sealed CardAction.
 */
@ExperimentalCoroutinesApi
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var mockThemeRepository: ThemeRepository
    private lateinit var configFlow: MutableStateFlow<ThemeConfig>

    private val wie2026Config = ThemeConfig(
        eventName = "WiE 2026",
        tagline = "Women in Engineering Conference",
        sponsors = listOf(
            SponsorConfig(name = "Google", logoAsset = "google_logo.png"),
            SponsorConfig(name = "Apple", logoAsset = "apple_logo.png")
        ),
        cards = listOf(
            CardConfig(label = "Ask Me Anything", action = "navigate:chat", icon = "chat"),
            CardConfig(label = "Find a Location", action = "navigate:map", icon = "map"),
            CardConfig(label = "Keynote", action = "inline:keynote", icon = "star"),
            CardConfig(label = "Sessions", action = "inline:sessions", icon = "schedule"),
            CardConfig(label = "Facilities", action = "navigate:facilities", icon = "location"),
            CardConfig(label = "Event Info", action = "navigate:eventinfo", icon = "info")
        ),
        schedule = listOf(ScheduleItem(time = "9:00 AM", title = "Keynote")),
        speakers = listOf(SpeakerInfo(name = "Dr. Jane Smith"))
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

    private fun createViewModel(): HomeViewModel = HomeViewModel(mockThemeRepository, testScope)

    // Test 1: HomeViewModel.cards returns cards from ThemeConfig (6 cards for WiE)
    @Test
    fun `cards returns all cards from ThemeConfig`() = testScope.runTest {
        val viewModel = createViewModel()
        val cards = viewModel.cards.first()

        assertEquals(6, cards.size)
        assertEquals("Ask Me Anything", cards[0].label)
        assertEquals("navigate:chat", cards[0].action)
        assertEquals("Find a Location", cards[1].label)
        assertEquals("Keynote", cards[2].label)
    }

    // Test 2: HomeViewModel.eventName returns ThemeConfig.eventName
    @Test
    fun `eventName returns ThemeConfig eventName`() = testScope.runTest {
        val viewModel = createViewModel()
        val eventName = viewModel.eventName.first()

        assertEquals("WiE 2026", eventName)
    }

    // Test 3: HomeViewModel.sponsors returns ThemeConfig.sponsors list
    @Test
    fun `sponsors returns ThemeConfig sponsors list`() = testScope.runTest {
        val viewModel = createViewModel()
        val sponsors = viewModel.sponsors.first()

        assertEquals(2, sponsors.size)
        assertEquals("Google", sponsors[0].name)
        assertEquals("Apple", sponsors[1].name)
    }

    // Test 4: parseCardAction("navigate:chat") returns NavigateToTab(Screen.Chat)
    @Test
    fun `parseCardAction navigate chat returns NavigateToTab Chat`() = testScope.runTest {
        val viewModel = createViewModel()
        val action = viewModel.parseCardAction("navigate:chat")

        assertTrue(action is CardAction.NavigateToTab)
        val navAction = action as CardAction.NavigateToTab
        assertTrue(navAction.screen is Screen.Chat)
    }

    // Test 5: parseCardAction("inline:keynote") returns ShowInlineContent("keynote")
    @Test
    fun `parseCardAction inline keynote returns ShowInlineContent keynote`() = testScope.runTest {
        val viewModel = createViewModel()
        val action = viewModel.parseCardAction("inline:keynote")

        assertTrue(action is CardAction.ShowInlineContent)
        val inlineAction = action as CardAction.ShowInlineContent
        assertEquals("keynote", inlineAction.contentKey)
    }

    @Test
    fun `parseCardAction navigate map returns NavigateToTab Map`() = testScope.runTest {
        val viewModel = createViewModel()
        val action = viewModel.parseCardAction("navigate:map")

        assertTrue(action is CardAction.NavigateToTab)
        val navAction = action as CardAction.NavigateToTab
        assertTrue(navAction.screen is Screen.Map)
    }

    @Test
    fun `parseCardAction navigate facilities returns NavigateToTab Facilities`() = testScope.runTest {
        val viewModel = createViewModel()
        val action = viewModel.parseCardAction("navigate:facilities")

        assertTrue(action is CardAction.NavigateToTab)
        val navAction = action as CardAction.NavigateToTab
        assertTrue(navAction.screen is Screen.Facilities)
    }

    @Test
    fun `parseCardAction navigate eventinfo returns NavigateToTab EventInfo`() = testScope.runTest {
        val viewModel = createViewModel()
        val action = viewModel.parseCardAction("navigate:eventinfo")

        assertTrue(action is CardAction.NavigateToTab)
        val navAction = action as CardAction.NavigateToTab
        assertTrue(navAction.screen is Screen.EventInfo)
    }

    @Test
    fun `parseCardAction inline sessions returns ShowInlineContent sessions`() = testScope.runTest {
        val viewModel = createViewModel()
        val action = viewModel.parseCardAction("inline:sessions")

        assertTrue(action is CardAction.ShowInlineContent)
        val inlineAction = action as CardAction.ShowInlineContent
        assertEquals("sessions", inlineAction.contentKey)
    }

    @Test
    fun `tagline returns ThemeConfig tagline`() = testScope.runTest {
        val viewModel = createViewModel()
        val tagline = viewModel.tagline.first()

        assertEquals("Women in Engineering Conference", tagline)
    }

    @Test
    fun `cards updates when ThemeConfig changes`() = testScope.runTest {
        val viewModel = createViewModel()

        // Initial state: 6 cards
        assertEquals(6, viewModel.cards.first().size)

        // Update the config
        val newConfig = wie2026Config.copy(cards = listOf(
            CardConfig(label = "Only Card", action = "navigate:chat", icon = "chat")
        ))
        configFlow.value = newConfig

        assertEquals(1, viewModel.cards.first().size)
        assertEquals("Only Card", viewModel.cards.first()[0].label)
    }
}
