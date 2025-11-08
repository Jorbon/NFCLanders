package edu.jorbonism.nfclanders

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.util.Log

abstract class TagConnection {
    abstract fun readBlock(blockIndex: Int): ByteArray?
    abstract fun writeBlock(blockIndex: Int, data: ByteArray): Unit?
}


class TagConnectionNFC : TagConnection() {
    var uid: ByteArray? = null
    var mfc: MifareClassic? = null
    private var authenticatedSector = -1

    fun open(tag: Tag) {
        this.reset()

        val mfc = MifareClassic.get(tag)
        if (mfc.sectorCount != 16 || mfc.blockCount != 64) {
            Log.e("TagConnectionNFC", "Tag has the wrong data size: ${mfc.sectorCount} sectors, ${mfc.blockCount} blocks.")
            return
        }

        try {
            mfc.connect()
        } catch (e: Exception) {
            Log.e("TagConnectionNFC", "Could not connect to tag: $e")
            return
        }

        this.uid = tag.id
        this.mfc = mfc
    }

    fun reset() {
        // Calling close keeps crashing despite all attempts to catch exceptions
//        if (mfc?.isConnected?: false) try {
//            mfc?.close()
//        } catch (_: Exception) {}
        mfc = null
        uid = null
        authenticatedSector = -1
    }

    private fun authenticateSector(sectorIndex: Int, key: ByteArray? = null): Boolean? {
        if (authenticatedSector == sectorIndex) return true
        var success = false

        try {
            success = (mfc?: return null).authenticateSectorWithKeyA(sectorIndex, key?: calculateKeyA(sectorIndex, uid?: return null))
        } catch (e: Exception) {
            this.reset()
            return null
        }

        if (!success) return false
        authenticatedSector = sectorIndex
        // TODO: Check for read/write permissions
        return true
    }

    override fun readBlock(blockIndex: Int): ByteArray? {
        val mfc = mfc?: return null
        val sectorIndex = mfc.blockToSector(blockIndex)
        // Prevent access to last block in sector
        if (blockIndex == trailerBlockIndex(sectorIndex)) return null
        if (this.authenticateSector(sectorIndex) != true) return null
        try {
            return mfc.readBlock(blockIndex)
        } catch (e: Exception) {
            return null
        }
    }

    override fun writeBlock(blockIndex: Int, data: ByteArray): Unit? {
        val mfc = mfc?: return null
        val sectorIndex = mfc.blockToSector(blockIndex)
        if (blockIndex == 0) return null // Never need to edit block 0
        // Prevent access to last block in sector
        if (blockIndex == trailerBlockIndex(sectorIndex)) return null
        if (this.authenticateSector(sectorIndex) != true) return null
        try {
            return mfc.writeBlock(blockIndex, data)
        } catch (e: Exception) {
            Log.e("TagConnectionNFC", "Failed to write block $blockIndex: $e")
//            this.reset()
            return null
        }
    }

    fun readDump(): Pair<ByteArray?, String?> {
        val mfc = mfc?: return Pair(null, "No tag connection.")
        val dump = ByteArray(1024)
        for (blockIndex in 0 until 64) {
            val sectorIndex = mfc.blockToSector(blockIndex)

            var foundKey = false
            for (key in arrayOf(
                null,
                MifareClassic.KEY_DEFAULT,
                byteArrayOf(0, 0, 0, 0, 0, 0),
                MifareClassic.KEY_NFC_FORUM,
                MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY,
            )) {
                if (this.authenticateSector(sectorIndex, key)?: return Pair(null, "Lost tag connection.")) {
                    foundKey = true
                    break
                }
            }
            if (!foundKey) return Pair(null, "Failed to authenticate sector $sectorIndex, key is unknown.")

            try {
                mfc.readBlock(blockIndex).copyInto(dump, blockIndex * 0x10, 0, 0x10)
            } catch (e: Exception) {
                return Pair(null, "Failed to read block 0x${formatByte(blockIndex.toByte())}: $e")
            }
        }
        return Pair(dump, null)
    }

