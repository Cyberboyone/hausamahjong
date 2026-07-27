package com.nakudin.hausamahjong.game

import kotlinx.serialization.Serializable

@Serializable
data class Board(
    val width: Int,
    val height: Int,
    val maxLayers: Int,
    val tiles: MutableList<Tile> = mutableListOf()
) {
    fun getTileAt(x: Int, y: Int, layer: Int): Tile? {
        return tiles.find { it.x == x && it.y == y && it.layer == layer && !it.isMatched && !it.isInSlot }
    }

    fun getTopTileAt(x: Int, y: Int): Tile? {
        for (layer in maxLayers - 1 downTo 0) {
            val tile = getTileAt(x, y, layer)
            if (tile != null) return tile
        }
        return null
    }

    fun getAllTiles(): List<Tile> = tiles.filter { !it.isMatched }

    fun getAllActiveTiles(): List<Tile> = tiles.filter { !it.isMatched && !it.isInSlot }

    fun getFreeTiles(): List<Tile> = MatchEngine.getFreeTiles(this)

    fun removeTile(tile: Tile) {
        tile.isMatched = true
        tile.isInSlot = false
        MatchEngine.invalidateCache()
        flipUncoveredTiles()
    }

    fun removeTiles(a: Tile, b: Tile) {
        a.isMatched = true
        b.isMatched = true
        a.isInSlot = false
        b.isInSlot = false
        MatchEngine.invalidateCache()
        flipUncoveredTiles()
    }

    fun flipUncoveredTiles() {
        for (tile in tiles) {
            if (tile.isMatched || tile.isInSlot) continue
            val aboveTile = getTileAt(tile.x, tile.y, tile.layer + 1)
            tile.isFaceUp = aboveTile == null || aboveTile.isInSlot
        }
    }

    fun isCleared(): Boolean = tiles.all { it.isMatched }

    fun copyWithUpdatedTiles(newTiles: List<Tile>): Board {
        val newBoard = Board(width, height, maxLayers, newTiles.toMutableList())
        // Preserve isInSlot state
        for (newTile in newBoard.tiles) {
            val oldTile = tiles.find { it.id == newTile.id }
            if (oldTile != null) {
                newTile.isInSlot = oldTile.isInSlot
                newTile.isFaceUp = oldTile.isFaceUp
            }
        }
        return newBoard
    }

    /**
     * Move a tile to the slot (temporarily remove from board play)
     */
    fun moveToSlot(tile: Tile) {
        tile.isInSlot = true
        MatchEngine.invalidateCache()
        flipUncoveredTiles()
    }

    /**
     * Return a tile from slot back to board
     */
    fun returnFromSlot(tile: Tile) {
        tile.isInSlot = false
        MatchEngine.invalidateCache()
        flipUncoveredTiles()
    }

    /**
     * Shuffle remaining unmatched tiles (for "stuck" recovery)
     */
    fun shuffleRemaining() {
        val activeTiles = tiles.filter { !it.isMatched && !it.isInSlot }
        val symbols = activeTiles.map { it.symbolId }.shuffled()
        for ((i, tile) in activeTiles.withIndex()) {
            tile.symbolId = symbols[i]
            tile.isFaceUp = true
        }
        MatchEngine.invalidateCache()
        flipUncoveredTiles()
    }

    override fun toString(): String {
        return "Board(${width}x${height}x$maxLayers, tiles=${tiles.size}, free=${getFreeTiles().size})"
    }
}