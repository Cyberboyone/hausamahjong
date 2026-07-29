package com.nakudin.hausamahjong.game

import kotlin.random.Random

object MatchEngine {

    // Cache for free tiles (invalidated on board changes)
    private var freeTilesCache: List<Tile>? = null
    private var cacheBoardKey: String? = null

    fun invalidateCache() {
        freeTilesCache = null
        cacheBoardKey = null
    }

    private fun boardKey(board: Board, excludeSlotTiles: Boolean = false): String {
        return board.tiles
            .filter { !it.isMatched && (!excludeSlotTiles || !it.isInSlot) }
            .sortedWith(compareBy({ it.id }))
            .joinToString(",") { "${it.id}:${it.isMatched}:${it.isFaceUp}:${it.isInSlot}" }
    }

    fun getFreeTiles(board: Board, excludeSlotTiles: Boolean = true): List<Tile> {
        val key = boardKey(board, excludeSlotTiles)
        if (freeTilesCache != null && cacheBoardKey == key) {
            return freeTilesCache!!
        }
        val freeTiles = mutableListOf<Tile>()
        for (tile in board.getAllTiles()) {
            if (excludeSlotTiles && tile.isInSlot) continue
            if (isTileFree(tile, board, excludeSlotTiles)) {
                freeTiles.add(tile)
            }
        }
        freeTilesCache = freeTiles
        cacheBoardKey = key
        return freeTiles
    }

    fun isTileFree(tile: Tile, board: Board, excludeSlotTiles: Boolean = true): Boolean {
        if (tile.isMatched) return false
        if (excludeSlotTiles && tile.isInSlot) return false

        // Check if blocked from above
        val aboveTile = board.getTileAt(tile.x, tile.y, tile.layer + 1)
        if (aboveTile != null && !aboveTile.isMatched && !(excludeSlotTiles && aboveTile.isInSlot)) return false

        // Check if blocked on left or right (Mahjong solitaire rule: at least one side must be free)
        val leftTile = board.getTileAt(tile.x - 1, tile.y, tile.layer)
        val leftFree = leftTile == null || leftTile.isMatched || (excludeSlotTiles && leftTile.isInSlot)
        if (leftFree) return true

        val rightTile = board.getTileAt(tile.x + 1, tile.y, tile.layer)
        val rightFree = rightTile == null || rightTile.isMatched || (excludeSlotTiles && rightTile.isInSlot)
        return rightFree
    }

    fun canMatch(tileA: Tile, tileB: Tile, board: Board, excludeSlotTiles: Boolean = true): Boolean {
        if (tileA.id == tileB.id) return false
        if (tileA.symbolId != tileB.symbolId) return false
        if (tileA.isMatched || tileB.isMatched) return false
        if (excludeSlotTiles && (tileA.isInSlot || tileB.isInSlot)) return false
        return isTileFree(tileA, board, excludeSlotTiles) && isTileFree(tileB, board, excludeSlotTiles)
    }

    fun applyMatch(board: Board, tileA: Tile, tileB: Tile): Board {
        if (!canMatch(tileA, tileB, board)) return board
        val updatedTiles = board.tiles.map { tile ->
            when {
                tile.id == tileA.id || tile.id == tileB.id -> tile.copyWith(isMatched = true)
                else -> tile
            }
        }
        invalidateCache()
        return board.copyWithUpdatedTiles(updatedTiles)
    }

    fun isBoardCleared(board: Board): Boolean = board.isCleared()

    fun findMatchingPair(board: Board, excludeSlotTiles: Boolean = true): Pair<Tile, Tile>? {
        val freeTiles = getFreeTiles(board, excludeSlotTiles)
        val symbolGroups = freeTiles.groupBy { it.symbolId }

        for ((_, tiles) in symbolGroups) {
            if (tiles.size >= 2) {
                for (i in 0 until tiles.size - 1) {
                    for (j in i + 1 until tiles.size) {
                        if (canMatch(tiles[i], tiles[j], board, excludeSlotTiles)) {
                            return tiles[i] to tiles[j]
                        }
                    }
                }
            }
        }
        return null
    }

    fun isSolvable(board: Board, maxDepth: Int = 100): Boolean {
        val seen = mutableSetOf<Long>()
        return isSolvableRecursive(board, seen, 0, maxDepth)
    }

