package com.nakudin.hausamahjong.data

import android.content.Context
import android.content.SharedPreferences

object ShopManager {
    private const val PREFS_NAME = "shop_data"
    private const val KEY_PURCHASED_PREFIX = "purchased_"
    private const val KEY_OWNED_THEMES = "owned_themes"
    private const val KEY_CURRENT_THEME = "current_theme"

    private var prefs: SharedPreferences? = null

    enum class ItemType {
        CONSUMABLE, // Hints, Undos
        PERMANENT,  // Themes, Power-ups
        CURRENCY    // Coin packs
    }

    data class ShopItem(
        val id: String,
        val name: String,
        val description: String,
        val iconRes: Int,
        val type: ItemType,
        val cost: Int,
        val rewardAmount: Int = 0, // For consumables: how many hints/undos
        val powerUpType: String? = null // "hint", "undo", "reveal", "shuffle"
    )

    private val allItems = listOf(
        // Consumables
        ShopItem("hint_1", "Single Hint", "Reveal one matching pair", R.drawable.ic_shop_hint, ItemType.CONSUMABLE, 150, 1, "hint"),
        ShopItem("hint_5", "Hint Pack (5)", "5 hints for the price of 4", R.drawable.ic_shop_hint_pack, ItemType.CONSUMABLE, 600, 5, "hint"),
        ShopItem("hint_10", "Hint Bundle (10)", "10 hints + 2 bonus", R.drawable.ic_shop_hint_pack, ItemType.CONSUMABLE, 1200, 12, "hint"),

        ShopItem("undo_1", "Single Undo", "Reverse your last move", R.drawable.ic_shop_undo, ItemType.CONSUMABLE, 100, 1, "undo"),
        ShopItem("undo_5", "Undo Pack (5)", "5 undos for the price of 4", R.drawable.ic_shop_undo_pack, ItemType.CONSUMABLE, 400, 5, "undo"),
        ShopItem("undo_10", "Undo Bundle (10)", "10 undos + 2 bonus", R.drawable.ic_shop_undo_pack, ItemType.CONSUMABLE, 800, 12, "undo"),

        // Power-ups
        ShopItem("reveal_all", "Reveal All", "Show all face-down tiles for 10 seconds", R.drawable.ic_shop_reveal, ItemType.CONSUMABLE, 500, 1, "reveal"),
        ShopItem("shuffle_board", "Shuffle", "Rearrange remaining tiles", R.drawable.ic_shop_shuffle, ItemType.CONSUMABLE, 300, 1, "shuffle"),
        ShopItem("extra_slot", "+1 Slot", "Permanently increase slot capacity to 5", R.drawable.ic_shop_slot, ItemType.PERMANENT, 5000, 0, "slot"),

        // Themes
        ShopItem("theme_default", "Classic Green", "Original emerald theme", R.drawable.ic_theme_green, ItemType.PERMANENT, 0, 0, "theme_green"),
        ShopItem("theme_gold", "Golden Sands", "Warm desert gold theme", R.drawable.ic_theme_gold, ItemType.PERMANENT, 2000, 0, "theme_gold"),
        ShopItem("theme_sunset", "Saharan Sunset", "Orange-red sunset theme", R.drawable.ic_theme_sunset, ItemType.PERMANENT, 3000, 0, "theme_sunset"),
        ShopItem("theme_night", "Midnight Blue", "Deep night theme", R.drawable.ic_theme_night, ItemType.PERMANENT, 3000, 0, "theme_night"),
        ShopItem("theme_royal", "Royal Purple", "Regal purple theme", R.drawable.ic_theme_purple, ItemType.PERMANENT, 5000, 0, "theme_purple"),
        ShopItem("theme_ocean", "Atlantic Blue", "Cool ocean theme", R.drawable.ic_achievement_ocean, ItemType.PERMANENT, 5000, 0, "theme_blue"),

        // Coin packs
        ShopItem("coins_500", "Small Purse", "₦500 coins", R.drawable.ic_shop_coin_small, ItemType.CURRENCY, 500, 500),
        ShopItem("coins_1200", "Coin Pouch", "₦1,200 coins (20% bonus)", R.drawable.ic_shop_coin_medium, ItemType.CURRENCY, 1000, 1200),
        ShopItem("coins_3000", "Treasure Chest", "₦3,000 coins (50% bonus)", R.drawable.ic_shop_coin_large, ItemType.CURRENCY, 2000, 3000),
        ShopItem("coins_10000", "Vault", "₦10,000 coins (100% bonus)", R.drawable.ic_shop_coin_huge, ItemType.CURRENCY, 5000, 10000),
    )

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getPrefs(): SharedPreferences =
        prefs ?: throw IllegalStateException("ShopManager not initialized")

    fun getAllItems(): List<ShopItem> = allItems

    fun getItem(id: String): ShopItem? = allItems.find { it.id == id }

    fun isOwned(item: ShopItem): Boolean {
        return getPrefs().getBoolean("$KEY_PURCHASED_PREFIX${item.id}", false)
    }

    fun purchase(item: ShopItem): Boolean {
        if (!CoinManager.canAfford(item.cost)) return false
        if (isOwned(item)) return false

        CoinManager.spendCoins(item.cost)
        getPrefs().edit().putBoolean("$KEY_PURCHASED_PREFIX${item.id}", true).apply()
        return true
    }

    fun getOwnedThemes(): List<String> {
        val set = getPrefs().getStringSet(KEY_OWNED_THEMES, emptySet()) ?: emptySet()
        return set.toList()
    }

    fun setOwnedTheme(themeId: String) {
        val themes = getOwnedThemes().toMutableSet()
        themes.add(themeId)
        getPrefs().edit().putStringSet(KEY_OWNED_THEMES, themes).apply()
    }

    fun getCurrentTheme(): String = getPrefs().getString(KEY_CURRENT_THEME, "theme_default") ?: "theme_default"

    fun setCurrentTheme(themeId: String) {
        getPrefs().edit().putString(KEY_CURRENT_THEME, themeId).apply()
    }
}