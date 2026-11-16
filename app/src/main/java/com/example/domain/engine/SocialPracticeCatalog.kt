package com.example.domain.engine

import com.example.domain.model.ArticulationExercise
import com.example.domain.model.ConversationDialogueTurn
import com.example.domain.model.ConversationScenario
import com.example.domain.model.EvaluationRubricDimension
import com.example.domain.model.ListeningExercise
import com.example.domain.model.PracticeCategory
import com.example.domain.model.PracticeDifficulty
import com.example.domain.model.PracticeExercise
import com.example.domain.model.SocialModuleIdentifier

/**
 * Milestone 12 - Phase 2: Central Offline-First Exercise Catalogue.
 * Contains structured exercises, deterministic dialogue scenarios,
 * listening passages, and articulation drills for all 8 Social modules.
 */
object SocialPracticeCatalog {

    val standardRubricDimensions = listOf(
        EvaluationRubricDimension("Clarity", 0.30f, "Directness, structure, and verbal clarity"),
        EvaluationRubricDimension("Relevance", 0.25f, "Responsiveness to the situation or prompt"),
        EvaluationRubricDimension("Conciseness", 0.25f, "Information density without repetitive rambling"),
        EvaluationRubricDimension("Confidence", 0.20f, "Assertiveness, non-defensive tone, and posture")
    )