    private fun isSolvableRecursive(board: Board, seen: MutableSet<Long>, depth: Int, maxDepth: Int): Boolean {
        if (isBoardCleared(board)) return true
        if (depth >= maxDepth) return false

        val zobristKey = zobristHash(board)
        if (zobristKey in seen) return false
        seen.add(zobristKey)

        val freeTiles = getFreeTiles(board)
        val groups = freeTiles.groupBy { it.symbolId }

        for ((_, tiles) in groups) {
            if (tiles.size >= 2) {
                for (i in 0 until tiles.size - 1) {
                    for (j in i + 1 until tiles.size) {
                        if (canMatch(tiles[i], tiles[j], board)) {
                            val newBoard = applyMatch(board, tiles[i], tiles[j])
                            val branchSeen = mutableSetOf(zobristKey)
                            if (isSolvableRecursive(newBoard, branchSeen, depth + 1, maxDepth)) return true
                        }
                    }
                }
            }
        }

        return false
    }

    // Zobrist-style hashing for board state
    private val zobristTables: Array<Array<Long>> by lazy {
        val rng = Random(12345)
        val tileCount = 500
        val stateCount = 4 // unmatched, matched, faceUp, inSlot combinations
        Array(tileCount) { Array(stateCount) { rng.nextLong() } }
    }

    fun zobristHash(board: Board): Long {
        var hash = 0L
        for (tile in board.tiles) {
            if (tile.id < zobristTables.size) {
                val stateIndex = when {
                    tile.isMatched -> 0
                    tile.isInSlot -> 1
                    tile.isFaceUp -> 2
                    else -> 3
                }
                hash = hash xor zobristTables[tile.id][stateIndex]
            }
        }
        return hash
    }

    fun getHint(board: Board): Pair<Tile, Tile>? = findMatchingPair(board)

    fun getPossibleMovesCount(board: Board): Int {
        val freeTiles = getFreeTiles(board)
        val symbolGroups = freeTiles.groupBy { it.symbolId }
        return symbolGroups.values.sumOf { tiles ->
            if (tiles.size >= 2) tiles.size * (tiles.size - 1) / 2 else 0
        }
    }

    /**
     * Shuffle remaining unmatched tiles on the board.
     * Redistributes symbols randomly among current positions.
     */
    fun shuffleBoard(board: Board): Board {
        val unmatched = board.tiles.filter { !it.isMatched && !it.isInSlot }
        val symbols = unmatched.map { it.symbolId }.shuffled()
        val rng = Random(System.currentTimeMillis())
        val shuffled = symbols.shuffled(rng)

        val updatedTiles = board.tiles.map { tile ->
            if (!tile.isMatched && !tile.isInSlot) {
                val idx = unmatched.indexOfFirst { it.id == tile.id }
                tile.copyWith(symbolId = shuffled[idx], isFaceUp = true)
            } else tile
        }
        invalidateCache()
        return board.copyWithUpdatedTiles(updatedTiles)
    }

    /**
     * Analyze which pairs the player missed.
     */
    fun findMissedPairs(board: Board, moveHistory: List<Move>): List<Pair<Tile, Tile>> {
        val allFreeTiles = getFreeTiles(board)
        val missed = mutableListOf<Pair<Tile, Tile>>()
        val usedSymbols = mutableSetOf<String>()

        val symbolGroups = allFreeTiles.groupBy { it.symbolId }
        for ((symbol, tiles) in symbolGroups) {
            if (tiles.size >= 2 && symbol !in usedSymbols) {
                // Check if this pair was ever available to match
                for (i in 0 until tiles.size - 1) {
                    for (j in i + 1 until tiles.size) {
                        if (canMatch(tiles[i], tiles[j], board)) {
                            // Check if this pair was taken by the player
                            val wasMatched = moveHistory.any { move ->
                                (move.tileA.id == tiles[i].id || move.tileA.id == tiles[j].id) ||
                                        (move.tileB.id == tiles[i].id || move.tileB.id == tiles[j].id)
                            }
                            if (!wasMatched) {
                                missed.add(tiles[i] to tiles[j])
                                usedSymbols.add(symbol)
                                break
                            }
                        }
                    }
                }
            }
        }
        return missed
    }
}