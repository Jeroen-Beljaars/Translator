package com.example.translator.database.translation

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.translator.database.TranslatorRoomDatabase
import com.example.translator.model.Language
import com.example.translator.model.Translation
import kotlinx.coroutines.*

class TranslationRepository(context: Context) {

    private val translationDao: TranslationDao

    init {
        val database =
            TranslatorRoomDatabase.getDatabase(
                context
            )
        translationDao = database!!.translationDao()
    }

    fun getTranslations(): LiveData<List<Translation>> {
        return translationDao.getTranslationHistory()
    }

    fun insertTranslation(translation: Translation) : Deferred<Unit> {
        return CoroutineScope(Dispatchers.IO).async {
            translationDao.insertTranslation(translation)
        }
    }

    fun updateTranslation(translation: Translation) : Deferred<Unit> {
        return CoroutineScope(Dispatchers.IO).async {
            translationDao.updateTranslation(translation)
        }
    }

    fun deleteTranslation(translation: Translation) : Deferred<Unit> {
        return CoroutineScope(Dispatchers.IO).async {
            translationDao.deleteTranslation(translation)
        }
    }
}