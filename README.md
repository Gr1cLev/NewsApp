# NewsApp

Modern Android news application built with Jetpack Compose, implementing MVVM architecture, Dependency Injection with Hilt, and RESTful API integration.

## ✨ Features

- 📰 Browse news by categories (Sports, Business, Technology, etc.)
- 🔍 Search articles with real-time results
- 🔖 Bookmark favorite articles (per-user profile)
- 👤 User authentication & profile management
- 🌓 Dark/Light theme toggle
- 🎨 Material Design 3 UI
- 🔄 Pull-to-refresh
- 📱 Offline support with local caching
- 🤖 **ML-powered personalized recommendations** (Phase 1 completed)
- 📊 **Firebase Analytics tracking** (Phase 1 completed)

## 🏗️ Architecture

- **MVVM Pattern**: ViewModels + StateFlow for reactive UI
- **Dependency Injection**: Hilt for compile-time DI
- **Network Layer**: Retrofit + OkHttp + Moshi
- **Local Storage**: Room database (coming soon) + SharedPreferences
- **Firebase**: Firestore, Auth, FCM (ready for integration)

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt 2.51.1
- **Networking**: Retrofit 2.11.0 + OkHttp 4.12.0
- **JSON**: Moshi 1.15.1
- **Firebase**: Firestore + Authentication + Analytics
- **Database**: Room 2.6.1 (for local caching)
- **Image Loading**: Coil
- **Video Player**: ExoPlayer (Media3)
- **Build**: Gradle Kotlin DSL
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 11+
- Gradle 8.13+

### Setup

1. **Clone repository**
   ```bash
   git clone https://github.com/Gr1cLev/NewsApp.git
   cd NewsApp
   ```

2. **Setup API Key** (Required!)
   - Register at [NewsAPI.org](https://newsapi.org/register) (FREE)
   - Get your API key
   - Create `local.properties` in root project:
     ```properties
     sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
     NEWS_API_KEY=your_api_key_here
     ```
   - See `API_KEY_SETUP.md` for detailed instructions

3. **Setup Firebase** (Required for ML features!)
   - Follow instructions in `FIREBASE_SETUP.md`
   - Download `google-services.json` from Firebase Console
   - Place in `app/` directory
   - Enable Authentication, Firestore, Analytics
   - See `FIRESTORE_STRUCTURE.md` for database schema

4. **Build & Run**
   ```bash
   ./gradlew clean build
   ```
   - Or use Android Studio: Run → Run 'app'

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [QUICK_START.md](QUICK_START.md) | 5-minute setup guide |
| [API_KEY_SETUP.md](API_KEY_SETUP.md) | How to setup NewsAPI key (secure) |
| [IMPLEMENTATION_STEP_1-3.md](IMPLEMENTATION_STEP_1-3.md) | Technical implementation details |
| [CONTOH_PENGGUNAAN.md](CONTOH_PENGGUNAAN.md) | Usage examples & code samples |
| [KESIMPULAN_STEP_1-3.md](KESIMPULAN_STEP_1-3.md) | Summary of completed work |
| [FIREBASE_SETUP.md](FIREBASE_SETUP.md) | **Firebase configuration guide** |
| [FIRESTORE_STRUCTURE.md](FIRESTORE_STRUCTURE.md) | **Database schema & relationships** |

## 🔐 Security

- ✅ API keys stored in `local.properties` (gitignored)
- ✅ BuildConfig for compile-time secrets
- ✅ Firebase Security Rules for user data protection
- ✅ User authentication with Firebase Auth
- ✅ Encrypted Firestore data per user
- ✅ HTTPS only connections
- 🔄 Password encryption with Security Crypto (planned)

## 🎯 Current Status

### ✅ Completed (Step 1-4)
- [x] Hilt Dependency Injection
- [x] Retrofit + API integration setup
- [x] Network layer with error handling
- [x] Secure API key management
- [x] Resource wrapper for state management
- [x] ViewModels with StateFlow
- [x] Pull-to-refresh functionality
- [x] **Firebase setup (Auth, Firestore, Analytics)**
- [x] **3 Firestore collections with relationships**
- [x] **User interaction tracking**
- [x] **Analytics event logging**

### 🚧 In Progress (Step 5-6)
- [ ] Login/Register UI screens
- [ ] Google Sign-In integration
- [ ] ML model development (data collection phase)
- [ ] Room database for offline caching
- [ ] Personalized recommendations UI

### 📋 Planned Features
- [ ] OAuth2 login (Google, Facebook)
- [ ] Push notifications (FCM)
- [ ] Reading history
- [ ] Article sharing
- [ ] Offline mode
- [ ] ML-powered recommendations
- [ ] Multi-language support

## 📱 Screenshots

*(Coming soon)*

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is open source and available under the MIT License.

## 🙏 Acknowledgments

- [NewsAPI.org](https://newsapi.org) for news data
- [Material Design 3](https://m3.material.io/) for UI guidelines
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern UI toolkit

---

**Developer**: Gr1cLev  
**Last Updated**: November 24, 2025
