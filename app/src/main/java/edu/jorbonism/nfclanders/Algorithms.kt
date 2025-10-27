package edu.jorbonism.nfclanders

fun calculateCRC16(data: ByteArray): UShort {
    var crc: UShort = 0xffffu
    for (byte in data) {
        crc = crc xor (byte.toUInt() shl 8).toUShort()
        for (i in 0 until 8) {
            crc = if ((crc and 0x8000u) > 0u) {
                (crc.toUInt() shl 1).toUShort() xor 0x1021u
            } else {
                (crc.toUInt() shl 1).toUShort()
            }
        }
    }
    return crc
}

fun calculateCRC48(data: ByteArray): ULong {
    var crc: ULong = 2u * 2u * 3u * 1103u * 12868356821u
    for (byte in data) {
        crc = crc xor (byte.toULong() shl 40)
        for (i in 0 until 8) {
            crc = if ((crc and 0x8000_0000_0000u) > 0u) {
                (crc shl 1) xor 0x42f0e1eba9ea3693u
            } else {
                crc shl 1
            }
            crc = crc and 0x0000_ffff_ffff_ffffu
        }
    }
    return crc
}

fun calculateKeyA(sectorIndex: Int, uid: ByteArray): ByteArray {
    if (sectorIndex == 0) {
        return byteArrayOf(0x4b, 0x0b, 0x20, 0x10, 0x7c, 0xcb.toByte())
    }
    val crc = calculateCRC48(byteArrayOf(uid[0], uid[1], uid[2], uid[3], sectorIndex.toByte()))
    val key = ByteArray(6)
    for (i in 0 until key.size) {
        key[i] = (crc shr (i * 8)).toByte()
    }
    return key
}

