package edu.jorbonism.nfclanders

import android.annotation.SuppressLint
import android.util.Log
import edu.jorbonism.nfclanders.enums.ToyType
import edu.jorbonism.nfclanders.enums.Character
import edu.jorbonism.nfclanders.enums.Game
import edu.jorbonism.nfclanders.enums.Hat
import edu.jorbonism.nfclanders.enums.HericChallenge
import edu.jorbonism.nfclanders.enums.Trinket
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.get

class TagHeader {
    var block0 = ByteArray(0x10)
    var toyType = ToyType("", 0u, Game.SpyrosAdventure)
    var tradingCardID: ULong = 0u
    var errorByte: Byte = 0

    fun getBytes(): ByteArray {
        val data = ByteArray(0x20)
        block0.copyInto(data, 0, 0, 0x10)
        bytesFromNumber(toyType.characterID, 3).copyInto(data, 0x10)
        data[0x13] = errorByte
        bytesFromNumber(tradingCardID, 8).copyInto(data, 0x14)
        data[0x1c] = toyType.deco.ord.toByte()
        data[0x1d] = toyType.variantFlagByte()
        val checksum = calculateCRC16(data.copyOfRange(0, 0x1e))
        bytesFromNumber(checksum, 2).copyInto(data, 0x1e)
        return data
    }

    companion object {
        private fun getChecksum(data: ByteArray): UShort {
            return calculateCRC16(data.copyOfRange(0, 0x1e))
        }

        fun readFromBytes(data: ByteArray): TagHeader? {
            val checksum = numberFromBytes(data, 0x1e, 2).toUShort()
            if (checksum != getChecksum(data)) {
                Log.e("TagHeader", "Bad checksum on tag!")
                return null
            }

            val header = TagHeader()
            data.copyInto(header.block0, 0, 0x00, 0x10)

            val characterID = numberFromBytes(data, 0x10, 3).toUInt()
            header.toyType = ToyType.fromData(characterID, data[0x1c], data[0x1d])

            header.errorByte = data[0x13]
            header.tradingCardID = numberFromBytes(data, 0x14, 8)
            return header
        }
    }
}


class Platforms {
    var wii      = false
    var xbox360  = false
    var ps3      = false
    var pc       = false
    var n3ds     = false
    var android  = false
    var xboxOne  = false
    var ps4      = false
    var ios      = false
    var nswitch  = false

    fun getData(): Pair<Byte, Byte> {
        return Pair(
            numberFromFlags(arrayOf(
                wii,
                xbox360,
                ps3,
                pc,
                n3ds,
            )).toByte(),
            numberFromFlags(arrayOf(
                android,
                xboxOne,
                ps4,
                ios,
                false,
                false,
                nswitch,
            )).toByte(),
        )
    }

    companion object {
        fun readFromData(platforms1: Byte, platforms3: Byte): Platforms {
            val p = Platforms()
            p.wii     = getFlag(platforms1, 0)
            p.xbox360 = getFlag(platforms1, 1)
            p.ps3     = getFlag(platforms1, 2)
            p.pc      = getFlag(platforms1, 3)
            p.n3ds    = getFlag(platforms1, 4)
            p.android = getFlag(platforms3, 0)
            p.xboxOne = getFlag(platforms3, 1)
            p.ps4     = getFlag(platforms3, 2)
            p.ios     = getFlag(platforms3, 3)
            p.nswitch = getFlag(platforms3, 6)
            return p
        }
    }
}

class Upgrades {
    var onPath     = false
    var bottomPath = false
    var main1      = false
    var main2      = false
    var main3      = false
    var main4      = false
    var path1      = false
    var path2      = false
    var path3      = false
    var soulGem    = false
    var wowPow     = false
    var altPath1   = false
    var altPath2   = false
    var altPath3   = false

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Main: ")
        arrayOf(main1, main2, main3, main4).forEachIndexed { i, b ->
            if (b) sb.append(i + 1)
            else sb.append("-")
        }

        sb.append(" ")
        if (onPath) {
            if (bottomPath) sb.append("Bottom")
            else sb.append("Top")
        } else sb.append("No")
        sb.append(" path: ")
        arrayOf(path1, path2, path3).forEachIndexed { i, b ->
            if (b) sb.append(i + 1)
            else sb.append("-")
        }

        sb.append(" Soul gem: ")
        if (soulGem) sb.append("Y")
        else sb.append("-")

        sb.append(" Wow pow: ")
        if (wowPow) sb.append("Y")
        else sb.append("-")

        sb.append(" Alt path: ")
        arrayOf(altPath1, altPath2, altPath3).forEachIndexed { i, b ->
            if (b) sb.append(i + 1)
            else sb.append("-")
        }

