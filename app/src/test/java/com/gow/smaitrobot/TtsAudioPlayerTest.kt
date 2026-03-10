package com.gow.smaitrobot

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TtsAudioPlayer pure logic.
 *
 * AudioTrack cannot be instantiated in JVM unit tests (requires Android hardware),
 * so we test the frame-parsing and volume-clamping logic directly via the testable
 * design that accepts an injectable audioWriter lambda.
 */
class TtsAudioPlayerTest {

    // ─── Binary frame routing ───

    @Test
    fun `handleBinaryFrame with 0x05 type byte routes PCM payload to writer`() {
        val written = mutableListOf<Triple<ByteArray, Int, Int>>()
        val player = TtsAudioPlayer { data, offset, length ->
            written.add(Triple(data, offset, length))
        }

        val pcm = byteArrayOf(0x10, 0x20, 0x30, 0x40)
        val frame = byteArrayOf(0x05.toByte()) + pcm

        val result = player.handleBinaryFrame(frame)

        assertTrue("Should return true for 0x05 frame", result)
        assertEquals("Should write exactly one chunk", 1, written.size)
        val (data, offset, length) = written[0]
        assertEquals("PCM offset should be 1", 1, offset)
        assertEquals("PCM length should be frame.size - 1", pcm.size, length)
        assertSame("Should pass the original frame array", frame, data)
    }

    @Test
    fun `handleBinaryFrame with empty array returns false`() {
        val player = TtsAudioPlayer { _, _, _ -> }
        val result = player.handleBinaryFrame(ByteArray(0))
        assertFalse("Empty frame should return false", result)
    }

    @Test
    fun `handleBinaryFrame with unknown type byte returns false`() {
        val player = TtsAudioPlayer { _, _, _ -> }
        val frame = byteArrayOf(0x01.toByte(), 0xFF.toByte())
        val result = player.handleBinaryFrame(frame)
        assertFalse("Unknown type 0x01 should return false", result)
    }

    @Test
    fun `handleBinaryFrame with 0x02 type byte returns false`() {
        val player = TtsAudioPlayer { _, _, _ -> }
        val frame = byteArrayOf(0x02.toByte(), 0x00)
        val result = player.handleBinaryFrame(frame)
        assertFalse("Type 0x02 (video) should return false", result)
    }

    @Test
    fun `handleBinaryFrame with 0x03 type byte returns false`() {
        val player = TtsAudioPlayer { _, _, _ -> }
        val frame = byteArrayOf(0x03.toByte(), 0x00)
        val result = player.handleBinaryFrame(frame)
        assertFalse("Type 0x03 (raw audio) should return false", result)
    }

    @Test
    fun `handleBinaryFrame with type-only frame (length 1) routes empty payload`() {
        val written = mutableListOf<Triple<ByteArray, Int, Int>>()
        val player = TtsAudioPlayer { data, offset, length ->
            written.add(Triple(data, offset, length))
        }

        val frame = byteArrayOf(0x05.toByte())
        val result = player.handleBinaryFrame(frame)

        assertTrue("Single-byte 0x05 frame should return true", result)
        assertEquals("Should write one chunk", 1, written.size)
        assertEquals("Length should be 0 for header-only frame", 0, written[0].third)
    }

    // ─── Volume clamping ───

    @Test
    fun `clampVolume returns value unchanged when within 0 to 1 range`() {
        assertEquals(0.5f, TtsAudioPlayer.clampVolume(0.5f), 0.001f)
    }

    @Test
    fun `clampVolume clamps values above 1 to 1`() {
        assertEquals(1.0f, TtsAudioPlayer.clampVolume(1.5f), 0.001f)
        assertEquals(1.0f, TtsAudioPlayer.clampVolume(Float.MAX_VALUE), 0.001f)
    }

    @Test
    fun `clampVolume clamps values below 0 to 0`() {
        assertEquals(0.0f, TtsAudioPlayer.clampVolume(-0.1f), 0.001f)
        assertEquals(0.0f, TtsAudioPlayer.clampVolume(-Float.MAX_VALUE), 0.001f)
    }

    @Test
    fun `clampVolume returns 0 for exactly 0`() {
        assertEquals(0.0f, TtsAudioPlayer.clampVolume(0.0f), 0.001f)
    }

    @Test
    fun `clampVolume returns 1 for exactly 1`() {
        assertEquals(1.0f, TtsAudioPlayer.clampVolume(1.0f), 0.001f)
    }

    // ─── Writer invocation ───

    @Test
    fun `handleBinaryFrame does not invoke writer for non-0x05 frames`() {
        var writerCalled = false
        val player = TtsAudioPlayer { _, _, _ -> writerCalled = true }

        player.handleBinaryFrame(byteArrayOf(0x01.toByte(), 0x00))
        player.handleBinaryFrame(ByteArray(0))

        assertFalse("Writer should not be called for non-TTS frames", writerCalled)
    }
}
