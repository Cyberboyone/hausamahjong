package com.nakudin.hausamahjong.game

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.random.Random

@Serializable
data class LevelData(
    val levelNumber: Int,
    val width: Int,
    val height: Int,
    val layers: Int,
    val tiles: List<TileData> = emptyList(),
    val layoutType: String = "diamond",
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

        assignFaceUpStatus(board, data)
        MatchEngine.invalidateCache()
        return board
    }

    private fun assignFaceUpStatus(board: Board, data: LevelData) {
        val faceUpRatio = getFaceUpRatio(data.difficulty)
        val topLayerTiles = board.tiles.filter {
            board.getTileAt(it.x, it.y, it.layer + 1) == null
        }

        val shuffled = topLayerTiles.shuffled()
        val faceUpCount = (topLayerTiles.size * faceUpRatio).toInt().coerceAtLeast(1)

        for (tile in board.tiles) {
            tile.isFaceUp = false
        }
        for (tile in shuffled.take(faceUpCount)) {
            tile.isFaceUp = true
        }
    }

    private fun getFaceUpRatio(difficulty: Int): Float {
        return when {
            difficulty <= 2 -> 1.0f
            difficulty <= 4 -> 0.75f
            difficulty <= 6 -> 0.5f
            difficulty <= 8 -> 0.35f
            else -> 0.25f
        }
    }

    fun generateLevel(levelNumber: Int): LevelData {
        val difficulty = calculateDifficulty(levelNumber)
        val (size, layers, numSymbols) = getLevelParams(difficulty, levelNumber)

        val width = size
        val height = size

        // Try multiple seeds to find a solvable board
        val maxRetries = 100
        for (attempt in 0 until maxRetries) {
            val seed = levelNumber * 31 + 7 + attempt * 997
            val rng = Random(seed)

            val shuffledSymbols = allSymbols.shuffled(rng)
            val levelSymbols = shuffledSymbols.take(numSymbols)

            val positions = mutableListOf<Triple<Int, Int, Int>>()
            val layoutType = selectLayoutType(levelNumber, attempt)
            for (layer in 0 until layers) {
                when (layoutType) {
                    "diamond" -> addDiamondPositions(positions, width, height, layers, layer)
                    "pyramid" -> addPyramidPositions(positions, width, height, layers, layer)
                    "cross" -> addCrossPositions(positions, width, height, layers, layer)
                    "staggered" -> addStaggeredPositions(positions, width, height, layers, layer)
                    else -> addDiamondPositions(positions, width, height, layers, layer)
                }
            }

            val shuffledPositions = positions.shuffled(rng)
            val sizeLimit = (shuffledPositions.size / 2) * 2
            val tileData = mutableListOf<TileData>()

            val symbolRng = Random(seed + 1)
            val shuffledSymbolsForLevel = levelSymbols.shuffled(symbolRng)

            for (i in 0 until sizeLimit step 2) {
                val symbol = shuffledSymbolsForLevel[(i / 2) % shuffledSymbolsForLevel.size]
                val pos1 = shuffledPositions[i]
                val pos2 = shuffledPositions[i + 1]
                tileData.add(TileData(symbol, pos1.third, pos1.first, pos1.second))
                tileData.add(TileData(symbol, pos2.third, pos2.first, pos2.second))
            }

            tileData.shuffle(rng)

            val levelData = LevelData(
                levelNumber = levelNumber,
                width = width,
                height = height,
                layers = layers,
                tiles = tileData,
                layoutType = layoutType,
                difficulty = difficulty,
                name_en = "Level $levelNumber",
                name_ha = "Mataki $levelNumber"
            )

            // Verify solvability
            val board = loadBoard(levelData)
            if (MatchEngine.isSolvable(board)) {
                return levelData
            }
            // If not solvable, retry with next seed
        }

        // Fallback: return without solvability check (extremely rare)
        val seed = levelNumber * 31 + 7
        val rng = Random(seed)
        val shuffledSymbols = allSymbols.shuffled(rng)
        val levelSymbols = shuffledSymbols.take(numSymbols)
        val layoutType = selectLayoutType(levelNumber, 0)

        val positions = mutableListOf<Triple<Int, Int, Int>>()
        for (layer in 0 until layers) {
            addDiamondPositions(positions, width, height, layers, layer)
        }

        val shuffledPositions = positions.shuffled(rng)
        val sizeLimit = (shuffledPositions.size / 2) * 2
        val tileData = mutableListOf<TileData>()
        val symbolRng = Random(seed + 1)
        val shuffledSymbolsForLevel = levelSymbols.shuffled(symbolRng)

        for (i in 0 until sizeLimit step 2) {
            val symbol = shuffledSymbolsForLevel[(i / 2) % shuffledSymbolsForLevel.size]
            val pos1 = shuffledPositions[i]
            val pos2 = shuffledPositions[i + 1]
            tileData.add(TileData(symbol, pos1.third, pos1.first, pos1.second))
            tileData.add(TileData(symbol, pos2.third, pos2.first, pos2.second))
        }
        tileData.shuffle(rng)

        return LevelData(
            levelNumber = levelNumber,
            width = width,
            height = height,
            layers = layers,
            tiles = tileData,
            layoutType = layoutType,
            difficulty = difficulty,
            name_en = "Level $levelNumber",
            name_ha = "Mataki $levelNumber"
        )
    }

    private fun selectLayoutType(level: Int, attempt: Int): String {
        val layouts = listOf("diamond", "pyramid", "cross", "staggered")
        return layouts[(level + attempt) % layouts.size]
    }

    private fun addDiamondPositions(
        positions: MutableList<Triple<Int, Int, Int>>,
        width: Int, height: Int, maxLayers: Int, layer: Int
    ) {
        val centerX = width / 2
        val centerY = height / 2
        val radius = maxLayers - layer
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (abs(x - centerX) + abs(y - centerY) <= radius) {
                    positions.add(Triple(x, y, layer))
                }
            }
        }
    }

    private fun addPyramidPositions(
        positions: MutableList<Triple<Int, Int, Int>>,
        width: Int, height: Int, maxLayers: Int, layer: Int
    ) {
        val centerX = width / 2
        val centerY = height / 2
        val layerSize = maxLayers - layer
        val startX = centerX - layerSize + 1
        val startY = centerY - layerSize + 1
        val endX = centerX + layerSize - 1
        val endY = centerY + layerSize - 1
        for (x in startX..endX) {
            for (y in startY..endY) {
                if (x in 0 until width && y in 0 until height) {
                    positions.add(Triple(x, y, layer))
                }
            }
        }
    }

    private fun addCrossPositions(
        positions: MutableList<Triple<Int, Int, Int>>,
        width: Int, height: Int, maxLayers: Int, layer: Int
    ) {
        val centerX = width / 2
        val centerY = height / 2
        val armLength = maxLayers - layer + 1
        for (i in -armLength..armLength) {
            val x = centerX + i
            if (x in 0 until width) {
                positions.add(Triple(x, centerY, layer))
            }
            val y = centerY + i
            if (y in 0 until height && i != 0) {
                positions.add(Triple(centerX, y, layer))
            }
        }
    }

    private fun addStaggeredPositions(
        positions: MutableList<Triple<Int, Int, Int>>,
        width: Int, height: Int, maxLayers: Int, layer: Int
    ) {
        val offset = layer % 2
        for (x in 0 until width) {
            for (y in 0 until height) {
                if ((x + y + offset) % 2 == 0) {
                    positions.add(Triple(x, y, layer))
                }
            }
        }
    }

    private fun calculateDifficulty(level: Int): Int {
        val wavePosition = (level - 1) % 50
        val baseDifficulty = ((level - 1) / 50) + 1

        val isEasyWave = wavePosition % 15 == 0 || wavePosition % 25 == 0

        return if (isEasyWave && baseDifficulty > 2) {
            (baseDifficulty - 2).coerceAtLeast(1)
        } else {
            baseDifficulty.coerceIn(1, 10)
        }
    }

    private fun getLevelParams(difficulty: Int, levelNumber: Int): Triple<Int, Int, Int> {
        return when (difficulty) {
            1 -> Triple(3, 1, 2)
            2 -> Triple(4, 1, 4)
            3 -> Triple(4, 1, 6)
            4 -> Triple(5, 2, 6)
            5 -> Triple(5, 2, 8)
            6 -> Triple(5, 2, 10)
            7 -> Triple(6, 2, 10)
            8 -> Triple(6, 3, 12)
            9 -> Triple(6, 3, 14)
            10 -> Triple(7, 3, 16)
            else -> Triple(5, 2, 8)
        }
    }

    private fun generateLayoutTiles(data: LevelData, symbols: List<String>): List<TileData> {
        return when (data.layoutType) {
            "pyramid" -> generatePyramidTiles(data, symbols)
            "diamond" -> generateDiamondTiles(data, symbols)
            "cross" -> generateCrossTiles(data, symbols)
            "staggered" -> generateStaggeredTiles(data, symbols)
            else -> generateDiamondTiles(data, symbols)
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
                    val distX = abs(x - centerX)
                    val distY = abs(y - centerY)
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

    private fun generateCrossTiles(data: LevelData, symbols: List<String>): List<TileData> {
        val tiles = mutableListOf<TileData>()
        val centerX = data.width / 2
        val centerY = data.height / 2
        var tileId = 0

        for (layer in 0 until data.layers) {
            val armLength = data.layers - layer + 1
            for (i in -armLength..armLength) {
                val x = centerX + i
                if (x in 0 until data.width) {
                    val symbol = symbols[(tileId / 2) % symbols.size]
                    tiles.add(TileData(symbol, layer, x, centerY))
                    tileId++
                }
                val y = centerY + i
                if (y in 0 until data.height && i != 0) {
                    val symbol = symbols[(tileId / 2) % symbols.size]
                    tiles.add(TileData(symbol, layer, centerX, y))
                    tileId++
                }
            }
        }

        return ensureEvenPairs(tiles, symbols)
    }

    private fun generateStaggeredTiles(data: LevelData, symbols: List<String>): List<TileData> {
        val tiles = mutableListOf<TileData>()
        var tileId = 0

        for (layer in 0 until data.layers) {
            val offset = layer % 2
            for (x in 0 until data.width) {
                for (y in 0 until data.height) {
                    if ((x + y + offset) % 2 == 0) {
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
            symbolCounts[symbol] = symbolCounts[symbol]!!.dropLast(1).toMutableList()
        }

        val totalTiles = tiles.size
        if (totalTiles % 2 != 0) {
            tiles.removeAt(tiles.size - 1)
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

    fun generateLevelData(levelNumber: Int, layoutType: String, width: Int, height: Int, layers: Int): LevelData {
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