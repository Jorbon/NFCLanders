package edu.jorbonism.nfclanders.enums

enum class Game(val ord: UByte) {
    SpyrosAdventure(0u),
    Giants(1u),
    SwapForce(2u),
    TrapTeam(3u),
    Superchargers(4u),
    Imaginators(5u),
    ;

    override fun toString(): String {
        return when (this) {
            SpyrosAdventure -> "Spyro's Adventure"
            Giants          -> "Giants"
            SwapForce       -> "Swap Force"
            TrapTeam        -> "Trap Team"
            Superchargers   -> "Superchargers"
            Imaginators     -> "Imaginators"
        }
    }
}
