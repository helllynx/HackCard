package org.hack.card.utils

import kotlin.experimental.and
//
//object HexUtils {
//    private val HEX_CHAR_TABLE = byteArrayOf(
//        '0'.toByte(),
//        '1'.toByte(),
//        '2'.toByte(),
//        '3'.toByte(),
//        '4'.toByte(),
//        '5'.toByte(),
//        '6'.toByte(),
//        '7'.toByte(),
//        '8'.toByte(),
//        '9'.toByte(),
//        'A'.toByte(),
//        'B'.toByte(),
//        'C'.toByte(),
//        'D'.toByte(),
//        'E'.toByte(),
//        'F'.toByte()
//    )
//
//    fun toString(raw: ByteArray): String {
//        val len = raw.size
//        val hex = ByteArray(2 * len)
//        var index = 0
//        var pos = 0
//        for (b in raw) {
//            if (pos >= len) break
//            pos++
//            val v: Int = (b and 0xFF.toByte()).toInt()
//            hex[index++] = HEX_CHAR_TABLE[v ushr 4]
//            hex[index++] = HEX_CHAR_TABLE[v and 0xF]
//        }
//        return String(hex)
//    }
//
//    fun fromString(hex: String): ByteArray {
//        var len = hex.length
//        require(len % 2 != 1) { "hex length is not even" }
//        len = len / 2 // actual
//        val bytes = ByteArray(len)
//        for (i in 0 until len) {
//            bytes[i] = (hex.substring(i * 2, i * 2 + 2).toInt(16) and 0xFF).toByte()
//        }
//        return bytes
//    }
//}

