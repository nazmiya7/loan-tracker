package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AiAdvisorResult(
    val text: String,
    val isFromGemini: Boolean,
    val note: String? = null
)

object AiAdvisorService {
    private const val TAG = "AiAdvisorService"
    private const val MODEL_NAME = "gemini-1.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun askAdvisor(
        userQuestion: String,
        loanContext: LoanLiveContext,
        conversationHistory: List<ChatMessage>,
        customApiKey: String? = null
    ): AiAdvisorResult = withContext(Dispatchers.IO) {
        val resolvedKey = resolveApiKey(customApiKey)

        // If no valid key is available, use LocalLoanExpertEngine directly
        if (resolvedKey.isNullOrBlank() || resolvedKey == "MY_GEMINI_API_KEY") {
            Log.d(TAG, "No valid Gemini API key configured. Utilizing LocalLoanExpertEngine.")
            val response = LocalLoanExpertEngine.generateExpertResponse(userQuestion, loanContext)
            return@withContext AiAdvisorResult(
                text = response,
                isFromGemini = false,
                note = "Answered by built-in financial math engine. (Configure your Gemini API key in Secrets panel or Settings for full AI chat)"
            )
        }

        try {
            val requestJson = buildRequestBody(userQuestion, loanContext, conversationHistory)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$resolvedKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                    Log.w(TAG, "Gemini API HTTP ${response.code}: $responseBody")
                    val fallback = LocalLoanExpertEngine.generateExpertResponse(userQuestion, loanContext)
                    return@withContext AiAdvisorResult(
                        text = fallback,
                        isFromGemini = false,
                        note = "Gemini API returned status ${response.code}. Served via built-in financial expert engine."
                    )
                }

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "")
                        if (text.isNotBlank()) {
                            return@withContext AiAdvisorResult(
                                text = text,
                                isFromGemini = true,
                                note = "Live Gemini 3.5 Flash Response"
                            )
                        }
                    }
                }

                // If candidates empty or blocked, fallback
                val fallback = LocalLoanExpertEngine.generateExpertResponse(userQuestion, loanContext)
                return@withContext AiAdvisorResult(
                    text = fallback,
                    isFromGemini = false,
                    note = "Served via built-in loan calculation engine."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error contacting Gemini API", e)
            val fallback = LocalLoanExpertEngine.generateExpertResponse(userQuestion, loanContext)
            return@withContext AiAdvisorResult(
                text = fallback,
                isFromGemini = false,
                note = "Network/API unreachable (${e.localizedMessage ?: "error"}). Served via built-in loan calculation engine."
            )
        }
    }

    private fun resolveApiKey(customApiKey: String?): String? {
        if (!customApiKey.isNullOrBlank() && customApiKey != "MY_GEMINI_API_KEY") {
            return customApiKey.trim()
        }
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            null
        }
        if (!buildKey.isNullOrBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey.trim()
        }
        return null
    }

    private fun buildRequestBody(
        userQuestion: String,
        loanContext: LoanLiveContext,
        conversationHistory: List<ChatMessage>
    ): JSONObject {
        val root = JSONObject()

        // System Instruction
        val systemInstruction = JSONObject()
        val sysParts = JSONArray()
        val sysPart = JSONObject()
        sysPart.put(
            "text",
            """
            You are 'LoanIQ Expert', a world-class banking loan officer and financial mathematician specializing in retail loans, home loans, and particularly the SBI daily-reducing balance interest method.
            
            You have access to the user's REAL LIVE LOAN DATA. You MUST reference and utilize their exact live balance, interest rate, daily accrual, and EMI figures in your explanations.
            Format amounts cleanly in Indian Rupees (₹) with Indian number formatting (e.g. ₹9,26,450).
            Always explain clearly why prepayments immediately cut daily interest under the daily-reducing formula: (Principal × Rate / 365).
            Structure your advice with clear headers, bullet points, and exact figures. Be encouraging, transparent, and mathematically rigorous.
            """.trimIndent()
        )
        sysParts.put(sysPart)
        systemInstruction.put("parts", sysParts)
        root.put("systemInstruction", systemInstruction)

        // Contents (conversation history + latest message with context)
        val contentsArray = JSONArray()

        // Include recent history (last 4 messages for conversational continuity)
        val recentHistory = conversationHistory.takeLast(4)
        for (msg in recentHistory) {
            val msgObj = JSONObject()
            msgObj.put("role", if (msg.sender == ChatSender.USER) "user" else "model")
            val parts = JSONArray()
            val part = JSONObject()
            part.put("text", msg.text)
            parts.put(part)
            msgObj.put("parts", parts)
            contentsArray.put(msgObj)
        }

        // Current message with full live loan context
        val contextPrompt = """
        [CURRENT LIVE LOAN DATA]:
        - Outstanding Principal Balance: ₹${String.format(Locale.US, "%.2f", loanContext.currentOutstandingPrincipal)}
        - Annual Interest Rate: ${loanContext.annualRatePercent}% p.a.
        - Daily Interest Rate: ${String.format(Locale.US, "%.6f", loanContext.dailyRatePercent)}% per day (${loanContext.annualRatePercent}% ÷ 365)
        - Today's Daily Interest Accrual: ₹${String.format(Locale.US, "%.2f", loanContext.dailyInterestToday)}/day
        - Scheduled Monthly EMI: ₹${String.format(Locale.US, "%.2f", loanContext.monthlyEmi)} (Due Day: ${loanContext.scheduledDueDay}th of each month)
        - Total Payments Made to Date: ₹${String.format(Locale.US, "%.2f", loanContext.totalPaymentsMade)}
        - Total Interest Charged to Date: ₹${String.format(Locale.US, "%.2f", loanContext.totalInterestCharged)}
        - Principal Reduction Progress: ${String.format(Locale.US, "%.1f", loanContext.principalReductionPercent)}%
        - Estimated Payoff Completion: ${loanContext.estimatedPayoffCompletionDateText} (${loanContext.estimatedPayoffMonths} months / ${loanContext.estimatedPayoffYearsMonthsText})
        - This Month's Accrued/Forecasted Interest: ₹${String.format(Locale.US, "%.2f", loanContext.thisMonthEstimatedInterest)}
        - Recent Account Transactions:
        ${loanContext.recentTransactionsSummary}
        ==================================================
        USER QUESTION:
        $userQuestion
        """.trimIndent()

        val currentMsgObj = JSONObject()
        currentMsgObj.put("role", "user")
        val currentParts = JSONArray()
        val currentPart = JSONObject()
        currentPart.put("text", contextPrompt)
        currentParts.put(currentPart)
        currentMsgObj.put("parts", currentParts)
        contentsArray.put(currentMsgObj)

        root.put("contents", contentsArray)

        // Generation Config
        val genConfig = JSONObject()
        genConfig.put("temperature", 0.7)
        root.put("generationConfig", genConfig)

        return root
    }
}
