package com.nakudin.hausamahjong.game

object MatchEngine {

    fun getFreeTiles(board: Board): List<Tile> {
        val freeTiles = mutableListOf<Tile>()
        for (tile in board.getAllTiles()) {
            if (isTileFree(tile, board)) {
                freeTiles.add(tile)
            }
        }
        return freeTiles
    }

    fun isTileFree(tile: Tile, board: Board): Boolean {
        if (tile.isMatched) return false

        // Check if blocked from above
        val aboveTile = board.getTileAt(tile.x, tile.y, tile.layer + 1)
        if (aboveTile != null && !aboveTile.isMatched) return false

        // Check if blocked on left
        val leftTile = board.getTileAt(tile.x - 1, tile.y, tile.layer)
        val leftFree = leftTile == null || leftTile.isMatched

        // Check if blocked on right
        val rightTile = board.getTileAt(tile.x + 1, tile.y, tile.layer)
        val rightFree = rightTile == null || rightTile.isMatched

        return leftFree || rightFree
    }

    fun canMatch(tileA: Tile, tileB: Tile, board: Board): Boolean {
        if (tileA.id == tileB.id) return false
        if (tileA.symbolId != tileB.symbolId) return false
        if (tileA.isMatched || tileB.isMatched) return false
        return isTileFree(tileA, board) && isTileFree(tileB, board)
    }

    fun applyMatch(board: Board, tileA: Tile, tileB: Tile): Board {
        if (!canMatch(tileA, tileB, board)) return board

        val updatedTiles = board.tiles.map { tile ->
            when {
                tile.id == tileA.id || tile.id == tileB.id -> tile.copyWith(isMatched = true)
                else -> tile
            }
        }
        return board.copyWithUpdatedTiles(updatedTiles)
    }

    fun isBoardCleared(board: Board): Boolean = board.isCleared()

    fun findMatchingPair(board: Board): Pair<Tile, Tile>? {
        val freeTiles = getFreeTiles(board)
        val symbolGroups = freeTiles.groupBy { it.symbolId }

        for ((_, tiles) in symbolGroups) {
            if (tiles.size >= 2) {
                for (i in 0 until tiles.size - 1) {
                    for (j in i + 1 until tiles.size) {
                        if (canMatch(tiles[i], tiles[j], board)) {
                            return tiles[i] to tiles[j]
                        }
                    }
                }
            }
        }
        return null
    }

    fun isSolvable(board: Board): Boolean {
        val memo = mutableSetOf<String>()
        return isSolvableRecursive(board, memo)
    }

    private fun isSolvableRecursive(board: Board, memo: MutableSet<String>): Boolean {
        val boardKey = boardToKey(board)
        if (boardKey in memo) return false
        if (isBoardCleared(board)) return true

        memo.add(boardKey)

        val pair = findMatchingPair(board)
        if (pair == null) return false

        val newBoard = applyMatch(board, pair.first, pair.second)
        return isSolvableRecursive(newBoard, memo)
    }

    private fun boardToKey(board: Board): String {
        return board.tiles
            .filter { !it.isMatched }
            .sortedWith(compareBy({ it.x }, { it.y }, { it.layer }))
            .joinToString(",") { "${it.x}:${it.y}:${it.layer}:${it.symbolId}" }
    }

    fun getHint(board: Board): Pair<Tile, Tile>? = findMatchingPair(board)

    fun getPossibleMovesCount(board: Board): Int {
        val freeTiles = getFreeTiles(board)
        val symbolGroups = freeTiles.groupBy { it.symbolId }
        return symbolGroups.values.sumOf { tiles ->
            if (tiles.size >= 2) tiles.size * (tiles.size - 1) / 2 else 0
        }
    }
}