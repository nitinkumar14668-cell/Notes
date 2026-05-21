package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        NoteDatabase::class.java, "note-sync-db"
    ).fallbackToDestructiveMigration().build()

    private val repository = NoteRepository(db.noteDao())

    val notes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentNoteId = MutableStateFlow<Int?>(null)
    val currentNoteId: StateFlow<Int?> = _currentNoteId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentNote: StateFlow<Note?> = _currentNoteId.flatMapLatest { id ->
        if (id != null) repository.getNoteById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentNoteVersions: StateFlow<List<NoteVersion>> = _currentNoteId.flatMapLatest { id ->
        if (id != null) repository.getVersions(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentNoteComments: StateFlow<List<NoteComment>> = _currentNoteId.flatMapLatest { id ->
        if (id != null) repository.getComments(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectNote(id: Int?) {
        _currentNoteId.value = id
    }

    fun saveNote(id: Int?, title: String, content: String) {
        viewModelScope.launch {
            if (id == null) {
                val newId = repository.insert(Note(title = title, content = content))
                repository.addVersion(newId.toInt(), content)
                _currentNoteId.value = newId.toInt()
            } else {
                repository.update(Note(id = id, title = title, content = content))
                repository.addVersion(id, content)
            }
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
            if (_currentNoteId.value == id) {
                _currentNoteId.value = null
            }
        }
    }

    fun addComment(text: String) {
        val noteId = _currentNoteId.value ?: return
        viewModelScope.launch {
            repository.addComment(noteId, text)
        }
    }
    
    fun simulateSync() {
        // Mock sync behavior over all notes
        viewModelScope.launch {
            repository.allNotes.first().forEach { note ->
                if (!note.isSynced) {
                    repository.update(note.copy(isSynced = true))
                }
            }
        }
    }
}
