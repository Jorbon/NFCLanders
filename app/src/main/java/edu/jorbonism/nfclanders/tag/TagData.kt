package edu.jorbonism.nfclanders.tag

import android.util.Log
import edu.jorbonism.nfclanders.bytesFromNumber
import edu.jorbonism.nfclanders.calculateCRC16
import edu.jorbonism.nfclanders.enums.Hat
import edu.jorbonism.nfclanders.enums.HeroicChallenge
import edu.jorbonism.nfclanders.enums.Trinket
import edu.jorbonism.nfclanders.getFlag
import edu.jorbonism.nfclanders.numberFromBytes
import edu.jorbonism.nfclanders.numberFromFlags

class TagData {

    // Main data
    var nickname = ""
    var money: UShort = 0u
    var upgrades = Upgrades()
    var hat = Hat.None
    var trinket = Trinket.None
    var xp1: UInt = 0u
    var xp2: UShort = 0u
    var xp3: UInt = 0u

    // Extra data
    var heroicChallenges = Array<Boolean>(HeroicChallenge.entries.size) { false }
    var heroPoints: UShort = 0u
    var challengeLevel: UInt = 0u
    var elementCollectionCount1: UInt = 0u
    var elementCollectionCount2: UInt = 0u
    var elementCollectionCount3: UInt = 0u
    var accoladeRank2: UInt = 0u
    var accoladeRank3: UInt = 0u

    // Extra data (unsupported)
    var battlegrounds: UInt = 0u
    var quests = ByteArray(25)

    // Owner info
    var ownerID = ByteArray(8)
    var ownerCount: UByte = 0u

    // Metadata
    var writeTime = TagTime()
    var resetTime = TagTime()
    var seenPlatforms = Platforms()
    var secondsOnPortal: UInt = 0u

    // Not useful
    var lastGameBuildYearFrom2000: UByte = 0u
    var lastGameBuildMonth: UByte = 0u
    var lastGameBuildDay: UByte = 0u
    var prEventData = ByteArray(2)
    var wiiData = ByteArray(4)
    var xbox360Data = ByteArray(4)
    var usageInfo = ByteArray(12)
    var unknown7F: Byte = 0
    var unknownA0 = ByteArray(16)