        return sb.toString()
    }

    fun getFlags(): Pair<UInt, UInt> {
        return Pair(
            numberFromFlags(arrayOf(
                onPath,
                bottomPath,
                main1,
                main2,
                main3,
                main4,
                path1,
                path2,
                path3,
                soulGem,
            )).toUInt(),
            numberFromFlags(arrayOf(
                wowPow,
                altPath1,
                altPath2,
                altPath3,
            )).toUInt(),
        )
    }

    companion object {
        fun readFromFlags(flags1: UInt, flags2: UInt): Upgrades {
            val upgrades = Upgrades()
            upgrades.onPath     = getFlag(flags1, 0)
            upgrades.bottomPath = getFlag(flags1, 1)
            upgrades.main1      = getFlag(flags1, 2)
            upgrades.main2      = getFlag(flags1, 3)
            upgrades.main3      = getFlag(flags1, 4)
            upgrades.main4      = getFlag(flags1, 5)
            upgrades.path1      = getFlag(flags1, 6)
            upgrades.path2      = getFlag(flags1, 7)
            upgrades.path3      = getFlag(flags1, 8)
            upgrades.soulGem    = getFlag(flags1, 9)
            upgrades.wowPow     = getFlag(flags2, 0)
            upgrades.altPath1   = getFlag(flags2, 1)
            upgrades.altPath2   = getFlag(flags2, 2)
            upgrades.altPath3   = getFlag(flags2, 3)
            return upgrades
        }
    }
}

class TagTime {
    var minute   : UByte  = 0u
    var hour     : UByte  = 0u
    var day      : UByte  = 0u
    var month    : UByte  = 0u
    var year     : UShort = 0u

    override fun toString(): String {
        if (minute < 10u) {
            return "$hour:0$minute $month/$day $year"
        } else {
            return "$hour:$minute $month/$day $year"
        }
    }

    fun getBytes(): ByteArray {
        val data = ByteArray(6)
        data[0] = minute.toByte()
        data[1] = hour.toByte()
        data[2] = day.toByte()
        data[3] = month.toByte()
        bytesFromNumber(year, 2).copyInto(data, 4)
        return data
    }

    companion object {
        fun readFromBytes(data: ByteArray): TagTime {
            val time = TagTime()
            time.minute = data[0].toUByte()
            time.hour   = data[1].toUByte()
            time.day    = data[2].toUByte()
            time.month  = data[3].toUByte()
            time.year   = numberFromBytes(data, 4, 2).toUShort()
            return time
        }
    }
}

class TagData {
    // Metadata
    var writeTime = TagTime()
    var resetTime = TagTime()
    var seenPlatforms = Platforms()
    var secondsOnPortal: UInt = 0u

    // Main data
    var nickname = ByteArray(32)
    var money: UShort = 0u
    var xp1: UInt = 0u
    var xp2: UShort = 0u
    var xp3: UInt = 0u
    var upgrades = Upgrades()
    var hat = Hat.None
    var trinket = Trinket.None

    // Extra data
    var heroicChallenges = Array<Boolean>(HericChallenge.entries.size) { false }
    var heroPoints: UShort = 0u
    var challengeLevel: UInt = 0u
    var quests = ByteArray(25)
    var elementCollectionCount1: UInt = 0u
    var elementCollectionCount2: UInt = 0u
    var elementCollectionCount3: UInt = 0u
    var accoladeRank2: UInt = 0u
    var accoladeRank3: UInt = 0u
    var battlegrounds: UInt = 0u

    // Owner info
    var ownerID = ByteArray(8)
    var ownerCount: UByte = 0u

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

        // Block 0
        bytesFromNumber(xp1                 , 3).copyInto(data1, 0x00)
        bytesFromNumber(money               , 2).copyInto(data1, 0x03)
        bytesFromNumber(secondsOnPortal     , 4).copyInto(data1, 0x05)
        data1[0x09] = areaSequence1
        // Block 1
        bytesFromNumber(flags1              , 3).copyInto(data1, 0x10)
        data1[0x13] = platforms.first
        bytesFromNumber(hat[0]              , 2).copyInto(data1, 0x14)
        data1[0x16] = 1 // Always write with 2 regions
        data1[0x17] = platforms.second
        ownerID.copyInto(data1, 0x18)
        // Blocks 2 & 3
        nickname.copyInto(data1, 0x20)
        // Block 4
        writeTime.getBytes().copyInto(data1, 0x40)
        bytesFromNumber(heroicChallenges and 0xffffffffu, 4).copyInto(data1, 0x46)
        bytesFromNumber(heroPoints          , 2).copyInto(data1, 0x4a)
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
        bytesFromNumber(challengeLevel      , 4).copyInto(data1, 0x6c)

