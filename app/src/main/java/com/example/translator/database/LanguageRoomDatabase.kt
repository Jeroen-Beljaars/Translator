package com.example.translator.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.translator.model.Language
import com.example.translator.model.Language.Companion.getSupportedLanguages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@Database(entities = [Language::class], version = 1, exportSchema = false)
abstract class LanguageRoomDatabase : RoomDatabase() {

    abstract fun languageDao(): LanguageDao

    companion object {
        private const val DATABASE_NAME = "LANGUAGE_DATABASE"

        @Volatile
        private var INSTANCE: LanguageRoomDatabase? = null

        fun getDatabase(context: Context): LanguageRoomDatabase? {
            if (INSTANCE == null) {
                synchronized(LanguageRoomDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            LanguageRoomDatabase::class.java, DATABASE_NAME
                        )
                            .fallbackToDestructiveMigration()
                            .addCallback(object : RoomDatabase.Callback() {
                                override fun onCreate(db: SupportSQLiteDatabase) {
                                    super.onCreate(db)
                                    INSTANCE?.let { database ->
                                        CoroutineScope(Dispatchers.IO).launch {
                                            database.languageDao().insertAll(getSupportedLanguages())
                                        }
                                    }
                                }
                            })

                            .build()
                    }
                }
            }
            return INSTANCE
        }
    }
}