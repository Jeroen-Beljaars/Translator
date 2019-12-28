package com.example.translator.database.language

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.translator.database.TranslatorRoomDatabase
import com.example.translator.model.Language
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LanguageRepository(context: Context) {

    private val languageDao: LanguageDao

    init {
        val database =
            TranslatorRoomDatabase.getDatabase(
                context
            )
        languageDao = database!!.languageDao()
    }

    fun getLanguages(): LiveData<List<Language>> {
        return languageDao.getLanguages()
    }

    fun updateLanguage(language: Language) {
        CoroutineScope(Dispatchers.IO).launch {
            languageDao.updateLanguage(language)
        }
    }

    fun getSelectedFromLanguage(): LiveData<Language> {
        return languageDao.getSelectedFromLanguage()
    }

    fun getSelectedToLanguage(): LiveData<Language> {
        return languageDao.getSelectedToLanguage()
    }
}