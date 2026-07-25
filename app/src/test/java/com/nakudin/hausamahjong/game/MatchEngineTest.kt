package com.nakudin.hausamahjong.game

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MatchEngineTest {

    private lateinit var board: Board

    @Before
    fun setup() {
        board = Board(6, 4, 1)
        board.tiles.add(Tile(0, "kolanut", 0, 0, 0))
        board.tiles.add(Tile(1, "kolanut", 0, 1, 0))
        board.tiles.add(Tile(2, "kalangu", 0, 2, 0))
        board.tiles.add(Tile(3, "kalangu", 0, 3, 0))
        board.tiles.add(Tile(4, "goje", 0, 4, 0))
        board.tiles.add(Tile(5, "goje", 0, 5, 0))
    }

    @Test
    fun `getFreeTiles returns tiles with free sides`() {
        val free = MatchEngine.getFreeTiles(board)
        assertEquals(6, free.size)
    }

    @Test
    fun `isTileFree returns false for blocked tile`() {
        val blockedBoard = Board(3, 1, 1)
        blockedBoard.tiles.add(Tile(0, "a", 0, 0, 0))
        blockedBoard.tiles.add(Tile(1, "a", 0, 1, 0))
        blockedBoard.tiles.add(Tile(2, "a", 0, 2, 0))

        val blockedTile = blockedBoard.getTileAt(1, 0, 0)!!
        assertFalse(MatchEngine.isTileFree(blockedTile, blockedBoard))
    }

    @Test
    fun `isTileFree returns true for edge tiles`() {
        val tile = board.getTileAt(0, 0, 0)!!
        assertTrue(MatchEngine.isTileFree(tile, board))
    }

    @Test
    fun `canMatch returns true for matching symbols`() {
        val tileA = board.getTileAt(0, 0, 0)!!
        val tileB = board.getTileAt(1, 0, 0)!!
        assertTrue(MatchEngine.canMatch(tileA, tileB, board))
    }

    @Test
    fun `canMatch returns false for different symbols`() {
        val tileA = board.getTileAt(0, 0, 0)!!
        val tileB = board.getTileAt(2, 0, 0)!!
        assertFalse(MatchEngine.canMatch(tileA, tileB, board))
    }

    @Test
    fun `canMatch returns false for same tile`() {
        val tile = board.getTileAt(0, 0, 0)!!
        assertFalse(MatchEngine.canMatch(tile, tile, board))
    }

    @Test
    fun `applyMatch marks tiles as matched`() {
        val tileA = board.getTileAt(0, 0, 0)!!
        val tileB = board.getTileAt(1, 0, 0)!!
        val newBoard = MatchEngine.applyMatch(board, tileA, tileB)

        assertTrue(newBoard.tiles[0].isMatched)
        assertTrue(newBoard.tiles[1].isMatched)
    }

    @Test
    fun `applyMatch fails for invalid match`() {
        val tileA = board.getTileAt(0, 0, 0)!!
        val tileB = board.getTileAt(2, 0, 0)!!
        val newBoard = MatchEngine.applyMatch(board, tileA, tileB)

        assertFalse(newBoard.tiles[0].isMatched)
        assertFalse(newBoard.tiles[2].isMatched)
    }

    @Test
    fun `isBoardCleared returns true when all tiles matched`() {
        for (tile in board.tiles) {
            tile.isMatched = true
        }
        assertTrue(MatchEngine.isBoardCleared(board))
    }

    @Test
    fun `isBoardCleared returns false when some tiles remain`() {
        board.tiles[0].isMatched = true
        board.tiles[1].isMatched = true
        assertFalse(MatchEngine.isBoardCleared(board))
    }

    @Test
    fun `findMatchingPair returns valid pair`() {
        val pair = MatchEngine.findMatchingPair(board)
        assertNotNull(pair)
        assertEquals(pair!!.first.symbolId, pair.second.symbolId)
    }

    @Test
    fun `findMatchingPair returns null when no matches possible`() {
        val tileA = board.getTileAt(0, 0, 0)!!
        tileA.isMatched = true
        val tileB = board.getTileAt(1, 0, 0)!!
        tileB.isMatched = true
        val tileC = board.getTileAt(2, 0, 0)!!
        tileC.isMatched = true
        val tileD = board.getTileAt(3, 0, 0)!!
        tileD.isMatched = true

        val pair = MatchEngine.findMatchingPair(board)
        assertNull(pair)
    }

    @Test
    fun `getPossibleMovesCount returns correct count`() {
        val count = MatchEngine.getPossibleMovesCount(board)
        assertEquals(3, count)
    }

    @Test
    fun `isSolvable returns true for clearable board`() {
        assertTrue(MatchEngine.isSolvable(board))
    }
}