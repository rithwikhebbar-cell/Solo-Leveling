package com.example.domain.engine

import com.example.domain.model.AdvancedScenario
import com.example.domain.model.AdvancedScenarioCategory
import com.example.domain.model.ConversationDialogueTurn
import com.example.domain.model.InterviewQuestion
import com.example.domain.model.LeadershipDecisionScenario
import com.example.domain.model.LeadershipOption
import com.example.domain.model.PracticeDifficulty
import com.example.domain.model.SocialModuleIdentifier

/**
 * Milestone 12 - Phase 3: Advanced Scenario & Simulation Library.
 * Features realistic college, networking, workplace, negotiation, interview, and leadership decision trees.
 */
object AdvancedScenarioLibrary {

    val scenarios: List<AdvancedScenario> = listOf(
        // A. College & Academic Life
        AdvancedScenario(
            scenarioId = "ADV_ACAD_01",
            category = AdvancedScenarioCategory.COLLEGE_ACADEMIC,
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            title = "Resolving Group Project Free-Riding",
            description = "Address a classmate who has failed to submit their assigned section 2 days before the presentation.",
            contextPrompt = "Teammate: 'Sorry guys, I was super overwhelmed with other classes and didn't start the slides yet.'",
            npcName = "Jordan",
            npcRole = "Struggling Classmate",
            learningObjective = "Balance empathy with firm team commitments; re-scope remaining work without taking on their entire load.",
            difficulty = PracticeDifficulty.INTERMEDIATE,
            dialogueScript = listOf(
                ConversationDialogueTurn(
                    turnIndex = 1,
                    speakerRole = "NPC",
                    messageText = "Sorry guys, I was super overwhelmed with other classes and didn't start the slides yet.",
                    suggestedApproaches = listOf(
                        "Direct & Collaborative: 'I hear that you're overwhelmed, but our deadline is Thursday. What portion can you finish by tonight?'",
                        "Firm Boundary: 'We understand, but we cannot absorb your slides. Let's look at the remaining cards together right now.'"
                    )
                ),
                ConversationDialogueTurn(
                    turnIndex = 2,
                    speakerRole = "NPC",
                    messageText = "I think I can finish the architecture diagram, but I won't have time for the research appendix.",
                    suggestedApproaches = listOf(
                        "Lock in commitment: 'Deal. You deliver the architecture diagram by 9 PM tonight, and we'll trim the appendix together.'"
                    )
                )
            )
        ),

        // B. Professional Networking
        AdvancedScenario(
            scenarioId = "ADV_NET_01",
            category = AdvancedScenarioCategory.PROFESSIONAL_NETWORKING,
            module = SocialModuleIdentifier.NETWORKING_RELATIONSHIPS,
            title = "Approaching an Industry Tech Lead",
            description = "At a conference banquet, introduce yourself to an Engineering Director whose distributed systems talk you attended.",
            contextPrompt = "Director is grabbing a coffee between sessions.",
            npcName = "Elena Rostova",
            npcRole = "VP of Infrastructure",
            learningObjective = "Deliver a 20-second respectful introduction referencing a specific technical takeaway from her keynote.",
            difficulty = PracticeDifficulty.BASIC,
            dialogueScript = listOf(
                ConversationDialogueTurn(
                    turnIndex = 1,
                    speakerRole = "NPC",
                    messageText = "Hello there. Enjoying the conference sessions so far?",
                    suggestedApproaches = listOf(
                        "Targeted observation: 'Thoroughly! Your point on database shard rebalancing during live traffic solved a design hurdle for my team.'",
                        "Concise intro: 'Yes, particularly your infrastructure talk! I'm an engineer building offline-first architectures.'"
                    )
                ),
                ConversationDialogueTurn(
                    turnIndex = 2,
                    speakerRole = "NPC",
                    messageText = "Oh nice, shard rebalancing is always tricky! What consensus protocol are you running locally?",
                    suggestedApproaches = listOf(
                        "Answer technically in under 2 sentences and ask for her opinion on local compaction."
                    )
                )
            )
        ),

        // C. Professional Communication
        AdvancedScenario(
            scenarioId = "ADV_PROF_01",
            category = AdvancedScenarioCategory.PROFESSIONAL_COMMUNICATION,
            module = SocialModuleIdentifier.PROFESSIONAL_EXCELLENCE,
            title = "Communicating an Unforeseen Technical Delay",
            description = "Inform an executive stakeholder that third-party API instability will push release back by 48 hours.",
            contextPrompt = "Stakeholder asks for a release readiness confirmation.",
            npcName = "Marcus",
            npcRole = "Product Director",
            learningObjective = "Deliver the bad news cleanly (BLUF), explain the root failure without blame, and present a risk-free mitigation.",
            difficulty = PracticeDifficulty.ADVANCED,
            dialogueScript = listOf(
                ConversationDialogueTurn(
                    turnIndex = 1,
                    speakerRole = "NPC",
                    messageText = "Hey team, are we green to announce the deployment to our pilot beta customers tomorrow morning?",
                    suggestedApproaches = listOf(
                        "BLUF: 'Marcus, we must hold the rollout by 48 hours. Third-party authentication latency is causing 8% failure rates in staging.'",
                        "Risk mitigation: 'Holding launch protects user trust. The upstream team has deployed a patch, and we will re-test tomorrow at 10 AM.'"
                    )
                )
            )
        ),

        // D. Negotiation Simulator
        AdvancedScenario(
            scenarioId = "ADV_NEG_01",
            category = AdvancedScenarioCategory.NEGOTIATION_SIMULATOR,
            module = SocialModuleIdentifier.NEGOTIATION_PERSUASION,
            title = "Internship Stipend & Remote Days Negotiation",
            description = "Negotiate compensation and two remote days for a summer software engineering internship.",
            contextPrompt = "Recruiter sends standard contract offer: in-office 5 days, $25/hr.",
            npcName = "Sarah",
            npcRole = "Talent Acquisition Manager",
            learningObjective = "Show enthusiasm for the company mission, present market rate benchmarks, and request hybrid flexibility politely.",
            difficulty = PracticeDifficulty.INTERMEDIATE,
            dialogueScript = listOf(
                ConversationDialogueTurn(
                    turnIndex = 1,
                    speakerRole = "NPC",
                    messageText = "We are excited to bring you onboard at $25/hr in our downtown office starting June 1st!",
                    suggestedApproaches = listOf(
                        "Gratitude & Anchor: 'I'm thrilled about the offer and eager to contribute to the core SDK! Based on my previous internship and current market averages, would the team consider $30/hr and a hybrid 3/2 schedule?'"
                    )
                ),
                ConversationDialogueTurn(
                    turnIndex = 2,
                    speakerRole = "NPC",
                    messageText = "The hourly budget is capped at $28 for interns, but we can definitely approve Tuesdays and Thursdays remote.",
                    suggestedApproaches = listOf(
                        "Close constructively: 'That compensation adjustment and hybrid flexibility works wonderfully. I accept!'"
                    )
                )
            )
        )
    )

