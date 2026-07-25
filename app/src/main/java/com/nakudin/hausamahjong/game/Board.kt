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
        return tiles.find { it.x == x && it.y == y && it.layer == layer && !it.isMatched }
    }

    fun getTopTileAt(x: Int, y: Int): Tile? {
        for (layer in maxLayers - 1 downTo 0) {
            val tile = getTileAt(x, y, layer)
            if (tile != null) return tile
        }
        return null
    }

    fun getAllTiles(): List<Tile> = tiles.filter { !it.isMatched }

    fun getFreeTiles(): List<Tile> = MatchEngine.getFreeTiles(this)

    fun removeTile(tile: Tile) {
        tile.isMatched = true
    }

    fun removeTiles(a: Tile, b: Tile) {
        a.isMatched = true
        b.isMatched = true
    }

    fun isCleared(): Boolean = tiles.all { it.isMatched }

    fun copyWithUpdatedTiles(newTiles: List<Tile>): Board {
        val newBoard = Board(width, height, maxLayers, newTiles.toMutableList())
        return newBoard
    }

    override fun toString(): String {
        return "Board(${width}x${height}x$maxLayers, tiles=${tiles.size}, free=${getFreeTiles().size})"
    }
}