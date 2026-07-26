package com.nakudin.hausamahjong.game

import kotlinx.serialization.Serializable

@Serializable
data class Tile(
    val id: Int,
    val symbolId: String,
    val layer: Int,
    val x: Int,
    val y: Int,
    var isMatched: Boolean = false,
    var isFaceUp: Boolean = false
) {
    fun copyWith(
        id: Int = this.id,
        symbolId: String = this.symbolId,
        layer: Int = this.layer,
        x: Int = this.x,
        y: Int = this.y,
        isMatched: Boolean = this.isMatched,
        isFaceUp: Boolean = this.isFaceUp
    ): Tile = Tile(id, symbolId, layer, x, y, isMatched, isFaceUp)

    fun matches(other: Tile): Boolean = this.symbolId == other.symbolId

    fun isAtSamePosition(other: Tile): Boolean = this.x == other.x && this.y == other.y && this.layer == other.layer
}