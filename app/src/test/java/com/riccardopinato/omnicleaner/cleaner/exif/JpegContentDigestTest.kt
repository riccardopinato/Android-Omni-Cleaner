package com.riccardopinato.omnicleaner.cleaner.exif

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class JpegContentDigestTest {
    @Test
    fun exifApp1_isExcludedFromContentDigest() {
        val imagePayload = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(),
            0xFF.toByte(), 0xDA.toByte(), 0x00, 0x02,
            0x01, 0x02, 0x03,
            0xFF.toByte(), 0xD9.toByte(),
        )
        val withExif = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(),
            0xFF.toByte(), 0xE1.toByte(), 0x00, 0x08,
            'E'.code.toByte(), 'x'.code.toByte(), 'i'.code.toByte(), 'f'.code.toByte(), 0x00, 0x00,
            0xFF.toByte(), 0xDA.toByte(), 0x00, 0x02,
            0x01, 0x02, 0x03,
            0xFF.toByte(), 0xD9.toByte(),
        )

        val original = JpegContentDigest.read(ByteArrayInputStream(imagePayload))
        val cleaned = JpegContentDigest.read(ByteArrayInputStream(withExif))

        assertArrayEquals(original, cleaned)
    }
}