    // Interview Preparation Question Bank
    val interviewQuestions: List<InterviewQuestion> = listOf(
        InterviewQuestion(
            questionId = "INT_01",
            interviewType = "BEHAVIORAL",
            questionText = "Tell me about a time you had a fundamental technical disagreement with a team member. How did you resolve it?",
            starFrameworkFocus = "STAR: Describe the Situation, your Task, the collaborative Action taken, and the measurable Result.",
            targetTimeSeconds = 120,
            tips = listOf("Focus on the technical problem, not personal animosity", "Highlight data or prototype benchmarking used to decide", "Mention what you learned")
        ),
        InterviewQuestion(
            questionId = "INT_02",
            interviewType = "HR",
            questionText = "Why are you interested in our engineering team, and what makes you stand out from other candidates?",
            starFrameworkFocus = "Focus on culture fit, specific technical contributions, and alignment with company product challenges.",
            targetTimeSeconds = 90,
            tips = listOf("Cite a specific feature or technical challenge the company faces", "Link your past projects directly to their stack")
        ),
        InterviewQuestion(
            questionId = "INT_03",
            interviewType = "TECHNICAL",
            questionText = "Walk me through how you would architect a resilient offline-first synchronization engine for a mobile app.",
            starFrameworkFocus = "Architecture structure: Local storage, SyncQueue, Idempotency keys, Conflict resolution, and Network listeners.",
            targetTimeSeconds = 180,
            tips = listOf("Start with high-level data flow before diving into SQLite tables", "Explicitly address conflict resolution strategies")
        )
    )

    // Leadership Decision Simulator Scenarios
    val leadershipScenarios: List<LeadershipDecisionScenario> = listOf(
        LeadershipDecisionScenario(
            scenarioId = "LEAD_DEC_01",
            title = "Critical Security Vulnerability vs Product Launch",
            backgroundContext = "It is 2 hours before the scheduled public launch. Security audits uncover a moderate privilege-escalation vulnerability in social profile exports.",
            stakeholders = listOf("Marketing Director (Press waiting)", "Lead Security Architect", "Executive Sponsor"),
            constraints = listOf("PR campaign starts in 120 minutes", "Patch requires 6 hours to write and verify"),
            options = listOf(
                LeadershipOption(
                    optionId = "OPT_DELAY",
                    title = "Postpone launch by 24 hours to patch",
                    actionDescription = "Take the marketing hit, maintain uncompromising security integrity, and patch the leak before any user data is exposed.",
                    tradeOffs = "Marketing frustration, but eliminates long-term breach liability.",
                    expectedOutcomeScore = 95
                ),
                LeadershipOption(
                    optionId = "OPT_FEATURE_FLAG",
                    title = "Disable social export feature and proceed with launch",
                    actionDescription = "Feature-flag off the vulnerable export endpoint immediately, proceed with 95% of launch scope, and patch export in v1.0.1.",
                    tradeOffs = "Pragmatic balance: launch proceeds safely without risking user data.",
                    expectedOutcomeScore = 92
                ),
                LeadershipOption(
                    optionId = "OPT_LAUNCH_ANYWAY",
                    title = "Launch on schedule and hotfix overnight",
                    actionDescription = "Proceed with full release and quietly deploy fix at 2 AM.",
                    tradeOffs = "Severe security negligence. High risk of immediate zero-day exploitation.",
                    expectedOutcomeScore = 25
                )
            ),
            reflectionPrompt = "How do you explain the decision to executive stakeholders who prioritize launch momentum over technical debt?"
        )
    )
}
