package com.example.farmproject.ui.auth.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmproject.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SignUpUiState {
    data object Idle : SignUpUiState
    data object Loading : SignUpUiState
    data object Success : SignUpUiState
    data class Error(val message: String) : SignUpUiState
}

class ManagerSignUpViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun signUp(
        fullName: String,
        email: String,
        phoneNumber: String,
        password: String
    ) {
        viewModelScope.launch {
            _uiState.value = SignUpUiState.Loading
            val result = repository.signUpManager(
                fullName = fullName,
                email = email,
                phoneNumber = phoneNumber.ifBlank { null },
                password = password
            )
            result.fold(
                onSuccess = {
                    _uiState.value = SignUpUiState.Success
                },
                onFailure = { error ->
                    _uiState.value = SignUpUiState.Error(
                        error.message ?: "Sign up failed. Please try again."
                    )
                }
            )
        }
    }

    @Suppress("unused")
    fun resetState() {
        _uiState.value = SignUpUiState.Idle
    }
}
