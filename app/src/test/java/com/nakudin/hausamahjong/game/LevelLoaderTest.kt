package com.nakudin.hausamahjong.game

import org.junit.Assert.*
import org.junit.Test

class LevelLoaderTest {

    @Test
    fun `loadBoard creates correct board dimensions`() {
        val data = LevelData(
            levelNumber = 1,
            width = 6,
            height = 4,
            layers = 1,
            tiles = listOf(
                TileData("kolanut", 0, 0, 0),
                TileData("kolanut", 0, 1, 0)
            )
        )

        val board = LevelLoader.loadBoard(data)
        assertEquals(6, board.width)
        assertEquals(4, board.height)
        assertEquals(1, board.maxLayers)
    }

    @Test
    fun `loadBoard creates tiles with correct positions`() {
        val data = LevelData(
            levelNumber = 1,
            width = 6,
            height = 4,
            layers = 1,
            tiles = listOf(
                TileData("kolanut", 0, 0, 0),
                TileData("kolanut", 0, 1, 0)
            )
        )

        val board = LevelLoader.loadBoard(data)
        assertEquals(2, board.tiles.size)
        assertEquals("kolanut", board.tiles[0].symbolId)
        assertEquals(0, board.tiles[0].layer)
        assertEquals(0, board.tiles[0].x)
        assertEquals(0, board.tiles[0].y)
    }

    @Test
    fun `validateLevel catches odd tile count`() {
        val data = LevelData(
            levelNumber = 1,
            width = 6,
            height = 4,
            layers = 1,
            tiles = listOf(
                TileData("kolanut", 0, 0, 0),
                TileData("kolanut", 0, 1, 0),
                TileData("kalangu", 0, 2, 0)
            )
        )

        val errors = LevelLoader.validateLevel(data)
        assertTrue(errors.any { it.contains("even number") })
    }

    @Test
    fun `validateLevel catches odd symbol count`() {
        val data = LevelData(
            levelNumber = 1,
            width = 6,
            height = 4,
            layers = 1,
            tiles = listOf(
                TileData("kolanut", 0, 0, 0),
                TileData("kolanut", 0, 1, 0),
                TileData("kalangu", 0, 2, 0),
                TileData("kalangu", 0, 3, 0),
                TileData("goje", 0, 4, 0)
            )
        )

        val errors = LevelLoader.validateLevel(data)
        assertTrue(errors.any { it.contains("odd count") })
    }

    @Test
    fun `validateLevel catches out of bounds tiles`() {
        val data = LevelData(
            levelNumber = 1,
            width = 3,
            height = 1,
            layers = 1,
            tiles = listOf(
                TileData("a", 0, 0, 0),
                TileData("a", 0, 5, 0)
            )
        )

        val errors = LevelLoader.validateLevel(data)
        assertTrue(errors.any { it.contains("out of bounds") })
    }

    @Test
    fun `validateLevel catches duplicate positions`() {
        val data = LevelData(
            levelNumber = 1,
            width = 6,
            height = 4,
            layers = 1,
            tiles = listOf(
                TileData("a", 0, 0, 0),
                TileData("a", 0, 0, 0)
            )
        )

        val errors = LevelLoader.validateLevel(data)
        assertTrue(errors.any { it.contains("Duplicate") })
    }

    @Test
    fun `generateSimpleLevel creates valid level`() {
        val level = LevelLoader.generateSimpleLevel(1, listOf("a", "b", "c"), 6, 1)
        assertEquals(6, level.tiles.size)
        assertEquals(1, level.layers)
        assertEquals("Mataki 1", level.name_ha)
    }

    @Test
    fun `loadBoardFromJson parses valid JSON`() {
        val json = """
            {
                "levelNumber": 1,
                "width": 6,
                "height": 4,
                "layers": 1,
                "tiles": [
                    {"symbolId": "a", "layer": 0, "x": 0, "y": 0},
                    {"symbolId": "a", "layer": 0, "x": 1, "y": 0}
                ]
            }
        """.trimIndent()

        val board = LevelLoader.loadBoardFromJson(json)
        assertEquals(6, board.width)
        assertEquals(2, board.tiles.size)
    }
}