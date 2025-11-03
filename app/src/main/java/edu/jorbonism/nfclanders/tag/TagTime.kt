package edu.jorbonism.nfclanders.tag

import android.os.Build
import edu.jorbonism.nfclanders.bytesFromNumber
import edu.jorbonism.nfclanders.numberFromBytes
import java.time.LocalDateTime

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

        fun now(): TagTime {
            val time = TagTime()
            val now = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDateTime.now()
            } else return time
            time.minute = now.minute.toUByte()
            time.hour   = now.hour.toUByte()
            time.day    = now.dayOfMonth.toUByte()
            time.month  = now.monthValue.toUByte()
            time.year   = now.year.toUShort()
            return time
        }
    }
}
