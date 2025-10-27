package edu.jorbonism.nfclanders

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.util.Log
import java.io.IOException

abstract class TagConnection {
    abstract fun readBlock(block: Int): ByteArray?
    abstract fun writeBlock(block: Int, data: ByteArray): Unit?
}


class TagConnectionNFC : TagConnection() {
    var uid: ByteArray? = null
    var mfc: MifareClassic? = null
    private var authenticatedSector = -1

    fun open(tag: Tag) {
        this.reset()

        val mfc = MifareClassic.get(tag)
        if (mfc.sectorCount != 16 || mfc.blockCount != 64) {
            Log.e("TagConnectionNFC", "Tag has the wrong data size: ${mfc.sectorCount} sectors, ${mfc.blockCount} blocks")
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

    fun setupCardIfBlank(): Unit? {
        val uid = uid?: return null
        val mfc = mfc?: return null

        for (sectorIndex in 0 until 16) {
            val newKey = calculateKeyA(sectorIndex, uid)
            if (mfc.authenticateSectorWithKeyA(sectorIndex, newKey)) continue

            var foundKey = false
            for (key in arrayOf(
                MifareClassic.KEY_DEFAULT,
                byteArrayOf(0, 0, 0, 0, 0, 0),
                MifareClassic.KEY_NFC_FORUM,
                MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY,
            )) {
                if (mfc.authenticateSectorWithKeyA(sectorIndex, key)) {
                    val blockIndex = mfc.sectorToBlock(sectorIndex) + mfc.getBlockCountInSector(sectorIndex) - 1
                    try {
                        val data = mfc.readBlock(blockIndex)
                        newKey.copyInto(data, 0, 0, 6)
                        mfc.writeBlock(blockIndex, data)
                    } catch (e: Exception) {
                        Log.e("Card Setup", "Error setting key A for sector $sectorIndex: $e")
                        return null
                    }
                    foundKey = true
                    break
                }
            }

            if (!foundKey) {
                Log.e("Card Setup", "Could not find key for sector $sectorIndex")
                return null
            }
        }

        return Unit
    }

    private fun authenticateSector(sector: Int): Unit? {
        if (authenticatedSector == sector) return Unit
        val success = (mfc?: return null).authenticateSectorWithKeyA(sector, calculateKeyA(sector, uid?: return null))
        if (!success) {
            Log.e("TagConnectionNFC", "KeyA auth failed on sector $sector")
//            this.reset()
            return null
        }
        authenticatedSector = sector
        // TODO: Check for read/write permissions
        return Unit
    }

    override fun readBlock(block: Int): ByteArray? {
        val mfc = mfc?: return null
        val sector = mfc.blockToSector(block)
        // Prevent access to last block in sector
        if (block == mfc.sectorToBlock(sector) + mfc.getBlockCountInSector(sector) - 1) return null

        this.authenticateSector(sector)?: return null
        try {
            return mfc.readBlock(block)
        } catch (e: Exception) {
            Log.e("TagConnectionNFC", "Failed to read block $block: $e")
//            this.reset()
            return null
        }
    }

    override fun writeBlock(block: Int, data: ByteArray): Unit? {
        val mfc = mfc?: return null
        val sector = mfc.blockToSector(block)
        if (block == 0) return null // Never need to edit block 0
        // Prevent access to last block in sector
        if (block == mfc.sectorToBlock(sector) + mfc.getBlockCountInSector(sector) - 1) return null

        this.authenticateSector(sector)?: return null
        try {
            return mfc.writeBlock(block, data)
        } catch (e: Exception) {
            Log.e("TagConnectionNFC", "Failed to write block $block: $e")
//            this.reset()
            return null
        }
    }
}


class TagConnectionDump : TagConnection() {
    private var dump: ByteArray? = null

    fun open(dump: ByteArray) {
        if (dump.size != 0x400) {
            Log.e("TagConnectionDump", "Dump has the wrong size: ${dump.size} bytes")
            return
        }
        this.dump = dump
    }

    fun reset() {
        dump = null
    }

    override fun readBlock(block: Int): ByteArray? {
        return (dump?: return null).copyOfRange(block * 0x10, (block + 1) * 0x10)
    }

    override fun writeBlock(block: Int, data: ByteArray): Unit? {
        data.copyInto(dump?: return null, block * 0x10, 0, 0x10)
        return Unit
    }

    fun logDump() {
        val dump = dump?: return
        for (i in 0 until 0x40) {
            Log.i(null, "Block ${formatByte(i.toByte())}: ${formatByteArray(dump.copyOfRange(i * 0x10, (i + 1) * 0x10))}")
        }
    }
}
