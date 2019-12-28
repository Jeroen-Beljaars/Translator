package com.example.translator.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.translator.database.language.LanguageDao
import com.example.translator.database.translation.TranslationDao
import com.example.translator.model.Language
import com.example.translator.model.Language.Companion.getSupportedLanguages
import com.example.translator.model.Translation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@TypeConverters(Converters::class)
@Database(entities = [Language::class, Translation::class], version = 1, exportSchema = false)
abstract class TranslatorRoomDatabase : RoomDatabase() {

    abstract fun languageDao(): LanguageDao
    abstract fun translationDao(): TranslationDao

    companion object {
        private const val DATABASE_NAME = "TRANSLATOR_DATABASE"

        @Volatile
        private var INSTANCE: TranslatorRoomDatabase? = null

        fun getDatabase(context: Context): TranslatorRoomDatabase? {
            if (INSTANCE == null) {
                synchronized(TranslatorRoomDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            TranslatorRoomDatabase::class.java, DATABASE_NAME
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