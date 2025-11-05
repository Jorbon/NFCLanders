package edu.jorbonism.nfclanders.tag

import android.annotation.SuppressLint
import android.util.Log
import edu.jorbonism.nfclanders.TagConnection
import edu.jorbonism.nfclanders.WriteError
import edu.jorbonism.nfclanders.formatByte
import edu.jorbonism.nfclanders.tag.TagHeader
import edu.jorbonism.nfclanders.formatByteArray
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class TagContents(
    var header: TagHeader? = null,
    var data: TagData? = null,
) {

    fun writeToConnection(connection: TagConnection, writeHeader: Boolean): WriteError? {
        val header = header?: return WriteError(WriteError.Stage.WritingHeader, "No header data to write.")
        // Always update block0 to match the tag
        header.block0 = connection.readBlock(0)?: return WriteError(WriteError.Stage.WritingHeader, "Failed to access block 0.")

        var newHeader = false
        val headerBytes = header.getBytes()
        val block1 = headerBytes.copyOfRange(0x10, 0x20)
        val block1Tag = connection.readBlock(1)?: return WriteError(WriteError.Stage.WritingHeader, "Failed to access block 1.")
        if (!block1.contentEquals(block1Tag)) {
            // TODO: Check if header is supposed to be writeable
            if (writeHeader) {
                connection.writeBlock(1, block1)?: return WriteError(WriteError.Stage.WritingHeader, "Failed to write header, likely tried to change the identity of a real Skylander.")
                newHeader = true
            } else {
                Log.e(null, "Header data doesn't match! Aborting tag write. Use 'write header' mode to change custom tag identity, or update the header to match.")
                return WriteError(WriteError.Stage.WritingHeader, "Identity doesn't match, use the correct tag or choose \"Write Header\" to change the identity of a writeable tag.")
            }
        }

        val data = data?: return null

        if (newHeader) {
            // New header, overwrite all data with new encryption key
            var index = 0
            val dataPairA = data.getBytes(1, 1)
            for (i in 0 until 7) {
                val block = dataPairA.first.copyOfRange(i * 0x10, (i + 1) * 0x10)
                writeEncryptDataBlock(connection, headerBytes, index, false, block)?: return WriteError(WriteError.Stage.WritingData, "Failed to write upper data block $index.")
                index += 1
            }
            for (i in 0 until 4) {
                val block = dataPairA.second.copyOfRange(i * 0x10, (i + 1) * 0x10)
                writeEncryptDataBlock(connection, headerBytes, index, false, block)?: return WriteError(WriteError.Stage.WritingData, "Failed to write upper data block $index.")
                index += 1
            }

            index = 0
            val dataPairB = data.getBytes(0, 0)
            for (i in 0 until 7) {
                val block = dataPairB.first.copyOfRange(i * 0x10, (i + 1) * 0x10)
                writeEncryptDataBlock(connection, headerBytes, index, true , block)?: return WriteError(WriteError.Stage.WritingData, "Failed to write lower data block $index.")
                index += 1
            }
            for (i in 0 until 4) {
                val block = dataPairB.second.copyOfRange(i * 0x10, (i + 1) * 0x10)
                writeEncryptDataBlock(connection, headerBytes, index, true , block)?: return WriteError(WriteError.Stage.WritingData, "Failed to write lower data block $index.")
                index += 1
            }

        } else {
            // Set up to write to correct area defined by area sequences
            val as1A = (readDecryptDataBlock(connection, headerBytes, 0x0, false)?: return WriteError(WriteError.Stage.WritingData, "Failed to read upper area sequence 1."))[0x9]
            val as1B = (readDecryptDataBlock(connection, headerBytes, 0x0, true )?: return WriteError(WriteError.Stage.WritingData, "Failed to read lower area sequence 1."))[0x9]
            val as2A = (readDecryptDataBlock(connection, headerBytes, 0x7, false)?: return WriteError(WriteError.Stage.WritingData, "Failed to read upper area sequence 2."))[0x2]
            val as2B = (readDecryptDataBlock(connection, headerBytes, 0x7, true )?: return WriteError(WriteError.Stage.WritingData, "Failed to read lower area sequence 2."))[0x2]

            // Just use the higher sequence + 1, even if the diff is more than 1
            val b1 = (as1A - as1B).toByte() >= 0
            val b2 = (as2A - as2B).toByte() >= 0
            val as1 = ((if (b1) as1A else as1B) + 1).toByte()
            val as2 = ((if (b2) as2A else as2B) + 1).toByte()

            val dataPair = data.getBytes(as1, as2)
            var index = 0
            for (i in 0 until 7) {
                val block = dataPair.first.copyOfRange(i * 0x10, (i + 1) * 0x10)
                writeEncryptDataBlock(connection, headerBytes, index, b1, block)?: return WriteError(WriteError.Stage.WritingData, "Failed to write data block $index.")
                index += 1
            }
            for (i in 0 until 4) {
                val block = dataPair.second.copyOfRange(i * 0x10, (i + 1) * 0x10)
                writeEncryptDataBlock(connection, headerBytes, index, b2, block)?: return WriteError(WriteError.Stage.WritingData, "Failed to write data block $index.")
                index += 1
            }
        }

        return null
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

        fun readFromConnection(connection: TagConnection): Pair<TagContents?, String?> {
            val headerBytes = ByteArray(0x20)
            val block0 = connection.readBlock(0)?: return Pair(null, "Failed to read header block 0.")
            val block1 = connection.readBlock(1)?: return Pair(null, "Failed to read header block 1.")
            block0.copyInto(headerBytes, 0x00, 0, 0x10)
            block1.copyInto(headerBytes, 0x10, 0, 0x10)

            val contents = TagContents()
            contents.header = TagHeader.Companion.readFromBytes(headerBytes)?: return Pair(null, "Header checksum failed.")

            var index = 0
            val data1A = ByteArray(0x70)
            for (i in 0 until 7) {
                val block = readDecryptDataBlock(connection, headerBytes, index, false)?: return Pair(contents, "Failed to read upper data block $index.")
                block.copyInto(data1A, i * 0x10, 0, 0x10)
                index += 1
            }
            val data2A = ByteArray(0x40)
            for (i in 0 until 4) {
                val block = readDecryptDataBlock(connection, headerBytes, index, false)?: return Pair(contents, "Failed to read upper data block $index.")
                block.copyInto(data2A, i * 0x10, 0, 0x10)
                index += 1
            }

            index = 0
            val data1B = ByteArray(0x70)
            for (i in 0 until 7) {
                val block = readDecryptDataBlock(connection, headerBytes, index, true)?: return Pair(contents, "Failed to read lower data block $index.")
                block.copyInto(data1B, i * 0x10, 0, 0x10)
                index += 1
            }
            val data2B = ByteArray(0x40)
            for (i in 0 until 4) {
                val block = readDecryptDataBlock(connection, headerBytes, index, true)?: return Pair(contents, "Failed to read lower data block $index.")
                block.copyInto(data2B, i * 0x10, 0, 0x10)
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
                            return Pair(contents, "Data region 1 fields have non-consecutive area sequences.")
                        }
                    }
                }?: data1A
            }?: as1B?.let { data1B }?: return Pair(contents, "Data region 1 checksums failed.")

            val data2 = if (TagData.part1HasPart2(data1)) {
                val as2A = TagData.validatePart2(data2A)
                val as2B = TagData.validatePart2(data2B)
                as2A?.let { a ->
                    as2B?.let { b ->
                        when ((a - b).toByte()) {
                            1.toByte() -> data2A
                            (-1).toByte() -> data2B
                            else -> {
                                Log.e("TagContents", "Data region 2 fields have non-consecutive area sequences.")
                                null
                            }
                        }
                    }?: data2A
                }?: as2B?.let { data2B }
            } else null

            contents.data = TagData.readFromBytes(data1, data2)
            return Pair(contents, null)
        }
    }
}

