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
            val blockIndex = mfc.sectorToBlock(sectorIndex) + mfc.getBlockCountInSector(sectorIndex) - 1
            val newKey = calculateKeyA(sectorIndex, uid)

            if (mfc.authenticateSectorWithKeyA(sectorIndex, newKey)) {
                try {
                    val data = mfc.readBlock(blockIndex)
                    if (!data.copyOfRange(6, 10).contentEquals(dataSectorAccessBits)) {
                        newKey.copyInto(data, 0, 0, 6)
                        if (sectorIndex > 0) dataSectorAccessBits.copyInto(data, 6, 0, 4)
                        mfc.writeBlock(blockIndex, data)
                    }
                } catch (e: Exception) {
                    Log.e("Card Setup", "Error setting key A for sector $sectorIndex: $e")
                    return null
                }
                continue
            }

            var foundKey = false
            for (key in arrayOf(

                MifareClassic.KEY_DEFAULT,
                byteArrayOf(0, 0, 0, 0, 0, 0),
                MifareClassic.KEY_NFC_FORUM,
                MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY,
            )) {
                if (mfc.authenticateSectorWithKeyA(sectorIndex, key)) {
                    try {
                        val data = mfc.readBlock(blockIndex)
                        newKey.copyInto(data, 0, 0, 6)
                        if (sectorIndex > 0) dataSectorAccessBits.copyInto(data, 6, 0, 4)
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

    private fun authenticateSector(sectorIndex: Int): Unit? {
        if (authenticatedSector == sectorIndex) return Unit
        val success = (mfc?: return null).authenticateSectorWithKeyA(sectorIndex, calculateKeyA(sectorIndex, uid?: return null))
        if (!success) {
            Log.e("TagConnectionNFC", "KeyA auth failed on sector $sectorIndex")
//            this.reset()
            return null
        }
        authenticatedSector = sectorIndex
        // TODO: Check for read/write permissions
        return Unit
    }

    fun readDump(): ByteArray? {
        val mfc = mfc?: return null
        val dump = ByteArray(1024)
        for (blockIndex in 0 until 64) {
            val sectorIndex = mfc.blockToSector(blockIndex)
            this.authenticateSector(sectorIndex)?: return null
            try {
                mfc.readBlock(blockIndex).copyInto(dump, blockIndex * 0x10, 0, 0x10)
            } catch (e: Exception) {
                Log.e("TagConnectionNFC", "Dump read failed at block $blockIndex: $e")
                return null
            }
        }
        return dump
    }

    override fun readBlock(blockIndex: Int): ByteArray? {
        val mfc = mfc?: return null
        val sectorIndex = mfc.blockToSector(blockIndex)
        // Prevent access to last block in sector
        if (blockIndex == mfc.sectorToBlock(sectorIndex) + mfc.getBlockCountInSector(sectorIndex) - 1) return null

        this.authenticateSector(sectorIndex)?: return null
        try {
            return mfc.readBlock(blockIndex)
        } catch (e: Exception) {
            Log.e("TagConnectionNFC", "Failed to read block $blockIndex: $e")
//            this.reset()
            return null
        }
    }

    override fun writeBlock(blockIndex: Int, data: ByteArray): Unit? {
        val mfc = mfc?: return null
        val sectorIndex = mfc.blockToSector(blockIndex)
        if (blockIndex == 0) return null // Never need to edit block 0
        // Prevent access to last block in sector
        if (blockIndex == mfc.sectorToBlock(sectorIndex) + mfc.getBlockCountInSector(sectorIndex) - 1) return null

        this.authenticateSector(sectorIndex)?: return null
        try {
            return mfc.writeBlock(blockIndex, data)
        } catch (e: Exception) {
            Log.e("TagConnectionNFC", "Failed to write block $blockIndex: $e")
//            this.reset()
            return null
        }
    }

    companion object {
        val dataSectorAccessBits = byteArrayOf(0x7f, 0x0f, 0x08, 0x69)
    }
}


@Suppress("unused")
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
