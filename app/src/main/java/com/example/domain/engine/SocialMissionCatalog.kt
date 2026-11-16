package com.example.domain.engine

import com.example.domain.model.MissionDifficultyRank
import com.example.domain.model.RealWorldSocialMission
import com.example.domain.model.SocialModuleIdentifier

/**
 * Milestone 12 - Phase 4: Central Real-World Social Missions Catalogue.
 * Covers all 8 Social Mastery modules across 4 difficulty tiers (E, D, C, B Rank).
 * Includes alternative missions when a user declines an activity.
 */
object SocialMissionCatalog {

    val missions: List<RealWorldSocialMission> = listOf(
        // =====================================================================
        // 1. COMMUNICATION MASTERY
        // =====================================================================
        RealWorldSocialMission(
            missionId = "MSN_COMM_E01",
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            difficulty = MissionDifficultyRank.E_RANK,
            title = "Meaningful Daily Check-In",
            description = "Initiate an intentional 3-minute in-person or voice conversation asking an open-ended question.",
            estimatedDurationMinutes = 5,
            learningObjective = "Break passive hesitation and practice open-ended conversational inquiry.",
            stepByStepInstructions = listOf(
                "Identify a peer, coworker, or acquaintance you interact with daily.",
                "Ask an open question such as 'What has been the most interesting part of your week so far?'",
                "Listen attentively without looking at your phone or interrupting.",
                "Paraphrase or ask a relevant follow-up question before closing."
            ),
            completionCriteria = listOf(
                "Conversation lasted at least 2 minutes.",
                "Asked at least one open-ended question.",
                "Listened without interrupting or checking devices."
            ),
            reflectionQuestions = listOf(
                "What open question did you ask, and how did the other person react?",
                "Did you feel an impulse to interrupt? How did you manage it?",
                "What did you learn about the other person?"
            ),
            evidenceRequirements = listOf(
                "Submit a written self-reflection describing the interaction context and takeaways.",
                "No audio recording or contact details required."
            ),
            xpEligibilityRules = "Awards standard E-Rank XP (25 XP) upon submitting a verified reflection.",
            aiCoachingTip = "Start with open body language. Keep your question open-ended (beginning with What or How rather than Did or Are).",
            isDeclineable = true,
            alternativeMissionId = "MSN_COMM_E01_ALT"
        ),
        RealWorldSocialMission(
            missionId = "MSN_COMM_E01_ALT",
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            difficulty = MissionDifficultyRank.E_RANK,
            title = "Meaningful Written Check-In (Alternative)",
            description = "Write a thoughtful, personalized message to a peer or teammate asking for their perspective.",
            estimatedDurationMinutes = 5,
            learningObjective = "Practice deliberate written inquiry and conversational warmth.",
            stepByStepInstructions = listOf(
                "Select a colleague or friend you have not messaged recently.",
                "Send a 2-3 sentence personalized message inquiring about a specific project or interest.",
                "Wait for their reply and respond thoughtfully."
            ),
            completionCriteria = listOf(
                "Message was personalized and non-transactional.",
                "Asked a thoughtful question."
            ),
            reflectionQuestions = listOf(
                "Who did you message, and what was the focus of your inquiry?",
                "How did structuring your message improve its clarity?"
            ),
            evidenceRequirements = listOf("Self-reflection describing the exchange."),
            xpEligibilityRules = "Awards standard E-Rank XP (25 XP) upon reflection submission.",
            aiCoachingTip = "Avoid generic 'Hey, what's up?'. Refer to a specific shared topic or interest.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_COMM_D02",
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            difficulty = MissionDifficultyRank.D_RANK,
            title = "Explain a Concept Using PREP",
            description = "Explain a technical, academic, or work concept in under 2 minutes using Point, Reason, Example, Point.",
            estimatedDurationMinutes = 10,
            learningObjective = "Structure spontaneous thoughts with executive conciseness.",
            stepByStepInstructions = listOf(
                "Choose a concept you know well (e.g., git branching, database indexing, caching).",
                "Explain it to a teammate or study partner using PREP structure.",
                "Verify whether they understood by asking what they took away."
            ),
            completionCriteria = listOf(
                "Followed PREP structure clearly.",
                "Completed explanation in under 2 minutes.",
                "Partner understood the core message."
            ),
            reflectionQuestions = listOf(
                "What was your Point, Reason, Example, and concluding Point?",
                "Did you notice yourself using filler words?",
                "How did the listener respond to the structured format?"
            ),
            evidenceRequirements = listOf("Self-reflection detailing the PREP structure used."),
            xpEligibilityRules = "Awards D-Rank XP (35 XP) upon verified reflection.",
            aiCoachingTip = "State your bottom line first. If your listener understands your point immediately, the details land much more cleanly.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_COMM_C03",
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            difficulty = MissionDifficultyRank.C_RANK,
            title = "Active Listening Paraphrasing Drill",
            description = "In a meeting or conversation, practice reflective listening by summarizing the speaker's key points before adding your own.",
            estimatedDurationMinutes = 15,
            learningObjective = "Demonstrate active comprehension and reduce conversational friction.",
            stepByStepInstructions = listOf(
                "During a discussion where someone shares an opinion or update, wait until they pause.",
                "Say: 'What I am hearing is [paraphrase core point]. Is that accurate?'",
                "Allow them to confirm or clarify before offering your thoughts."
            ),
            completionCriteria = listOf(
                "Paraphrased accurately without distorting their words.",
                "Verified understanding with the speaker."
            ),
            reflectionQuestions = listOf(
                "How did the speaker react when you summarized their viewpoint?",
                "Did paraphrasing prevent any potential misunderstanding?"
            ),
            evidenceRequirements = listOf("Self-reflection or human-confirmed teammate observation."),
            xpEligibilityRules = "Awards C-Rank XP (50 XP).",
            aiCoachingTip = "Paraphrasing does not mean agreeing; it simply proves you respected them enough to listen accurately.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_COMM_B04",
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            difficulty = MissionDifficultyRank.B_RANK,
            title = "Difficult Conversation Alignment",
            description = "Address a sensitive interpersonal misalignment or recurring communication breakdown with tact and clarity.",
            estimatedDurationMinutes = 20,
            learningObjective = "Master high-tension communication with psychological safety.",
            stepByStepInstructions = listOf(
                "Request a private 10-minute check-in with the individual.",
                "State the shared goal first (e.g., project success, mutual respect).",
                "Describe the observable issue objectively without blaming character.",
                "Co-create a concrete communication protocol moving forward."
            ),
            completionCriteria = listOf(
                "Conducted privately and constructively.",
                "Agreed upon mutual next steps."
            ),
            reflectionQuestions = listOf(
                "How did you establish psychological safety before addressing the issue?",
                "What mutual agreement was reached?"
            ),
            evidenceRequirements = listOf("Comprehensive self-reflection on the outcome."),
            xpEligibilityRules = "Awards B-Rank XP (65 XP).",
            aiCoachingTip = "Focus on the gap between intention and impact. Use 'When X happened, the impact was Y' rather than 'You always do X'.",
            isDeclineable = true
        ),

        // =====================================================================
        // 2. PUBLIC SPEAKING & PRESENTATION
        // =====================================================================
        RealWorldSocialMission(
            missionId = "MSN_PUB_E01",
            module = SocialModuleIdentifier.PUBLIC_SPEAKING,
            difficulty = MissionDifficultyRank.E_RANK,
            title = "60-Second Focused Speech",
            description = "Deliver a 60-second structured technical or motivational thought to a friend or record it in-app.",
            estimatedDurationMinutes = 5,
            learningObjective = "Overcome vocal startup friction and maintain steady delivery.",
            stepByStepInstructions = listOf(
                "Pick one topic you care about.",
                "Speak clearly for 60 seconds without long pauses.",
                "Focus on vocal projection and breathing from your diaphragm."
            ),
            completionCriteria = listOf(
                "Spoke for 50-70 seconds.",
                "Maintained audible projection."
            ),
            reflectionQuestions = listOf(
                "How steady was your vocal delivery?",
                "What filler words did you notice?"
            ),
            evidenceRequirements = listOf("Self-reflection or in-app speech practice record."),
            xpEligibilityRules = "Awards E-Rank XP (25 XP).",
            aiCoachingTip = "Take a full diaphragmatic breath before opening your mouth. Silence sounds confident; filler words sound hesitant.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_PUB_D02",
            module = SocialModuleIdentifier.PUBLIC_SPEAKING,
            difficulty = MissionDifficultyRank.D_RANK,
            title = "Ask a Question in a Group Forum",
            description = "Ask a concise, well-formulated question during a lecture, team meeting, or seminar.",
            estimatedDurationMinutes = 10,
            learningObjective = "Project confidence in front of a live group.",
            stepByStepInstructions = listOf(
                "Prepare your question mentally before raising your hand.",
                "State your question in 1 or 2 sentences at full conversational volume.",
                "Listen to the answer without fidgeting."
            ),
            completionCriteria = listOf(
                "Asked question publicly in a meeting with 3+ attendees.",
                "Question was concise and on-topic."
            ),
            reflectionQuestions = listOf(
                "What question did you ask, and how did the speaker address it?",
                "How did you feel right before speaking?"
            ),
            evidenceRequirements = listOf("Self-reflection or peer confirmation."),
            xpEligibilityRules = "Awards D-Rank XP (35 XP).",
            aiCoachingTip = "Don't preface your question with 'This might be a dumb question...'. State the question directly.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_PUB_C03",
            module = SocialModuleIdentifier.PUBLIC_SPEAKING,
            difficulty = MissionDifficultyRank.C_RANK,
            title = "Deliver a 5-Minute Technical Briefing",
            description = "Present a 5-minute technical briefing or demo to classmates or colleagues with visual aids.",
            estimatedDurationMinutes = 20,
            learningObjective = "Pace structured delivery across multi-slide technical content.",
            stepByStepInstructions = listOf(
                "Prepare 3-5 visual slides or an outline.",
                "Deliver the presentation to 2 or more people.",
                "Facilitate 2 minutes of Q&A afterwards."
            ),
            completionCriteria = listOf(
                "Presented to 2+ people for 4-6 minutes.",
                "Handled questions with poise."
            ),
            reflectionQuestions = listOf(
                "How did you manage time across your talking points?",
                "What question challenged you, and how did you answer?"
            ),
            evidenceRequirements = listOf("Self-reflection + optional slide deck link/title."),
            xpEligibilityRules = "Awards C-Rank XP (50 XP).",
            aiCoachingTip = "Make eye contact with different individuals across the room. Don't look solely at your slides.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_PUB_B04",
            module = SocialModuleIdentifier.PUBLIC_SPEAKING,
            difficulty = MissionDifficultyRank.B_RANK,
            title = "Lead an Interactive Workshop",
            description = "Facilitate a 20+ minute interactive workshop, teaching a complex framework to a group.",
            estimatedDurationMinutes = 30,
            learningObjective = "Sustain group engagement, vocal cadence, and dynamic facilitation.",
            stepByStepInstructions = listOf(
                "Structure session into: Concept introduction, Hands-on exercise, Group debrief.",
                "Engage the audience with interactive prompts.",
                "Keep the room focused on key outcomes."
            ),
            completionCriteria = listOf(
                "Session conducted with 4+ attendees.",
                "Included interactive participation."
            ),
            reflectionQuestions = listOf(
                "How did you re-engage attendees who seemed quiet?",
                "What would you streamline in your delivery next time?"
            ),
            evidenceRequirements = listOf("Self-reflection + human confirmed feedback."),
            xpEligibilityRules = "Awards B-Rank XP (65 XP).",
            aiCoachingTip = "Pause for 3 full seconds after asking a question to let the room think. Don't rush to fill the silence.",
            isDeclineable = true
        ),

        // =====================================================================
        // 3. NEGOTIATION & PERSUASION
        // =====================================================================
        RealWorldSocialMission(
            missionId = "MSN_NEG_E01",
            module = SocialModuleIdentifier.NEGOTIATION_PERSUASION,
            difficulty = MissionDifficultyRank.E_RANK,
            title = "Respectful Difference of Opinion",
            description = "Express a dissenting viewpoint respectfully during a team or group discussion.",
            estimatedDurationMinutes = 5,
            learningObjective = "Overcome people-pleasing hesitation while maintaining diplomatic warmth.",
            stepByStepInstructions = listOf(
                "When an approach is proposed that has a flaw, say: 'I see that perspective, and I'd like to suggest an alternative tradeoff...'",
                "State your reason based on evidence, not personal preference.",
                "Invite feedback on your suggestion."
            ),
            completionCriteria = listOf(
                "Offered a polite alternative viewpoint without aggression.",
                "Based reasoning on observable criteria."
            ),
            reflectionQuestions = listOf(
                "What was the alternative perspective you presented?",
                "How did the group react?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards E-Rank XP (25 XP).",
            aiCoachingTip = "Use 'Yes, and...' or 'I agree with the goal, and here is a consideration for risk...'",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_NEG_D02",
            module = SocialModuleIdentifier.NEGOTIATION_PERSUASION,
            difficulty = MissionDifficultyRank.D_RANK,
            title = "Task & Responsibility Negotiation",
            description = "Negotiate fair division of labor or delivery dates in a group assignment or workplace sprint.",
            estimatedDurationMinutes = 15,
            learningObjective = "Establish win-win collaborative task commitments.",
            stepByStepInstructions = listOf(
                "List the shared responsibilities objectively.",
                "Propose an initial distribution aligned with team members' strengths.",
                "Reach mutual agreement without one person absorbing all tedious work."
            ),
            completionCriteria = listOf(
                "Reached mutual agreement on task ownership.",
                "Documented responsibilities clearly."
            ),
            reflectionQuestions = listOf(
                "How did you balance strengths and workload fairly?",
                "Did any disagreement arise, and how was it handled?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards D-Rank XP (35 XP).",
            aiCoachingTip = "Anchor the discussion in outcomes rather than hours. Ask: 'What part of this excites you most?'",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_NEG_C03",
            module = SocialModuleIdentifier.NEGOTIATION_PERSUASION,
            difficulty = MissionDifficultyRank.C_RANK,
            title = "Scope Creep Defense & Tradeoff",
            description = "When asked for an unplanned request or urgent feature, negotiate an explicit tradeoff.",
            estimatedDurationMinutes = 15,
            learningObjective = "Defend sprint integrity with principled compromise.",
            stepByStepInstructions = listOf(
                "Acknowledge the stakeholder's urgency.",
                "Offer two valid options: Option A (deliver on time with current scope), Option B (include new request by delaying feature Y).",
                "Let the stakeholder make the informed choice."
            ),
            completionCriteria = listOf(
                "Avoided defensive hostility.",
                "Presented clear tradeoff options."
            ),
            reflectionQuestions = listOf(
                "What options did you present, and which was selected?",
                "How did providing options shift the power dynamic?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards C-Rank XP (50 XP).",
            aiCoachingTip = "Never say 'No, that's impossible'. Say 'We can definitely do that if we deprioritize X. Which do you prefer?'",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_NEG_B04",
            module = SocialModuleIdentifier.NEGOTIATION_PERSUASION,
            difficulty = MissionDifficultyRank.B_RANK,
            title = "High-Stakes Contract / Compensation Negotiation",
            description = "Prepare, present, and negotiate terms for compensation, budget allocation, or vendor pricing.",
            estimatedDurationMinutes = 30,
            learningObjective = "Master anchored negotiation and value articulation.",
            stepByStepInstructions = listOf(
                "Research market benchmarks and establish your BATNA (Best Alternative to a Negotiated Agreement).",
                "Articulate demonstrated value and impact metrics.",
                "Anchor firmly and comfortably hold pauses after stating your number."
            ),
            completionCriteria = listOf(
                "Negotiation attempted with researched benchmark data.",
                "Maintained composure and professional rapport."
            ),
            reflectionQuestions = listOf(
                "What was your anchor and how did the counterparty respond?",
                "What concessions were exchanged?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards B-Rank XP (65 XP).",
            aiCoachingTip = "Silence is your strongest tactical tool after stating your proposal. Do not bid against yourself.",
            isDeclineable = true
        ),

        // =====================================================================
        // 4. LEADERSHIP & TEAMWORK
        // =====================================================================
        RealWorldSocialMission(
            missionId = "MSN_LEAD_E01",
            module = SocialModuleIdentifier.LEADERSHIP_TEAMWORK,
            difficulty = MissionDifficultyRank.E_RANK,
            title = "Clarify Team Action Items",
            description = "At the end of a group meeting, explicitly summarize action items, owners, and deadlines.",
            estimatedDurationMinutes = 5,
            learningObjective = "Practice proactive operational leadership without formal authority.",
            stepByStepInstructions = listOf(
                "In the final 2 minutes of a meeting, say: 'To make sure we're aligned, let's recap action items.'",
                "State who owns what and by when.",
                "Confirm everyone is clear on their next immediate step."
            ),
            completionCriteria = listOf(
                "Recapped deliverables and owners aloud or in writing.",
                "Confirmed consensus."
            ),
            reflectionQuestions = listOf(
                "Did summarizing reveal any unstated confusion?",
                "How did the team respond to the recap?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards E-Rank XP (25 XP).",
            aiCoachingTip = "Leadership is not a title; it is the willingness to make ambiguity clear.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_LEAD_D02",
            module = SocialModuleIdentifier.LEADERSHIP_TEAMWORK,
            difficulty = MissionDifficultyRank.D_RANK,
            title = "Deliver Radical Candor Feedback",
            description = "Give private, specific, behavior-focused feedback to a peer or teammate.",
            estimatedDurationMinutes = 10,
            learningObjective = "Care personally while challenging directly.",
            stepByStepInstructions = listOf(
                "Find a private moment within 24 hours of the observed behavior.",
                "Describe the specific situation, behavior, and impact (SBI model).",
                "Ask how you can support them in resolving it."
            ),
            completionCriteria = listOf(
                "Feedback was given privately.",
                "Focused on behavior, not character."
            ),
            reflectionQuestions = listOf(
                "How did you ensure the feedback was received supportively?",
                "What behavior was targeted and what next step was agreed upon?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards D-Rank XP (35 XP).",
            aiCoachingTip = "Praise in public, criticize in private. Clear is kind; unclear is unkind.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_LEAD_C03",
            module = SocialModuleIdentifier.LEADERSHIP_TEAMWORK,
            difficulty = MissionDifficultyRank.C_RANK,
            title = "Facilitate Team Retrospective",
            description = "Lead a 20-minute retrospective to analyze what went well and what to improve.",
            estimatedDurationMinutes = 20,
            learningObjective = "Foster psychological safety and continuous team improvement.",
            stepByStepInstructions = listOf(
                "Organize retrospective into: What worked, What held us back, What we will do differently.",
                "Ensure every participant speaks.",
                "Select the top 2 actionable process improvements."
            ),
            completionCriteria = listOf(
                "Facilitated structured team retrospective.",
                "Extracted 2 concrete action items."
            ),
            reflectionQuestions = listOf(
                "How did you handle differing opinions on team bottlenecks?",
                "What action items were adopted?"
            ),
            evidenceRequirements = listOf("Self-reflection or team confirmed feedback."),
            xpEligibilityRules = "Awards C-Rank XP (50 XP).",
            aiCoachingTip = "Focus on the system, not the person. Ask 'What about our process allowed this bug to pass?'",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_LEAD_B04",
            module = SocialModuleIdentifier.LEADERSHIP_TEAMWORK,
            difficulty = MissionDifficultyRank.B_RANK,
            title = "Crisis / Blocker De-escalation",
            description = "Take initiative during a critical delivery blocker or deadline crunch to stabilize the team.",
            estimatedDurationMinutes = 30,
            learningObjective = "Demonstrate calm, decisive crisis leadership under pressure.",
            stepByStepInstructions = listOf(
                "Gather the necessary stakeholders in a war-room huddle.",
                "Calmly assess the ground truth without panic.",
                "Assign isolated investigation paths and communicate status to outside leadership."
            ),
            completionCriteria = listOf(
                "Demonstrated emotional stability.",
                "Established clear triage path."
            ),
            reflectionQuestions = listOf(
                "How did your demeanor affect the room?",
                "What was the resolution timeline?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards B-Rank XP (65 XP).",
            aiCoachingTip = "Your team mirrors your emotional state. If you remain grounded and methodical, panic dissipates.",
            isDeclineable = true
        ),

        // =====================================================================
        // 5. NETWORKING & RELATIONSHIPS
        // =====================================================================
        RealWorldSocialMission(
            missionId = "MSN_NET_E01",
            module = SocialModuleIdentifier.NETWORKING_RELATIONSHIPS,
            difficulty = MissionDifficultyRank.E_RANK,
            title = "Introduce Yourself to Someone New",
            description = "Introduce yourself to one new classmate, colleague, or event attendee.",
            estimatedDurationMinutes = 5,
            learningObjective = "Break social friction and expand professional surface area.",
            stepByStepInstructions = listOf(
                "Approach someone during an informal break or coffee event.",
                "Smile, make eye contact, and introduce your name and project/role.",
                "Ask what they are currently working on."
            ),
            completionCriteria = listOf(
                "Introduced yourself to a new person.",
                "Exchanged basic professional or academic background."
            ),
            reflectionQuestions = listOf(
                "Who did you introduce yourself to?",
                "What did you discuss, and how easy was the conversation starter?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards E-Rank XP (25 XP).",
            aiCoachingTip = "The 3-second rule: when you see someone you'd like to meet, introduce yourself within 3 seconds before hesitation kicks in.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_NET_D02",
            module = SocialModuleIdentifier.NETWORKING_RELATIONSHIPS,
            difficulty = MissionDifficultyRank.D_RANK,
            title = "Thoughtful Question Outreach",
            description = "Approach a speaker, professor, or guest presenter with a specific question about their talk.",
            estimatedDurationMinutes = 10,
            learningObjective = "Engage seniors and mentors with substantive intellectual curiosity.",
            stepByStepInstructions = listOf(
                "Attend a presentation, seminar, or tech talk.",
                "Wait until after the talk or introduce yourself at the podium.",
                "Mention one specific insight from their talk and ask a targeted follow-up question."
            ),
            completionCriteria = listOf(
                "Referenced a specific detail from their presentation.",
                "Asked an insightful question."
            ),
            reflectionQuestions = listOf(
                "What specific topic did you ask about?",
                "How did the speaker respond to your insight?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards D-Rank XP (35 XP).",
            aiCoachingTip = "Do not ask for a job or favor. Ask for their perspective on a technical challenge.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_NET_C03",
            module = SocialModuleIdentifier.NETWORKING_RELATIONSHIPS,
            difficulty = MissionDifficultyRank.C_RANK,
            title = "Personalized Cold Outreach",
            description = "Send a tailored, respectful 3-sentence message to an industry practitioner or mentor seeking 15 minutes of advice.",
            estimatedDurationMinutes = 15,
            learningObjective = "Master high-conversion, respectful professional outreach.",
            stepByStepInstructions = listOf(
                "Research the individual's specific background or recent publication.",
                "Explain why you are reaching out to them specifically.",
                "Make a clear, low-friction ask (e.g. 15-minute phone call on a specific question)."
            ),
            completionCriteria = listOf(
                "Message was personalized and concise.",
                "Included a specific, low-friction question."
            ),
            reflectionQuestions = listOf(
                "How did you personalize your message?",
                "What was their response, or what did you learn about outreach drafting?"
            ),
            evidenceRequirements = listOf("Self-reflection with message outline."),
            xpEligibilityRules = "Awards C-Rank XP (50 XP).",
            aiCoachingTip = "Show that you did your homework. Mention a specific project or article they authored.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_NET_B04",
            module = SocialModuleIdentifier.NETWORKING_RELATIONSHIPS,
            difficulty = MissionDifficultyRank.B_RANK,
            title = "Value-Add Network Maintenance",
            description = "Reconnect with a former colleague, mentor, or friend by sharing a resource relevant to their work.",
            estimatedDurationMinutes = 15,
            learningObjective = "Nurture long-term relationship capital without asking for favors.",
            stepByStepInstructions = listOf(
                "Identify a contact you haven't spoken with in 3+ months.",
                "Find an article, tool, or insight directly applicable to their challenges.",
                "Send a short note: 'Saw this and thought of our conversation about X. Hope you're doing well!'"
            ),
            completionCriteria = listOf(
                "Reached out with zero ask.",
                "Provided genuine value or insight."
            ),
            reflectionQuestions = listOf(
                "Who did you reconnect with, and what resource did you share?",
                "How did they respond?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards B-Rank XP (65 XP).",
            aiCoachingTip = "The best time to network is when you don't need anything.",
            isDeclineable = true
        ),

        // =====================================================================
        // 6. EMOTIONAL & SOCIAL INTELLIGENCE
        // =====================================================================
        RealWorldSocialMission(
            missionId = "MSN_EMO_E01",
            module = SocialModuleIdentifier.EMOTIONAL_INTELLIGENCE,
            difficulty = MissionDifficultyRank.E_RANK,
            title = "Perspective-Taking Reflection",
            description = "During a disagreement, consciously explain the other person's perspective from their shoes before defending yourself.",
            estimatedDurationMinutes = 5,
            learningObjective = "De-center personal ego and build cognitive empathy.",
            stepByStepInstructions = listOf(
                "When someone disagrees with your view, pause.",
                "Say: 'Let me make sure I understand your perspective first: you are concerned that...'",
                "Wait for them to validate that you understood them."
            ),
            completionCriteria = listOf(
                "Summarized their perspective accurately before offering your own.",
                "Other person confirmed accuracy."
            ),
            reflectionQuestions = listOf(
                "How did validating their perspective shift the emotional temperature?",
                "Did their argument make more sense once you articulated it?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards E-Rank XP (25 XP).",
            aiCoachingTip = "Seek first to understand, then to be understood.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_EMO_D02",
            module = SocialModuleIdentifier.EMOTIONAL_INTELLIGENCE,
            difficulty = MissionDifficultyRank.D_RANK,
            title = "Composed Response in Stress",
            description = "When faced with an unexpected criticism or sharp email, pause for 5 minutes before replying.",
            estimatedDurationMinutes = 10,
            learningObjective = "Practice emotional regulation and separate stimulus from response.",
            stepByStepInstructions = listOf(
                "Notice the physical signs of emotional reactivity (heart rate, tension).",
                "Take 3 deep breaths and step away for at least 5 minutes.",
                "Draft a response focusing purely on facts and constructive solutions."
            ),
            completionCriteria = listOf(
                "Paused before responding.",
                "Response was factual, professional, and free of defensive sarcasm."
            ),
            reflectionQuestions = listOf(
                "What triggered the initial emotional surge?",
                "How did pausing change the drafted response?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards D-Rank XP (35 XP).",
            aiCoachingTip = "Between stimulus and response there is a space. In that space is our power to choose our response.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_EMO_C03",
            module = SocialModuleIdentifier.EMOTIONAL_INTELLIGENCE,
            difficulty = MissionDifficultyRank.C_RANK,
            title = "Setting a Firm, Respectful Boundary",
            description = "Communicate a firm personal or professional boundary clearly without over-explaining or apologizing unnecessarily.",
            estimatedDurationMinutes = 10,
            learningObjective = "Assert self-respect and boundaries with emotional composure.",
            stepByStepInstructions = listOf(
                "Identify an unreasonable request or encroaching demand.",
                "State your boundary clearly: 'I cannot take that on right now because my focus is on X.'",
                "Do not invent false excuses or apologize excessively."
            ),
            completionCriteria = listOf(
                "Communicated boundary without hostility.",
                "Remained firm under follow-up pressure."
            ),
            reflectionQuestions = listOf(
                "What boundary did you establish?",
                "Did you feel guilt, and how did you process it?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards C-Rank XP (50 XP).",
            aiCoachingTip = "A boundary is not a wall to punish others; it is a fence to protect your energy and integrity.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_EMO_B04",
            module = SocialModuleIdentifier.EMOTIONAL_INTELLIGENCE,
            difficulty = MissionDifficultyRank.B_RANK,
            title = "De-escalate an Angry Stakeholder",
            description = "Listen to, acknowledge, and defuse an emotionally charged conflict or frustrated stakeholder.",
            estimatedDurationMinutes = 20,
            learningObjective = "Master radical empathy under high emotional confrontation.",
            stepByStepInstructions = listOf(
                "Let the upset person vent without interruption.",
                "Validate their frustration: 'I understand why this delay is deeply frustrating.'",
                "Pivot to collaborative problem solving: 'Here is what we can do immediately to fix this.'"
            ),
            completionCriteria = listOf(
                "Maintained calm vocal tone.",
                "Successfully shifted conversation from anger to solutions."
            ),
            reflectionQuestions = listOf(
                "What tactics prevented you from getting drawn into an argument?",
                "What solution was agreed upon?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards B-Rank XP (65 XP).",
            aiCoachingTip = "When someone is shouting, lower your voice. The contrast forces them to lean in and calm down.",
            isDeclineable = true
        ),

        // =====================================================================
        // 7. PROFESSIONAL EXCELLENCE
        // =====================================================================
        RealWorldSocialMission(
            missionId = "MSN_PRO_E01",
            module = SocialModuleIdentifier.PROFESSIONAL_EXCELLENCE,
            difficulty = MissionDifficultyRank.E_RANK,
            title = "BLUF Status Update",
            description = "Deliver a 3-bullet asynchronous or oral status update using Bottom Line Up Front.",
            estimatedDurationMinutes = 5,
            learningObjective = "Eliminate fluff from daily professional updates.",
            stepByStepInstructions = listOf(
                "State your bottom line in bullet 1: What is completed.",
                "State bullet 2: What is in progress today.",
                "State bullet 3: Any blockers or next milestones."
            ),
            completionCriteria = listOf(
                "Used BLUF format.",
                "Total text under 60 words or spoken under 45 seconds."
            ),
            reflectionQuestions = listOf(
                "Did BLUF make your status easier to read?",
                "What feedback did your team provide?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards E-Rank XP (25 XP).",
            aiCoachingTip = "Executives don't have time to read mystery novels. Give them the conclusion first.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_PRO_D02",
            module = SocialModuleIdentifier.PROFESSIONAL_EXCELLENCE,
            difficulty = MissionDifficultyRank.D_RANK,
            title = "Proactive Delay Communication",
            description = "Communicate an expected deadline slippage 24+ hours in advance with a clear recovery plan.",
            estimatedDurationMinutes = 10,
            learningObjective = "Demonstrate extreme ownership and predictive accountability.",
            stepByStepInstructions = listOf(
                "As soon as you anticipate a delay, do not wait until the deadline.",
                "Notify your manager or team: state the cause, new proposed date, and mitigation actions taken.",
                "Ask if this impacts other dependencies."
            ),
            completionCriteria = listOf(
                "Communicated before deadline arrived.",
                "Provided concrete revised delivery date."
            ),
            reflectionQuestions = listOf(
                "How did early notice prevent downstream panic?",
                "What mitigation did you implement?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards D-Rank XP (35 XP).",
            aiCoachingTip = "Surprise delays destroy trust. Proactive communication builds confidence even when schedules slip.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_PRO_C03",
            module = SocialModuleIdentifier.PROFESSIONAL_EXCELLENCE,
            difficulty = MissionDifficultyRank.C_RANK,
            title = "Run an Efficient 15-Minute Standup",
            description = "Facilitate a structured 15-minute meeting with an agenda and end with clear action items.",
            estimatedDurationMinutes = 15,
            learningObjective = "Master meeting hygiene and timekeeping.",
            stepByStepInstructions = listOf(
                "Share agenda 10 minutes prior.",
                "Start exactly on time.",
                "Keep discussions on track; take off-topic rabbit holes 'offline'.",
                "End on time or early."
            ),
            completionCriteria = listOf(
                "Meeting finished within 15 minutes.",
                "Action items captured."
            ),
            reflectionQuestions = listOf(
                "How did you handle tangents?",
                "Did attendees appreciate ending on time?"
            ),
            evidenceRequirements = listOf("Self-reflection + peer confirmation."),
            xpEligibilityRules = "Awards C-Rank XP (50 XP).",
            aiCoachingTip = "Protect your team's focus. If a meeting has no agenda, it shouldn't happen.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_PRO_B04",
            module = SocialModuleIdentifier.PROFESSIONAL_EXCELLENCE,
            difficulty = MissionDifficultyRank.B_RANK,
            title = "Lead an Executive Architecture Review",
            description = "Present a design or technical decision document to principal engineers or management.",
            estimatedDurationMinutes = 30,
            learningObjective = "Navigate high-stakes technical scrutiny and upward communication.",
            stepByStepInstructions = listOf(
                "Circulate design doc in advance.",
                "Address key risks, scalability, and security tradeoffs up front.",
                "Field questions with calm, evidence-based reasoning."
            ),
            completionCriteria = listOf(
                "Presented to senior leadership.",
                "Documented architecture decisions clearly."
            ),
            reflectionQuestions = listOf(
                "What was the most challenging technical critique?",
                "How did you address it with poise?"
            ),
            evidenceRequirements = listOf("Self-reflection + design document title."),
            xpEligibilityRules = "Awards B-Rank XP (65 XP).",
            aiCoachingTip = "Strong opinions, loosely held. Defend your design with data, but pivot gracefully when superior evidence emerges.",
            isDeclineable = true
        ),

        // =====================================================================
        // 8. LANGUAGE DEVELOPMENT
        // =====================================================================
        RealWorldSocialMission(
            missionId = "MSN_LANG_E01",
            module = SocialModuleIdentifier.LANGUAGE_DEVELOPMENT,
            difficulty = MissionDifficultyRank.E_RANK,
            title = "3-Minute Spoken Summary in Target Language",
            description = "Record or deliver a 3-minute spoken summary of your day in your target language.",
            estimatedDurationMinutes = 5,
            learningObjective = "Build linguistic fluidity and spontaneous sentence production.",
            stepByStepInstructions = listOf(
                "Select your target language (e.g. English, Korean, German, Spanish).",
                "Speak continuously for 3 minutes about your goals today.",
                "Do not pause to look up words in a dictionary; paraphrase around gaps."
            ),
            completionCriteria = listOf(
                "Spoke for at least 2.5 minutes.",
                "Circumlocuted around unfamiliar words without switching languages."
            ),
            reflectionQuestions = listOf(
                "What words did you struggle with, and how did you explain around them?",
                "How did your pronunciation cadence feel?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards E-Rank XP (25 XP).",
            aiCoachingTip = "Fluency is not knowing every word; it's the ability to keep talking when you don't know a word.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_LANG_D02",
            module = SocialModuleIdentifier.LANGUAGE_DEVELOPMENT,
            difficulty = MissionDifficultyRank.D_RANK,
            title = "Live Target Language Interaction",
            description = "Hold a 5-minute live conversation with a peer or native speaker in your target language.",
            estimatedDurationMinutes = 10,
            learningObjective = "Apply conversational vocabulary under real-time interactive pressure.",
            stepByStepInstructions = listOf(
                "Find a language exchange partner, coworker, or classmate.",
                "Discuss a shared interest or current event.",
                "Ask at least two questions to keep the dialogue flowing."
            ),
            completionCriteria = listOf(
                "Conversation lasted 5+ minutes in target language.",
                "Maintained mutual engagement."
            ),
            reflectionQuestions = listOf(
                "What was the conversation topic?",
                "Where did you feel most confident?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards D-Rank XP (35 XP).",
            aiCoachingTip = "Mistakes are proof of practice. Native speakers appreciate the effort far more than perfection.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_LANG_C03",
            module = SocialModuleIdentifier.LANGUAGE_DEVELOPMENT,
            difficulty = MissionDifficultyRank.C_RANK,
            title = "Explain a Technical Topic in Target Language",
            description = "Explain an engineering or complex workflow problem entirely in the target language without switching back.",
            estimatedDurationMinutes = 15,
            learningObjective = "Master domain-specific technical vocabulary and syntactical precision.",
            stepByStepInstructions = listOf(
                "Select a technical problem you solved recently.",
                "Explain the architecture, root cause, and fix in the target language.",
                "Use precise terminology (e.g. concurrency, throughput, latency)."
            ),
            completionCriteria = listOf(
                "Delivered explanation without reverting to native language.",
                "Used at least 5 technical vocabulary terms accurately."
            ),
            reflectionQuestions = listOf(
                "What technical terms did you use?",
                "How did structuring the explanation challenge your syntax?"
            ),
            evidenceRequirements = listOf("Self-reflection."),
            xpEligibilityRules = "Awards C-Rank XP (50 XP).",
            aiCoachingTip = "Think directly in the target language rather than translating from your native language first.",
            isDeclineable = true
        ),
        RealWorldSocialMission(
            missionId = "MSN_LANG_B04",
            module = SocialModuleIdentifier.LANGUAGE_DEVELOPMENT,
            difficulty = MissionDifficultyRank.B_RANK,
            title = "Lead a Discussion in Target Language",
            description = "Facilitate a 15-minute group discussion or presentation completely in your target language.",
            estimatedDurationMinutes = 20,
            learningObjective = "Attain leadership-grade conversational fluency in a non-native language.",
            stepByStepInstructions = listOf(
                "Prepare discussion topics in the target language.",
                "Lead the conversation, encouraging others to participate.",
                "Summarize key conclusions fluently."
            ),
            completionCriteria = listOf(
                "Facilitated 15-minute discussion in target language.",
                "Demonstrated conversational command."
            ),
            reflectionQuestions = listOf(
                "How did you feel facilitating in a non-native tongue?",
                "What linguistic milestones did this achieve?"
            ),
            evidenceRequirements = listOf("Self-reflection + peer confirmation."),
            xpEligibilityRules = "Awards B-Rank XP (65 XP).",
            aiCoachingTip = "Cadence matters more than speed. Speak with steady, deliberate rhythm.",
            isDeclineable = true
        )
    )

    fun getMissionsForModule(module: SocialModuleIdentifier): List<RealWorldSocialMission> {
        return missions.filter { it.module == module }
    }

    fun getMissionsByDifficulty(difficulty: MissionDifficultyRank): List<RealWorldSocialMission> {
        return missions.filter { it.difficulty == difficulty }
    }

    fun getMissionById(missionId: String): RealWorldSocialMission? {
        return missions.firstOrNull { it.missionId == missionId }
    }

    fun getAlternativeMission(missionId: String): RealWorldSocialMission? {
        val mission = getMissionById(missionId) ?: return null
        val altId = mission.alternativeMissionId ?: return null
        return getMissionById(altId)
    }
}
