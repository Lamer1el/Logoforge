package com.logoforge.app.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object PollinationsApi {
    const val BASE = "https://gen.pollinations.ai"
    const val API_KEY = "sk_dYYiB25GJyFfZNQx9WnLKopbLHMUDfPn"
    const val TEXT_MODEL = "openai/gpt-5.4-nano"
    const val IMAGE_MODEL = "black-forest-labs/flux.1-schnell"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    class ApiException(msg: String) : Exception(msg)

    /** Logotipen: превращает описание пользователя в промпт для генерации логотипа. */
    suspend fun enhancePrompt(userDescription: String): String = withContext(Dispatchers.IO) {
        val system = """
            Ты — Logotipen, ИИ-дизайнер логотипов. Пользователь описывает логотип или бренд.
            Составь короткий (до 60 слов) промпт НА АНГЛИЙСКОМ для генерации изображения логотипа.
            Промпт должен описывать: плоский минималистичный векторный логотип (flat vector logo),
            стиль, форму, палитру, фон. Не используй кавычки и markdown. Только сам промпт.
        """.trimIndent()
        val body = JSONObject().apply {
            put("model", TEXT_MODEL)
            put("store", false)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", system))
                put(JSONObject().put("role", "user").put("content", userDescription))
            })
        }
        val req = Request.Builder()
            .url("$BASE/v1/chat/completions")
            .header("Authorization", "Bearer $API_KEY")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw ApiException("HTTP ${resp.code}")
            val text = resp.body?.string() ?: throw ApiException("empty body")
            val content = JSONObject(text)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
            content.trim().removeSurrounding("\"")
        }
    }

    /** Генерация логотипа по промпту. Возвращает bitmap. */
    suspend fun generateLogo(prompt: String, width: Int = 1024, height: Int = 1024): Bitmap =
        withContext(Dispatchers.IO) {
            val url = "$BASE/image/${java.net.URLEncoder.encode(prompt, "UTF-8")}" +
                "?model=$IMAGE_MODEL&width=$width&height=$height&nologo=true"
            val req = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $API_KEY")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw ApiException("HTTP ${resp.code}")
                val bytes = resp.body?.bytes() ?: throw ApiException("empty body")
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw ApiException("Не удалось декодировать изображение")
            }
        }

    /** Сохраняет bitmap в файл и возвращает путь. */
    suspend fun saveBitmap(bmp: Bitmap, dir: File): String = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val f = File(dir, "logo_${System.currentTimeMillis()}.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        f.absolutePath
    }
}
