# 🚀 QUICK START GUIDE - Step 1-3

## ⚡ Langkah Cepat (5 Menit)

### 1. Setup API Key (WAJIB!)

1. **Daftar NewsAPI** (2 menit)
   - Buka: https://newsapi.org/register
   - Isi form registrasi (gratis)
   - Verifikasi email
   - Copy API key yang diberikan

2. **Tambahkan ke Project** (1 menit)
   - Buka file `local.properties` (di root project)
   - Jika belum ada, copy dari `local.properties.template`
   - Edit baris:
     ```properties
     NEWS_API_KEY=paste_api_key_anda_disini
     ```
   - Save file

3. **Sync & Build** (2 menit)
   ```bash
   ./gradlew clean build
   ```

### 2. Verifikasi Setup

**Run app** → Jika berhasil:
- ✅ App compile tanpa error
- ✅ Hilt injection working
- ✅ Ready untuk step 4

---

## 📝 Lokasi File Penting

```
NewsApp/
├── local.properties              ← TAMBAHKAN API KEY DISINI! (AMAN dari git)
├── local.properties.template     ← Template untuk reference
├── API_KEY_SETUP.md             ← Panduan lengkap
├── IMPLEMENTATION_STEP_1-3.md   ← Dokumentasi detail
│
├── app/src/main/java/com/example/newsapp/
│   ├── NewsApplication.kt       ← Hilt Application (BARU)
│   ├── MainActivity.kt          ← Updated dengan @AndroidEntryPoint
│   │
│   ├── di/                      ← Dependency Injection (BARU)
│   │   ├── NetworkModule.kt     ← Retrofit, OkHttp, API setup
│   │   └── RepositoryModule.kt  ← Repository providers
│   │
│   ├── network/                 ← Network Layer (BARU)
│   │   ├── NewsApiService.kt    ← API endpoints
│   │   ├── dto/
│   │   │   └── NewsApiModels.kt ← Response DTOs
│   │   └── mapper/
│   │       └── NewsMapper.kt    ← DTO → Domain conversion
│   │
│   ├── data/
│   │   └── NewsRepository.kt    ← Refactored (class, DI, network support)
│   │
│   └── util/                    ← Utilities (BARU)
│       └── Resource.kt          ← State wrapper (Success/Error/Loading)
```

---

## 🧪 Test Manual (Optional)

Untuk memastikan semua berjalan, tambahkan di `MainActivity`:

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var newsRepository: NewsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Test API (optional - untuk debug)
        lifecycleScope.launch {
            val result = newsRepository.fetchArticlesFromNetwork("technology")
            when (result) {
                is Resource.Success -> {
                    Log.d("API_TEST", "✅ Success: ${result.data.size} articles")
                }
                is Resource.Error -> {
                    Log.e("API_TEST", "❌ Error: ${result.message}")
                }
                is Resource.Loading -> {
                    Log.d("API_TEST", "⏳ Loading...")
                }
            }
        }
        
        // Rest of onCreate...
    }
}
```

Check Logcat dengan filter "API_TEST" untuk lihat hasilnya.

---

## ❓ FAQ

### Q: API key saya tidak terdeteksi?
**A**: 
1. Pastikan file `local.properties` di root project (sejajar `settings.gradle.kts`)
2. Format harus: `NEWS_API_KEY=your_key` (tanpa spasi, tanpa quotes)
3. Sync project: File → Sync Project with Gradle Files
4. Clean build: `./gradlew clean build`

### Q: App bisa jalan tanpa API key?
**A**: Ya! Ada fallback ke "demo mode" yang pakai data lokal dari `assets/news_data.json`

### Q: API key aman dari GitHub?
**A**: Ya! File `local.properties` sudah di `.gitignore`, tidak akan ter-push.

### Q: Rate limit habis?
**A**: Free tier = 100 req/hari. Untuk development, gunakan data lokal dulu atau upgrade plan.

---

## 🎯 Next Action

Setelah Step 1-3 selesai, lanjut ke:

**Step 4**: Create ViewModels
- HomeViewModel
- ArticleDetailViewModel
- Dll

Baca detail di: `IMPLEMENTATION_STEP_1-3.md`

---

**Need Help?** Check:
- `API_KEY_SETUP.md` - Panduan API key detail
- `IMPLEMENTATION_STEP_1-3.md` - Dokumentasi lengkap
- Logcat dengan filter "NewsRepository" atau "OkHttp" untuk debug
