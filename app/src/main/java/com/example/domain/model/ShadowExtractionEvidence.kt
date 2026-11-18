package com.example.domain.model

enum class ShadowEvidenceEligibility {
    ELIGIBLE,
    ALREADY_EXTRACTED,
    INCOMPLETE,
    NOT_VERIFIED,
    WRONG_ACCOUNT,
    SOURCE_UNAVAILABLE
}

data class ShadowExtractionEvidenceItem(
    val sourceRecordId: String,
    val accountOwnerId: String,
    val sourceType: ShadowExtractionSourceType,
    val title: String,
    val description: String,
    val timestampEpochMs: Long,
    val verificationStatus: String,
    val eligibility: ShadowEvidenceEligibility,
    val eligibilityReason: String,
    val rewardSummary: String
)
