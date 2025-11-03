@file:Suppress("unused")
package edu.jorbonism.nfclanders

import android.util.Log


fun formatByte(byte: Byte): String {
    return byte.toUByte().toString(16).padStart(2, '0')
}

fun formatByteArray(data: ByteArray): String {
    val line = StringBuilder()
    for (byte in data) {
        line.append(formatByte(byte))
        line.append(' ')
    }
    return line.toString()
}

fun logDump(dump: ByteArray) {
    for (i in 0 until 0x40) {
        Log.i(null, "Block ${formatByte(i.toByte())}: ${formatByteArray(dump.copyOfRange(i * 0x10, (i + 1) * 0x10))}")
    }
}

fun numberFromBytes(data: ByteArray, offset: Int = 0, size: Int = data.size - offset): ULong {
    var n: ULong = 0u
    for (i in 0 until size) {
        n = n or (data[i + offset].toUByte().toULong() shl (i * 8))
    }
    return n
}

fun bytesFromNumber(n: ULong, size: Int = 8): ByteArray {
    return ByteArray(size) { i ->
        ((n shr (i * 8)) and 0xffu).toByte()
    }
}
fun bytesFromNumber(n: Byte  , size: Int = 1): ByteArray { return bytesFromNumber(n.toULong(), size) }
fun bytesFromNumber(n: UByte , size: Int = 1): ByteArray { return bytesFromNumber(n.toULong(), size) }
fun bytesFromNumber(n: Short , size: Int = 2): ByteArray { return bytesFromNumber(n.toULong(), size) }
fun bytesFromNumber(n: UShort, size: Int = 2): ByteArray { return bytesFromNumber(n.toULong(), size) }
fun bytesFromNumber(n: Int   , size: Int = 4): ByteArray { return bytesFromNumber(n.toULong(), size) }
fun bytesFromNumber(n: UInt  , size: Int = 4): ByteArray { return bytesFromNumber(n.toULong(), size) }
fun bytesFromNumber(n: Long  , size: Int = 8): ByteArray { return bytesFromNumber(n.toULong(), size) }


fun getFlag(n: ULong, shift: Int): Boolean {
    return (n shr shift) and 1u == 1u.toULong()
}
fun getFlag(n: Byte  , shift: Int): Boolean { return getFlag(n.toULong(), shift) }
fun getFlag(n: UByte , shift: Int): Boolean { return getFlag(n.toULong(), shift) }
fun getFlag(n: Short , shift: Int): Boolean { return getFlag(n.toULong(), shift) }
fun getFlag(n: UShort, shift: Int): Boolean { return getFlag(n.toULong(), shift) }
fun getFlag(n: Int   , shift: Int): Boolean { return getFlag(n.toULong(), shift) }
fun getFlag(n: UInt  , shift: Int): Boolean { return getFlag(n.toULong(), shift) }
fun getFlag(n: Long  , shift: Int): Boolean { return getFlag(n.toULong(), shift) }


fun numberFromFlags(flags: Array<Boolean>): ULong {
    var n: ULong = 0u
    flags.forEachIndexed { i, flag ->
        if (flag) n = n or (1u.toULong() shl i)
    }
    return n
}

