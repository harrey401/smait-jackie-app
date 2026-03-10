package com.gow.smaitrobot

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

/**
 * Unit tests for CaeAudioManager pure functions.
 *
 * Tests are designed around the static/companion object helper methods exposed by
 * CaeAudioManager for testability. These tests verify:
 *  - adapt8chTo6chCaeFormat: channel IDs 1..6 in 32-bit slots
 *  - extract4chRaw: ch0-ch3 extracted from 8ch data
 *  - Frame type bytes for sendAudio (0x01) and sendRaw4ch (0x03)
 *  - DOA JSON text format (not binary)
 */
class CaeAudioManagerTest {

    // ─── Test data helpers ───

    /**
     * Build a synthetic 8-channel interleaved S16LE frame (16 bytes per frame).
     * Each channel gets a unique sample value for easy verification.
     * ch0=1, ch1=2, ..., ch7=8 (as 16-bit LE)
     */
    private fun syntheticFrame(numFrames: Int = 1): ByteArray {
        val data = ByteArray(numFrames * 16)
        for (f in 0 until numFrames) {
            val off = f * 16
            for (ch in 0 until 8) {
                val sample = (ch + 1).toShort()  // ch0=1, ch1=2, etc.
                data[off + ch * 2 + 0] = (sample.toInt() and 0xFF).toByte()  // lo
                data[off + ch * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()  // hi
            }
        }
        return data
    }

    // ─── adapt8chTo6chCaeFormat tests ───

    @Test
    fun `adapt8chTo6chCaeFormat - one frame produces 24 bytes`() {
        val input = syntheticFrame(1)  // 16 bytes
        val output = CaeAudioManager.adapt8chTo6chCaeFormat(input)
        assertEquals("One 8ch frame → 24 bytes output", 24, output.size)
    }

    @Test
    fun `adapt8chTo6chCaeFormat - two frames produce 48 bytes`() {
        val input = syntheticFrame(2)  // 32 bytes
        val output = CaeAudioManager.adapt8chTo6chCaeFormat(input)
        assertEquals("Two 8ch frames → 48 bytes output", 48, output.size)
    }

    @Test
    fun `adapt8chTo6chCaeFormat - first byte of each slot is 0x00`() {
        val input = syntheticFrame(1)
        val output = CaeAudioManager.adapt8chTo6chCaeFormat(input)
        for (ch in 0 until 6) {
            assertEquals("Slot $ch byte[0] must be 0x00", 0x00.toByte(), output[ch * 4 + 0])
        }
    }

    @Test
    fun `adapt8chTo6chCaeFormat - channel IDs are 1 through 6`() {
        val input = syntheticFrame(1)
        val output = CaeAudioManager.adapt8chTo6chCaeFormat(input)
        for (ch in 0 until 6) {
            val expectedId = (ch + 1).toByte()
            assertEquals("Slot $ch channel ID must be ${ch + 1}", expectedId, output[ch * 4 + 1])
        }
    }

    @Test
    fun `adapt8chTo6chCaeFormat - ch0 maps to first slot PCM bytes`() {
        // ch0 sample = 1 (0x0001 LE) → lo=0x01, hi=0x00
        val input = syntheticFrame(1)
        val output = CaeAudioManager.adapt8chTo6chCaeFormat(input)
        // Slot 0: [0x00, 0x01, lo, hi] where lo/hi are ch0 sample bytes
        assertEquals("Slot 0 pcm_lo should be ch0 sample lo", 0x01.toByte(), output[2])  // ch0 lo
        assertEquals("Slot 0 pcm_hi should be ch0 sample hi", 0x00.toByte(), output[3])  // ch0 hi
    }

    @Test
    fun `adapt8chTo6chCaeFormat - ch6 maps to slot 4 (ref1)`() {
        // srcOffsets = [0, 2, 4, 6, 12, 14] — slot 4 = ch6 at byte offset 12
        // ch6 sample = 7 (0x0007 LE)
        val input = syntheticFrame(1)
        val output = CaeAudioManager.adapt8chTo6chCaeFormat(input)
        // Slot 4: channel ID = 5, pcm = ch6 sample = 7
        assertEquals("Slot 4 channel ID = 5", 5.toByte(), output[4 * 4 + 1])
        assertEquals("Slot 4 pcm_lo = 7 (ch6 sample lo)", 0x07.toByte(), output[4 * 4 + 2])
        assertEquals("Slot 4 pcm_hi = 0 (ch6 sample hi)", 0x00.toByte(), output[4 * 4 + 3])
    }

    @Test
    fun `adapt8chTo6chCaeFormat - ch7 maps to slot 5 (ref2)`() {
        // srcOffsets slot 5 = ch7 at byte offset 14
        // ch7 sample = 8 (0x0008 LE)
        val input = syntheticFrame(1)
        val output = CaeAudioManager.adapt8chTo6chCaeFormat(input)
        // Slot 5: channel ID = 6, pcm = ch7 sample = 8
        assertEquals("Slot 5 channel ID = 6", 6.toByte(), output[5 * 4 + 1])
        assertEquals("Slot 5 pcm_lo = 8 (ch7 sample lo)", 0x08.toByte(), output[5 * 4 + 2])
        assertEquals("Slot 5 pcm_hi = 0 (ch7 sample hi)", 0x00.toByte(), output[5 * 4 + 3])
    }

    // ─── extract4chRaw tests ───

    @Test
    fun `extract4chRaw - one frame produces 8 bytes`() {
        val input = syntheticFrame(1)  // 16 bytes
        val output = CaeAudioManager.extract4chRaw(input)
        assertEquals("One 8ch frame → 8 bytes (ch0-ch3 S16LE)", 8, output.size)
    }

    @Test
    fun `extract4chRaw - two frames produce 16 bytes`() {
        val input = syntheticFrame(2)  // 32 bytes
        val output = CaeAudioManager.extract4chRaw(input)
        assertEquals("Two 8ch frames → 16 bytes", 16, output.size)
    }

    @Test
    fun `extract4chRaw - first channel bytes match ch0 from input`() {
        val input = syntheticFrame(1)
        val output = CaeAudioManager.extract4chRaw(input)
        // ch0 lo = input[0], ch0 hi = input[1]
        assertEquals("ch0 lo byte", input[0], output[0])
        assertEquals("ch0 hi byte", input[1], output[1])
    }

    @Test
    fun `extract4chRaw - fourth channel bytes match ch3 from input`() {
        val input = syntheticFrame(1)
        val output = CaeAudioManager.extract4chRaw(input)
        // ch3 lo = input[6], ch3 hi = input[7]
        assertEquals("ch3 lo byte", input[6], output[6])
        assertEquals("ch3 hi byte", input[7], output[7])
    }

    @Test
    fun `extract4chRaw - does not include ch4 or higher from input`() {
        // ch4 lo is at input[8], ch4 hi at input[9]
        // The 8-byte output must equal the first 8 bytes of input
        val input = syntheticFrame(1)
        val output = CaeAudioManager.extract4chRaw(input)
        val expected = input.copyOfRange(0, 8)
        assertArrayEquals("4ch raw must be first 8 bytes of 8ch frame", expected, output)
    }

    // ─── buildAudioFrame / sendAudio frame format tests ───

    @Test
    fun `buildAudioFrame prepends 0x01 type byte`() {
        val payload = byteArrayOf(0x10, 0x20, 0x30)
        val frame = CaeAudioManager.buildAudioFrame(payload)
        assertEquals("Frame length = 1 + payload", 4, frame.size)
        assertEquals("First byte must be 0x01 (AUDIO_CAE)", 0x01.toByte(), frame[0])
        assertEquals("Payload byte 0 at index 1", 0x10.toByte(), frame[1])
        assertEquals("Payload byte 1 at index 2", 0x20.toByte(), frame[2])
        assertEquals("Payload byte 2 at index 3", 0x30.toByte(), frame[3])
    }

    // ─── buildRaw4chFrame / sendRaw4ch frame format tests ───

    @Test
    fun `buildRaw4chFrame prepends 0x03 type byte`() {
        val payload = byteArrayOf(0x10, 0x20, 0x30)
        val frame = CaeAudioManager.buildRaw4chFrame(payload)
        assertEquals("Frame length = 1 + payload", 4, frame.size)
        assertEquals("First byte must be 0x03 (AUDIO_RAW)", 0x03.toByte(), frame[0])
        assertEquals("Payload byte 0 at index 1", 0x10.toByte(), frame[1])
    }

    // ─── buildDoaJson / sendDoaAngle JSON text format tests ───

    @Test
    fun `buildDoaJson produces JSON text with type doa`() {
        val json = CaeAudioManager.buildDoaJson(45, 2)
        val parsed = JSONObject(json)
        assertEquals("type field must be doa", "doa", parsed.getString("type"))
    }

    @Test
    fun `buildDoaJson includes angle field`() {
        val json = CaeAudioManager.buildDoaJson(135, 1)
        val parsed = JSONObject(json)
        assertEquals("angle field must match input", 135, parsed.getInt("angle"))
    }

    @Test
    fun `buildDoaJson includes beam field`() {
        val json = CaeAudioManager.buildDoaJson(45, 3)
        val parsed = JSONObject(json)
        assertEquals("beam field must match input", 3, parsed.getInt("beam"))
    }

    @Test
    fun `buildDoaJson produces valid JSON string (not binary bytes)`() {
        val json = CaeAudioManager.buildDoaJson(90, 0)
        // Must be a valid JSON string — parseable
        val parsed = JSONObject(json)  // throws if not valid JSON
        assertTrue("type key present", parsed.has("type"))
        assertTrue("angle key present", parsed.has("angle"))
        assertTrue("beam key present", parsed.has("beam"))
    }

    @Test
    fun `buildDoaJson does not start with binary type byte`() {
        val json = CaeAudioManager.buildDoaJson(45, 1)
        // Must start with '{' (JSON object), NOT with 0x03 binary type byte
        assertTrue("DOA JSON must start with '{'", json.startsWith("{"))
    }
}
