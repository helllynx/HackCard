package org.hack.card

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Environment
import java.io.*
import java.lang.Math.pow
//import java.text.DateFormat
import java.util.*
import kotlin.experimental.and

class Dump(// raw
    var uid: ByteArray, var data: Array<ByteArray>
) {
    // parsed
    var cardNumber = 0
        protected set
    var balance = 0
        protected set
//    var lastUsageDate: Date? = null
//        protected set
    var lastValidatorId = 0
        protected set

    protected fun parse() { // block#0 bytes#3-6
        cardNumber = intval(
            data[0][3],
            data[0][4],
            data[0][5],
            data[0][6]
        ) shr 4
        // block#1 bytes#0-1
//        lastValidatorId = intval(data[1][0], data[1][1])
        // block#1 bytes#2-4½
//        val lastUsageDay = intval(data[1][2], data[1][3])
//        if (lastUsageDay > 0) {
//            var lastUsageTime = intval(
//                (data[1][4].toInt() shr 4 and 0x0F) as Byte,
//                (data[1][5].toInt() shr 4 and 0x0F or data[1][4].toInt() shl 4 and 0xF0) as Byte
//            ).toDouble()
//            lastUsageTime = lastUsageTime / 120.0
//            val lastUsageHour = Math.floor(lastUsageTime).toInt()
//            val lastUsageMinute = Math.round(lastUsageTime % 1 * 60).toInt()
//            val c =
//                Calendar.getInstance(TimeZone.getTimeZone("GMT+3"))
//            c[1992, 0, 1, lastUsageHour] = lastUsageMinute
////            c.add(Calendar.DATE, lastUsageDay - 1)
////            lastUsageDate = c.time
//        } else {
////            lastUsageDate = null
//        }
        // block#1 bytes#8.5-10.5 (??)
        balance = intval(
            (data[1][8] and 15),
            data[1][9],  //  87654321
            (data[1][10] and 248.toByte())
        ) / 200
    }

    @Throws(IOException::class)
    fun write(tag: Tag) {
        val mfc = getMifareClassic(tag)
        if (!Arrays.equals(tag.id, uid)) {
            throw IOException(
                "Card UID mismatch:"
//                        + toString(tag.id.toHex()) + " (card) != "
//                        + toString(uid) + " (dump)"
            )
        }
        val numBlocksToWrite =
            BLOCK_COUNT - 1 // do not overwrite last block (keys)
        val startBlockIndex = mfc.sectorToBlock(SECTOR_INDEX)
        for (i in 0 until numBlocksToWrite) {
            mfc.writeBlock(startBlockIndex + i, data[i])
        }
    }

    @Throws(IOException::class)
    fun save(dir: File): File {
        val state = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED != state) {
            throw IOException("Can not write to external storage")
        }
        if (!dir.isDirectory) {
            throw IOException("Not a dir")
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("Can not make save dir")
        }
        val file = File(dir, makeFilename())
        val stream = FileOutputStream(file)
        val out = OutputStreamWriter(stream)
        out.write(uidAsString + "\r\n")
        for (block in dataAsStrings) {
            out.write(block + "\r\n")
        }
        out.close()
        return file
    }

    protected fun makeFilename(): String {
        val now = Date()
        return String.format(
            FILENAME_FORMAT,
            now.year + 1900, now.month + 1, now.date,
            now.hours, now.minutes, now.seconds,
            cardNumber, balance
        )
    }

    val uidAsString: String
        get() = uid.toHex()

    val dataAsStrings: Array<String?>
        get() {
            val blocks = arrayOfNulls<String>(data.size)
            for (i in data.indices) {
                blocks[i] = data[i].toHex()
            }
            return blocks
        }

