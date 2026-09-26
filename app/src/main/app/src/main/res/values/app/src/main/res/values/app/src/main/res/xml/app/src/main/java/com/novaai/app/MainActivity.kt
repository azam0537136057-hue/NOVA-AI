package com.novaai.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val BACKEND_URL = "http://10.0.2.2:3000"
private const val PREFS = "nova_ai"
private const val KEY_USER = "user_name"
private const val KEY_MESSAGES = "messages"
private const val KEY_PREMIUM = "premium"
private const val KEY_USAGE = "usage"
private const val FREE_LIMIT = 10
private const val PREMIUM_LIMIT = 100

private val BgDark = Color(0xFF0F0F1A)
private val CardBg = Color(0xFF1A1A2E)
private val Accent = Color(0xFF7C5CFF)
private val TextPrimary = Color(0xFFF0F0F5)
private val TextSecondary = Color(0xFFAAAAAA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NovaApp(this) }
    }
}

private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

private fun loadMessages(c: Context): MutableList<String> {
    val raw = prefs(c).getString(KEY_MESSAGES, null)
        ?: return mutableListOf("NOVA AI: مرحبًا! أنا NOVA AI 👋")
    val arr = JSONArray(raw)
    return MutableList(arr.length()) { i -> arr.optString(i) }
}

private fun saveMessages(c: Context, messages: List<String>) {
    val arr = JSONArray()
    messages.forEach { arr.put(it) }
    prefs(c).edit().putString(KEY_MESSAGES, arr.toString()).apply()
}

private suspend fun sendMessage(message: String): String = withContext(Dispatchers.IO) {
    val conn = (URL("$BACKEND_URL/api/chat").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 12000
        readTimeout = 60000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    }
    try {
        conn.outputStream.use { it.write(JSONObject().put("message", message).toString().toByteArray()) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw Exception("خطأ من الخادم ($code)")
        JSONObject(response).optString("reply", "لم يصل رد")
    } finally {
        conn.disconnect()
    }
}

@Composable
fun NovaApp(context: Context) {
    val scope = rememberCoroutineScope()
    var userName by remember { mutableStateOf(prefs(context).getString(KEY_USER, "") ?: "") }
    var showAccount by remember { mutableStateOf(userName.isBlank()) }
    var premium by remember { mutableStateOf(prefs(context).getBoolean(KEY_PREMIUM, false)) }
    var usage by remember { mutableIntStateOf(prefs(context).getInt(KEY_USAGE, 0)) }
    var showPlans by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<String>().apply { addAll(loadMessages(context)) } }
    val limit = if (premium) PREMIUM_LIMIT else FREE_LIMIT
    val remaining = (limit - usage).coerceAtLeast(0)

    if (showAccount) {
        Column(
            Modifier.fillMaxSize().background(BgDark).padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("NOVA AI", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Accent)
            Spacer(Modifier.height(8.dp))
            Text("إنشاء حساب محلي", color = TextSecondary)
            Spacer(Modifier.height(24.dp))
            var name by remember { mutableStateOf(userName) }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسمك") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Color(0xFF3A3A55)
                )
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        userName = name.trim()
                        prefs(context).edit().putString(KEY_USER, userName).apply()
                        showAccount = false
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) { Text("متابعة") }
        }
        return
    }

    if (showPlans) {
        Column(Modifier.fillMaxSize().background(BgDark).padding(24.dp)) {
            TextButton(onClick = { showPlans = false }) { Text("← رجوع", color = Accent) }
            Text("خطط NOVA AI", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                Column(Modifier.padding(16.dp)) {
                    Text("مجاني", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("10 استخدامات", color = TextSecondary)
                    if (!premium) Text("✓ الحالية", color = Accent)
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF221A3A))) {
                Column(Modifier.padding(16.dp)) {
                    Text("Premium", color = Accent, fontWeight = FontWeight.Bold)
                    Text("100 استخدام", color = TextSecondary)
                    if (premium) Text("✓ مفعّلة", color = Accent)
                    else Button(
                        onClick = {
                            premium = true
                            prefs(context).edit().putBoolean(KEY_PREMIUM, true).apply()
                            showPlans = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) { Text("تفعيل للتجربة") }
                }
            }
        }
        return
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = BgDark, surface = CardBg)) {
        Scaffold(
            containerColor = BgDark,
            topBar = {
                TopAppBar(
                    title = { Text("NOVA AI", color = TextPrimary, fontWeight = FontWeight.Bold) },
                    actions = {
                        TextButton(onClick = { showPlans = true }) {
                            Text(if (premium) "Premium" else "ترقية", color = Accent)
                        }
                        TextButton(onClick = { showAccount = true }) {
                            Text("حسابي", color = TextSecondary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
                )
            },
            bottomBar = {
                Column(Modifier.fillMaxWidth().background(CardBg).padding(12.dp)) {
                    Text(
                        if (premium) "Premium • $usage / $PREMIUM_LIMIT" else "مجاني • $usage / $FREE_LIMIT",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (usage.toFloat() / limit).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Accent,
                        trackColor = Color(0xFF2A2A40