        // Block 7
        data2[0x02] = areaSequence2
        bytesFromNumber(xp2                 , 2).copyInto(data2, 0x03)
        data2[0x05] = hat[1].toByte()
        bytesFromNumber(flags2              , 2).copyInto(data2, 0x06)
        bytesFromNumber(xp3                 , 4).copyInto(data2, 0x08)
        data2[0x0c] = hat[2].toByte()
        data2[0x0d] = trinket.ord.toByte()
        data2[0x0e] = hat[3].toByte()
        data2[0x0f] = unknown7F
        // Blocks 8 & 9
        bytesFromNumber(battlegrounds       , 4).copyInto(data2, 0x10)
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

        fun readFromBytes(data1: ByteArray, data2: ByteArray?): TagData? {
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
            td.nickname             = data1.copyOfRange(0x20, 0x40)
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

            td.seenPlatforms = Platforms.readFromData(platforms1, platforms3)
            td.upgrades = Upgrades.readFromFlags(flags1, flags2)
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

class TagContents {
    var header: TagHeader? = null
    var data: TagData? = null
//    var oldData: TagData? = null

    fun writeToConnection(connection: TagConnection, writeHeader: Boolean): Unit? {
        val header = header?: return null
        // Always update block0 to match the tag
        header.block0 = connection.readBlock(0)?: return null

        var newHeader = false
        val headerBytes = header.getBytes()
        val block1 = headerBytes.copyOfRange(0x10, 0x20)
        val block1Tag = connection.readBlock(1)?: return null
        if (!block1.contentEquals(block1Tag)) {
            // TODO: Check if header is supposed to be writeable
            if (writeHeader) {
                connection.writeBlock(1, block1)?: {
                    Log.w(null, "Header write failed! Aborting tag write. Likely attempted to change the identity of a real tag.")
                    Log.w(null, "Block 1 on tag: ${formatByteArray(block1Tag)}")
                    Log.w(null, "New block 1   : ${formatByteArray(block1)}")
                }
                newHeader = true
            } else {
                Log.e(null, "Header data doesn't match! Aborting tag write. Use 'write header' mode to change custom tag identity, or update the header to match.")
                return null
            }
        }

        val data = data?: return Unit

        if (newHeader) {
            // New header, overwrite all data with new encryption key
            var index = 0
            val dataPairA = data.getBytes(1, 1)
            for (i in 0 until 7) {
                writeEncryptDataBlock(connection, headerBytes, index, false, dataPairA.first.copyOfRange(i * 0x10, (i + 1) * 0x10))?: return null
                index += 1
            }
            for (i in 0 until 4) {
                writeEncryptDataBlock(connection, headerBytes, index, false, dataPairA.second.copyOfRange(i * 0x10, (i + 1) * 0x10))?: return null
                index += 1
            }

            index = 0
            val dataPairB = data.getBytes(0, 0)
            for (i in 0 until 7) {
                writeEncryptDataBlock(connection, headerBytes, index, true , dataPairB.first.copyOfRange(i * 0x10, (i + 1) * 0x10))?: return null
                index += 1
            }
            for (i in 0 until 4) {
                writeEncryptDataBlock(connection, headerBytes, index, true , dataPairB.second.copyOfRange(i * 0x10, (i + 1) * 0x10))?: return null
                index += 1
            }

        } else {
            // Set up to write to correct area defined by area sequences
            val as1A = (readDecryptDataBlock(connection, headerBytes, 0x0, false)?: return null)[0x9]
            val as1B = (readDecryptDataBlock(connection, headerBytes, 0x0, true )?: return null)[0x9]
            val as2A = (readDecryptDataBlock(connection, headerBytes, 0x7, false)?: return null)[0x2]
            val as2B = (readDecryptDataBlock(connection, headerBytes, 0x7, true )?: return null)[0x2]

            // Just use the higher sequence + 1, even if the diff is more than 1
            val b1 = (as1A - as1B).toByte() >= 0
            val b2 = (as2A - as2B).toByte() >= 0
            val as1 = ((if (b1) as1A else as1B) + 1).toByte()
            val as2 = ((if (b2) as2A else as2B) + 1).toByte()

            val dataPair = data.getBytes(as1, as2)
            var index = 0
            for (i in 0 until 7) {
                writeEncryptDataBlock(connection, headerBytes, index, b1, dataPair.first.copyOfRange(i * 0x10, (i + 1) * 0x10))?: return null
                index += 1
            }
            for (i in 0 until 4) {
                writeEncryptDataBlock(connection, headerBytes, index, b2, dataPair.second.copyOfRange(i * 0x10, (i + 1) * 0x10))?: return null
                index += 1
            }
        }

        return Unit
    }

    companion object {
        val md: MessageDigest = MessageDigest.getInstance("MD5")
        @SuppressLint("GetInstance")
        val cipher: Cipher = Cipher.getInstance("AES/ECB/NoPadding")

        // Convert the block index for the data struct into the actual block number on the tag
        private fun dataIndexToBlock(index: Int, b: Boolean): Int {
            // Skip over access flag block every 3 index values
            return (if (b) 0x24 else 0x08) + index + (index / 3)
        }

        private fun getEncryptionKey(headerBytes: ByteArray, block: Int): ByteArray? {
            val toHash = ByteArray(86)
            headerBytes.copyInto(toHash, 0, 0, 0x20)
            toHash[0x20] = block.toByte()
            byteArrayOf(
                0x20, 0x43, 0x6F, 0x70, 0x79, 0x72, 0x69, 0x67,
                0x68, 0x74, 0x20, 0x28, 0x43, 0x29, 0x20, 0x32,
                0x30, 0x31, 0x30, 0x20, 0x41, 0x63, 0x74, 0x69,
                0x76, 0x69, 0x73, 0x69, 0x6F, 0x6E, 0x2E, 0x20,
                0x41, 0x6C, 0x6C, 0x20, 0x52, 0x69, 0x67, 0x68,
                0x74, 0x73, 0x20, 0x52, 0x65, 0x73, 0x65, 0x72,
                0x76, 0x65, 0x64, 0x2E, 0x20,
            ).copyInto(toHash, 0x21)
            assert(toHash.last() == 0x20.toByte())
            return md.digest(toHash)
        }

        private fun readDecryptDataBlock(connection: TagConnection, headerBytes: ByteArray, index: Int, b: Boolean): ByteArray? {
            val block = dataIndexToBlock(index, b)
            val key = getEncryptionKey(headerBytes, block)?: return null

            val encrypted = connection.readBlock(block)?: return null
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
            val decrypted = cipher.doFinal(encrypted)
            return decrypted
        }

        private fun writeEncryptDataBlock(connection: TagConnection, headerBytes: ByteArray, index: Int, b: Boolean, data: ByteArray): Unit? {
            val block = dataIndexToBlock(index, b)
            val key = getEncryptionKey(headerBytes, block)?: return null

            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
            connection.writeBlock(block, cipher.doFinal(data))
            return Unit
        }

        fun readFromConnection(connection: TagConnection): TagContents? {
            val headerBytes = ByteArray(0x20)
            (connection.readBlock(0)?: return null).copyInto(headerBytes, 0x00, 0, 0x10)
            (connection.readBlock(1)?: return null).copyInto(headerBytes, 0x10, 0, 0x10)

            val contents = TagContents()
            contents.header = TagHeader.readFromBytes(headerBytes)?: return null

            var index = 0
            val data1A = ByteArray(0x70)
            for (i in 0 until 7) {
                (readDecryptDataBlock(connection, headerBytes, index, false)?: return contents).copyInto(data1A, i * 0x10, 0, 0x10)
                index += 1
            }
            val data2A = ByteArray(0x40)
            for (i in 0 until 4) {
                (readDecryptDataBlock(connection, headerBytes, index, false)?: return contents).copyInto(data2A, i * 0x10, 0, 0x10)
                index += 1
            }

            index = 0
            val data1B = ByteArray(0x70)
            for (i in 0 until 7) {
                (readDecryptDataBlock(connection, headerBytes, index, true)?: return contents).copyInto(data1B, i * 0x10, 0, 0x10)
                index += 1
            }
            val data2B = ByteArray(0x40)
            for (i in 0 until 4) {
                (readDecryptDataBlock(connection, headerBytes, index, true)?: return contents).copyInto(data2B, i * 0x10, 0, 0x10)
                index += 1
            }

            val as1A = TagData.validatePart1(data1A)
            val as1B = TagData.validatePart1(data1B)
            val data1 = as1A?.let { a ->
                as1B?.let { b ->
                    when ((a - b).toByte()) {
                        1.toByte() -> data1A
                        (-1).toByte() -> data1B
                        else -> {
                            Log.e("TagContents", "Data part 1 fields have non-consecutive area sequences!")
                            return contents
                        }
                    }
                }?: data1A
            }?: as1B?.let { data1B }?: return contents

            val data2 = if (TagData.part1HasPart2(data1)) {
                val as2A = TagData.validatePart2(data2A)
                val as2B = TagData.validatePart2(data2B)
                as2A?.let { a ->
                    as2B?.let { b ->
                        when ((a - b).toByte()) {
                            1.toByte() -> data2A
                            (-1).toByte() -> data2B
                            else -> {
                                Log.e("TagContents", "Data part 2 fields have non-consecutive area sequences!")
                                null
                            }
                        }
                    }?: data2A
                }?: as2B?.let { data2B }
            } else null

            contents.data = TagData.readFromBytes(data1, data2)
            return contents
        }
    }
}