//    val lastUsageDateAsString: String
//        get() = if (lastUsageDate == null) {
//            "<NEVER USED>"
//        } else DateFormat.getDateTimeInstance(
//            DateFormat.MEDIUM,
//            DateFormat.SHORT
//        ).format(lastUsageDate)

    val lastValidatorIdAsString: String
        get() = "ID# $lastValidatorId"

    val balanceAsString: String
        get() = "$balance RUB"

    val cardNumberAsString: String
        get() = formatCardNumber(cardNumber)

    override fun toString(): String {
        return "[Card UID=" + uidAsString + " " + balanceAsString + "RUR]"
    }

    companion object {
        const val FILENAME_FORMAT = "%04d-%02d-%02d_%02d%02d%02d_%d_%dRUB.txt"
        const val FILENAME_REGEXP =
            "([0-9]{4})-([0-9]{2})-([0-9]{2})_([0-9]{6})_([0-9]+)_([0-9]+)RUB.txt"
        const val BLOCK_COUNT = 4
        const val BLOCK_SIZE = MifareClassic.BLOCK_SIZE
        const val SECTOR_INDEX = 8
        val KEY_B = byteArrayOf(
            0xE3.toByte(),
            0x51.toByte(),
            0x73.toByte(),
            0x49.toByte(),
            0x4A.toByte(),
            0x81.toByte()
        )
        val KEY_A = byteArrayOf(
            0xA7.toByte(),
            0x3F.toByte(),
            0x5D.toByte(),
            0xC1.toByte(),
            0xD3.toByte(),
            0x33.toByte()
        )
        val KEY_0 = byteArrayOf(
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte()
        )

        @Throws(IOException::class)
        fun fromTag(tag: Tag): Dump {
            val mfc = getMifareClassic(tag)
            val blockCount = mfc.getBlockCountInSector(SECTOR_INDEX)
            if (blockCount < BLOCK_COUNT) {
                throw IOException("Wtf? Not enough blocks on this card")
            }
            val data = Array(
                BLOCK_COUNT
            ) { ByteArray(BLOCK_SIZE) }
            for (i in 0 until BLOCK_COUNT) {
                data[i] = mfc.readBlock(mfc.sectorToBlock(SECTOR_INDEX) + i)
            }
            return Dump(tag.id, data)
        }

        @Throws(IOException::class)
        fun fromFile(file: File?): Dump {
            val fs = FileInputStream(file)
            val scanner = Scanner(fs, "US-ASCII")
            val uid = scanner.nextLine().hexStringToByteArray()
            val data = Array(
                BLOCK_COUNT
            ) { ByteArray(BLOCK_SIZE) }
            for (i in 0 until BLOCK_COUNT) {
                data[i] = (scanner.nextLine()).hexStringToByteArray()
            }
            return Dump(uid, data)
        }

        @Throws(IOException::class)
        protected fun getMifareClassic(tag: Tag?): MifareClassic {
            val mfc = MifareClassic.get(tag)
            mfc.connect()
            // fucked up card
            if (mfc.authenticateSectorWithKeyA(
                    SECTOR_INDEX,
                    KEY_0
                ) && mfc.authenticateSectorWithKeyB(
                    SECTOR_INDEX,
                    KEY_0
                )
            ) {
                return mfc
            }
            // good card
            if (mfc.authenticateSectorWithKeyA(
                    SECTOR_INDEX,
                    KEY_A
                ) && mfc.authenticateSectorWithKeyB(
                    SECTOR_INDEX,
                    KEY_B
                )
            ) {
                return mfc
            }
            throw IOException("No permissions")
        }

        @JvmStatic
        fun formatCardNumber(cardNumber: Int): String {
            val cardNum3 = cardNumber % 1000
            val cardNum2 = Math.floor(cardNumber / 1000.toDouble()).toInt() % 1000
            val cardNum1 = Math.floor(cardNumber / 1000000.toDouble()).toInt() % 1000
            return String.format("%04d %03d %03d", cardNum1, cardNum2, cardNum3)
        }

        protected fun intval(vararg bytes: Byte): Int {
            var value = 0
            for (i in 0 until bytes.size) {
                var x = bytes[bytes.size - i - 1].toInt()
                while (x < 0) x = 256 + x
                value += x * pow(0x100.toDouble(), i.toDouble()).toInt()
            }
            return value
        }
    }

    init {
        parse()
    }
}

fun ByteArray.toHex() = this.joinToString(separator = "") { it.toInt().and(0xff).toString(16).padStart(2, '0') }
fun String.hexStringToByteArray() = ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }