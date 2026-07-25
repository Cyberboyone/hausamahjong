# Hausa Mahjong

A mahjong-style tile-pair matching puzzle game with authentic Hausa/Northern Nigeria cultural theming.

## Features

- **Pyramid-style mahjong** with multi-layered tile formations
- **75+ Hausa cultural tiles** across 3 categories:
  - Cultural items (Kalangu, Rumbu, Lalle, etc.)
  - People/Roles (Sarki, Waziri, Hakimi, etc.)
  - Landmarks (Dala Hill, Kano City Wall, Emir's Palace, etc.)
- **36 verified Hausa proverbs** shown between levels
- **Bilingual support** - English and Hausa language toggle
- **Fair ad pacing** - skippable interstitials, optional rewarded ads
- **Remove Ads** - one-time in-app purchase

## Tech Stack

- Kotlin, native Android SDK
- Custom `View` for board rendering (Canvas-based)
- Unity LevelPlay Ads (interstitial + rewarded)
- JSON for level layouts and content
- Google Play Billing for IAP
- Min SDK: 21 (Android 5.0)

## Project Structure

```
app/src/main/java/com/nakudin/hausamahjong/
├── game/           # Core game logic (Tile, Board, MatchEngine, GameState)
├── ui/             # Activities and Views (BoardView, GameActivity, MenuActivity)
├── ads/            # Ad management (AdManager, PurchaseManager)
└── data/           # Repositories (LevelRepository, ProverbRepository, TileSetRepository)
```

## Build

```bash
./gradlew assembleDebug
```

## License

Private - All rights reserved.