package com.riccardopinato.omnicleaner.exif

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.ByteArrayInputStream

class JpegContentDigestTest {
    @Test
    fun ignoresExifPayloadButProtectsImageData() {
        val first = jpeg(
            exifPayload = byteArrayOf(1, 2, 3),
            imagePayload = byteArrayOf(10, 20, 30, 40),
        )
        val second = jpeg(
            exifPayload = byteArrayOf(9, 8, 7, 6),
            imagePayload = byteArrayOf(10, 20, 30, 40),
        )
        val changedImage = jpeg(
            exifPayload = byteArrayOf(1, 2, 3),
            imagePayload = byteArrayOf(10, 20, 99, 40),
        )

        val firstDigest = JpegContentDigest.read(ByteArrayInputStream(first))
        val secondDigest = JpegContentDigest.read(ByteArrayInputStream(second))
        val changedDigest = JpegContentDigest.read(ByteArrayInputStream(changedImage))

        assertArrayEquals(firstDigest, secondDigest)
        assertFalse(firstDigest.contentEquals(changedDigest))
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsMpfMultiImageJpeg() {
        val bytes = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(),
            0xFF.toByte(), 0xE2.toByte(),
            0x00, 0x06,
            'M'.code.toByte(), 'P'.code.toByte(), 'F'.code.toByte(), 0x00,
            0xFF.toByte(), 0xDA.toByte(),
            0x00, 0x02,
            0x01,
            0xFF.toByte(), 0xD9.toByte(),
        )

        JpegContentDigest.read(ByteArrayInputStream(bytes))
    }


    @Test(expected = IllegalStateException::class)
    fun blocksXmpGpsSidecarBeforeClaimingCleanLocation() {
        val xmpHeader = "http://ns.adobe.com/xap/1.0/\u0000".toByteArray(Charsets.US_ASCII)
        val xmp = "<rdf:Description exif:GPSLatitude=\"45,30\" />"
            .toByteArray(Charsets.UTF_8)
        JpegContentDigest.read(
            ByteArrayInputStream(jpegWithAuxiliarySegment(0xE1, xmpHeader + xmp)),
            forbidAuxiliaryMetadata = true,
        )
    }

    @Test(expected = IllegalStateException::class)
    fun blocksIptcLocationSegments() {
        JpegContentDigest.read(
            ByteArrayInputStream(jpegWithAuxiliarySegment(0xED, byteArrayOf(1, 2, 3))),
            forbidAuxiliaryMetadata = true,
        )
    }

    @Test
    fun permitsCleanExifOnlyJpegInGpsPrivacyMode() {
        val bytes = jpeg(exifPayload = byteArrayOf(1, 2, 3), imagePayload = byteArrayOf(3, 2, 1))
        assertArrayEquals(
            JpegContentDigest.read(ByteArrayInputStream(bytes)),
            JpegContentDigest.read(ByteArrayInputStream(bytes), forbidAuxiliaryMetadata = true),
        )
    }

    private fun jpegWithAuxiliarySegment(marker: Int, segment: ByteArray): ByteArray {
        val length = segment.size + 2
        return byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), marker.toByte(),
            (length shr 8).toByte(), length.toByte()) +
            segment +
            byteArrayOf(0xFF.toByte(), 0xDA.toByte(), 0x00, 0x02, 0x31,
                0xFF.toByte(), 0xD9.toByte())
    }
    private fun jpeg(
        exifPayload: ByteArray,
        imagePayload: ByteArray,
    ): ByteArray {
        val exifHeader = byteArrayOf(
            'E'.code.toByte(), 'x'.code.toByte(), 'i'.code.toByte(), 'f'.code.toByte(), 0, 0,
        )
        val appPayload = exifHeader + exifPayload
        val appLength = appPayload.size + 2

        return buildList<Byte> {
            add(0xFF.toByte())
            add(0xD8.toByte())
            add(0xFF.toByte())
            add(0xE1.toByte())
            add((appLength shr 8).toByte())
            add(appLength.toByte())
            addAll(appPayload.toList())
            add(0xFF.toByte())
            add(0xDA.toByte())
            add(0x00)
            add(0x02)
            addAll(imagePayload.toList())
            add(0xFF.toByte())
            add(0xD9.toByte())
        }.toByteArray()
    }
}
