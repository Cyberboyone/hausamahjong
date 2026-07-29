package com.nakudin.hausamahjong.game

import kotlinx.serialization.Serializable

enum class TileCategory {
    NORMAL,
    BONUS,
    LOCKED,
    WILD,
    BOMB,
    KEY,
    MULTIPLIER
}

@Serializable
data class Tile(
    val id: Int,
    var symbolId: String,
    val layer: Int,
    val x: Int,
    val y: Int,
    var isMatched: Boolean = false,
    var isFaceUp: Boolean = false,
    @kotlinx.serialization.Transient
    var isInSlot: Boolean = false,
    var category: TileCategory = TileCategory.NORMAL,
    var isFrozen: Boolean = false,
    var multiplier: Int = 1,
    var unlockKey: String? = null
) {
    fun copyWith(
        id: Int = this.id,
        symbolId: String = this.symbolId,
        layer: Int = this.layer,
        x: Int = this.x,
        y: Int = this.y,
        isMatched: Boolean = this.isMatched,
        isFaceUp: Boolean = this.isFaceUp,
        isInSlot: Boolean = this.isInSlot,
        category: TileCategory = this.category,
        isFrozen: Boolean = this.isFrozen,
        multiplier: Int = this.multiplier,
        unlockKey: String? = this.unlockKey
    ): Tile = Tile(
        id, symbolId, layer, x, y, isMatched, isFaceUp, isInSlot,
        category, isFrozen, multiplier, unlockKey
    )

    fun canMatchWith(other: Tile): Boolean {
        if (this.category == TileCategory.WILD || other.category == TileCategory.WILD) return true
        if (this.category == TileCategory.LOCKED && this.isFrozen) return false
        if (other.category == TileCategory.LOCKED && other.isFrozen) return false
        return this.symbolId == other.symbolId
    }

    fun isAtSamePosition(other: Tile): Boolean = this.x == other.x && this.y == other.y && this.layer == other.layer

    fun getEffectiveSymbolId(): String = if (category == TileCategory.WILD) "wild" else symbolId

    fun applyMatchEffect(board: Board): List<Tile> {
        return when (category) {
            TileCategory.BOMB -> explode(board)
            TileCategory.KEY -> unlockMatchingTiles(board)
            TileCategory.MULTIPLIER -> applyMultiplier(board)
            else -> emptyList()
        }
    }

    private fun explode(board: Board): List<Tile> {
        val affected = mutableListOf<Tile>()
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                val tile = board.getTileAt(x + dx, y + dy, layer)
                if (tile != null && !tile.isMatched && !tile.isInSlot) {
                    affected.add(tile.copyWith(isMatched = true))
                }
            }
        }
        return affected
    }

    private fun unlockMatchingTiles(board: Board): List<Tile> {
        val unlocked = mutableListOf<Tile>()
        if (unlockKey != null) {
            for (tile in board.getAllTiles()) {
                if (tile.category == TileCategory.LOCKED && tile.unlockKey == unlockKey && tile.symbolId == this.symbolId) {
                    unlocked.add(tile.copyWith(category = TileCategory.NORMAL, isFrozen = false, unlockKey = null))
                }
            }
        }
        return unlocked
    }

    private fun applyMultiplier(board: Board): List<Tile> {
        // Multiplier effect is applied in GameState scoring
        return emptyList()
    }

    fun onThaw(): Tile = copyWith(category = TileCategory.NORMAL, isFrozen = false)
}