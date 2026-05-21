package com.cryptolens.app.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.auth.domain.model.AuthUser
import com.cryptolens.app.features.auth.domain.usecase.GetCurrentUserUseCase
import com.cryptolens.app.features.auth.domain.usecase.LogoutUseCase
import com.cryptolens.app.features.auth.domain.usecase.UpdatePasswordUseCase
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
    val showChangePasswordDialog: Boolean = false,
    val isChangingPassword: Boolean = false,
    val currentPassword: String     = "",
    val newPassword: String         = "",
    val confirmPassword: String     = "",
    val changePasswordError: String? = null,
    val appVersion: String          = "1.0.0",
)

sealed class ProfileEvent {
    data object ShowLogoutDialog    : ProfileEvent()
    data object DismissLogoutDialog : ProfileEvent()
    data object ConfirmLogout       : ProfileEvent()
    data object ShowClearPortfolioDialog : ProfileEvent()
    data object DismissClearPortfolioDialog : ProfileEvent()
    data object ConfirmClearPortfolio : ProfileEvent()
    data object ShowChangePasswordDialog : ProfileEvent()
    data object DismissChangePasswordDialog : ProfileEvent()
    data class CurrentPasswordChanged(val value: String) : ProfileEvent()
    data class NewPasswordChanged(val value: String) : ProfileEvent()
    data class ConfirmPasswordChanged(val value: String) : ProfileEvent()
    data object SubmitChangePassword : ProfileEvent()
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
    private val updatePasswordUseCase: UpdatePasswordUseCase,
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
            is ProfileEvent.ShowChangePasswordDialog -> _uiState.update {
                it.copy(
                    showChangePasswordDialog = true,
                    changePasswordError = null,
                )
            }
            is ProfileEvent.DismissChangePasswordDialog -> _uiState.update {
                it.copy(
                    showChangePasswordDialog = false,
                    isChangingPassword = false,
                    currentPassword = "",
                    newPassword = "",
                    confirmPassword = "",
                    changePasswordError = null,
                )
            }
            is ProfileEvent.CurrentPasswordChanged ->
                _uiState.update { it.copy(currentPassword = event.value, changePasswordError = null) }
            is ProfileEvent.NewPasswordChanged ->
                _uiState.update { it.copy(newPassword = event.value, changePasswordError = null) }
            is ProfileEvent.ConfirmPasswordChanged ->
                _uiState.update { it.copy(confirmPassword = event.value, changePasswordError = null) }
            is ProfileEvent.SubmitChangePassword -> handleChangePassword()
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

    private fun handleChangePassword() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update {
                it.copy(isChangingPassword = true, changePasswordError = null)
            }

            when (val result = updatePasswordUseCase(
                currentPassword = state.currentPassword,
                newPassword = state.newPassword,
                confirmPassword = state.confirmPassword,
            )) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            showChangePasswordDialog = false,
                            isChangingPassword = false,
                            currentPassword = "",
                            newPassword = "",
                            confirmPassword = "",
                            changePasswordError = null,
                        )
                    }
                    _effect.emit(ProfileEffect.ShowSuccess("Password updated"))
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isChangingPassword = false,
                            changePasswordError = result.message,
                        )
                    }
                }
                else -> _uiState.update { it.copy(isChangingPassword = false) }
            }
        }
    }

    private fun showComingSoon(feature: String) {
        viewModelScope.launch {
            _effect.emit(ProfileEffect.ShowSuccess("$feature is coming soon"))
        }
    }
}
