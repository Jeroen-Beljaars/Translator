package com.example.translator.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.translator.model.Language
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LanguageRepository(context: Context) {

    private val languageDao: LanguageDao

    init {
        val database = LanguageRoomDatabase.getDatabase(context)
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