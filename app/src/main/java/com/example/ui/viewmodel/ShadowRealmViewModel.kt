package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.domain.model.ShadowMonarchProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ShadowRealmUiState(
    val monarchProfile: ShadowMonarchProfile = ShadowMonarchProfile(),
    val shadowEssenceBalance: Int = 100
)

class ShadowRealmViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ShadowRealmUiState())
    val uiState: StateFlow<ShadowRealmUiState> = _uiState.asStateFlow()
}
