package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

data class GuruAiTokenWallet(
  val guruId: String = "",
  val balanceTokens: Int = 0,
  val totalPurchasedTokens: Int = 0,
  val totalUsedTokens: Int = 0
)

data class GuruAiGenerateRequest(
  val feature: String,
  val distribusiId: String,
  val subjectTitle: String,
  val className: String,
  val semester: String,
  val languageCode: String,
  val prompt: String,
  val count: Int,
  val questionType: String = "",
  val existingItems: List<String> = emptyList(),
  val academicYear: String = ""
)

data class GuruAiMaterialItem(
  val title: String,
  val body: String,
  val bulletPoints: List<String>
)

data class GuruAiMaterialResult(
  val title: String,
  val summary: String,
  val items: List<GuruAiMaterialItem>
)

data class GuruAiQuestionItem(
  val prompt: String,
  val options: List<String>,
  val answer: String
)

data class GuruAiQuestionResult(
  val title: String,
  val instruction: String,
  val languageCode: String,
  val questionType: String,
  val questions: List<GuruAiQuestionItem>
)

sealed interface GuruAiGenerateResult {
  data class MaterialSuccess(
    val material: GuruAiMaterialResult,
    val wallet: GuruAiTokenWallet,
    val chargedTokens: Int
  ) : GuruAiGenerateResult

  data class QuestionSuccess(
    val question: GuruAiQuestionResult,
    val wallet: GuruAiTokenWallet,
    val chargedTokens: Int
  ) : GuruAiGenerateResult

  data class Error(
    val message: String,
    val wallet: GuruAiTokenWallet? = null
  ) : GuruAiGenerateResult
}

class GuruAiRemoteDataSource {
  suspend fun fetchTokenBalance(
    guruId: String
  ): GuruAiTokenWallet? = withContext(Dispatchers.IO) {
    if (guruId.isBlank()) return@withContext null
    runCatching {
      val payload = JSONObject().apply {
        put("action", "balance")
        put("guru_id", guruId)
      }
      val response = postFunction(payload)
      if (!response.optBoolean("ok", false)) return@runCatching null
      response.optJSONObject("wallet")?.toWallet()
    }.getOrNull()
  }

  suspend fun generateContent(
    guruId: String,
    guruName: String,
    request: GuruAiGenerateRequest
  ): GuruAiGenerateResult = withContext(Dispatchers.IO) {
    if (guruId.isBlank()) {
      return@withContext GuruAiGenerateResult.Error("Data guru belum lengkap.")
    }
    if (request.prompt.isBlank()) {
      return@withContext GuruAiGenerateResult.Error("Instruksi AI wajib diisi.")
    }

    return@withContext try {
      val payload = JSONObject().apply {
        put("action", "generate")
        put("guru_id", guruId)
        put("guru_nama", guruName)
        put("distribusi_id", request.distribusiId)
        put("feature", request.feature)
        put("subject_title", request.subjectTitle)
        put("class_name", request.className)
        put("semester", request.semester)
        put("language", request.languageCode)
        put("prompt", request.prompt)
        put("count", request.count)
        put("question_type", request.questionType)
        put("academic_year", request.academicYear)
        put("existing_items", JSONArray().apply {
          request.existingItems.forEach { item ->
            if (item.isNotBlank()) put(item)
          }
        })
      }
      val response = postFunction(payload)
      val wallet = response.optJSONObject("wallet")?.toWallet()
      if (!response.optBoolean("ok", false)) {
        return@withContext GuruAiGenerateResult.Error(
          message = response.optString("error").ifBlank { "Gagal membuat konten AI." },
          wallet = wallet
        )
      }

      val chargedTokens = response.optJSONObject("usage")?.optInt("charged_tokens", 0) ?: 0
      val result = response.optJSONObject("result") ?: JSONObject()
      when (request.feature.lowercase()) {
        "soal" -> GuruAiGenerateResult.QuestionSuccess(
          question = result.toQuestionResult(),
          wallet = wallet ?: GuruAiTokenWallet(guruId = guruId),
          chargedTokens = chargedTokens
        )
        else -> GuruAiGenerateResult.MaterialSuccess(
          material = result.toMaterialResult(),
          wallet = wallet ?: GuruAiTokenWallet(guruId = guruId),
          chargedTokens = chargedTokens
        )
      }
    } catch (_: SocketTimeoutException) {
      GuruAiGenerateResult.Error("Koneksi AI terlalu lama. Coba lagi.")
    } catch (error: Exception) {
      GuruAiGenerateResult.Error(error.message ?: "Gagal menghubungi AI.")
    }
  }

  private fun postFunction(payload: JSONObject): JSONObject {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/functions/v1/generate-ai-content"
    val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      connectTimeout = 20_000
      readTimeout = 90_000
      doOutput = true
      setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
      setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
      setRequestProperty("Content-Type", "application/json")
      setRequestProperty("Accept", "application/json")
      setRequestProperty("Accept-Charset", "UTF-8")
    }

    return try {
      connection.outputStream.use { stream ->
        stream.write(payload.toString().toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      val code = connection.responseCode
      val body = connection.readPayload(code in 200..299)
      val json = runCatching { JSONObject(body.ifBlank { "{}" }) }.getOrElse {
        JSONObject().put("ok", false).put("error", body.ifBlank { "HTTP $code" })
      }
      if (code !in 200..299 && !json.has("error")) {
        json.put("error", "HTTP $code")
      }
      json
    } finally {
      connection.disconnect()
    }
  }
}

private fun JSONObject.toWallet(): GuruAiTokenWallet {
  return GuruAiTokenWallet(
    guruId = optString("guru_id").trim(),
    balanceTokens = optInt("balance_tokens", 0),
    totalPurchasedTokens = optInt("total_purchased_tokens", 0),
    totalUsedTokens = optInt("total_used_tokens", 0)
  )
}

private fun JSONObject.toMaterialResult(): GuruAiMaterialResult {
  val itemsArray = optJSONArray("items") ?: JSONArray()
  val items = buildList {
    for (index in 0 until itemsArray.length()) {
      val row = itemsArray.optJSONObject(index) ?: continue
      add(
        GuruAiMaterialItem(
          title = row.optString("title").trim(),
          body = row.optString("body").trim(),
          bulletPoints = row.optJSONArray("bullet_points").toStringList()
        )
      )
    }
  }.filter { it.title.isNotBlank() || it.body.isNotBlank() }
  return GuruAiMaterialResult(
    title = optString("title").trim(),
    summary = optString("summary").trim(),
    items = items
  )
}

private fun JSONObject.toQuestionResult(): GuruAiQuestionResult {
  val questionsArray = optJSONArray("questions") ?: JSONArray()
  val questions = buildList {
    for (index in 0 until questionsArray.length()) {
      val row = questionsArray.optJSONObject(index) ?: continue
      add(
        GuruAiQuestionItem(
          prompt = row.optString("prompt").trim(),
          options = row.optJSONArray("options").toStringList(),
          answer = row.optString("answer").trim()
        )
      )
    }
  }.filter { it.prompt.isNotBlank() }
  return GuruAiQuestionResult(
    title = optString("title").trim(),
    instruction = optString("instruction").trim(),
    languageCode = optString("language").trim().ifBlank { "ID" },
    questionType = optString("question_type").trim(),
    questions = questions
  )
}

private fun JSONArray?.toStringList(): List<String> {
  if (this == null) return emptyList()
  return buildList {
    for (index in 0 until length()) {
      val value = optString(index).trim()
      if (value.isNotBlank()) add(value)
    }
  }
}

private fun HttpURLConnection.readPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}
