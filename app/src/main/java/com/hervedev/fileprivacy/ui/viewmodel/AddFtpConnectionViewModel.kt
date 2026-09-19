package com.hervedev.fileprivacy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hervedev.fileprivacy.data.CredentialStorage
import com.hervedev.fileprivacy.data.FtpConnectionTester
import com.hervedev.fileprivacy.data.db.AppDatabase
import com.hervedev.fileprivacy.data.db.FtpConnectionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddFtpConnectionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val ftpConnectionDao = db.ftpConnectionDao()
    private val credentialStorage = CredentialStorage(application)

    val name = MutableStateFlow("")
    val serverAddress = MutableStateFlow("")
    val port = MutableStateFlow("21")
    val username = MutableStateFlow("")
    val password = MutableStateFlow("")
    val useFtps = MutableStateFlow(false)

    private val _testState = MutableStateFlow<TestConnectionState>(TestConnectionState.Idle)
    val testState: StateFlow<TestConnectionState> = _testState.asStateFlow()

    fun onFtpsToggle(enabled: Boolean) {
        useFtps.value = enabled
        if (enabled && port.value == "21") {
            port.value = "990"
        } else if (!enabled && port.value == "990") {
            port.value = "21"
        }
    }

    fun testConnection() {
        val server = serverAddress.value.trim()
        val portInt = port.value.toIntOrNull() ?: 21
        val user = username.value.trim()
        val pwd = password.value
        val ftps = useFtps.value

        if (server.isEmpty()) {
            _testState.value = TestConnectionState.Error("Adresse du serveur requise")
            return
        }

        _testState.value = TestConnectionState.Testing
        viewModelScope.launch {
            val result = FtpConnectionTester.testFtpConnection(
                serverAddress = server,
                port = portInt,
                username = user,
                password = pwd,
                useFtps = ftps
            )
            if (result.isSuccess) {
                _testState.value = TestConnectionState.Success
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Erreur de connexion"
                _testState.value = TestConnectionState.Error(errorMsg)
            }
        }
    }

    fun resetTestState() {
        _testState.value = TestConnectionState.Idle
    }

    fun saveConnection(onSuccess: () -> Unit) {
        val nameVal = name.value.trim().ifEmpty { serverAddress.value.trim() }
        val server = serverAddress.value.trim()
        val portInt = port.value.toIntOrNull() ?: (if (useFtps.value) 990 else 21)
        val user = username.value.trim()
        val pwd = password.value
        val ftps = useFtps.value

        if (server.isEmpty()) return

        viewModelScope.launch {
            val entity = FtpConnectionEntity(
                name = nameVal,
                serverAddress = server,
                port = portInt,
                username = user,
                useFtps = ftps
            )
            val newId = ftpConnectionDao.insert(entity)
            if (pwd.isNotEmpty()) {
                credentialStorage.savePassword(newId, pwd, type = "ftp")
            }
            onSuccess()
        }
    }
}
