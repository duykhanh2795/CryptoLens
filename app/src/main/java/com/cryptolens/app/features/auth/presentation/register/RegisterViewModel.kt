package com.cryptolens.app.features.auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.features.auth.domain.usecase.RegisterUseCase
import com.cryptolens.app.core.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val displayName: String          = "",
    val email: String                = "",
    val password: String             = "",
    val confirmPassword: String      = "",
    val isPasswordVisible: Boolean   = false,
    val isConfirmVisible: Boolean    = false,
    val isLoading: Boolean           = false,
    val agreedToTerms: Boolean       = false,
    val displayNameError: String?    = null,
    val emailError: String?          = null,
    val passwordError: String?       = null,
    val confirmPasswordError: String? = null,
)

sealed class RegisterEvent {
    data class DisplayNameChanged(val value: String)     : RegisterEvent()
    data class EmailChanged(val value: String)           : RegisterEvent()
    data class PasswordChanged(val value: String)        : RegisterEvent()
    data class ConfirmPasswordChanged(val value: String) : RegisterEvent()
    data object TogglePasswordVisibility                 : RegisterEvent()
    data object ToggleConfirmVisibility                  : RegisterEvent()
    data object ToggleTerms                              : RegisterEvent()
    data object Submit                                   : RegisterEvent()
}

sealed class RegisterEffect {
    data object NavigateToHome                  : RegisterEffect()
    data class ShowError(val message: String)   : RegisterEffect()
    data class ShowSuccess(val message: String) : RegisterEffect()
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<RegisterEffect>()
    val effect: SharedFlow<RegisterEffect> = _effect.asSharedFlow()

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.DisplayNameChanged ->
                _uiState.update { it.copy(displayName = event.value, displayNameError = null) }
            is RegisterEvent.EmailChanged ->
                _uiState.update { it.copy(email = event.value, emailError = null) }
            is RegisterEvent.PasswordChanged ->
                _uiState.update { it.copy(password = event.value, passwordError = null) }
            is RegisterEvent.ConfirmPasswordChanged ->
                _uiState.update { it.copy(confirmPassword = event.value, confirmPasswordError = null) }
            is RegisterEvent.TogglePasswordVisibility ->
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            is RegisterEvent.ToggleConfirmVisibility ->
                _uiState.update { it.copy(isConfirmVisible = !it.isConfirmVisible) }
            is RegisterEvent.ToggleTerms ->
                _uiState.update { it.copy(agreedToTerms = !it.agreedToTerms) }
            is RegisterEvent.Submit -> handleRegister()
        }
    }

    private fun handleRegister() {
        val s = _uiState.value
        if (!s.agreedToTerms) {
            viewModelScope.launch {
                _effect.emit(RegisterEffect.ShowError("Please agree to the Terms of Service"))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = registerUseCase(s.email, s.password, s.confirmPassword, s.displayName)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(RegisterEffect.ShowSuccess("Account created! Please check your email."))
                    _effect.emit(RegisterEffect.NavigateToHome)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(RegisterEffect.ShowError(result.message))
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
