package edu.jorbonism.nfclanders.tag

import edu.jorbonism.nfclanders.getFlag
import edu.jorbonism.nfclanders.numberFromFlags

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
