package com.nakudin.hausamahjong.game

import kotlinx.serialization.Serializable

@Serializable
data class LevelData(
    val levelNumber: Int,
    val width: Int,
    val height: Int,
    val layers: Int,
    val tiles: List<TileData> = emptyList(),
    val layoutType: String = "pyramid",
    val name_ha: String = "",
    val name_en: String = "",
    val difficulty: Int = 1,
    val description: String = ""
)

@Serializable
data class TileData(
    val symbolId: String,
    val layer: Int,
    val x: Int,
    val y: Int
)

object LevelLoader {

    private val allSymbols = listOf(
        "rijiya", "danga", "rumbu", "kofar_mata", "shirya",
        "kalangu", "kaho", "falo", "gurmi", "shantu",
        "sulke", "takobi", "garkuwa", "baka", "kwari",
        "lalle", "adire", "ado", "tulu", "turmi",
        "kujera", "haske", "taguwa", "rugar_fulani", "masallaci",
        "sarki", "waziri", "hakimi", "malam", "jakada",
        "gimbiya", "babban_gida", "mai_unguwa", "doki", "liman",
        "sarkin_noma", "maigadi", "barde", "mai_wakar_bori", "magajin_gari",
        "mai_kasuwa", "yarinya", "yaro", "soro", "malami",
        "mai_dawa", "mai_rake", "sarkin_fada",
        "dala_hill", "kano_wall", "kofar_mata_dye", "gidan_rumfa", "kurmi_market",
        "emir_palace_zaria", "zaria_wall", "kajuru_castle", "jemea_palace", "nok_culture",
        "sultan_palace", "shehu_tomb"
    )

    fun loadBoard(data: LevelData, availableSymbols: List<String> = allSymbols): Board {
        val tiles = if (data.tiles.isEmpty()) {
            generateLayoutTiles(data, availableSymbols)
        } else {
            data.tiles
        }

        val board = Board(data.width, data.height, data.layers)
        var tileId = 0
        for (tileData in tiles) {
            val tile = Tile(
                id = tileId++,
                symbolId = tileData.symbolId,
                layer = tileData.layer,
                x = tileData.x,
                y = tileData.y
            )
            board.tiles.add(tile)
        }

        assignFaceUpStatus(board)

        return board
    }

    private fun assignFaceUpStatus(board: Board) {
        for (tile in board.tiles) {
            val aboveTile = board.getTileAt(tile.x, tile.y, tile.layer + 1)
            val hasAbove = aboveTile != null && !aboveTile.isMatched
            tile.isFaceUp = !hasAbove
        }
    }

    private fun generateLayoutTiles(data: LevelData, symbols: List<String>): List<TileData> {
        return when (data.layoutType) {
            "pyramid" -> generatePyramidTiles(data, symbols)
            "diamond" -> generateDiamondTiles(data, symbols)
            "mixed" -> generateMixedTiles(data, symbols)
            else -> generatePyramidTiles(data, symbols)
        }
    }

    private fun generatePyramidTiles(data: LevelData, symbols: List<String>): List<TileData> {
        val tiles = mutableListOf<TileData>()
        val centerX = data.width / 2
        val centerY = data.height / 2
        var tileId = 0

        for (layer in 0 until data.layers) {
            val layerSize = data.layers - layer
            val startX = centerX - layerSize + 1
            val startY = centerY - layerSize + 1
            val endX = centerX + layerSize - 1
            val endY = centerY + layerSize - 1

            for (x in startX..endX) {
                for (y in startY..endY) {
                    if (x in 0 until data.width && y in 0 until data.height) {
                        val symbol = symbols[(tileId / 2) % symbols.size]
                        tiles.add(TileData(symbol, layer, x, y))
                        tileId++
                    }
                }
            }
        }

        return ensureEvenPairs(tiles, symbols)
    }

