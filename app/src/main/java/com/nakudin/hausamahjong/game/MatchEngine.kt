package com.nakudin.hausamahjong.game

import kotlin.random.Random

object MatchEngine {

    // Cache for free tiles (invalidated on board changes)
    private var freeTilesCache: List<Tile>? = null
    private var cacheBoardKey: String? = null

    // Zobrist hash tables - precomputed at initialization
    private val ZOBRIST_TILE_COUNT = 500
    private val ZOBRIST_STATE_COUNT = 4
    private val zobristTables: Array<Array<Long>> = Array(ZOBRIST_TILE_COUNT) { Array(ZOBRIST_STATE_COUNT) { Random(12345).nextLong() } }

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
        if (!tileA.canMatchWith(tileB)) return false
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

    data class MoveScore(
        val tileA: Tile,
        val tileB: Tile,
        val score: Int,
        val tilesUnblocked: Int,
        val layerCleared: Boolean,
        val createsNewMatches: Boolean
    )

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

    fun findAllMatchingPairs(board: Board, excludeSlotTiles: Boolean = true): List<Pair<Tile, Tile>> {
        val freeTiles = getFreeTiles(board, excludeSlotTiles)
        val symbolGroups = freeTiles.groupBy { it.symbolId }
        val pairs = mutableListOf<Pair<Tile, Tile>>()

        for ((_, tiles) in symbolGroups) {
            if (tiles.size >= 2) {
                for (i in 0 until tiles.size - 1) {
                    for (j in i + 1 until tiles.size) {
                        if (canMatch(tiles[i], tiles[j], board, excludeSlotTiles)) {
                            pairs.add(tiles[i] to tiles[j])
                        }
                    }
                }
            }
        }
        return pairs
    }

    fun getValidMoves(board: Board, excludeSlotTiles: Boolean = true): List<MoveScore> {
        val freeTiles = getFreeTiles(board, excludeSlotTiles)
        val symbolGroups = freeTiles.groupBy { it.symbolId }
        val moves = mutableListOf<MoveScore>()

        for ((_, tiles) in symbolGroups) {
            if (tiles.size >= 2) {
                for (i in 0 until tiles.size - 1) {
                    for (j in i + 1 until tiles.size) {
                        if (canMatch(tiles[i], tiles[j], board, excludeSlotTiles)) {
                            val score = calculateMoveScore(tiles[i], tiles[j], board)
                            moves.add(score)
                        }
                    }
                }
            }
        }

        // Sort by score descending (best moves first)
        return moves.sortedByDescending { it.score }
    }

    private fun calculateMoveScore(tileA: Tile, tileB: Tile, board: Board): MoveScore {
        var score = 100 // base score

        // Bonus for clearing higher layers
        val maxLayer = board.maxLayers - 1
        val avgLayer = (tileA.layer + tileB.layer) / 2f
        score += ((maxLayer - avgLayer) * 50).toInt()

        // Count tiles that would be unblocked
        val newBoard = applyMatch(board, tileA, tileB)
        val tilesUnblocked = countNewlyFreedTiles(board, newBoard)
        score += tilesUnblocked * 30

        // Bonus for clearing a layer
        val layerCleared = isLayerCleared(newBoard, tileA.layer)

        // Check if this creates new matches
        val newFreeTiles = getFreeTiles(newBoard)
        val newSymbolGroups = newFreeTiles.groupBy { it.symbolId }
        val createsNewMatches = newSymbolGroups.values.any { it.size >= 2 }
        if (createsNewMatches) score += 100

        // Bonus for bonus/wild tiles
        if (tileA.category == TileCategory.BONUS || tileB.category == TileCategory.BONUS) score += 200
        if (tileA.category == TileCategory.WILD || tileB.category == TileCategory.WILD) score += 150
        if (tileA.category == TileCategory.LOCKED || tileB.category == TileCategory.LOCKED) score += 300

        return MoveScore(tileA, tileB, score, tilesUnblocked, layerCleared, createsNewMatches)
    }

    private fun countNewlyFreedTiles(oldBoard: Board, newBoard: Board): Int {
        val oldFree = getFreeTiles(oldBoard).map { it.id }.toSet()
        val newFree = getFreeTiles(newBoard).map { it.id }.toSet()
        return newFree.minus(oldFree).size
    }

    private fun isLayerCleared(board: Board, layer: Int): Boolean {
        return board.getTilesAtLayer(layer).all { it.isMatched }
    }

    fun getHint(board: Board): Pair<Tile, Tile>? {
        val moves = getValidMoves(board)
        return if (moves.isNotEmpty()) moves[0].tileA to moves[0].tileB else null
    }

    fun getPossibleMovesCount(board: Board): Int {
        val freeTiles = getFreeTiles(board)
        val symbolGroups = freeTiles.groupBy { it.symbolId }
        return symbolGroups.values.sumOf { tiles ->
            if (tiles.size >= 2) tiles.size * (tiles.size - 1) / 2 else 0
        }
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
                            // Create a new seen set for this branch
                            val branchSeen = mutableSetOf<Long>().apply { addAll(seen) }
                            if (isSolvableRecursive(newBoard, branchSeen, depth + 1, maxDepth)) return true
                        }
                    }
                }
            }
        }

        return false
    }

    fun shuffleBoard(board: Board, maxAttempts: Int = 10): Board {
        val unmatched = board.tiles.filter { !it.isMatched && !it.isInSlot }
        if (unmatched.size < 2) return board

        for (attempt in 0 until maxAttempts) {
            val symbols = unmatched.map { it.symbolId }.shuffled()
            val rng = Random(System.currentTimeMillis() + attempt)

            val updatedTiles = board.tiles.map { tile ->
                if (!tile.isMatched && !tile.isInSlot) {
                    val idx = unmatched.indexOfFirst { it.id == tile.id }
                    if (idx >= 0 && idx < symbols.size) {
                        tile.copyWith(symbolId = symbols[idx], isFaceUp = true)
                    } else tile
                } else tile
            }

            val newBoard = board.copyWithUpdatedTiles(updatedTiles)

            // Validate solvability after shuffle
            if (isSolvable(newBoard, maxDepth = 50)) {
                invalidateCache()
                return newBoard
            }
        }

        // Fallback: return last attempt even if not verified solvable
        invalidateCache()
        return board
    }

    fun findMissedPairs(board: Board, moveHistory: List<MoveRecord>): List<Pair<Tile, Tile>> {
        val allFreeTiles = getFreeTiles(board)
        val missed = mutableListOf<Pair<Tile, Tile>>()
        val usedSymbols = mutableSetOf<String>()

        val symbolGroups = allFreeTiles.groupBy { it.symbolId }
        for ((symbol, tiles) in symbolGroups) {
            if (tiles.size >= 2 && symbol !in usedSymbols) {
                for (i in 0 until tiles.size - 1) {
                    for (j in i + 1 until tiles.size) {
                        if (canMatch(tiles[i], tiles[j], board)) {
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

    fun zobristHash(board: Board): Long {
        var hash = 0L
        for (tile in board.tiles) {
            if (tile.id < ZOBRIST_TILE_COUNT) {
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

    fun getHintWithPriority(board: Board): Pair<Tile, Tile>? {
        val moves = getValidMoves(board)
        return moves.firstOrNull()?.let { it.tileA to it.tileB }
    }
}