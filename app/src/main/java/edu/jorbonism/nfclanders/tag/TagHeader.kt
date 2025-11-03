package edu.jorbonism.nfclanders.tag

import android.util.Log
import edu.jorbonism.nfclanders.bytesFromNumber
import edu.jorbonism.nfclanders.calculateCRC16
import edu.jorbonism.nfclanders.enums.Deco
import edu.jorbonism.nfclanders.enums.Game
import edu.jorbonism.nfclanders.enums.ToyType
import edu.jorbonism.nfclanders.numberFromBytes

class TagHeader {
    var block0 = ByteArray(0x10)
    var toyType = ToyType("", 0u, Deco.Normal, Game.SpyrosAdventure)
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
            header.toyType = ToyType.Companion.fromData(characterID, data[0x1c], data[0x1d])

            header.errorByte = data[0x13]
            header.tradingCardID = numberFromBytes(data, 0x14, 8)
            return header
        }
    }
}