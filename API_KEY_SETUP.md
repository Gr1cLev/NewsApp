# 🔐 API KEY SETUP GUIDE

## Cara Menambahkan API Key (AMAN dari GitHub)

### 1. Dapatkan API Key dari NewsAPI.org
1. Buka https://newsapi.org/register
2. Daftar dengan email Anda (GRATIS)
3. Setelah verifikasi email, Anda akan mendapat API key
4. Copy API key tersebut

### 2. Tambahkan API Key ke Project
1. Buka file `local.properties` di root project (sejajar dengan `settings.gradle.kts`)
   - Jika belum ada, buat file baru dengan nama `local.properties`
2. Tambahkan baris berikut:
   ```properties
   sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
   NEWS_API_KEY=paste_api_key_anda_disini
   ```
3. Save file

### 3. Contoh `local.properties`
```properties
## This file must *NOT* be checked into Version Control Systems,
# as it contains information specific to your local configuration.
#
# Location of the SDK. This is only used by Gradle.
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk

# News API Key (dapatkan dari https://newsapi.org)
NEWS_API_KEY=abc123def456ghi789jkl012mno345pqr
```

### 4. Verifikasi Setup
- File `local.properties` sudah otomatis ada di `.gitignore`
- **AMAN** untuk disimpan di komputer lokal
- **TIDAK AKAN** ter-push ke GitHub
- Setiap developer team harus punya `local.properties` sendiri

### 5. Build Project
```bash
./gradlew clean build
```

## 🚨 PENTING - Jangan Lakukan Ini!

### ❌ JANGAN hardcode API key di kode:
```kotlin
// JANGAN SEPERTI INI!
const val API_KEY = "abc123def456" // Akan ter-push ke GitHub!
```

### ✅ Gunakan BuildConfig (sudah di-setup):
```kotlin
// Sudah di-setup di NetworkModule.kt
val apiKey = BuildConfig.NEWS_API_KEY // Aman!
```

## 📝 Notes untuk Team
- Setiap developer harus setup `local.properties` sendiri
- API key GRATIS tier: 100 requests/hari
- Untuk production, upgrade ke paid plan atau gunakan backend proxy

## 🔄 Fallback Mode
Jika API key tidak di-set, app akan otomatis menggunakan **demo mode** dengan data lokal dari `assets/news_data.json`

## 📚 Dokumentasi API
- Docs: https://newsapi.org/docs
- Dashboard: https://newsapi.org/account
- Rate Limit: 100 requests/day (free tier)
