package com.example.data.repository

import com.example.domain.model.ShadowMonarchProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ShadowArmyRepository {
    fun getMonarchProfile(userId: String = ""): Flow<ShadowMonarchProfile> = flowOf(ShadowMonarchProfile())
    suspend fun getMonarchProfileOnce(userId: String = ""): ShadowMonarchProfile = ShadowMonarchProfile()
}
