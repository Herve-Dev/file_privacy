package com.hervedev.fileprivacy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hervedev.fileprivacy.data.CredentialStorage
import com.hervedev.fileprivacy.data.WebDavConnectionTester
import com.hervedev.fileprivacy.data.db.AppDatabase
import com.hervedev.fileprivacy.data.db.WebDavConnectionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddWebDavConnectionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val webDavConnectionDao = db.webDavConnectionDao()
    private val credentialStorage = CredentialStorage(application)

    val name = MutableStateFlow("")
    val serverUrl = MutableStateFlow("")
    val port = MutableStateFlow("80")
    val username = MutableStateFlow("")
    val basePath = MutableStateFlow("")
    val password = MutableStateFlow("")

    var isPortManuallyEdited = false
    var isBasePathManuallyEdited = false

    private val _testState = MutableStateFlow<TestConnectionState>(TestConnectionState.Idle)
    val testState: StateFlow<TestConnectionState> = _testState.asStateFlow()

    fun onServerUrlChange(url: String) {
        serverUrl.value = url
        if (!isPortManuallyEdited) {
            val isHttps = url.trim().startsWith("https://", ignoreCase = true)
            port.value = if (isHttps) "443" else "80"
        }
    }

    fun onPortChange(p: String) {
        isPortManuallyEdited = true
        port.value = p
    }

    fun onUsernameChange(user: String) {
        username.value = user
        if (!isBasePathManuallyEdited) {
            val trimmedUser = user.trim()
            basePath.value = if (trimmedUser.isNotEmpty()) "/remote.php/dav/files/$trimmedUser/" else ""
        }
    }

    fun onBasePathChange(path: String) {
        isBasePathManuallyEdited = true
        basePath.value = path
    }

    fun testConnection() {
        val url = serverUrl.value.trim()
        val portInt = port.value.toIntOrNull() ?: (if (url.startsWith("https://", ignoreCase = true)) 443 else 80)
        val path = basePath.value.trim()
        val user = username.value.trim()
        val pwd = password.value

        if (url.isEmpty()) {
            _testState.value = TestConnectionState.Error("Adresse du serveur requise")
            return
        }

        _testState.value = TestConnectionState.Testing
        viewModelScope.launch {
            val result = WebDavConnectionTester.testWebDavConnection(
                serverUrl = url,
                port = portInt,
                basePath = path,
                username = user,
                password = pwd
            )
            if (result.isSuccess) {
                _testState.value = TestConnectionState.Success
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Erreur de connexion WebDAV"
                _testState.value = TestConnectionState.Error(errorMsg)
            }
        }
    }

    fun resetTestState() {
        _testState.value = TestConnectionState.Idle
    }

    fun saveConnection(onSuccess: () -> Unit) {
        val nameVal = name.value.trim().ifEmpty { serverUrl.value.trim() }
        val url = serverUrl.value.trim()
        val portInt = port.value.toIntOrNull() ?: (if (url.startsWith("https://", ignoreCase = true)) 443 else 80)
        val path = basePath.value.trim()
        val user = username.value.trim()
        val pwd = password.value

        if (url.isEmpty()) return

        viewModelScope.launch {
            val entity = WebDavConnectionEntity(
                name = nameVal,
                serverUrl = url,
                port = portInt,
                username = user,
                basePath = path
            )
            val newId = webDavConnectionDao.insert(entity)
            if (pwd.isNotEmpty()) {
                credentialStorage.savePassword(newId, pwd, type = "webdav")
            }
            onSuccess()
        }
    }
}
