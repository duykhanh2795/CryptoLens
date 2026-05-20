package com.cryptolens.app.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.auth.domain.model.AuthUser
import com.cryptolens.app.features.auth.domain.usecase.GetCurrentUserUseCase
import com.cryptolens.app.features.auth.domain.usecase.LogoutUseCase
import com.cryptolens.app.features.portfolio.domain.usecase.ClearPortfolioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: AuthUser?             = null,
    val isLoading: Boolean          = false,
    val showLogoutDialog: Boolean   = false,
    val showClearPortfolioDialog: Boolean = false,
    val appVersion: String          = "1.0.0",
)

sealed class ProfileEvent {
    data object ShowLogoutDialog    : ProfileEvent()
    data object DismissLogoutDialog : ProfileEvent()
    data object ConfirmLogout       : ProfileEvent()
    data object ShowClearPortfolioDialog : ProfileEvent()
    data object DismissClearPortfolioDialog : ProfileEvent()
    data object ConfirmClearPortfolio : ProfileEvent()
    data class ComingSoon(val feature: String) : ProfileEvent()
}

sealed class ProfileEffect {
    data object NavigateToLogin             : ProfileEffect()
    data class ShowError(val message: String) : ProfileEffect()
    data class ShowSuccess(val message: String) : ProfileEffect()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val clearPortfolio: ClearPortfolioUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ProfileEffect>()
    val effect: SharedFlow<ProfileEffect> = _effect.asSharedFlow()

    init {
        _uiState.update { it.copy(user = getCurrentUser()) }
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.ShowLogoutDialog    -> _uiState.update { it.copy(showLogoutDialog = true) }
            is ProfileEvent.DismissLogoutDialog -> _uiState.update { it.copy(showLogoutDialog = false) }
            is ProfileEvent.ConfirmLogout       -> handleLogout()
            is ProfileEvent.ShowClearPortfolioDialog ->
                _uiState.update { it.copy(showClearPortfolioDialog = true) }
            is ProfileEvent.DismissClearPortfolioDialog ->
                _uiState.update { it.copy(showClearPortfolioDialog = false) }
            is ProfileEvent.ConfirmClearPortfolio -> handleClearPortfolio()
            is ProfileEvent.ComingSoon -> showComingSoon(event.feature)
        }
    }

    private fun handleLogout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showLogoutDialog = false) }
            when (val result = logoutUseCase()) {
                is Resource.Success -> _effect.emit(ProfileEffect.NavigateToLogin)
                is Resource.Error   -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(ProfileEffect.ShowError(result.message))
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun handleClearPortfolio() {
        viewModelScope.launch {
            val userId = getCurrentUser()?.id.orEmpty()
            if (userId.isBlank()) {
                _uiState.update { it.copy(showClearPortfolioDialog = false) }
                _effect.emit(ProfileEffect.ShowError("Please log in before clearing portfolio data"))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, showClearPortfolioDialog = false) }
            when (val result = clearPortfolio(userId)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(ProfileEffect.ShowSuccess("Portfolio cleared"))
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(ProfileEffect.ShowError(result.message))
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun showComingSoon(feature: String) {
        viewModelScope.launch {
            _effect.emit(ProfileEffect.ShowSuccess("$feature is coming soon"))
        }
    }
}
