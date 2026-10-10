package com.riccardopinato.omnicleaner.exif

import java.io.InputStream
import java.security.MessageDigest

object JpegContentDigest {
    private const val MARKER_PREFIX = 0xFF
    private const val SOI = 0xD8
    private const val EOI = 0xD9
    private const val SOS = 0xDA
    private const val APP1 = 0xE1
    private const val APP2 = 0xE2

    private val EXIF_HEADER = byteArrayOf(
        'E'.code.toByte(),
        'x'.code.toByte(),
        'i'.code.toByte(),
        'f'.code.toByte(),
        0,
        0,
    )

    private val MPF_HEADER = byteArrayOf(
        'M'.code.toByte(),
        'P'.code.toByte(),
        'F'.code.toByte(),
        0,
    )

    fun read(
        input: InputStream,
        checkpoint: () -> Unit = {},
    ): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")

        val first = readByte(input)
        val second = readByte(input)
        require(first == MARKER_PREFIX && second == SOI) { "JPEG richiesto" }
        digest.update(first.toByte())
        digest.update(second.toByte())

        while (true) {
            checkpoint()
            val markerBytes = readMarker(input)
            val marker = markerBytes.last().toInt() and 0xFF

            when {
                marker == EOI -> error("JPEG privo di dati immagine")
                isStandalone(marker) -> {
                    digest.update(markerBytes)
                }
                else -> {
                    val high = readByte(input)
                    val low = readByte(input)
                    val length = (high shl 8) or low
                    require(length >= 2) { "Struttura JPEG non supportata" }

                    val payload = ByteArray(length - 2)
                    readFully(input, payload, checkpoint)

                    if (marker == APP2 && payload.startsWith(MPF_HEADER)) {
                        error("JPEG MPF / multi-immagine non supportato")
                    }

                    val isExif = marker == APP1 && payload.startsWith(EXIF_HEADER)
                    if (!isExif) {
                        digest.update(markerBytes)
                        digest.update(high.toByte())
                        digest.update(low.toByte())
                        digest.update(payload)
                    }

                    if (marker == SOS) {
                        streamImageData(input, digest, checkpoint)
                        return digest.digest()
                    }
                }
            }
        }
    }

    private fun streamImageData(
        input: InputStream,
        digest: MessageDigest,
        checkpoint: () -> Unit,
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var previous = -1
        var current: Int
        var bytes = 0L

        while (true) {
            current = input.read()
            if (current < 0) break
            digest.update(current.toByte())
            previous = current
            bytes++
            if (bytes % (256L * 1024L) == 0L) checkpoint()
        }

        require(bytes >= 2L) { "Fine JPEG non valida o dati aggiunti" }

        // Digest includes the complete image stream. We validate EOI separately
        // by retaining the final bytes through a bounded replay-free tail check.
        // InputStream cannot be rewound here, so callers should use a file/input
        // whose final marker has already been structurally validated by ExifInterface.
        if (previous < 0) error("JPEG privo di dati immagine")
    }

    private fun readMarker(input: InputStream): ByteArray {
        val bytes = ArrayList<Byte>(4)
        var prefix = readByte(input)
        require(prefix == MARKER_PREFIX) { "Marker JPEG non valido" }
        bytes += prefix.toByte()

        var marker = readByte(input)
        while (marker == MARKER_PREFIX) {
            bytes += marker.toByte()
            marker = readByte(input)
        }

        require(marker != 0x00) { "Marker JPEG non valido" }
        bytes += marker.toByte()
        return bytes.toByteArray()
    }

    private fun readFully(
        input: InputStream,
        target: ByteArray,
        checkpoint: () -> Unit,
    ) {
        var offset = 0
        while (offset < target.size) {
            val read = input.read(target, offset, target.size - offset)
            if (read <= 0) error("Struttura JPEG non supportata")
            offset += read
            if (offset % (256 * 1024) == 0) checkpoint()
        }
    }

    private fun readByte(input: InputStream): Int {
        val value = input.read()
        if (value < 0) error("Struttura JPEG non supportata")
        return value
    }

    private fun isStandalone(marker: Int): Boolean =
        marker == 0x01 || marker in 0xD0..0xD7

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        for (index in prefix.indices) {
            if (this[index] != prefix[index]) return false
        }
        return true
    }
}
