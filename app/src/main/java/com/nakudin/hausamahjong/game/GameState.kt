package com.nakudin.hausamahjong.game

data class MoveRecord(
    val tileA: Tile,
    val tileB: Tile,
    val boardSnapshot: List<Tile>
)

data class GameState(
    val levelNumber: Int,
    var moves: Int = 0,
    var hintsUsed: Int = 0,
    var undosUsed: Int = 0,
    var flipsUsed: Int = 0,
    var score: Int = 0,
    val maxHints: Int = 3,
    var timeElapsed: Long = 0L,
    var startTime: Long = System.currentTimeMillis(),
    val moveHistory: MutableList<MoveRecord> = mutableListOf(),
    var isComplete: Boolean = false,
    var isFailed: Boolean = false
) {
    val noHint: Boolean get() = hintsUsed == 0
    val noUndo: Boolean get() = undosUsed == 0

    fun recordMatch(tileA: Tile, tileB: Tile, board: Board) {
        val snapshot = board.tiles.map {
            Tile(it.id, it.symbolId, it.layer, it.x, it.y, it.isMatched, it.isFaceUp, it.isInSlot)
        }
        moveHistory.add(MoveRecord(tileA, tileB, snapshot))
        moves++
        score += 100 + (10 - hintsUsed.coerceAtMost(10)) * 10
    }

    fun recordFlip() {
        flipsUsed++
    }

    fun recordUndo() {
        undosUsed++
    }

    fun undo(board: Board): Pair<Tile, Tile>? {
        if (moveHistory.isEmpty()) return null

        val lastMove = moveHistory.removeAt(moveHistory.size - 1)
        val tiles = board.tiles

        for (originalTile in lastMove.boardSnapshot) {
            val boardTile = tiles.find { it.id == originalTile.id }
            if (boardTile != null) {
                boardTile.isMatched = originalTile.isMatched
                boardTile.isFaceUp = originalTile.isFaceUp
                boardTile.isInSlot = originalTile.isInSlot
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

    fun getElapsedTime(): Long = System.currentTimeMillis() - startTime

    fun reset(levelNumber: Int) {
        moves = 0
        hintsUsed = 0
        undosUsed = 0
        flipsUsed = 0
        score = 0
        timeElapsed = 0L
        startTime = System.currentTimeMillis()
        moveHistory.clear()
        isComplete = false
        isFailed = false
    }
}