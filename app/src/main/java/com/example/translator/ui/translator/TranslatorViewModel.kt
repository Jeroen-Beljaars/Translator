package com.example.translator.ui.translator

import android.app.Application
import androidx.lifecycle.*
import com.example.translator.api.TranslateApi
import com.example.translator.database.language.LanguageRepository
import com.example.translator.database.translation.TranslationRepository
import com.example.translator.model.Language
import com.example.translator.model.TranslateResponse
import com.example.translator.model.Translation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.util.*

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val translateApi = TranslateApi

    private val languageRepository =
        LanguageRepository(this.context)

    private val translationRepository =
        TranslationRepository(this.context)

    val fromLanguage = MediatorLiveData<Language>()
    val toLanguage = MediatorLiveData<Language>()
    val textToTranslate = MutableLiveData<String>()
    val translatedText = MediatorLiveData<TranslateResponse>()

    val translationHistory = MediatorLiveData<List<Translation>>()

    private val processTranslation =
        OnCompleteListener<String> { task ->
            if (task.isSuccessful) {
                translatedText.value = TranslateResponse(task.result, null)
            } else {
                translatedText.value = TranslateResponse(null, task.exception)
            }
        }

    init {
        this.getTranslationHistory()

        fromLanguage.addSource(this.getSelectedFromLanguage()) { fromLanguage ->
            this@TranslatorViewModel.fromLanguage.apply {
                value = fromLanguage
            }
        }

        toLanguage.addSource(this.getSelectedToLanguage()) { toLanguage ->
            this.toLanguage.apply {
                value = toLanguage
            }
        }

        // Automatically translate the text after the user enters some text
        translatedText.addSource(textToTranslate) {translate().addOnCompleteListener(processTranslation)}
        translatedText.addSource(fromLanguage) {
            if(this.textToTranslate.value != "") {
                translate().addOnCompleteListener(processTranslation)
            }
        }
        translatedText.addSource(toLanguage) {
            if(this.textToTranslate.value != "") {
                translate().addOnCompleteListener(processTranslation)
            }
        }
    }

    /**
     * Translate the text to the destination language
     */
    private fun translate(): Task<String> {
        if (this.fromLanguage.value != null && this.toLanguage.value != null && this.textToTranslate.value != null){
            return this.translateApi.translate(
                this.fromLanguage.value!!.languageId,
                this.toLanguage.value!!.languageId,
                this.textToTranslate.value!!
            )
        } else {
            return Tasks.forResult("")
        }
    }

    fun syncTranslationHistory(event: LiveData<Long>) {
        this.translationHistory.addSource(event) {
            getTranslationHistory()
            this.translationHistory.removeSource(event)
        }
    }

    fun storeTranslation(translation: Translation? = null) {
        var translationToAdd = translation
        if (translation == null && this.fromLanguage.value != null && this.toLanguage.value != null &&
            this.textToTranslate.value != null &&
            this.translatedText.value?.error == null &&
            this.translatedText.value?.result != ""){
            translationToAdd = Translation(
                this@TranslatorViewModel.fromLanguage.value!!.languageId,
                this@TranslatorViewModel.toLanguage.value!!.languageId,
                this@TranslatorViewModel.textToTranslate.value!!,
                this@TranslatorViewModel.translatedText.value!!.result!!
            )
        }
        if(translationToAdd != null) {
            CoroutineScope(Dispatchers.Main).async {
                this@TranslatorViewModel.translationRepository.insertTranslation(
                   translationToAdd
                ).await()
                this@TranslatorViewModel.getTranslationHistory()
            }
        }
    }

    private fun getTranslationHistory() {
        val translations = this.translationRepository.getTranslations()
        translationHistory.addSource(translations) { translationHistory ->
            this.translationHistory.apply {
                value = translationHistory
            }
            this.translationHistory.removeSource(translations)
        }
    }

    fun deleteTranslation(translation: Translation) {
        CoroutineScope(Dispatchers.Main).async {
            this@TranslatorViewModel.translationRepository.deleteTranslation(translation).await()
            this@TranslatorViewModel.getTranslationHistory()
        }
    }

    fun favoriteTranslation(translation: Translation){
        CoroutineScope(Dispatchers.Main).async {
            translation.isFavorite = !translation.isFavorite
            translationRepository.updateTranslation(translation).await()
            this@TranslatorViewModel.getTranslationHistory()
        }
    }

    /**
     * Get the fromLanguage from the database
     */
    private fun getSelectedFromLanguage(): LiveData<Language> {
        return languageRepository.getSelectedFromLanguage()
    }

    /**
     * Get the toLanguage from the database
     */
    private fun getSelectedToLanguage(): LiveData<Language> {
        return languageRepository.getSelectedToLanguage()
    }

    /**
     * Change the fromLanguage (origin language)
     *
     * @param language the language in which we want to change the toLanguage
     * @param swapping Are the languages being swapped, if true then don't check for double
     *                 languages
     */
    fun selectFromLanguage(language: Language, swapping: Boolean = false) {
        if (swapping || !this.checkDoubleLanguage(language, true)) {
            // Unselect the old language
            var oldLanguage = this.fromLanguage.value
            oldLanguage!!.selectedAsFromLanguage = false
            languageRepository.updateLanguage(oldLanguage)

            // Select the new language and store that we selected it
            language.selectedAsFromLanguage = true
            languageRepository.updateLanguage(language)
            this.fromLanguage.apply {
                value = language
            }
        }
    }

    /**
     * Change the to language (destination language)
     *
     * @param language the language in which we want to change the toLanguage
     * @param swapping Are the languages being swapped, if true then don't check for double
     *                 languages
     */
    fun selectToLanguage(language: Language, swapping: Boolean = false) {
        if (swapping || !this.checkDoubleLanguage(language, false)) {
            // Unselect the old language
            var oldLanguage = this.toLanguage.value
            oldLanguage!!.selectedAsToLanguage = false
            languageRepository.updateLanguage(oldLanguage)

            // Select the new language and store that we selected it
            language.selectedAsToLanguage = true
            languageRepository.updateLanguage(language)
            this.toLanguage.apply {
                value = language
            }
        }
    }

    /**
     *  Check if the user tries to set the same languages.
     *
     *  @param language:               The language in which the user wants to set either
     *                                 the fromLanguage or toLanguage
     *
     *  @param changingFromLanguage:   is the fromLanguage being changed? if false then the
     *                                 toLanguage is being changed
     */
    private fun checkDoubleLanguage(
        language: Language,
        changingFromLanguage: Boolean = false
    ): Boolean {
        if ((changingFromLanguage && language == this.toLanguage.value) ||
            (!changingFromLanguage && language == this.fromLanguage.value)
        ) {
            this.switchLanguages()
            return true
        }
        return false
    }

    /**
     * Swap the from and to language
     */
    fun switchLanguages() {
        // temporary store the toLanguage
        val tempToLanguage = this.toLanguage.value!!

        selectToLanguage(this.fromLanguage.value!!, true)
        selectFromLanguage(tempToLanguage, true)
    }
}