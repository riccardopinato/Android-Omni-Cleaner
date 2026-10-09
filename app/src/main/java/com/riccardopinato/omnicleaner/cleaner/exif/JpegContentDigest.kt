package com.riccardopinato.omnicleaner.cleaner.exif

import java.io.EOFException
import java.io.InputStream
import java.security.MessageDigest

object JpegContentDigest {
    fun read(
        input: InputStream,
        cancelCheck: () -> Unit = {},
    ): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val first = readByte(input)
        val second = readByte(input)
        require(first == 0xFF && second == 0xD8) { "Not a JPEG stream" }
        digest.update(0xFF.toByte())
        digest.update(0xD8.toByte())

        while (true) {
            cancelCheck()
            var prefix = readByte(input)
            while (prefix != 0xFF) {
                digest.update(prefix.toByte())
                prefix = readByte(input)
            }

            var marker = readByte(input)
            while (marker == 0xFF) marker = readByte(input)

            if (marker == 0xD9) {
                digest.update(0xFF.toByte())
                digest.update(marker.toByte())
                return digest.digest()
            }

            if (marker == 0xDA) {
                val high = readByte(input)
                val low = readByte(input)
                val length = (high shl 8) or low
                require(length >= 2) { "Invalid JPEG SOS length" }
                val payload = input.readNBytesCompat(length - 2)
                digest.update(0xFF.toByte())
                digest.update(marker.toByte())
                digest.update(high.toByte())
                digest.update(low.toByte())
                digest.update(payload)

                val buffer = ByteArray(64 * 1024)
                while (true) {
                    cancelCheck()
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
                return digest.digest()
            }

            if (marker in 0xD0..0xD7 || marker == 0x01) {
                digest.update(0xFF.toByte())
                digest.update(marker.toByte())
                continue
            }

            val high = readByte(input)
            val low = readByte(input)
            val length = (high shl 8) or low
            require(length >= 2) { "Invalid JPEG segment length" }
            val payload = input.readNBytesCompat(length - 2)
            val isExifApp1 = marker == 0xE1 &&
                payload.size >= 6 &&
                payload[0] == 'E'.code.toByte() &&
                payload[1] == 'x'.code.toByte() &&
                payload[2] == 'i'.code.toByte() &&
                payload[3] == 'f'.code.toByte() &&
                payload[4] == 0.toByte() &&
                payload[5] == 0.toByte()

            if (!isExifApp1) {
                digest.update(0xFF.toByte())
                digest.update(marker.toByte())
                digest.update(high.toByte())
                digest.update(low.toByte())
                digest.update(payload)
            }
        }
    }

    private fun readByte(input: InputStream): Int {
        val value = input.read()
        if (value < 0) throw EOFException("Unexpected end of JPEG")
        return value
    }

    private fun InputStream.readNBytesCompat(count: Int): ByteArray {
        val result = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val read = read(result, offset, count - offset)
            if (read < 0) throw EOFException("Unexpected end of JPEG segment")
            offset += read
        }
        return result
    }
}
