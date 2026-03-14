package com.gow.smaitrobot

import app.cash.turbine.test
import com.gow.smaitrobot.data.websocket.BinaryFrame
import com.gow.smaitrobot.data.websocket.Connected
import com.gow.smaitrobot.data.websocket.Disconnected
import com.gow.smaitrobot.data.websocket.JsonMessage
import com.gow.smaitrobot.data.websocket.WebSocketEvent
import com.gow.smaitrobot.data.websocket.WebSocketRepository
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for WebSocketRepository and WebSocketEvent sealed class.
 *
 * Tests use a mock OkHttpClient to avoid real network connections.
 * The WebSocketListener is captured from the connect() call and invoked
 * directly to simulate server-sent frames.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketRepositoryTest {

    private lateinit var mockClient: OkHttpClient
    private lateinit var mockWebSocket: WebSocket
    private lateinit var repository: WebSocketRepository
    private lateinit var capturedListener: WebSocketListener

    @Before
    fun setUp() {
        mockClient = mock()
        mockWebSocket = mock()
        repository = WebSocketRepository(mockClient)

        // Capture the WebSocketListener passed to newWebSocket()
        val listenerCaptor = argumentCaptor<WebSocketListener>()
        whenever(mockClient.newWebSocket(any(), listenerCaptor.capture())).thenReturn(mockWebSocket)

        repository.connect("ws://localhost:8765")
        capturedListener = listenerCaptor.firstValue
    }

    // ── Test 1: WebSocketEvent sealed class has correct subtypes ─────────────

    @Test
    fun `Test 1 - WebSocketEvent sealed class has BinaryFrame subtype`() {
        val bytes = byteArrayOf(0x05.toByte(), 0x01, 0x02)
        val event: WebSocketEvent = BinaryFrame(bytes)
        assertTrue(event is BinaryFrame)
        assertNotNull((event as BinaryFrame).bytes)
    }

    @Test
    fun `Test 1b - WebSocketEvent sealed class has JsonMessage subtype`() {
        val event: WebSocketEvent = JsonMessage("transcript", """{"type":"transcript","text":"hello"}""")
        assertTrue(event is JsonMessage)
        assertEquals("transcript", (event as JsonMessage).type)
        assertNotNull(event.payload)
    }

    @Test
    fun `Test 1c - WebSocketEvent sealed class has Connected subtype`() {
        val event: WebSocketEvent = Connected
        assertTrue(event is Connected)
    }

    @Test
    fun `Test 1d - WebSocketEvent sealed class has Disconnected subtype`() {
        val event: WebSocketEvent = Disconnected("timeout")
        assertTrue(event is Disconnected)
        assertEquals("timeout", (event as Disconnected).reason)
    }

    // ── Test 2: connect() creates OkHttp3 WebSocket with correct URL ─────────

    @Test
    fun `Test 2 - connect creates OkHttp3 WebSocket with correct URL`() {
        // Already called in setUp; verify correct URL was used
        val requestCaptor = argumentCaptor<okhttp3.Request>()
        verify(mockClient).newWebSocket(requestCaptor.capture(), any())
        assertEquals("ws://localhost:8765", requestCaptor.firstValue.url.toString())
    }

    // ── Test 3: Binary frame type 0x05 emits BinaryFrame with full bytes ─────

    @Test
    fun `Test 3 - binary frame type 0x05 emits BinaryFrame with full byte array`() = runTest {
        repository.events.test {
            val ttsBytes = byteArrayOf(0x05.toByte(), 0x00, 0x01, 0x7F)
            capturedListener.onMessage(mockWebSocket, ttsBytes.toByteString())

            val event = awaitItem()
            assertTrue("Expected BinaryFrame but got $event", event is BinaryFrame)
            val binaryEvent = event as BinaryFrame
            assertEquals(4, binaryEvent.bytes.size)
            assertEquals(0x05.toByte(), binaryEvent.bytes[0])
        }
    }

    // ── Test 4: Binary frame type 0x06 emits BinaryFrame (map PNG) ───────────

    @Test
    fun `Test 4 - binary frame type 0x06 emits BinaryFrame for map PNG`() = runTest {
        repository.events.test {
            val mapBytes = byteArrayOf(0x06.toByte(), 0x89.toByte(), 0x50.toByte()) // PNG header hint
            capturedListener.onMessage(mockWebSocket, mapBytes.toByteString())

            val event = awaitItem()
            assertTrue(event is BinaryFrame)
            assertEquals(0x06.toByte(), (event as BinaryFrame).bytes[0])
        }
    }

    // ── Test 5: JSON text message emits JsonMessage with extracted type ───────

    @Test
    fun `Test 5 - JSON text message with type transcript emits JsonMessage`() = runTest {
        repository.events.test {
            val json = """{"type":"transcript","text":"hello world","confidence":0.95}"""
            capturedListener.onMessage(mockWebSocket, json)

            val event = awaitItem()
            assertTrue(event is JsonMessage)
            val msg = event as JsonMessage
            assertEquals("transcript", msg.type)
            assertEquals(json, msg.payload)
        }
    }

    @Test
    fun `Test 5b - JSON message with unknown type uses unknown fallback`() = runTest {
        repository.events.test {
            val json = """{"some":"field"}"""
            capturedListener.onMessage(mockWebSocket, json)

            val event = awaitItem()
            assertTrue(event is JsonMessage)
            assertEquals("unknown", (event as JsonMessage).type)
        }
    }

    // ── Test 6: send(bytes) forwards to OkHttp3 WebSocket ────────────────────

    @Test
    fun `Test 6 - send bytes forwards to OkHttp3 WebSocket as ByteString`() {
        val bytes = byteArrayOf(0x01.toByte(), 0xAB.toByte(), 0xCD.toByte())
        repository.send(bytes)
        verify(mockWebSocket).send(bytes.toByteString())
    }

    // ── Test 7: send(json) forwards text to OkHttp3 WebSocket ─────────────────

    @Test
    fun `Test 7 - send json string forwards text to OkHttp3 WebSocket`() {
        val json = """{"type":"feedback","rating":5}"""
        repository.send(json)
        verify(mockWebSocket).send(json)
    }

    // ── Test 8: on connection failure, Disconnected event emitted ─────────────

    @Test
    fun `Test 8 - on connection failure Disconnected event is emitted`() = runTest {
        repository.events.test {
            val error = Exception("Connection refused")
            capturedListener.onFailure(mockWebSocket, error, null)

            val event = awaitItem()
            assertTrue("Expected Disconnected but got $event", event is Disconnected)
            assertEquals("Connection refused", (event as Disconnected).reason)
        }
    }

    // ── Test 9: isConnected StateFlow reflects connection state ───────────────

    @Test
    fun `Test 9 - isConnected starts false before connection`() {
        val freshRepo = WebSocketRepository(mockClient)
        assertFalse(freshRepo.isConnected.value)
    }

    @Test
    fun `Test 9b - isConnected becomes true after onOpen`() = runTest {
        capturedListener.onOpen(mockWebSocket, mock())
        assertTrue(repository.isConnected.value)
    }

    @Test
    fun `Test 9c - isConnected becomes false after onClosed`() = runTest {
        // First open it
        capturedListener.onOpen(mockWebSocket, mock())
        assertTrue(repository.isConnected.value)

        // Then close it
        capturedListener.onClosed(mockWebSocket, 1000, "Normal closure")
        assertFalse(repository.isConnected.value)
    }

    @Test
    fun `Test 9d - isConnected becomes false after onFailure`() = runTest {
        capturedListener.onOpen(mockWebSocket, mock())
        capturedListener.onFailure(mockWebSocket, Exception("timeout"), null)
        assertFalse(repository.isConnected.value)
    }
}
