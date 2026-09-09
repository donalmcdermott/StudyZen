package com.flx_apps.digitaldetox

import com.flx_apps.digitaldetox.examgate.ExamGateStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Verifies the ExamGate question-bank store: importing, counting, validated parsing, and the
 * per-app timed unlock. Guards the exam-gate overlay flow against regressions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExamGateStoreTest {

    private val context get() = RuntimeEnvironment.getApplication()

    private fun bankJson(vararg prompts: String): String {
        val questions = prompts.mapIndexed { index, prompt ->
            """
            {"topic":"T$index","prompt":"$prompt",
             "choices":["A","B"],"correctIndex":0,"explanation":""}
            """.trimIndent()
        }.joinToString(",")
        return """{"questions":[$questions]}"""
    }

    @Test
    fun `import then count reflects the imported bank`() {
        assertEquals(3, ExamGateStore.importQuestionBank(context, bankJson("Q1", "Q2", "Q3")))
        assertEquals(3, ExamGateStore.questionCount(context))
    }

    @Test
    fun `import replaces the previous bank`() {
        ExamGateStore.importQuestionBank(context, bankJson("Q1", "Q2"))
        ExamGateStore.importQuestionBank(context, bankJson("Q1"))
        assertEquals(1, ExamGateStore.questionCount(context))
    }

    @Test
    fun `import rejects an empty bank and keeps the previous one`() {
        ExamGateStore.importQuestionBank(context, bankJson("Q1", "Q2"))
        try {
            ExamGateStore.importQuestionBank(context, """{"questions":[]}""")
            throw AssertionError("expected require() to fail on an empty bank")
        } catch (expected: IllegalArgumentException) {
            // the failed import must not zero out the stored bank
            assertEquals(2, ExamGateStore.questionCount(context))
        }
    }

    @Test
    fun `import rejects malformed json and keeps the previous bank`() {
        ExamGateStore.importQuestionBank(context, bankJson("Q1"))
        try {
            ExamGateStore.importQuestionBank(context, "not json")
            throw AssertionError("expected malformed JSON to fail")
        } catch (expected: org.json.JSONException) {
            assertEquals(1, ExamGateStore.questionCount(context))
        }
    }

    @Test
    fun `malformed entries are skipped, valid ones still load`() {
        // first entry: invalid correctIndex; second entry: only one choice
        val json = """
            {"questions":[
              {"topic":"T","prompt":"Bad index","choices":["A","B"],"correctIndex":5,"explanation":""},
              {"topic":"T","prompt":"One choice","choices":["A"],"correctIndex":0,"explanation":""},
              {"topic":"T","prompt":"Good","choices":["A","B"],"correctIndex":1,"explanation":"E"}
            ]}
        """.trimIndent()
        assertEquals(1, ExamGateStore.importQuestionBank(context, json))
        val question = ExamGateStore.nextQuestion(context)
        assertEquals("Good", question.prompt)
        assertEquals(listOf("A", "B"), question.choices)
        assertEquals(1, question.correctIndex)
        assertEquals("E", question.explanation)
    }

    @Test
    fun `grant then isUnlocked gates per app package`() {
        ExamGateStore.grantUnlock(context, "com.example.app", minutes = 10)
        assertTrue(ExamGateStore.isUnlocked(context, "com.example.app"))
        assertFalse(ExamGateStore.isUnlocked(context, "com.other.app"))
        assertFalse(ExamGateStore.isUnlocked(context, ""))
    }
}