    fun getBytes(areaSequence1: Byte, areaSequence2: Byte): Pair<ByteArray, ByteArray> {
        val data1 = ByteArray(0x70)
        val data2 = ByteArray(0x40)

        val platforms = seenPlatforms.getData()
        val flags = upgrades.getFlags()
        var flags1 = flags.first
        var flags2 = flags.second
        flags1 = flags1 or ((elementCollectionCount1 and  0b11u) shl 10)
        flags2 = flags2 or ((accoladeRank2           and  0b11u) shl  4)
        flags2 = flags2 or ((elementCollectionCount2 and 0b111u) shl  6)
        flags2 = flags2 or ((accoladeRank3           and  0b11u) shl  9)
        flags2 = flags2 or ((elementCollectionCount3 and  0b11u) shl 11)
        val heroicChallenges = numberFromFlags(heroicChallenges)
        val hat = hat.getData()
        val nicknameBytes = ByteArray(0x20)
        nickname.substring(0 until 14).toByteArray(Charsets.ISO_8859_1).forEachIndexed { i, byte ->
            nicknameBytes[i * 2] = byte
        }

        // Block 0
        bytesFromNumber(xp1, 3).copyInto(data1, 0x00)
        bytesFromNumber(money, 2).copyInto(data1, 0x03)
        bytesFromNumber(secondsOnPortal, 4).copyInto(data1, 0x05)
        data1[0x09] = areaSequence1
        // Block 1
        bytesFromNumber(flags1, 3).copyInto(data1, 0x10)
        data1[0x13] = platforms.first
        bytesFromNumber(hat[0], 2).copyInto(data1, 0x14)
        data1[0x16] = 1 // Always write with 2 regions
        data1[0x17] = platforms.second
        ownerID.copyInto(data1, 0x18)
        // Blocks 2 & 3
        nicknameBytes.copyInto(data1, 0x20)
        // Block 4
        writeTime.getBytes().copyInto(data1, 0x40)
        bytesFromNumber(heroicChallenges and 0xffffffffu, 4).copyInto(data1, 0x46)
        bytesFromNumber(heroPoints, 2).copyInto(data1, 0x4a)
        data1[0x4c] = lastGameBuildYearFrom2000.toByte()
        data1[0x4d] = lastGameBuildMonth.toByte()
        data1[0x4e] = lastGameBuildDay.toByte()
        data1[0x4f] = ownerCount.toByte()
        // Block 5
        resetTime.getBytes().copyInto(data1, 0x50)
        prEventData.copyInto(data1, 0x56)
        wiiData.copyInto(data1, 0x58)
        xbox360Data.copyInto(data1, 0x5c)
        // Block 6
        usageInfo.copyInto(data1, 0x60)
        bytesFromNumber(challengeLevel, 4).copyInto(data1, 0x6c)

        // Block 7
        data2[0x02] = areaSequence2
        bytesFromNumber(xp2, 2).copyInto(data2, 0x03)
        data2[0x05] = hat[1].toByte()
        bytesFromNumber(flags2, 2).copyInto(data2, 0x06)
        bytesFromNumber(xp3, 4).copyInto(data2, 0x08)
        data2[0x0c] = hat[2].toByte()
        data2[0x0d] = trinket.ord.toByte()
        data2[0x0e] = hat[3].toByte()
        data2[0x0f] = unknown7F
        // Blocks 8 & 9
        bytesFromNumber(battlegrounds, 4).copyInto(data2, 0x10)
        bytesFromNumber(heroicChallenges shr 32, 3).copyInto(data2, 0x14)
        quests.copyInto(data2, 0x17)
        // Block A
        unknownA0.copyInto(data2, 0x30)

        // Checksums
        bytesFromNumber(getChecksum3(data1), 2).copyInto(data1, 0x0a)
        bytesFromNumber(getChecksum2(data1), 2).copyInto(data1, 0x0c)
        bytesFromNumber(getChecksum1(data1), 2).copyInto(data1, 0x0e)
        bytesFromNumber(getChecksum4(data2), 2).copyInto(data2, 0x00)

        return Pair(data1, data2)
    }

