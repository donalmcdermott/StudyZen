package com.flx_apps.digitaldetox.examgate

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

data class ExamQuestion(
    val prompt: String,
    val choices: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val topic: String,
)

object ExamGateStore {
    private const val PREFS = "exam_gate"
    private const val QUESTION_BANK = "question_bank_json"
    private const val UNLOCK_PREFIX = "unlock_until_"
    /** Default unlock duration, also referenced by the overlay unlock-hint string. */
    const val DEFAULT_UNLOCK_MINUTES = 10L

    private val fallbackQuestion = ExamQuestion(
        prompt = "Which organelle is the main site of aerobic respiration?",
        choices = listOf("Nucleus", "Mitochondrion", "Ribosome", "Golgi apparatus"),
        correctIndex = 1,
        explanation = "Aerobic respiration primarily occurs in mitochondria.",
        topic = "Biology",
    )

    fun importQuestionBank(context: Context, json: String): Int {
        val questions = parseQuestions(json)
        require(questions.isNotEmpty()) { "Question bank is empty" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(QUESTION_BANK, json)
            .apply()
        return questions.size
    }

    fun questionCount(context: Context): Int = loadQuestions(context).size

    fun nextQuestion(context: Context): ExamQuestion {
        val questions = loadQuestions(context)
        if (questions.isEmpty()) return fallbackQuestion
        return questions[Random.nextInt(questions.size)]
    }

    fun isUnlocked(context: Context, packageName: String): Boolean {
        if (packageName.isBlank()) return false
        val until = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(UNLOCK_PREFIX + packageName, 0L)
        return System.currentTimeMillis() < until
    }

    fun grantUnlock(
        context: Context,
        packageName: String,
        minutes: Long = DEFAULT_UNLOCK_MINUTES,
    ) {
        if (packageName.isBlank()) return
        val until = System.currentTimeMillis() + minutes * 60_000L
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(UNLOCK_PREFIX + packageName, until)
            .apply()
    }

    private fun loadQuestions(context: Context): List<ExamQuestion> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val imported = prefs.getString(QUESTION_BANK, null)
        val json = imported ?: context.assets.open("exam_questions.json").bufferedReader().use { it.readText() }
        return parseQuestions(json).ifEmpty { listOf(fallbackQuestion) }
    }

    private fun parseQuestions(json: String): List<ExamQuestion> {
        val root = JSONObject(json)
        val array = root.optJSONArray("questions") ?: JSONArray()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val choicesJson = item.getJSONArray("choices")
                val choices = buildList {
                    for (j in 0 until choicesJson.length()) add(choicesJson.getString(j))
                }
                val correctIndex = item.getInt("correctIndex")
                if (choices.size < 2 || correctIndex !in choices.indices) continue
                add(
                    ExamQuestion(
                        prompt = item.getString("prompt"),
                        choices = choices,
                        correctIndex = correctIndex,
                        explanation = item.optString("explanation", ""),
                        topic = item.optString("topic", "General"),
                    )
                )
            }
        }
    }
}
