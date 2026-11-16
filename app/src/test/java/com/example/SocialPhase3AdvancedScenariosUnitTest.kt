package com.example

import com.example.domain.engine.AdvancedScenarioLibrary
import com.example.domain.model.AdvancedScenarioCategory
import com.example.domain.model.SocialModuleIdentifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Milestone 12 - Phase 3: Advanced Scenarios, Interview Bank & Leadership Decision Unit Tests.
 */
class SocialPhase3AdvancedScenariosUnitTest {

    @Test
    fun testAdvancedScenariosCompleteness() {
        val scenarios = AdvancedScenarioLibrary.scenarios
        assertTrue("Advanced scenarios library must contain scenarios", scenarios.isNotEmpty())

        val categories = scenarios.map { it.category }.toSet()
        assertTrue("Must cover academic life", categories.contains(AdvancedScenarioCategory.COLLEGE_ACADEMIC))
        assertTrue("Must cover professional networking", categories.contains(AdvancedScenarioCategory.PROFESSIONAL_NETWORKING))
        assertTrue("Must cover professional communication", categories.contains(AdvancedScenarioCategory.PROFESSIONAL_COMMUNICATION))
        assertTrue("Must cover negotiation simulator", categories.contains(AdvancedScenarioCategory.NEGOTIATION_SIMULATOR))

        for (scenario in scenarios) {
            assertTrue("Scenario must have a title", scenario.title.isNotBlank())
            assertTrue("Scenario must specify learning objective", scenario.learningObjective.isNotBlank())
            assertTrue("Scenario must contain dialogue turns", scenario.dialogueScript.isNotEmpty())
        }
    }

    @Test
    fun testInterviewQuestionBank() {
        val questions = AdvancedScenarioLibrary.interviewQuestions
        assertTrue("Interview bank must contain questions", questions.isNotEmpty())
        assertTrue("Must have at least 3 distinct interview questions", questions.size >= 3)

        val types = questions.map { it.interviewType }.toSet()
        assertTrue("Must cover behavioral interviews", types.contains("BEHAVIORAL"))
        assertTrue("Must cover HR interviews", types.contains("HR"))
        assertTrue("Must cover technical interviews", types.contains("TECHNICAL"))

        for (q in questions) {
            assertTrue("Question must have STAR framework focus", q.starFrameworkFocus.isNotBlank())
            assertTrue("Target time must be between 60 and 240 seconds", q.targetTimeSeconds in 60..240)
        }
    }

    @Test
    fun testLeadershipDecisionSimulator() {
        val scenarios = AdvancedScenarioLibrary.leadershipScenarios
        assertTrue("Leadership decision scenarios must exist", scenarios.isNotEmpty())

        for (scenario in scenarios) {
            assertTrue("Must contain options to choose from", scenario.options.size >= 2)
            assertTrue("Must list stakeholders", scenario.stakeholders.isNotEmpty())
            assertTrue("Must list constraints", scenario.constraints.isNotEmpty())
            assertNotNull("Must include reflection prompt", scenario.reflectionPrompt)

            // Verify score distribution
            val highestScore = scenario.options.maxOf { it.expectedOutcomeScore }
            val lowestScore = scenario.options.minOf { it.expectedOutcomeScore }
            assertTrue("Highest outcome score must be >= 85", highestScore >= 85)
            assertTrue("Lowest outcome score must reflect consequences (<= 40)", lowestScore <= 40)
        }
    }
}
