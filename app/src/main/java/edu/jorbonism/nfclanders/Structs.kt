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

class TagHeader {
    var uid = ByteArray(4)
    var b0Data = ByteArray(12)
    var errorByte: Byte = 0
    var toyType = ToyType("", Character.DebugMinionSSA.id, Game.SpyrosAdventure)
    var tradingCardID: ULong = 0u

    fun getBytes(): ByteArray {
        val data = ByteArray(0x20)
        uid   .copyInto(data, 0, 0, 4)
        b0Data.copyInto(data, 4, 0, 12)
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
        fun readFromBytes(data: ByteArray): TagHeader? {
            val checksum = numberFromBytes(data, 0x1e, 2).toUShort()
            if (checksum != calculateCRC16(data.copyOfRange(0, 0x1e))) {
                Log.e("TagHeader", "Bad checksum on tag!")
                return null
            }

            val header = TagHeader()
            data.copyInto(header.uid   , 0, 0x00, 0x04)
            data.copyInto(header.b0Data, 0, 0x04, 0x10)

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

    companion object {
        fun readFromFlags(flags1: UInt, flags2: UShort): Upgrades {
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
    var regionCountID: UByte = 0u
    var areaSequence0: UByte = 0u
    var areaSequence1: UByte = 0u
    var writeTime = TagTime()
    var resetTime = TagTime()
    var seenPlatforms = Platforms()
    var secondsOnPortal: UInt = 0u

    // Main data
    var nickname = ""
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

    companion object {
        fun readFromBytes(data: ByteArray): TagData? {
            // TODO: extension condition + 3/4 checksums

            val td = TagData()

            // Block 0
            td.xp1                  = numberFromBytes(data, 0x00, 3).toUInt()
            td.money                = numberFromBytes(data, 0x03, 2).toUShort()
            td.secondsOnPortal      = numberFromBytes(data, 0x05, 4).toUInt()
            td.areaSequence0        = data[0x09].toUByte()
            // Block 1
            val flags1              = numberFromBytes(data, 0x10, 3).toUInt()
            val platforms1          = data[0x13]
            val hat1                = numberFromBytes(data, 0x14, 2).toUShort()
            td.regionCountID        = data[0x16].toUByte()
            val platforms3          = data[0x17]
            td.ownerID              = data.copyOfRange(0x18, 0x20)
            // Block 2 & 3
            td.nickname             = data.decodeToString(0x20, 0x40)
            // Block 4
            td.writeTime            = TagTime.readFromBytes(data.copyOfRange(0x40, 0x46))
            var heroicChallenges    = numberFromBytes(data, 0x46, 4)
            td.heroPoints           = numberFromBytes(data, 0x4a, 2).toUShort()
            td.lastGameBuildYearFrom2000 = data[0x4c].toUByte()
            td.lastGameBuildMonth   = data[0x4d].toUByte()
            td.lastGameBuildDay     = data[0x4e].toUByte()
            td.ownerCount           = data[0x4f].toUByte()
            // Block 5
            td.resetTime            = TagTime.readFromBytes(data.copyOfRange(0x50, 0x56))
            td.prEventData          = data.copyOfRange(0x56, 0x58)
            td.wiiData              = data.copyOfRange(0x58, 0x5c)
            td.xbox360Data          = data.copyOfRange(0x5c, 0x60)
            // Block 6
            td.usageInfo            = data.copyOfRange(0x60, 0x6c)
            td.challengeLevel       = numberFromBytes(data, 0x6c, 4).toUInt()

            var flags2: UShort = 0u
            var hat2: UByte = 0u
            var hat3: UByte = 0u
            var hat5: UByte = 0u
            val condition = false
            if (condition) {
                // Block 7
                td.areaSequence1    = data[0x72].toUByte()
                td.xp2              = numberFromBytes(data, 0x73, 2).toUShort()
                hat2                = data[0x75].toUByte()
                flags2              = numberFromBytes(data, 0x76, 2).toUShort()
                td.xp3              = numberFromBytes(data, 0x78, 4).toUInt()
                hat3                = data[0x7c].toUByte()
                td.trinket          = Trinket.entries[data[0x7d].toInt()]
                hat5                = data[0x7e].toUByte()
                td.unknown7F        = data[0x7f]
                // Block 8 & 9
                td.battlegrounds    = numberFromBytes(data, 0x80, 4).toUInt()
                heroicChallenges    = heroicChallenges or (numberFromBytes(data, 0x84, 3) shl 32)
                td.quests           = data.copyOfRange(0x87, 0xa0)
                // Block A
                td.unknownA0        = data.copyOfRange(0xa0, 0x10)
            }

            td.seenPlatforms = Platforms.readFromData(platforms1, platforms3)
            td.upgrades = Upgrades.readFromFlags(flags1, flags2)
            for (i in 0 until 56) td.heroicChallenges[i] = getFlag(heroicChallenges, i)
            td.hat = Hat.readFromData(hat1, hat2, hat3, hat5)

            return td
        }
    }
}

class TagContents {
    var header: TagHeader? = null
    var dataA: TagData? = null
    var dataB: TagData? = null

    private fun getEncryptionKey(block: Int): ByteArray? {
        val header = header?: return null
        val toHash = ByteArray(86)
        header.getBytes().copyInto(toHash, 0, 0, 0x20)
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

    private fun readDecryptDataBlock(connection: TagConnection, index: Int, b: Boolean): ByteArray? {
        val block = dataIndexToBlock(index, b)
        val key = this.getEncryptionKey(block)?: return null

        val encrypted = connection.readBlock(block)?: return null
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
        val decrypted = cipher.doFinal(encrypted)
        Log.i(null, "index: ${formatByte(index.toByte())}, block: ${formatByte(block.toByte())}, decrypted: ${formatByteArray(decrypted)}")
        return decrypted
    }

    private fun writeEncryptDataBlock(connection: TagConnection, index: Int, b: Boolean, data: ByteArray): Unit? {
        val block = dataIndexToBlock(index, b)
        val key = this.getEncryptionKey(block)?: return null

        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        connection.writeBlock(block, cipher.doFinal(data))
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

        fun readFromConnection(connection: TagConnection): TagContents? {
            val headerBytes = ByteArray(0x20)
            (connection.readBlock(0)?: return null).copyInto(headerBytes, 0x00, 0, 0x10)
            (connection.readBlock(1)?: return null).copyInto(headerBytes, 0x10, 0, 0x10)

            val contents = TagContents()
            contents.header = TagHeader.readFromBytes(headerBytes)?: return null

            val dataBytesA = ByteArray(0xB0)
            for (index in 0 until 0xB) {
                (contents.readDecryptDataBlock(connection, index, false)?: return null).copyInto(dataBytesA, index * 0x10, 0, 0x10)
            }

            val dataBytesB = ByteArray(0xB0)
            for (index in 0 until 0xB) {
                (contents.readDecryptDataBlock(connection, index, true)?: return null).copyInto(dataBytesB, index * 0x10, 0, 0x10)
            }

            contents.dataA = TagData.readFromBytes(dataBytesA)
            contents.dataB = TagData.readFromBytes(dataBytesB)
            return contents
        }
    }
}
