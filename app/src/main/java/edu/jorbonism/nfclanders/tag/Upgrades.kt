package edu.jorbonism.nfclanders.tag

import edu.jorbonism.nfclanders.getFlag
import edu.jorbonism.nfclanders.numberFromFlags

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
