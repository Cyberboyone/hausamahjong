package com.nakudin.hausamahjong.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Board(
    val width: Int,
    val height: Int,
    val maxLayers: Int,
    val tiles: MutableList<Tile> = mutableListOf()
) {
    @Transient
    private var spatialIndex: Array<Array<Array<Tile?>>>? = null

    @Transient
    private var spatialIndexDirty = true

    private fun ensureSpatialIndex() {
        if (spatialIndex != null && !spatialIndexDirty) return
        spatialIndex = Array(width) { Array(height) { Array(maxLayers) { null } } }
        for (tile in tiles) {
            if (!tile.isMatched && !tile.isInSlot && tile.x in 0 until width && tile.y in 0 until height && tile.layer in 0 until maxLayers) {
                spatialIndex!![tile.x][tile.y][tile.layer] = tile
            }
        }
        spatialIndexDirty = false
    }

    private fun invalidateSpatialIndex() {
        spatialIndexDirty = true
    }

    fun getTileAt(x: Int, y: Int, layer: Int): Tile? {
        if (x !in 0 until width || y !in 0 until height || layer !in 0 until maxLayers) return null
        ensureSpatialIndex()
        return spatialIndex!![x][y][layer]
    }

    fun getTopTileAt(x: Int, y: Int): Tile? {
        if (x !in 0 until width || y !in 0 until height) return null
        ensureSpatialIndex()
        for (layer in maxLayers - 1 downTo 0) {
            val tile = spatialIndex!![x][y][layer]
            if (tile != null) return tile
        }
        return null
    }

    fun getTilesAtPosition(x: Int, y: Int): List<Tile> {
        if (x !in 0 until width || y !in 0 until height) return emptyList()
        ensureSpatialIndex()
        return spatialIndex!![x][y].filterNotNull().toList()
    }

    fun getAllTiles(): List<Tile> = tiles.filter { !it.isMatched }

    fun getAllActiveTiles(): List<Tile> = tiles.filter { !it.isMatched && !it.isInSlot }

    fun getFreeTiles(): List<Tile> = MatchEngine.getFreeTiles(this)

    fun getBlockingTiles(tile: Tile): List<Tile> {
        val blockers = mutableListOf<Tile>()

        // Check above
        val above = getTileAt(tile.x, tile.y, tile.layer + 1)
        if (above != null && !above.isMatched && !above.isInSlot) {
            blockers.add(above)
        }

        // Check left
        val left = getTileAt(tile.x - 1, tile.y, tile.layer)
        if (left != null && !left.isMatched && !left.isInSlot) {
            blockers.add(left)
        }

        // Check right
        val right = getTileAt(tile.x + 1, tile.y, tile.layer)
        if (right != null && !right.isMatched && !right.isInSlot) {
            blockers.add(right)
        }

        return blockers
    }

    fun getTilesAtLayer(layer: Int): List<Tile> {
        ensureSpatialIndex()
        val result = mutableListOf<Tile>()
        for (x in 0 until width) {
            for (y in 0 until height) {
                val tile = spatialIndex!![x][y][layer]
                if (tile != null) result.add(tile)
            }
        }
        return result
    }

    fun getLayerCounts(): Map<Int, Int> {
        val counts = mutableMapOf<Int, Int>()
        for (tile in tiles) {
            if (!tile.isMatched && !tile.isInSlot) {
                counts[tile.layer] = counts.getOrDefault(tile.layer, 0) + 1
            }
        }
        return counts
    }

    fun removeTile(tile: Tile) {
        tile.isMatched = true
        tile.isInSlot = false
        MatchEngine.invalidateCache()
        invalidateSpatialIndex()
        flipUncoveredTiles()
    }

    fun removeTiles(a: Tile, b: Tile) {
        a.isMatched = true
        b.isMatched = true
        a.isInSlot = false
        b.isInSlot = false
        MatchEngine.invalidateCache()
        invalidateSpatialIndex()
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

    fun getRemainingCount(): Int = tiles.count { !it.isMatched && !it.isInSlot }

    fun copyWithUpdatedTiles(newTiles: List<Tile>): Board {
        val newBoard = Board(width, height, maxLayers, newTiles.toMutableList())
        // Preserve isInSlot and isFaceUp state
        for (newTile in newBoard.tiles) {
            val oldTile = tiles.find { it.id == newTile.id }
            if (oldTile != null) {
                newTile.isInSlot = oldTile.isInSlot
                newTile.isFaceUp = oldTile.isFaceUp
            }
        }
        newBoard.invalidateSpatialIndex()
        return newBoard
    }

    fun deepCopy(): Board {
        val copiedTiles = tiles.map { tile ->
            Tile(
                id = tile.id,
                symbolId = tile.symbolId,
                layer = tile.layer,
                x = tile.x,
                y = tile.y,
                isMatched = tile.isMatched,
                isFaceUp = tile.isFaceUp,
                isInSlot = tile.isInSlot,
                category = tile.category,
                isFrozen = tile.isFrozen,
                multiplier = tile.multiplier,
                unlockKey = tile.unlockKey
            )
        }
        val newBoard = Board(width, height, maxLayers, copiedTiles.toMutableList())
        newBoard.invalidateSpatialIndex()
        return newBoard
    }

    fun moveToSlot(tile: Tile) {
        tile.isInSlot = true
        MatchEngine.invalidateCache()
        invalidateSpatialIndex()
        flipUncoveredTiles()
    }

    fun returnFromSlot(tile: Tile) {
        tile.isInSlot = false
        MatchEngine.invalidateCache()
        invalidateSpatialIndex()
        flipUncoveredTiles()
    }

    fun shuffleRemaining() {
        val activeTiles = tiles.filter { !it.isMatched && !it.isInSlot }
        val symbols = activeTiles.map { it.symbolId }.shuffled()
        for ((i, tile) in activeTiles.withIndex()) {
            tile.symbolId = symbols[i]
            tile.isFaceUp = true
        }
        MatchEngine.invalidateCache()
        invalidateSpatialIndex()
        flipUncoveredTiles()
    }

    override fun toString(): String {
        return "Board(${width}x${height}x$maxLayers, tiles=${tiles.size}, free=${getFreeTiles().size})"
    }
}