    val exercises: List<PracticeExercise> = listOf(
        // 1. Communication Mastery
        PracticeExercise(
            exerciseId = "EX_COMM_01",
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            category = PracticeCategory.STRUCTURED_EXPRESSION,
            title = "The PREP Framework Drill",
            description = "Practice organizing your thoughts in real-time using Point, Reason, Example, Point.",
            instructions = "Deliver a 60-90 second response on 'Why remote or hybrid work requires deliberate communication habits'.",
            difficulty = PracticeDifficulty.BASIC,
            estimatedDurationMinutes = 5,
            learningObjective = "State point, provide one concrete reason, share one vivid example, and reiterate summary.",
            promptContent = "Prompt: State your opinion on whether daily team standups are effective, following PREP.",
            exampleResponse = "Point: Standups are vital when brief. Reason: They highlight blockers fast. Example: Yesterday an engineer was stuck on API keys until standup surfaced it. Point: Keep them under 10 minutes to maintain value.",
            rubricDimensions = standardRubricDimensions,
            minPassingScore = 70,
            feedbackTips = listOf("Avoid starting with apologies", "Keep the example under 20 seconds", "End on a decisive final statement")
        ),
        PracticeExercise(
            exerciseId = "EX_COMM_02",
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            category = PracticeCategory.ACTIVE_LISTENING,
            title = "Reflective Paraphrasing",
            description = "Mirror an emotionally charged message before answering logically.",
            instructions = "Read the prompt, identify the core emotion and logistical issue, and draft a response starting with 'What I hear you saying is...'.",
            difficulty = PracticeDifficulty.DEVELOPING,
            estimatedDurationMinutes = 5,
            learningObjective = "Acknowledge frustration without becoming defensive or invalidating.",
            promptContent = "Speaker says: 'Every time we push to staging, the mobile layout breaks and nobody tells the QA team!'",
            rubricDimensions = standardRubricDimensions,
            minPassingScore = 75,
            feedbackTips = listOf("Do not immediately explain why it broke", "Validate the QA team's wasted effort first", "Propose an automated alert step")
        ),

        // 2. Public Speaking & Presentation
        PracticeExercise(
            exerciseId = "EX_PUB_01",
            module = SocialModuleIdentifier.PUBLIC_SPEAKING,
            category = PracticeCategory.IMPROMPTU_SPEAKING,
            title = "The 60-Second Technical Pitch",
            description = "Explain a complex technical concept in 60 seconds without saying 'um', 'like', or 'you know'.",
            instructions = "Select a concept (e.g. Database Indexing, OAuth, Garbage Collection) and explain it clearly for a non-technical manager.",
            difficulty = PracticeDifficulty.INTERMEDIATE,
            estimatedDurationMinutes = 6,
            learningObjective = "Eliminate filler words by pausing silently instead of uttering filler phonemes.",
            promptContent = "Topic: Explain how Database Indexing speeds up queries using a real-world library book catalog analogy.",
            rubricDimensions = listOf(
                EvaluationRubricDimension("Pacing & Cadence", 0.35f, "Steady words-per-minute with deliberate pauses"),
                EvaluationRubricDimension("Analogy Quality", 0.35f, "Clarity of the non-technical metaphor"),
                EvaluationRubricDimension("Conciseness", 0.30f, "Staying strictly within the 60-second limit")
            ),
            minPassingScore = 75,
            feedbackTips = listOf("When you need to think, pause with closed lips", "Avoid rushed speech near the end", "Use vocal inflection for emphasis")
        ),

        // 3. Negotiation & Persuasion
        PracticeExercise(
            exerciseId = "EX_NEG_01",
            module = SocialModuleIdentifier.NEGOTIATION_PERSUASION,
            category = PracticeCategory.CONVERSATION,
            title = "Salary & Scope Counter-Offer",
            description = "Respond to an initial job offer by expressing gratitude while proposing a justified adjustment.",
            instructions = "You received an offer of $90,000 for a senior role where market average is $105,000.",
            difficulty = PracticeDifficulty.ADVANCED,
            estimatedDurationMinutes = 10,
            learningObjective = "Anchor your value to specific deliverables and market standards without sounding entitled.",
            promptContent = "Hiring Manager: 'We are thrilled to offer you the position at $90,000 with standard benefits.'",
            rubricDimensions = standardRubricDimensions,
            minPassingScore = 80,
            feedbackTips = listOf("Always anchor your counter with verifiable impact", "Keep gratitude authentic", "Propose a collaborative conversation rather than an ultimatum")
        ),

        // 4. Leadership & Teamwork
        PracticeExercise(
            exerciseId = "EX_LEAD_01",
            module = SocialModuleIdentifier.LEADERSHIP_TEAMWORK,
            category = PracticeCategory.PROFESSIONAL_COMMUNICATION,
            title = "Radical Candor: Direct Feedback",
            description = "Deliver critical performance feedback to a talented but chronically tardy peer.",
            instructions = "Deliver feedback using: Care Personally + Challenge Directly.",
            difficulty = PracticeDifficulty.INTERMEDIATE,
            estimatedDurationMinutes = 8,
            learningObjective = "Separate identity from behavior; explain concrete team consequences.",
            promptContent = "Teammate Alex arrives 15 minutes late to daily client standups, leaving you to stall.",
            rubricDimensions = standardRubricDimensions,
            minPassingScore = 75,
            feedbackTips = listOf("Describe the specific behavior, not their personality", "Highlight the impact on team credibility", "Ask for their perspective")
        ),

        // 5. Networking & Relationships
        PracticeExercise(
            exerciseId = "EX_NET_01",
            module = SocialModuleIdentifier.NETWORKING_RELATIONSHIPS,
            category = PracticeCategory.CONVERSATION,
            title = "High-Value Mentor Cold Outreach",
            description = "Draft a crisp 3-sentence message to an industry veteran seeking guidance.",
            instructions = "Reference their specific recent work, ask one precise question, and keep it under 75 words.",
            difficulty = PracticeDifficulty.BASIC,
            estimatedDurationMinutes = 5,
            learningObjective = "Respect their time and show genuine research rather than generic flattery.",
            promptContent = "Reach out to a Lead Architect whose blog post on distributed consensus you just read.",
            rubricDimensions = standardRubricDimensions,
            minPassingScore = 75,
            feedbackTips = listOf("Do not ask to 'pick their brain'", "Ask a specific question that can be answered in 2 sentences", "Make the call to action effortless")
        ),

        // 6. Emotional & Social Intelligence
        PracticeExercise(
            exerciseId = "EX_EMO_01",
            module = SocialModuleIdentifier.EMOTIONAL_INTELLIGENCE,
            category = PracticeCategory.CRITICAL_QUESTIONING,
            title = "De-escalating Defensive Reactivity",
            description = "Respond to an unfair accusation from a stressed collaborator.",
            instructions = "Take a breath, label the tension, and redirect toward common goals.",
            difficulty = PracticeDifficulty.INTERMEDIATE,
            estimatedDurationMinutes = 6,
            learningObjective = "Reflect emotion without taking the emotional bait or escalating.",
            promptContent = "Colleague: 'You clearly don't care about our deadline or you would have answered my ping immediately!'",
            rubricDimensions = standardRubricDimensions,
            minPassingScore = 75,
            feedbackTips = listOf("Do not respond with 'Calm down'", "Acknowledge the impending deadline pressure", "Clarify your status calmly")
        ),

        // 7. Professional Excellence
        PracticeExercise(
            exerciseId = "EX_PROF_01",
            module = SocialModuleIdentifier.PROFESSIONAL_EXCELLENCE,
            category = PracticeCategory.PROFESSIONAL_COMMUNICATION,
            title = "Executive Status Briefing",
            description = "Deliver a 30-second BLUF (Bottom Line Up Front) summary of an ongoing system outage.",
            instructions = "State current impact, root cause, estimated restoration time, and immediate mitigation.",
            difficulty = PracticeDifficulty.DEVELOPING,
            estimatedDurationMinutes = 4,
            learningObjective = "Provide actionable executive signal without technical jargon or hand-waving.",
            promptContent = "Incident: Payment gateway timeout spike affecting 12% of checkout attempts for 18 minutes.",
            rubricDimensions = standardRubricDimensions,
            minPassingScore = 75,
            feedbackTips = listOf("State the business impact in the first 5 seconds", "State what is being done right now", "Give a conservative ETA")
        ),

        // 8. Language Development
        PracticeExercise(
            exerciseId = "EX_LANG_01",
            module = SocialModuleIdentifier.LANGUAGE_DEVELOPMENT,
            category = PracticeCategory.ARTICULATION,
            title = "Precision Diction & Enunciation",
            description = "Deliver clear, rhythmic sentence structures with crisp consonant endings.",
            instructions = "Read the passage aloud at 130 words per minute, focusing on ending consonants (t, d, p, k).",
            difficulty = PracticeDifficulty.BASIC,
            estimatedDurationMinutes = 5,
            learningObjective = "Improve articulatory sharpness and prevent syllable swallowing.",
            promptContent = "Passage: 'Decisive leadership demands deliberate articulation, structured consensus, and rapid technical execution under pressure.'",
            rubricDimensions = listOf(
                EvaluationRubricDimension("Enunciation Clarity", 0.40f, "Crisp vowel and consonant definition"),
                EvaluationRubricDimension("Pacing & Rhythm", 0.35f, "Control between 120 and 140 WPM"),
                EvaluationRubricDimension("Expression", 0.25f, "Natural emphasis on operative verbs")
            ),
            minPassingScore = 70,
            feedbackTips = listOf("Exaggerate consonant sounds during practice", "Keep jaw relaxed", "Take a deep diaphragmatic breath prior to speaking")
        )
    )

