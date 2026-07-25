package com.nakudin.hausamahjong.game

data class Move(
    val tileA: Tile,
    val tileB: Tile,
    val boardSnapshot: List<Tile>
)

data class GameState(
    val levelNumber: Int,
    var moves: Int = 0,
    var hintsUsed: Int = 0,
    var score: Int = 0,
    val maxHints: Int = 3,
    var timeElapsed: Long = 0L,
    val moveHistory: MutableList<Move> = mutableListOf(),
    var isComplete: Boolean = false,
    var isFailed: Boolean = false
) {
    fun recordMatch(tileA: Tile, tileB: Tile, board: Board) {
        val snapshot = board.tiles.map {
            Tile(it.id, it.symbolId, it.layer, it.x, it.y, it.isMatched)
        }
        moveHistory.add(Move(tileA, tileB, snapshot))
        moves++
        score += 100 + (10 - hintsUsed.coerceAtMost(10)) * 10
    }

    fun undo(board: Board): Pair<Tile, Tile>? {
        if (moveHistory.isEmpty()) return null

        val lastMove = moveHistory.removeAt(moveHistory.size - 1)
        val tiles = board.tiles

        for (originalTile in lastMove.boardSnapshot) {
            val boardTile = tiles.find { it.id == originalTile.id }
            if (boardTile != null) {
                boardTile.isMatched = originalTile.isMatched
            }
        }

        moves--
        return lastMove.tileA to lastMove.tileB
    }

    fun useHint(): Boolean {
        if (hintsUsed >= maxHints) return false
        hintsUsed++
        return true
    }

    fun canUndo(): Boolean = moveHistory.isNotEmpty()

    fun canUseHint(): Boolean = hintsUsed < maxHints

    fun reset(levelNumber: Int) {
        moves = 0
        hintsUsed = 0
        score = 0
        timeElapsed = 0L
        moveHistory.clear()
        isComplete = false
        isFailed = false
    }
}