    companion object {
        private fun getChecksum1(data1: ByteArray): UShort {
            val checkBytes = ByteArray(0x10)
            data1.copyInto(checkBytes, 0, 0x00, 0x0e)
            checkBytes[0x0e] = 5
            checkBytes[0x0f] = 0
            return calculateCRC16(checkBytes)
        }

        private fun getChecksum2(data1: ByteArray): UShort {
            return calculateCRC16(data1.copyOfRange(0x10, 0x40))
        }

        private fun getChecksum3(data1: ByteArray): UShort {
            val checkBytes = ByteArray(0x110)
            data1.copyInto(checkBytes, 0, 0x40, 0x70)
            return calculateCRC16(checkBytes)
        }

        private fun getChecksum4(data2: ByteArray): UShort {
            val checkBytes = ByteArray(0x40)
            checkBytes[0] = 6
            checkBytes[1] = 1
            data2.copyInto(checkBytes, 2, 0x02, 0x40)
            return calculateCRC16(checkBytes)
        }

        fun part1HasPart2(data1: ByteArray): Boolean {
            return data1[0x16] != 0.toByte()
        }

        fun validatePart1(data1: ByteArray): Byte? {
            if (numberFromBytes(data1, 0x0e, 2).toUShort() != getChecksum1(data1)) {
                Log.e("TagData", "Checksum 1 failed!")
                return null
            }

            if (numberFromBytes(data1, 0x0c, 2).toUShort() != getChecksum2(data1)) {
                Log.e("TagData", "Checksum 2 failed!")
                return null
            }

            if (numberFromBytes(data1, 0x0a, 2).toUShort() != getChecksum3(data1)) {
                Log.e("TagData", "Checksum 3 failed!")
                return null
            }
            return data1[0x09]
        }

        fun validatePart2(data2: ByteArray): Byte? {
            if (numberFromBytes(data2, 0x00, 2).toUShort() != getChecksum4(data2)) {
                Log.e("TagData", "Checksum 4 failed!")
                return null
            }
            return data2[0x02]
        }

        fun readFromBytes(data1: ByteArray, data2: ByteArray?): TagData {
            val td = TagData()

            // Block 0
            td.xp1                  = numberFromBytes(data1, 0x00, 3).toUInt()
            td.money                = numberFromBytes(data1, 0x03, 2).toUShort()
            td.secondsOnPortal      = numberFromBytes(data1, 0x05, 4).toUInt()
            // Block 1
            val flags1              = numberFromBytes(data1, 0x10, 3).toUInt()
            val platforms1          = data1[0x13]
            val hat1                = numberFromBytes(data1, 0x14, 2).toUShort()
            val platforms3          = data1[0x17]
            td.ownerID              = data1.copyOfRange(0x18, 0x20)
            // Block 2 & 3
            val nicknameBytes = ByteArray(14)
            for (i in 0 until 14) {
                nicknameBytes[i] = data1[0x20 + i * 2]
            }
            td.nickname             = String(nicknameBytes, Charsets.ISO_8859_1)
            // Block 4
            td.writeTime            = TagTime.readFromBytes(data1.copyOfRange(0x40, 0x46))
            var heroicChallenges    = numberFromBytes(data1, 0x46, 4)
            td.heroPoints           = numberFromBytes(data1, 0x4a, 2).toUShort()
            td.lastGameBuildYearFrom2000 = data1[0x4c].toUByte()
            td.lastGameBuildMonth   = data1[0x4d].toUByte()
            td.lastGameBuildDay     = data1[0x4e].toUByte()
            td.ownerCount           = data1[0x4f].toUByte()
            // Block 5
            td.resetTime            = TagTime.readFromBytes(data1.copyOfRange(0x50, 0x56))
            td.prEventData          = data1.copyOfRange(0x56, 0x58)
            td.wiiData              = data1.copyOfRange(0x58, 0x5c)
            td.xbox360Data          = data1.copyOfRange(0x5c, 0x60)
            // Block 6
            td.usageInfo            = data1.copyOfRange(0x60, 0x6c)
            td.challengeLevel       = numberFromBytes(data1, 0x6c, 4).toUInt()

            var flags2: UInt = 0u
            var hat2: UByte = 0u
            var hat3: UByte = 0u
            var hat5: UByte = 0u
            data2?.let { data2 ->
                // Block 7
                td.xp2              = numberFromBytes(data2, 0x03, 2).toUShort()
                hat2                = data2[0x05].toUByte()
                flags2              = numberFromBytes(data2, 0x06, 2).toUInt()
                td.xp3              = numberFromBytes(data2, 0x08, 4).toUInt()
                hat3                = data2[0x0c].toUByte()
                td.trinket          = Trinket.entries[data2[0x0d].toInt()]
                hat5                = data2[0x0e].toUByte()
                td.unknown7F        = data2[0x0f]
                // Block 8 & 9
                td.battlegrounds    = numberFromBytes(data2, 0x10, 4).toUInt()
                heroicChallenges    = heroicChallenges or (numberFromBytes(data2, 0x14, 3) shl 32)
                td.quests           = data2.copyOfRange(0x17, 0x30)
                // Block A
                td.unknownA0        = data2.copyOfRange(0x30, 0x40)
            }

            td.seenPlatforms = Platforms.Companion.readFromData(platforms1, platforms3)
            td.upgrades = Upgrades.Companion.readFromFlags(flags1, flags2)
            td.elementCollectionCount1 = (flags1 shr 10) and  0b11u
            td.accoladeRank2           = (flags2 shr  4) and  0b11u
            td.elementCollectionCount2 = (flags2 shr  6) and 0b111u
            td.accoladeRank3           = (flags2 shr  9) and  0b11u
            td.elementCollectionCount3 = (flags2 shr 11) and  0b11u
            for (i in 0 until 56) td.heroicChallenges[i] = getFlag(heroicChallenges, i)
            td.hat = Hat.readFromData(hat1, hat2, hat3, hat5)

            return td
        }
    }
}
