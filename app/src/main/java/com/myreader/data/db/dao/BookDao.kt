package com.myreader.data.db.dao

import androidx.room.*
import com.myreader.data.db.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY addTime DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isFavorited = 1 ORDER BY addTime DESC")
    fun getFavorites(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE sourceId = :sourceId AND sourceUrl = :sourceUrl LIMIT 1")
    suspend fun getBySource(sourceId: String, sourceUrl: String): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE title LIKE '%' || :keyword || '%' OR author LIKE '%' || :keyword || '%'")
    suspend fun searchLocal(keyword: String): List<BookEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Query("UPDATE books SET lastReadChapterIndex = :chapterIndex, lastReadPosition = :position WHERE id = :bookId")
    suspend fun updateProgress(bookId: Long, chapterIndex: Int, position: Long)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: Long)
}
