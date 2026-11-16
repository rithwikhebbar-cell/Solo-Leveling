package com.example

import com.example.domain.engine.CommunicationPracticeEngine
import com.example.domain.engine.SocialPracticeCatalog
import com.example.domain.engine.SpeechEvaluationEngine
import com.example.domain.model.PracticeCategory
import com.example.domain.model.PracticeDifficulty
import com.example.domain.model.SocialActivityKind
import com.example.domain.model.SocialEvidenceSource
import com.example.domain.model.SocialModuleIdentifier
import com.example.domain.model.SocialSessionStatus
import com.example.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Milestone 12 - Phase 2: Communication Practice & Speech Evaluation Engine Unit Tests.
 */
class SocialPracticeEngineUnitTest {

    @Test
    fun testExerciseCatalogCompleteness() {
        val exercises = SocialPracticeCatalog.exercises
        assertTrue("Exercises catalogue must not be empty", exercises.isNotEmpty())
        assertTrue("Must have at least 8 structured exercises across modules", exercises.size >= 8)

        // Verify all 8 modules have at least one practice exercise
        val coveredModules = exercises.map { it.module }.toSet()
        assertEquals(
            "All 8 Social Modules must be covered in practice catalog",
            SocialModuleIdentifier.entries.size,
            coveredModules.size
        )
    }

    @Test
    fun testConversationScenariosCatalog() {
        val scenarios = SocialPracticeCatalog.conversationScenarios
        assertTrue("Conversation scenarios catalogue must not be empty", scenarios.isNotEmpty())
        for (scenario in scenarios) {
            assertTrue("Scenario must have positive target rounds", scenario.targetRounds > 0)
            assertTrue("Dialogue script must match or exceed target rounds", scenario.dialogueScript.size >= scenario.targetRounds)
            assertNotNull(scenario.openingDialogue)
        }
    }

    @Test
    fun testListeningExercisesCatalog() {
        val listEx = SocialPracticeCatalog.listeningExercises
        assertTrue("Listening exercises catalogue must not be empty", listEx.isNotEmpty())
        for (ex in listEx) {
            assertTrue("Passage must not be blank", ex.spokenOrWrittenPassage.isNotBlank())
            assertTrue("Key facts must not be empty", ex.keyFactsToIdentify.isNotEmpty())
        }
    }

    @Test
    fun testDeterministicTextEvaluation_SufficientResponse() {
        val exercise = SocialPracticeCatalog.exercises.first { it.exerciseId == "EX_COMM_01" }
        val goodResponse = "Point: Remote work requires deliberate communication habits to stay aligned. " +
                "Reason: Without hallway conversations, project blockers remain invisible unless explicitly surfaced. " +
                "Example: In our sprint yesterday, an API blocker was solved immediately because the developer posted an asynchronous standup update. " +
                "Point: Structured habits prevent miscommunication and drive team velocity."

        val eval = SpeechEvaluationEngine.evaluatePracticeTextResponse(
            exercise = exercise,
            userResponseText = goodResponse,
            durationSeconds = 90,
            hasAudioRecording = false
        )

        assertTrue("High-quality PREP response must pass", eval.isPassed)
        assertTrue("Score should meet or exceed minPassingScore (${exercise.minPassingScore})", eval.overallScore >= exercise.minPassingScore)
        assertTrue("Clarity dimension score should be >= 75", (eval.dimensionScores["Clarity"] ?: 0) >= 75)
        assertEquals(90, eval.measurableMetrics.durationSeconds)
        assertTrue(eval.measurableMetrics.wordCount > 30)
    }

    @Test
    fun testDeterministicTextEvaluation_TooShortFailsGracefully() {
        val exercise = SocialPracticeCatalog.exercises.first()
        val shortResponse = "I like standups."

        val eval = SpeechEvaluationEngine.evaluatePracticeTextResponse(
            exercise = exercise,
            userResponseText = shortResponse,
            durationSeconds = 10,
            hasAudioRecording = false
        )

        assertFalse("Too brief response must not pass", eval.isPassed)
        assertEquals(30, eval.overallScore)
        assertTrue("Improvement areas must guide user to add length", eval.improvementAreas.any { it.contains("too brief", ignoreCase = true) })
    }

    @Test
    fun testListeningComprehensionEvaluation() {
        val listEx = SocialPracticeCatalog.listeningExercises.first { it.exerciseId == "LIST_01" }
        val userSummary = "The hard deadline is Thursday 5 PM. Dave is blocked waiting on credentials from DevOps because of 401 token refresh errors. QA needs the build by Wednesday noon."
        val followUp = "Who is unblocking Dave with DevOps before Wednesday morning?"

        val eval = SpeechEvaluationEngine.evaluateListeningResponse(
            exercise = listEx,
            userSummaryText = userSummary,
            identifiedQuestionsText = followUp
        )

        assertTrue("Listening comprehension containing all key facts must pass", eval.isPassed)
        assertTrue("Score must be >= 70", eval.overallScore >= 70)
        assertTrue(eval.strengths.isNotEmpty())
    }

    @Test
    fun testMeasurableSpeechMetrics() {
        val text = "One two three four five six seven eight nine ten."
        val eval = SpeechEvaluationEngine.evaluatePracticeTextResponse(
            exercise = SocialPracticeCatalog.exercises.first(),
            userResponseText = text,
            durationSeconds = 30,
            hasAudioRecording = true
        )

        assertEquals(30, eval.measurableMetrics.durationSeconds)
        assertEquals(10, eval.measurableMetrics.wordCount)
        assertTrue(eval.measurableMetrics.audioRecordingAvailable)
        assertTrue(eval.measurableMetrics.transcriptAvailable)
    }
}