    fun formatBlankTag(): String? {
        val uid = uid?: return "No UID."
        val mfc = mfc?: return "No tag connection to format."

        val needsWrite: Array<Boolean> = Array(16) { false }
        val skylandersKeys: Array<ByteArray> = Array(16) { calculateKeyA(it, uid) }
        val sectorKeys: Array<ByteArray?> = Array(16) { null }

        // First pass: make sure all sectors are readable
        for (sectorIndex in 0 until 16) {
            if (this.authenticateSector(sectorIndex, skylandersKeys[sectorIndex])?: return "Lost tag connection.") {
                sectorKeys[sectorIndex] = skylandersKeys[sectorIndex]
                if (sectorIndex == 0) continue
                try {
                    val data = mfc.readBlock(trailerBlockIndex(sectorIndex))
                    if (!data.copyOfRange(6, 10).contentEquals(dataSectorAccessBits)) {
                        needsWrite[sectorIndex] = true
                    }
                } catch (e: Exception) {
                    return "Failed to read access bits for sector $sectorIndex: $e"
                }
            } else {
                for (key in arrayOf(
                    MifareClassic.KEY_DEFAULT,
                    byteArrayOf(0, 0, 0, 0, 0, 0),
                    MifareClassic.KEY_NFC_FORUM,
                    MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY,
                )) {
                    if (this.authenticateSector(sectorIndex, key)?: return "Lost tag connection.") {
                        sectorKeys[sectorIndex] = key
                        needsWrite[sectorIndex] = true
                        break
                    }
                }
                if (!needsWrite[sectorIndex]) {
                    return "Couldn't find authentication key for sector $sectorIndex."
                }
            }
        }

        // Second pass: ensure all needed changes will be writeable
        for (sectorIndex in 0 until 16) {
            if (!needsWrite[sectorIndex]) continue
            val key = sectorKeys[sectorIndex]?: skylandersKeys[sectorIndex]
            if (!(this.authenticateSector(sectorIndex, key)?: return "Lost tag connection.")) {
                return "Failed to authenticate sector $sectorIndex with previously working key, did it change?"
            }

            val blockIndex = trailerBlockIndex(sectorIndex)
            try {
                val data = mfc.readBlock(blockIndex)
                key.copyInto(data, 0, 0, 6)
                mfc.writeBlock(blockIndex, data)
            } catch (e: Exception) {
                return "Can't write to sector $sectorIndex trailer block: $e"
            }
        }

        // Third pass: write correct access trailers
        for (sectorIndex in 0 until 16) {
            if (!needsWrite[sectorIndex]) continue
            val key = sectorKeys[sectorIndex]?: skylandersKeys[sectorIndex]
            if (!(this.authenticateSector(sectorIndex, key)?: return "Lost tag connection.")) {
                return "Failed to authenticate sector $sectorIndex with previously working key, did it change?"
            }

            val blockIndex = trailerBlockIndex(sectorIndex)
            try {
                val data = mfc.readBlock(blockIndex)
                skylandersKeys[sectorIndex].copyInto(data, 0, 0, 6)
                if (sectorIndex > 0) dataSectorAccessBits.copyInto(data, 6, 0, 4)
                mfc.writeBlock(blockIndex, data)
            } catch (e: Exception) {
                return "Failed to write to sector $sectorIndex trailer block: $e"
            }
        }

        return null
    }

    companion object {
        val dataSectorAccessBits = byteArrayOf(0x7f, 0x0f, 0x08, 0x69)

        private fun trailerBlockIndex(sectorIndex: Int): Int {
            return sectorIndex * 4 + 3
        }
    }
}


@Suppress("unused")
class TagConnectionDump : TagConnection() {
    private var dump: ByteArray? = null

    fun open(dump: ByteArray) {
        if (dump.size != 0x400) {
            Log.e("TagConnectionDump", "Dump has the wrong size: ${dump.size} bytes.")
            return
        }
        this.dump = dump
    }

    fun reset() {
        dump = null
    }

    override fun readBlock(blockIndex: Int): ByteArray? {
        return (dump?: return null).copyOfRange(blockIndex * 0x10, (blockIndex + 1) * 0x10)
    }

    override fun writeBlock(blockIndex: Int, data: ByteArray): Unit? {
        data.copyInto(dump?: return null, blockIndex * 0x10, 0, 0x10)
        return Unit
    }

    fun logDump() {
        logDump(dump?: return)
    }
}
