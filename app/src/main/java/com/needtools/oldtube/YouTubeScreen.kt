package com.needtools.oldtube

import android.content.Context
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.core.content.edit
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit

// Модель даних для історії
data class HistoryItem(
    val videoId: String,
    val title: String,
    val url: String
)
const val EXCLUDED_VIDEO_ID = "mRLK9z5UMcE"
const val TAG = "YouTubeDebug"
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun YouTubeScreen(videoId: String) {

    val initialUrl = "https://m.youtube.com/watch?v=$videoId&autoplay=1"

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("oldtube_prefs", Context.MODE_PRIVATE) }
// Список історії тепер ініціалізується порожнім, але відразу завантажується
    var watchHistory by remember { mutableStateOf(listOf<HistoryItem>()) }
    // ЗАВАНТАЖЕННЯ при старті
    LaunchedEffect(Unit) {
        val savedData = sharedPrefs.getString("history_data", "")
        if (!savedData.isNullOrBlank()) {
            val list = savedData.split("|||").mapNotNull {
                val parts = it.split("|")
                if (parts.size == 3) HistoryItem(parts[0], parts[1], parts[2]) else null
            }
            watchHistory = list
        }
    }

// ЗБЕРЕЖЕННЯ при зміні списку
    LaunchedEffect(watchHistory) {
        // Ми прибрали умову isNotEmpty, щоб видалення (порожній список) теж зберігалося
        val dataToSave = watchHistory.joinToString("|||") { "${it.videoId}|${it.title}|${it.url}" }

        sharedPrefs.edit {
            putString("history_data", dataToSave)
            // Використовуємо commit() замість apply() для миттєвого запису,
            // якщо нам потрібна гарантія збереження перед закриттям
            commit()
        }

        Log.d(TAG, "--- ПЕРЕВІРКА ЗБЕРЕЖЕННЯ ---")
        Log.d(TAG, "Кількість елементів: ${watchHistory.size}")
        if (watchHistory.isNotEmpty()) {
            Log.d(TAG, "Останній доданий: ${watchHistory.first().title}")
        }
        Log.d(TAG, "Збережений рядок: $dataToSave")
    }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var canGoBackState by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = canGoBackState || sheetState.isVisible) {
        if (sheetState.isVisible) {
            scope.launch { sheetState.hide() }
        } else {
            webViewInstance?.goBack()
        }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = MaterialTheme.shapes.large,
        sheetContent = {
            HistorySheetContent(
                history = watchHistory,
                onDeleteItem = { idToDelete ->
                    // Саме ТУТ відбувається видалення
                    watchHistory = watchHistory.filter { it.videoId != idToDelete }
                    Log.d(TAG, "Відео видалено: $idToDelete")
                },
                onItemSelected = { selectedUrl ->
                    val cleanUrl = selectedUrl.substringBefore("&t=")
                    val finalUrl = if (cleanUrl.contains("?")) "$cleanUrl&autoplay=1" else "$cleanUrl?autoplay=1"
                    webViewInstance?.stopLoading()
                    webViewInstance?.loadUrl(finalUrl)
                    scope.launch { sheetState.hide() }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webViewInstance = this
                        this.requestFocus()

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            allowFileAccess = true
                            userAgentString = "Mozilla/5.0 (Linux; Android 5.0; SM-G900F Build/LRX21P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36"
                        }

                        webViewClient = object : WebViewClient() {

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                canGoBackState = view?.canGoBack() ?: false

                                // 1. Ховаємо кнопку
                                view?.evaluateJavascript(
                                    "(function() { " +
                                            "   var style = document.createElement('style');" +
                                            "   style.innerHTML = '.ytp-large-play-button { display: none !important; }';" +
                                            "   document.head.appendChild(style);" +
                                            "})();", null
                                )

                                // 2. Скрипт автозапуску (спроби протягом декількох секунд)
                                val playScript = """
                                    (function() {
                                        var count = 0;
                                        function start() {
                                            var v = document.querySelector('video');
                                            var b = document.querySelector('.ytp-large-play-button');
                                            if (v) { v.play(); v.muted = false; }
                                            if (b) { b.click(); }
                                            if (count < 5) { count++; setTimeout(start, 1000); }
                                        }
                                        start();
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(playScript, null)

                                url?.let { currentUrl ->
                                    addToHistory(
                                        currentUrl,
                                        view?.title,
                                        watchHistory,
                                        context
                                    ) { newList ->
                                        watchHistory = newList
                                    }
                                }
                            }
                        }
                        // 2. ДОДАЙТЕ ЦЕЙ БЛОК: Клієнт для отримання заголовка (Title)
                        webChromeClient = object : WebChromeClient() {

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                // Як тільки YouTube віддасть назву відео — додаємо в історію
                                view?.url?.let { currentUrl ->
                                    Log.d(TAG, "Отримано заголовок через ChromeClient: $title")
                                    addToHistory(currentUrl, title, watchHistory, context) { newList ->
                                        watchHistory = newList
                                    }
                                }
                            }
                        }
                        loadUrl(initialUrl)
                    }
                }
            )

            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 16.dp),
                backgroundColor = Color.Red,
                onClick = { scope.launch { sheetState.show() } }
            ) {
                Icon(Icons.Default.List, contentDescription = "History", tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistorySheetContent(
    history: List<HistoryItem>,           // 1. Список відео
    onDeleteItem: (String) -> Unit,       // 2. Дія: видалити (приймає ID)
    onItemSelected: (String) -> Unit      // 3. Дія: відкрити (приймає URL)
) {
    Surface(
        color = Color.White,
        contentColor = Color.Black,
        modifier = Modifier.fillMaxHeight(0.7f).fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // БЛОК АВАТАРА ТА ЗАГОЛОВКА
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                // Малюємо кругле зображення (аватар)
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = Color.Red.copy(alpha = 0.1f) // Світло-червоний фон
                ) {
                    // Використовуємо стандартну іконку персони
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = stringResource(R.string.desc_history_button), // Опис для Google TalkBack
                        tint = Color.Red,
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = stringResource(R.string.history_title), // "Мій OldTube. Історія переглядів."
                        style = MaterialTheme.typography.h6
                    )
                    Text(
                        text = stringResource(R.string.history_count, history.size),
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray
                    )
                    Text(
                        text = stringResource(R.string.history_hint), // "Клікнути — запустити, натиснути — видалити"
                        style = MaterialTheme.typography.overline, // Дуже маленький шрифт
                        color = Color.Red.copy(alpha = 0.6f), // Трішки виділимо кольором
                        lineHeight = TextUnit.Unspecified
                    )
                }
            }

            // Розділювач
            Divider(modifier = Modifier.padding(bottom = 12.dp))

            if (history.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.history_empty), color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(history,
                        key = { it.videoId }
                        ) { item ->
                        HistoryItemRow(
                            item = item,
                            onItemSelected = onItemSelected,
                            onItemLongClick = { onDeleteItem(item.videoId) } // Передаємо ID для видалення
                        )
                    }
                }

            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItemRow(
    item: HistoryItem,
    onItemSelected: (String) -> Unit,
    onItemLongClick: () -> Unit // Додаємо цей параметр
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onItemSelected(item.url) },
                onLongClick = { onItemLongClick() } // Видаляємо без запитань
            )
//            .clickable { onItemSelected(item.url) }
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "https://img.youtube.com/vi/${item.videoId}/mqdefault.jpg",
            contentDescription = null,
            modifier = Modifier.size(100.dp, 56.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = item.title,
            maxLines = 2,
            style = MaterialTheme.typography.body2
        )
    }
}


fun addToHistory(
    url: String,
    title: String?,
    currentHistory: List<HistoryItem>,
    context: Context,
    onUpdate: (List<HistoryItem>) -> Unit
) {
    // 1. Перевіряємо, чи це сторінка відео
    if (url.contains("watch?v=")) {
        val videoId = url.substringAfter("v=").substringBefore("&")

        // 2. Ігноруємо стартове відео
        if (videoId == EXCLUDED_VIDEO_ID) {
            return
        }

        // 3. Перевіряємо назву
        val isInvalid = title.isNullOrBlank() ||
                title == "YouTube" ||
                title == "m.youtube.com" ||
                title.contains("https://")

        val cleanTitle = if (isInvalid) {
            currentHistory.find { it.videoId == videoId }?.title ?: context.getString(R.string.video_loading)
        } else {
            title.replace("- YouTube", "").trim()
        }

        // 4. Створюємо новий елемент
        val newItem = HistoryItem(videoId, cleanTitle, url)

        // 5. Оновлюємо список
        val updatedList = (listOf(newItem) + currentHistory.filter { it.videoId != videoId }).take(100)

        // ЛОГ ДЛЯ ДІАГНОСТИКИ
        Log.d(TAG, "Спроба додати: ID=$videoId, Title=$cleanTitle")

        // Оновлюємо стан, тільки якщо список дійсно змінився
        if (currentHistory.firstOrNull()?.videoId != videoId || currentHistory.firstOrNull()?.title != cleanTitle) {
            onUpdate(updatedList)
        }

    }
}

