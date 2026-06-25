package com.myreader.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.myreader.data.db.dao.BookDao
import com.myreader.data.db.dao.ChapterDao
import com.myreader.data.db.entity.BookEntity
import com.myreader.data.db.entity.ChapterEntity

@Database(
    entities = [BookEntity::class, ChapterEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao

    companion object {
        fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "myreader.db"
            ).build()
        }
    }
}
