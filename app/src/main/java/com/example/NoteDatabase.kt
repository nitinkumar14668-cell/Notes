package com.example

import androidx.room.*
import androidx.room.OnConflictStrategy.Companion.REPLACE
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "note_versions",
    foreignKeys = [ForeignKey(entity = Note::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["noteId"])]
)
data class NoteVersion(
    @PrimaryKey(autoGenerate = true) val versionId: Int = 0,
    val noteId: Int,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "note_comments",
    foreignKeys = [ForeignKey(entity = Note::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["noteId"])]
)
data class NoteComment(
    @PrimaryKey(autoGenerate = true) val commentId: Int = 0,
    val noteId: Int,
    val text: String,
    val author: String = "Me",
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: Int): Flow<Note?>

    @Insert(onConflict = REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    // Versions
    @Insert(onConflict = REPLACE)
    suspend fun insertVersion(version: NoteVersion)
    
    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY timestamp DESC")
    fun getNoteVersions(noteId: Int): Flow<List<NoteVersion>>

    // Comments
    @Insert(onConflict = REPLACE)
    suspend fun insertComment(comment: NoteComment)
    
    @Query("SELECT * FROM note_comments WHERE noteId = :noteId ORDER BY timestamp ASC")
    fun getNoteComments(noteId: Int): Flow<List<NoteComment>>
}

@Database(entities = [Note::class, NoteVersion::class, NoteComment::class], version = 2, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()
    
    fun getNoteById(id: Int): Flow<Note?> = noteDao.getNoteById(id)

    suspend fun insert(note: Note): Long = noteDao.insertNote(note)
    suspend fun update(note: Note) = noteDao.updateNote(note)
    suspend fun deleteById(id: Int) = noteDao.deleteNoteById(id)

    suspend fun addVersion(noteId: Int, content: String) = noteDao.insertVersion(NoteVersion(noteId = noteId, content = content))
    fun getVersions(noteId: Int) = noteDao.getNoteVersions(noteId)

    suspend fun addComment(noteId: Int, text: String, author: String = "Me") = noteDao.insertComment(NoteComment(noteId = noteId, text = text, author = author))
    fun getComments(noteId: Int) = noteDao.getNoteComments(noteId)
}
