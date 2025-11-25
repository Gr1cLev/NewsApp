# 🎯 CONTOH PENGGUNAAN - Step 1-3

Dokumen ini berisi contoh konkret cara menggunakan implementasi yang sudah dibuat.

---

## 1️⃣ Menggunakan Repository dengan Dependency Injection

### Sebelum (Old Way):
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Old: Akses langsung object singleton
        val articles = NewsRepository.getArticles(this)
        NewsRepository.invalidateCache()
    }
}
```

### Sesudah (New Way - Hilt):
```kotlin
@AndroidEntryPoint  // TAMBAHKAN INI!
class MainActivity : ComponentActivity() {
    
    @Inject  // DI otomatis oleh Hilt
    lateinit var newsRepository: NewsRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // New: Pakai instance yang di-inject
        newsRepository.invalidateCache()
        
        // Test fetch dari network
        lifecycleScope.launch {
            val result = newsRepository.fetchArticlesFromNetwork("technology")
            handleResult(result)
        }
    }
    
    private fun handleResult(result: Resource<List<NewsArticle>>) {
        when (result) {
            is Resource.Success -> {
                Log.d("API", "✅ Got ${result.data.size} articles")
                // Update UI...
            }
            is Resource.Error -> {
                Log.e("API", "❌ Error: ${result.message}")
                // Show error...
            }
            is Resource.Loading -> {
                Log.d("API", "⏳ Loading...")
                // Show loading...
            }
        }
    }
}
```

---

## 2️⃣ Fetch Articles dari Network

### Basic Usage:
```kotlin
// Di coroutine scope (ViewModel atau lifecycle scope)
lifecycleScope.launch {
    // Fetch semua top headlines
    val result = newsRepository.fetchArticlesFromNetwork()
    
    when (result) {
        is Resource.Success -> {
            val articles = result.data
            println("Fetched ${articles.size} articles")
            articles.forEach { article ->
                println("- ${article.title}")
            }
        }
        is Resource.Error -> {
            println("Error: ${result.message}")
            result.exception?.printStackTrace()
        }
        is Resource.Loading -> {
            println("Loading...")
        }
    }
}
```

### Dengan Kategori:
```kotlin
lifecycleScope.launch {
    // Technology articles
    val techResult = newsRepository.fetchArticlesFromNetwork(
        category = "technology",
        country = "id"
    )
    
    // Sports articles
    val sportsResult = newsRepository.fetchArticlesFromNetwork(
        category = "sports",
        country = "id"
    )
    
    // Business articles
    val businessResult = newsRepository.fetchArticlesFromNetwork(
        category = "business",
        country = "id"
    )
}
```

---

## 3️⃣ Search Articles

```kotlin
lifecycleScope.launch {
    // Search berita tentang "Indonesia"
    val searchResult = newsRepository.searchArticles("Indonesia")
    
    when (searchResult) {
        is Resource.Success -> {
            val articles = searchResult.data
            println("Found ${articles.size} results for 'Indonesia'")
        }
        is Resource.Error -> {
            println("Search failed: ${searchResult.message}")
        }
        is Resource.Loading -> {
            // Won't happen karena safeApiCall langsung return result
        }
    }
}
```

### Advanced Search:
```kotlin
lifecycleScope.launch {
    // Multiple searches
    val queries = listOf("teknologi", "olahraga", "bisnis")
    
    queries.forEach { query ->
        val result = newsRepository.searchArticles(query)
        if (result is Resource.Success) {
            println("$query: ${result.data.size} articles")
        }
    }
}
```

---

## 4️⃣ Handling Errors Gracefully

```kotlin
suspend fun loadNewsWithFallback(category: String) {
    // Try network first
    val networkResult = newsRepository.fetchArticlesFromNetwork(category)
    
    if (networkResult is Resource.Success) {
        // Network success - use fresh data
        updateUI(networkResult.data)
    } else {
        // Network failed - fallback to local data
        Log.w("News", "Network failed, using local data")
        val localArticles = newsRepository.getArticles()
            .filter { it.category.equals(category, ignoreCase = true) }
        updateUI(localArticles)
    }
}