    // Interactive Conversation Scenarios (Deterministic offline roleplay)
    val conversationScenarios: List<ConversationScenario> = listOf(
        ConversationScenario(
            scenarioId = "SIM_INTRO_01",
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            title = "Introducing Yourself at an Engineering Meetup",
            contextDescription = "You are standing near the refreshment table at a tech meetup. Another engineer turns toward you.",
            npcName = "Maya",
            npcPersona = "Friendly Senior Mobile Developer at a Fintech startup.",
            openingDialogue = "Hey! Great talk on Kotlin Coroutines, wasn't it? Have you been working with them long?",
            objective = "Introduce yourself, mention your background cleanly, and ask about her experience.",
            dialogueScript = listOf(
                ConversationDialogueTurn(
                    turnIndex = 1,
                    speakerRole = "NPC",
                    messageText = "Hey! Great talk on Kotlin Coroutines, wasn't it? Have you been working with them long?",
                    suggestedApproaches = listOf(
                        "Enthusiastic: 'Yes! Been using them for state flows, especially with Room. How about you?'",
                        "Learning: 'I'm currently mastering them for an offline-first project! What brings you to this meetup?'",
                        "Brief: 'Quite a bit. The structured concurrency section was spot on. Are you building on Android?'"
                    )
                ),
                ConversationDialogueTurn(
                    turnIndex = 2,
                    speakerRole = "NPC",
                    messageText = "Awesome! Yeah, I lead Android at a payments app in town. Offline-first is tough with security tokens though. What are you building?",
                    suggestedApproaches = listOf(
                        "Concise Project Pitch: Describe your system in one clear sentence.",
                        "Shared Challenge: Ask how her team handles local encryption keys.",
                        "Direct Inquiry: Inquire about her tech stack and startup culture."
                    )
                ),
                ConversationDialogueTurn(
                    turnIndex = 3,
                    speakerRole = "NPC",
                    messageText = "That sounds like a serious engineering effort! Hey, let's connect on GitHub or LinkedIn before the lightning talks begin.",
                    suggestedApproaches = listOf(
                        "Exchange details smoothly and thank her for the conversation.",
                        "Confirm contact handle and suggest following up on her architecture approach."
                    )
                )
            ),
            targetRounds = 3,
            difficulty = PracticeDifficulty.BASIC
        ),

        ConversationScenario(
            scenarioId = "SIM_CAMPUS_01",
            module = SocialModuleIdentifier.NETWORKING_RELATIONSHIPS,
            title = "Starting a Conversation with a Professor",
            contextDescription = "After a lecture on Distributed Systems, you approach the professor during office hours.",
            npcName = "Professor Vance",
            npcPersona = "Busy but supportive academic researcher.",
            openingDialogue = "Yes, come in. How can I help you today regarding the distributed consensus paper?",
            objective = "State your question concisely, show you read the prerequisite material, and ask for guidance.",
            dialogueScript = listOf(
                ConversationDialogueTurn(
                    turnIndex = 1,
                    speakerRole = "NPC",
                    messageText = "Yes, come in. How can I help you today regarding the distributed consensus paper?",
                    suggestedApproaches = listOf(
                        "Direct: 'Thank you Professor. I was analyzing Raft log compaction and had a question on snapshotting.'",
                        "Thoughtful: 'I enjoyed the lecture! I noticed a tradeoff between Paxos latency and Raft simplicity.'"
                    )
                ),
                ConversationDialogueTurn(
                    turnIndex = 2,
                    speakerRole = "NPC",
                    messageText = "Good observation. Snapshotting becomes the bottleneck under write-heavy workloads. Have you looked at section 4's compaction benchmarks?",
                    suggestedApproaches = listOf(
                        "Honest engagement: Confirm you reviewed section 4 and ask about disk I/O implications.",
                        "Inquiry: Ask which paper or benchmark tool he recommends studying next."
                    )
                ),
                ConversationDialogueTurn(
                    turnIndex = 3,
                    speakerRole = "NPC",
                    messageText = "Check out the Ongaro dissertation appendix. Send me your notes once you've drafted the experiment. Good work.",
                    suggestedApproaches = listOf(
                        "Express appreciation, confirm the action item, and exit politely without overstaying."
                    )
                )
            ),
            targetRounds = 3,
            difficulty = PracticeDifficulty.INTERMEDIATE
        )
    )

