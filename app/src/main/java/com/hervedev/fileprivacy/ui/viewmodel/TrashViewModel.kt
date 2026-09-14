package com.hervedev.fileprivacy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hervedev.fileprivacy.data.LocalFileSource
import com.hervedev.fileprivacy.data.db.AppDatabase
import com.hervedev.fileprivacy.data.db.TrashEntryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val trashDao = db.trashEntryDao()
    private val localFileSource = LocalFileSource(application)

    val trashEntries: StateFlow<List<TrashEntryEntity>> = trashDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun restoreItem(entry: TrashEntryEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val success = localFileSource.restoreFromTrash(entry.trashPath)
            if (success) {
                onResult(true, "'${entry.fileName}' restauré")
            } else {
                onResult(false, "Échec de la restauration de '${entry.fileName}'")
            }
        }
    }

    fun permanentlyDeleteItem(entry: TrashEntryEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val success = localFileSource.permanentlyDelete(entry.trashPath)
            if (success) {
                onResult(true, "'${entry.fileName}' supprimé définitivement")
            } else {
                onResult(false, "Échec de la suppression de '${entry.fileName}'")
            }
        }
    }

    fun emptyTrash(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val entries = trashEntries.value
            var successCount = 0
            for (entry in entries) {
                if (localFileSource.permanentlyDelete(entry.trashPath)) {
                    successCount++
                }
            }
            onResult(true, "Corbeille vidée ($successCount élément(s) supprimé(s))")
        }
    }
}
