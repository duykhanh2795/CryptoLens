# CryptoLens 🪙

> App theo dõi và phân tích crypto realtime với AI — giao diện Binance-inspired, Kotlin + Jetpack Compose + Supabase

---

## 📁 Cấu trúc thư mục (Clean Architecture)

```
app/src/main/java/com/cryptolens/app/
│
├── MainActivity.kt
├── CryptoLensApp.kt
│
├── core/
│   ├── domain/util/
│   │   └── Resource.kt          ← Wrapper Success/Error/Loading
│   └── service/
│       └── CryptoFirebaseMessagingService.kt
│
├── di/
│   └── AppModule.kt             ← Hilt modules (Network, Repository)
│
├── navigation/
│   └── Navigation.kt            ← NavHost + BottomBar
│
├── ui/theme/
│   ├── Theme.kt                 ← Binance color palette
│   └── Typography.kt
│
└── features/
    ├── home/
    │   └── presentation/
    │       └── HomeScreen.kt
    │
    ├── market/
    │   ├── data/
    │   │   ├── remote/
    │   │   │   ├── CoinGeckoApiService.kt     ← Retrofit DTOs
    │   │   │   └── BinanceWebSocketManager.kt ← WS realtime
    │   │   └── repository/
    │   │       └── MarketRepositoryImpl.kt
    │   ├── domain/
    │   │   ├── model/         ← Coin, Kline, PriceTicker...
    │   │   ├── repository/    ← MarketRepository interface
    │   │   └── usecase/       ← GetTopCoins, ObserveTicker...
    │   └── presentation/
    │       └── marketlist/
    │           ├── MarketListViewModel.kt
    │           └── MarketListScreen.kt
    │
    └── portfolio/
        └── presentation/
            └── PortfolioScreen.kt
```

---

## 🚀 Setup & Chạy project

### 1. Clone / Mở project
- Mở Android Studio → **File → Open** → chọn thư mục `CryptoLens`
- Chờ Gradle sync (lần đầu ~5–10 phút)

### 2. Thêm API Keys vào `app/build.gradle.kts`
```kotlin
buildConfigField("String", "SUPABASE_URL",    "\"https://xxxx.supabase.co\"")
buildConfigField("String", "SUPABASE_ANON_KEY","\"your-anon-key\"")
buildConfigField("String", "GEMINI_API_KEY",   "\"your-gemini-api-key\"")
```

### 3. Thêm `google-services.json` (Firebase)
- Vào [console.firebase.google.com](https://console.firebase.google.com)
- Tạo project → Add Android app → package: `com.cryptolens.app`
- Tải `google-services.json` → đặt vào `app/` folder

### 4. Chạy app
- Kết nối điện thoại hoặc mở Emulator
- Bấm ▶ Run

---

## 🗺️ Roadmap — Các tính năng sẽ làm tiếp

| Tính năng | Trạng thái |
|---|---|
| Home screen | ✅ Done (UI static) |
| Market list (CoinGecko) | ✅ Done (ViewModel + UI) |
| Binance WebSocket realtime | ✅ Done (infrastructure) |
| Coin Detail screen | 🔲 Next |
| Candlestick chart (Canvas) | 🔲 Next |
| Portfolio CRUD | 🔲 Next |
| Supabase Auth (login/register) | 🔲 Next |
| AI Signal (Gemini API) | 🔲 Next |
| Price Alerts + FCM | 🔲 Next |
| News feed (CryptoPanic) | 🔲 Next |

---

## 🔑 APIs dùng trong project

| API | Mục đích | Auth |
|---|---|---|
| CoinGecko v3 | Market list, coin metadata | Không cần (demo) |
| Binance WebSocket | Giá realtime | Không cần |
| Binance REST | Klines / chart data | Không cần |
| Supabase | Auth, DB, Realtime | Cần API key |
| Gemini API | AI phân tích | Cần API key |
| Firebase FCM | Push notification | Cần google-services.json |

---

## 🏗️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: Clean Architecture + MVVM
- **DI**: Hilt
- **Networking**: Retrofit + OkHttp WebSocket
- **Serialization**: Kotlinx Serialization
- **Local DB**: Room + DataStore
- **Backend**: Supabase (Auth + Realtime + Storage)
- **Notifications**: Firebase FCM
- **Charts**: Vico + Compose Canvas
- **Image loading**: Coil 3
- **Background**: WorkManager