private fun updateUI(articles: List<NewsArticle>) {
    // Update your UI here
}
```

---

## 5️⃣ Kombinasi Network + Local Cache

```kotlin
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {
    
    private val _articles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val articles: StateFlow<List<NewsArticle>> = _articles.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun loadArticles(category: String? = null, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            if (!forceRefresh) {
                // Load local data first (instant)
                val localArticles = newsRepository.getArticles()
                if (category != null) {
                    _articles.value = localArticles.filter { 
                        it.category.equals(category, ignoreCase = true) 
                    }
                } else {
                    _articles.value = localArticles
                }
            }
            
            // Then fetch from network (fresh data)
            val networkResult = newsRepository.fetchArticlesFromNetwork(category)
            
            when (networkResult) {
                is Resource.Success -> {
                    _articles.value = networkResult.data
                    _error.value = null
                }
                is Resource.Error -> {
                    _error.value = networkResult.message
                    // Keep local data if network fails
                }
                is Resource.Loading -> {
                    // Already handled above
                }
            }
            
            _isLoading.value = false
        }
    }
}
```

---

## 6️⃣ Pull to Refresh Implementation

```kotlin
@Composable
fun NewsListScreen(viewModel: NewsViewModel = hiltViewModel()) {
    val articles by viewModel.articles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { 
            viewModel.loadArticles(forceRefresh = true) 
        }
    )
    
    Box(Modifier.pullRefresh(pullRefreshState)) {
        LazyColumn {
            items(articles) { article ->
                ArticleCard(article)
            }
        }
        
        PullRefreshIndicator(
            refreshing = isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Error snackbar
        error?.let { errorMessage ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Text(errorMessage)
            }
        }
    }
}
```

---

## 7️⃣ Testing API Connection

### Simple Test:
```kotlin
// Add to MainActivity for quick testing
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var newsRepository: NewsRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Quick API test
        testApiConnection()
    }
    
    private fun testApiConnection() {
        lifecycleScope.launch {
            Log.d("API_TEST", "Starting API test...")
            
            // Test 1: Fetch top headlines
            val result1 = newsRepository.fetchArticlesFromNetwork()
            logResult("Top Headlines", result1)
            
            // Test 2: Fetch technology news
            val result2 = newsRepository.fetchArticlesFromNetwork("technology")
            logResult("Technology", result2)
            
            // Test 3: Search
            val result3 = newsRepository.searchArticles("Indonesia")
            logResult("Search 'Indonesia'", result3)
            
            Log.d("API_TEST", "API test completed!")
        }
    }
    
    private fun logResult(label: String, result: Resource<List<NewsArticle>>) {
        when (result) {
            is Resource.Success -> {
                Log.d("API_TEST", "✅ $label: ${result.data.size} articles")
                result.data.take(3).forEach {
                    Log.d("API_TEST", "  - ${it.title}")
                }
            }
            is Resource.Error -> {
                Log.e("API_TEST", "❌ $label failed: ${result.message}")
            }
            is Resource.Loading -> {
                Log.d("API_TEST", "⏳ $label loading...")
            }
        }
    }
}
```

---

## 8️⃣ Error Handling Patterns

### Pattern 1: Retry Logic
```kotlin
suspend fun fetchWithRetry(
    maxRetries: Int = 3,
    category: String? = null
): Resource<List<NewsArticle>> {
    repeat(maxRetries) { attempt ->
        val result = newsRepository.fetchArticlesFromNetwork(category)
        
        if (result is Resource.Success) {
            return result
        }
        
        if (attempt < maxRetries - 1) {
            delay(1000 * (attempt + 1)) // Exponential backoff
            Log.d("Retry", "Attempt ${attempt + 2}/$maxRetries")
        }
    }
    
    return Resource.Error("Failed after $maxRetries attempts")
}
```

### Pattern 2: Cache-aside
```kotlin
suspend fun getArticlesWithCache(category: String): List<NewsArticle> {
    // Try network first
    val networkResult = newsRepository.fetchArticlesFromNetwork(category)
    
    return when (networkResult) {
        is Resource.Success -> {
            // Save to cache (Room - akan diimplementasi di step selanjutnya)
            // cacheRepository.saveArticles(networkResult.data)
            networkResult.data
        }
        is Resource.Error -> {
            // Fallback to cache
            // cacheRepository.getArticles(category) ?: fallback to local JSON
            newsRepository.getArticles()
                .filter { it.category.equals(category, ignoreCase = true) }
        }
        is Resource.Loading -> emptyList()
    }
}
```

---

## 9️⃣ Monitoring & Logging

### Network Logs:
```kotlin
// Sudah di-setup di NetworkModule
// Check Logcat dengan filter:
// - "OkHttp" untuk HTTP requests/responses
// - "NewsRepository" untuk repository operations
```

### Custom Logging:
```kotlin
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {
    
    fun loadNewsWithLogging(category: String) {
        viewModelScope.launch {
            Log.d("NewsVM", "Loading $category articles...")
            val startTime = System.currentTimeMillis()
            
            val result = newsRepository.fetchArticlesFromNetwork(category)
            val duration = System.currentTimeMillis() - startTime
            
            when (result) {
                is Resource.Success -> {
                    Log.d("NewsVM", "✅ Loaded ${result.data.size} articles in ${duration}ms")
                }
                is Resource.Error -> {
                    Log.e("NewsVM", "❌ Failed in ${duration}ms: ${result.message}")
                }
                is Resource.Loading -> {}
            }
        }
    }
}
```

---

## 🔟 Best Practices

### ✅ DO:
```kotlin
// 1. Always use Resource wrapper
val result: Resource<List<NewsArticle>> = repository.fetchArticles()

