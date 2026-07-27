package com.nakudin.hausamahjong.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nakudin.hausamahjong.R
import com.nakudin.hausamahjong.data.CoinManager
import com.nakudin.hausamahjong.data.CoinRewards
import com.nakudin.hausamahjong.data.ShopManager

class ShopActivity : AppCompatActivity(), CoinManager.OnCoinChangeListener {

    private lateinit var tvCoins: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var layoutConsumables: LinearLayout
    private lateinit var layoutThemes: LinearLayout
    private lateinit var layoutCoinPacks: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        setContentView(R.layout.activity_shop)

        tvCoins = findViewById(R.id.tvCoins)
        btnBack = findViewById(R.id.btnBack)
        layoutConsumables = findViewById(R.id.layoutConsumables)
        layoutThemes = findViewById(R.id.layoutThemes)
        layoutCoinPacks = findViewById(R.id.layoutCoinPacks)

        CoinManager.addListener(this)
        updateCoinDisplay()

        btnBack.setOnClickListener { finish() }

        populateShopItems()
    }

    private fun populateShopItems() {
        for (item in ShopManager.getAllItems()) {
            val row = when (item.type) {
                ShopManager.ItemType.CONSUMABLE -> layoutConsumables
                ShopManager.ItemType.PERMANENT -> layoutThemes
                ShopManager.ItemType.CURRENCY -> layoutCoinPacks
            }
            val view = LayoutInflater.from(this).inflate(R.layout.item_shop, row, false)
            val ivIcon = view.findViewById<ImageView>(R.id.ivIcon)
            val tvName = view.findViewById<TextView>(R.id.tvName)
            val tvDescription = view.findViewById<TextView>(R.id.tvDescription)
            val btnBuy = view.findViewById<Button>(R.id.btnBuy)

            tvName.text = item.name
            tvDescription.text = item.description

            val owned = ShopManager.isOwned(item)
            if (owned && item.type == ShopManager.ItemType.PERMANENT) {
                val isCurrent = ShopManager.getCurrentTheme() == item.id
                btnBuy.text = if (isCurrent) "Equipped" else "Equip"
                btnBuy.setBackgroundResource(R.drawable.bg_button_green)
            } else if (owned) {
                btnBuy.text = "Owned"
                btnBuy.isEnabled = false
            } else {
                btnBuy.text = "₦${item.cost}"
            }

            btnBuy.setOnClickListener {
                if (owned && item.type == ShopManager.ItemType.PERMANENT) {
                    ShopManager.setCurrentTheme(item.id)
                    Toast.makeText(this, "${item.name} equipped!", Toast.LENGTH_SHORT).show()
                    populateShopItems()
                    return@setOnClickListener
                }
                if (owned) return@setOnClickListener

                if (CoinManager.canAfford(item.cost)) {
                    ShopManager.purchase(item)
                    if (item.type == ShopManager.ItemType.PERMANENT) {
                        ShopManager.setOwnedTheme(item.id)
                        ShopManager.setCurrentTheme(item.id)
                    }
                    if (item.type == ShopManager.ItemType.CURRENCY) {
                        CoinManager.addCoins(item.rewardAmount, "purchase_${item.id}")
                    }
                    Toast.makeText(this, "Purchased ${item.name}!", Toast.LENGTH_SHORT).show()
                    updateCoinDisplay()
                    populateShopItems()
                } else {
                    Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show()
                }
            }

            row.addView(view)
        }
    }

    private fun updateCoinDisplay() {
        tvCoins.text = CoinManager.formatCoins(CoinManager.getCoins())
    }

    override fun onCoinsChanged(newAmount: Int, change: Int) {
        runOnUiThread { updateCoinDisplay() }
    }

    override fun onDestroy() {
        super.onDestroy()
        CoinManager.removeListener(this)
    }
}