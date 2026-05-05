package com.cryptolens.app.features.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.features.auth.domain.usecase.LoginUseCase
import com.cryptolens.app.features.auth.domain.usecase.ResetPasswordUseCase
import com.cryptolens.app.core.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String          = "",
    val password: String       = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean     = false,
    val emailError: String?    = null,
    val passwordError: String? = null,
)

sealed class LoginEvent {
    data class EmailChanged(val value: String)    : LoginEvent()
    data class PasswordChanged(val value: String) : LoginEvent()
    data object TogglePasswordVisibility          : LoginEvent()
    data object Submit                            : LoginEvent()
    data class ForgotPassword(val email: String)  : LoginEvent()
}

sealed class LoginEffect {
    data object NavigateToHome                    : LoginEffect()
    data class ShowError(val message: String)     : LoginEffect()
    data class ShowSuccess(val message: String)   : LoginEffect()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<LoginEffect>()
    val effect: SharedFlow<LoginEffect> = _effect.asSharedFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged ->
                _uiState.update { it.copy(email = event.value, emailError = null) }
            is LoginEvent.PasswordChanged ->
                _uiState.update { it.copy(password = event.value, passwordError = null) }
            is LoginEvent.TogglePasswordVisibility ->
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            is LoginEvent.Submit -> handleLogin()
            is LoginEvent.ForgotPassword -> handleForgotPassword(event.email)
        }
    }

    private fun handleLogin() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = loginUseCase(state.email, state.password)) {
                is Resource.Success -> _effect.emit(LoginEffect.NavigateToHome)
                is Resource.Error   -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(LoginEffect.ShowError(result.message))
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun handleForgotPassword(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = resetPasswordUseCase(email)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(LoginEffect.ShowSuccess("Reset email sent! Check your inbox."))
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(LoginEffect.ShowError(result.message))
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
