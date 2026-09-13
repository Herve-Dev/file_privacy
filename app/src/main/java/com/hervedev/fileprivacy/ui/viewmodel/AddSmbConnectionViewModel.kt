package com.hervedev.fileprivacy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hervedev.fileprivacy.data.CredentialStorage
import com.hervedev.fileprivacy.data.SmbConnectionTester
import com.hervedev.fileprivacy.data.db.AppDatabase
import com.hervedev.fileprivacy.data.db.SmbConnectionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TestConnectionState {
    object Idle : TestConnectionState()
    object Testing : TestConnectionState()
    object Success : TestConnectionState()
    data class Error(val message: String) : TestConnectionState()
}

class AddSmbConnectionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val smbConnectionDao = db.smbConnectionDao()
    private val credentialStorage = CredentialStorage(application)

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _serverAddress = MutableStateFlow("")
    val serverAddress: StateFlow<String> = _serverAddress.asStateFlow()

    private val _shareName = MutableStateFlow("")
    val shareName: StateFlow<String> = _shareName.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _port = MutableStateFlow("445")
    val port: StateFlow<String> = _port.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _testState = MutableStateFlow<TestConnectionState>(TestConnectionState.Idle)
    val testState: StateFlow<TestConnectionState> = _testState.asStateFlow()

    fun onNameChange(value: String) {
        _name.value = value
        _errorMessage.value = null
    }

    fun onServerAddressChange(value: String) {
        _serverAddress.value = value
        _errorMessage.value = null
        _testState.value = TestConnectionState.Idle
    }

    fun onShareNameChange(value: String) {
        _shareName.value = value
        _errorMessage.value = null
        _testState.value = TestConnectionState.Idle
    }

    fun onUsernameChange(value: String) {
        _username.value = value
        _testState.value = TestConnectionState.Idle
    }

    fun onPasswordChange(value: String) {
        _password.value = value
        _testState.value = TestConnectionState.Idle
    }

    fun onPortChange(value: String) {
        _port.value = value
        _testState.value = TestConnectionState.Idle
    }

    fun testConnection() {
        val trimmedAddress = _serverAddress.value.trim()
        val trimmedShare = _shareName.value.trim().removePrefix("/").removeSuffix("/")

        if (trimmedAddress.isEmpty() || trimmedShare.isEmpty()) {
            _testState.value = TestConnectionState.Error("Veuillez saisir au moins l'adresse du serveur et le nom du partage pour tester.")
            return
        }

        val portNumber = _port.value.trim().toIntOrNull() ?: 445

        viewModelScope.launch {
            _testState.value = TestConnectionState.Testing
            val result = SmbConnectionTester.testSmbConnection(
                serverAddress = trimmedAddress,
                shareName = trimmedShare,
                username = _username.value.trim(),
                password = _password.value,
                port = portNumber
            )

            result.fold(
                onSuccess = {
                    _testState.value = TestConnectionState.Success
                },
                onFailure = { exception ->
                    _testState.value = TestConnectionState.Error(
                        exception.message ?: "Échec de la connexion"
                    )
                }
            )
        }
    }

    fun resetTestState() {
        _testState.value = TestConnectionState.Idle
    }

    fun saveConnection(onSuccess: () -> Unit) {
        val trimmedName = _name.value.trim()
        val trimmedAddress = _serverAddress.value.trim()
        val trimmedShare = _shareName.value.trim().removePrefix("/").removeSuffix("/")

        if (trimmedName.isEmpty() || trimmedAddress.isEmpty() || trimmedShare.isEmpty()) {
            _errorMessage.value = "Les champs 'Nom', 'Adresse' et 'Partage' sont obligatoires"
            return
        }

        val portNumber = _port.value.trim().toIntOrNull() ?: 445

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val entity = SmbConnectionEntity(
                    name = trimmedName,
                    serverAddress = trimmedAddress,
                    shareName = trimmedShare,
                    username = _username.value.trim(),
                    port = portNumber
                )
                val generatedId = smbConnectionDao.insert(entity)

                val pwd = _password.value
                if (pwd.isNotEmpty()) {
                    credentialStorage.savePassword(generatedId, pwd)
                }

                _isSaving.value = false
                onSuccess()
            } catch (e: Exception) {
                _isSaving.value = false
                _errorMessage.value = "Erreur lors de l'enregistrement : ${e.localizedMessage}"
            }
        }
    }
}
