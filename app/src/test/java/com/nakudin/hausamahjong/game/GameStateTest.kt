package com.nakudin.hausamahjong.game

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GameStateTest {

    private lateinit var gameState: GameState

    @Before
    fun setup() {
        gameState = GameState(levelNumber = 1)
    }

    @Test
    fun `initial state has zero moves`() {
        assertEquals(0, gameState.moves)
    }

    @Test
    fun `initial state has zero hints used`() {
        assertEquals(0, gameState.hintsUsed)
    }

    @Test
    fun `initial state is not complete`() {
        assertFalse(gameState.isComplete)
    }

    @Test
    fun `initial state is not failed`() {
        assertFalse(gameState.isFailed)
    }

    @Test
    fun `useHint increments hintsUsed`() {
        assertTrue(gameState.useHint())
        assertEquals(1, gameState.hintsUsed)
    }

    @Test
    fun `useHint returns false when max hints reached`() {
        repeat(3) { gameState.useHint() }
        assertFalse(gameState.useHint())
        assertEquals(3, gameState.hintsUsed)
    }

    @Test
    fun `canUndo returns true when moves exist`() {
        val board = Board(6, 4, 1)
        board.tiles.add(Tile(0, "a", 0, 0, 0))
        board.tiles.add(Tile(1, "a", 0, 1, 0))

        val tileA = board.getTileAt(0, 0, 0)!!
        val tileB = board.getTileAt(1, 0, 0)!!
        board.removeTiles(tileA, tileB)

        gameState.recordMatch(tileA, tileB, board)
        assertTrue(gameState.canUndo())
    }

    @Test
    fun `undo returns tiles and decrements moves`() {
        val board = Board(6, 4, 1)
        board.tiles.add(Tile(0, "a", 0, 0, 0))
        board.tiles.add(Tile(1, "a", 0, 1, 0))

        val tileA = board.getTileAt(0, 0, 0)!!
        val tileB = board.getTileAt(1, 0, 0)!!
        board.removeTiles(tileA, tileB)

        gameState.recordMatch(tileA, tileB, board)
        assertEquals(1, gameState.moves)

        val undone = gameState.undo(board)
        assertNotNull(undone)
        assertEquals(0, gameState.moves)
    }

    @Test
    fun `undo returns null when no moves`() {
        val board = Board(6, 4, 1)
        assertNull(gameState.undo(board))
    }

    @Test
    fun `reset clears all state`() {
        gameState.moves = 10
        gameState.hintsUsed = 3
        gameState.score = 500
        gameState.isComplete = true
        gameState.isFailed = true

        gameState.reset(2)

        assertEquals(0, gameState.moves)
        assertEquals(0, gameState.hintsUsed)
        assertEquals(0, gameState.score)
        assertFalse(gameState.isComplete)
        assertFalse(gameState.isFailed)
        assertEquals(2, gameState.levelNumber)
    }

    @Test
    fun `canUseHint returns true when hints available`() {
        assertTrue(gameState.canUseHint())
    }
}