    // Dedicated Listening Exercises
    val listeningExercises: List<ListeningExercise> = listOf(
        ListeningExercise(
            exerciseId = "LIST_01",
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            title = "Meeting Sync: Extracting Blockers & Deadlines",
            spokenOrWrittenPassage = "Alright team, we need to ship the onboarding flow by Thursday 5 PM. Design finalized the icons, but backend auth is throwing 401s on token refresh. Dave is waiting on credentials from DevOps, so he can't test yet. Let's make sure QA gets an APK by Wednesday noon.",
            questionPrompt = "1. What is the hard customer deadline? 2. What is Dave's blocker? 3. When does QA need the build?",
            keyFactsToIdentify = listOf("Thursday 5 PM", "401s on token refresh / waiting on DevOps credentials", "Wednesday noon"),
            perspectiveSummary = "The project manager is balancing a tight Thursday ship date against blocked DevOps dependencies.",
            followUpQuestionCriteria = "An effective follow-up asks: 'Who is following up with DevOps to unblock Dave before Wednesday morning?'",
            minPassingScore = 70
        ),
        ListeningExercise(
            exerciseId = "LIST_02",
            module = SocialModuleIdentifier.EMOTIONAL_INTELLIGENCE,
            title = "Identifying Subtext and Unspoken Friction",
            spokenOrWrittenPassage = "No, it's fine. If the committee decided to reassign the mobile lead role to the external hire without consulting the project contributors, I guess that's just company policy. I'll just focus on my bug backlog.",
            questionPrompt = "What is the speaker's true underlying emotional state, and what is the real issue they are struggling with?",
            keyFactsToIdentify = listOf("Feeling bypassed / unvalued", "Disappointed about external hire taking lead", "Withdrawing engagement to just bug backlog"),
            perspectiveSummary = "The speaker feels disrespected and unappreciated after their contributions were overlooked.",
            followUpQuestionCriteria = "A supportive question validates: 'It sounds like you feel bypassed after investing so much in this codebase. Can we talk about your growth path?'",
            minPassingScore = 75
        )
    )

    // Dedicated Articulation and Pronunciation Exercises
    val articulationExercises: List<ArticulationExercise> = listOf(
        ArticulationExercise(
            exerciseId = "ART_EN_01",
            languageCode = "en",
            title = "Consonant Precision & Sibilance Control",
            targetPhonemesOrSound = "/s/, /z/, /tʃ/, /dʒ/",
            phraseToArticulate = "Strategic system consensus requires consistent precision, decisive judgment, and exceptional clarity.",
            targetPaceWpm = 130,
            tongueTwisterLevel = PracticeDifficulty.BASIC
        ),
        ArticulationExercise(
            exerciseId = "ART_EN_02",
            languageCode = "en",
            title = "Dental & Plosive Enunciation Drill",
            targetPhonemesOrSound = "/θ/, /ð/, /p/, /t/, /k/",
            phraseToArticulate = "Throughout the persistent technical review, the team tracked twenty-three distinct throughput bottlenecks.",
            targetPaceWpm = 125,
            tongueTwisterLevel = PracticeDifficulty.INTERMEDIATE
        ),
        ArticulationExercise(
            exerciseId = "ART_ES_01",
            languageCode = "es",
            title = "Spanish R & Vibrational Cadence",
            targetPhonemesOrSound = "/r/, /rr/, /d/",
            phraseToArticulate = "El rápido desarrollo de la arquitectura requiere disciplina constante y comunicación directa.",
            targetPaceWpm = 140,
            tongueTwisterLevel = PracticeDifficulty.BASIC
        )
    )
}