    private fun generateDiamondTiles(data: LevelData, symbols: List<String>): List<TileData> {
        val tiles = mutableListOf<TileData>()
        val centerX = data.width / 2
        val centerY = data.height / 2
        var tileId = 0

        for (layer in 0 until data.layers) {
            val radius = data.layers - layer
            for (x in 0 until data.width) {
                for (y in 0 until data.height) {
                    val distX = kotlin.math.abs(x - centerX)
                    val distY = kotlin.math.abs(y - centerY)
                    if (distX + distY <= radius) {
                        val symbol = symbols[(tileId / 2) % symbols.size]
                        tiles.add(TileData(symbol, layer, x, y))
                        tileId++
                    }
                }
            }
        }

        return ensureEvenPairs(tiles, symbols)
    }

    private fun generateMixedTiles(data: LevelData, symbols: List<String>): List<TileData> {
        val tiles = mutableListOf<TileData>()
        var tileId = 0

        for (layer in 0 until data.layers) {
            val size = data.width - layer * 2
            if (size <= 0) break
            val offset = layer

            for (x in offset until offset + size) {
                for (y in offset until offset + size) {
                    if (x in 0 until data.width && y in 0 until data.height) {
                        val symbol = symbols[(tileId / 2) % symbols.size]
                        tiles.add(TileData(symbol, layer, x, y))
                        tileId++
                    }
                }
            }
        }

        return ensureEvenPairs(tiles, symbols)
    }

    private fun ensureEvenPairs(tiles: MutableList<TileData>, symbols: List<String>): List<TileData> {
        val symbolCounts = tiles.groupBy { it.symbolId }.toMutableMap()

        val oddSymbols = symbolCounts.filter { it.value.size % 2 != 0 }
        for ((symbol, tileList) in oddSymbols) {
            val lastTile = tileList.last()
            tiles.remove(lastTile)
            symbolCounts[symbol] = symbolCounts[symbol]!!.dropLast(1)
        }

        val totalTiles = tiles.size
        if (totalTiles % 2 != 0) {
            tiles.removeLast()
        }

        tiles.shuffle()

        return tiles
    }

    fun loadBoards(json: String): List<LevelData> {
        return kotlinx.serialization.json.Json.decodeFromString<List<LevelData>>(json)
    }

    fun loadBoardFromJson(json: String): Board {
        val data = kotlinx.serialization.json.Json.decodeFromString<LevelData>(json)
        return loadBoard(data)
    }

    fun validateLevel(data: LevelData): List<String> {
        val errors = mutableListOf<String>()

        if (data.tiles.size < 2) {
            errors.add("Level must have at least 2 tiles")
        }

        if (data.tiles.size % 2 != 0) {
            errors.add("Level must have an even number of tiles")
        }

        val symbolCounts = data.tiles.groupBy { it.symbolId }
        for ((symbol, tileList) in symbolCounts) {
            if (tileList.size % 2 != 0) {
                errors.add("Symbol '$symbol' has odd count: ${tileList.size}")
            }
        }

        for ((i, tile) in data.tiles.withIndex()) {
            if (tile.x < 0 || tile.x >= data.width) {
                errors.add("Tile $i x=${tile.x} out of bounds [0, ${data.width})")
            }
            if (tile.y < 0 || tile.y >= data.height) {
                errors.add("Tile $i y=${tile.y} out of bounds [0, ${data.height})")
            }
            if (tile.layer < 0 || tile.layer >= data.layers) {
                errors.add("Tile $i layer=${tile.layer} out of bounds [0, ${data.layers})")
            }
        }

        val positions = data.tiles.map { "${it.x},${it.y},${it.layer}" }
        if (positions.size != positions.toSet().size) {
            errors.add("Duplicate tile positions found")
        }

        return errors
    }

    fun generateLevel(levelNumber: Int, layoutType: String, width: Int, height: Int, layers: Int): LevelData {
        return LevelData(
            levelNumber = levelNumber,
            width = width,
            height = height,
            layers = layers,
            layoutType = layoutType,
            name_en = "Level $levelNumber",
            name_ha = "Mataki $levelNumber",
            difficulty = (layers + 1) / 2
        )
    }
}