// 2. Handle all cases
when (result) {
    is Resource.Success -> { /* handle */ }
    is Resource.Error -> { /* handle */ }
    is Resource.Loading -> { /* handle */ }
}

// 3. Use coroutines properly
viewModelScope.launch {
    val result = repository.fetchArticles()
    // Process result
}

// 4. Provide user feedback
_isLoading.value = true
val result = repository.fetchArticles()
_isLoading.value = false
```

### ❌ DON'T:
```kotlin
// 1. Jangan ignore errors
val result = repository.fetchArticles()
val data = (result as Resource.Success).data // ❌ Bisa crash!

// 2. Jangan block UI thread
val result = runBlocking { // ❌ UI freeze!
    repository.fetchArticles()
}

// 3. Jangan hardcode API key
const val API_KEY = "abc123" // ❌ Akan ter-push ke GitHub!

// 4. Jangan spam API
repeat(1000) { // ❌ Rate limit!
    repository.fetchArticles()
}
```

---

## 📱 Integration Examples

Contoh file `HomeScreenExample.kt` (preview untuk step 4):

```kotlin
@Composable
fun HomeScreenExample(
    viewModel: HomeViewModel = hiltViewModel() // Hilt inject
) {
    val articles by viewModel.articles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadArticles()
    }
    
    Scaffold(
        topBar = { /* TopBar */ }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                isLoading && articles.isEmpty() -> {
                    CircularProgressIndicator(
                        Modifier.align(Alignment.Center)
                    )
                }
                error != null && articles.isEmpty() -> {
                    ErrorView(
                        message = error!!,
                        onRetry = { viewModel.loadArticles(forceRefresh = true) }
                    )
                }
                else -> {
                    LazyColumn {
                        items(articles) { article ->
                            ArticleCard(article)
                        }
                    }
                }
            }
        }
    }
}
```

---

## 🎓 Learning Path

1. **Start Simple**: Test API dengan MainActivity
2. **Add ViewModel**: Move logic ke ViewModel (step 4)
3. **Add Caching**: Integrate Room (step 5)
4. **Add Firebase**: Cloud sync (step 6)
5. **Polish**: Error handling, retry logic, offline mode

---

**Next**: Implement ViewModels di Step 4 untuk production-ready code! 🚀
