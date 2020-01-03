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

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val translateApi = TranslateApi

    private val languageRepository =
        LanguageRepository(this.context)

    private val translationRepository =
        TranslationRepository(this.context)

    var liveTranslation = false

    val fromLanguage = MediatorLiveData<Language>()
    val toLanguage = MediatorLiveData<Language>()
    val textToTranslate = MutableLiveData<String>()
    val translatedText = MediatorLiveData<TranslateResponse>()

    val translationHistory = MediatorLiveData<List<Translation>>()

    val selectedTranslation = MediatorLiveData<Translation>()
    var editModeEnabledForTranslation: Translation? = null

    val loading = MutableLiveData<Boolean>()

    private val processTranslation =
        OnCompleteListener<String> { task ->
            this.loading.apply {
                value = false
            }
            if (task.isSuccessful) {
                translatedText.value = TranslateResponse(task.result, null)
            } else {
                translatedText.value = TranslateResponse(null, task.exception)
            }
        }

    init {
        this.getTranslationHistory()

        this.getSelectedLanguages()

        this.toggleLiveTranslation(true)
    }

    /**
     * Translate the text to the destination language
     */
    private fun translate(): Task<String> {
        if (this.fromLanguage.value != null && this.toLanguage.value != null && this.textToTranslate.value != null) {
            this.loading.apply {
                value = true
            }
            return this.translateApi.translate(
                this.fromLanguage.value!!.languageId,
                this.toLanguage.value!!.languageId,
                this.textToTranslate.value!!
            )
        } else {
            return Tasks.forResult("")
        }
    }

    /**
     * Select a translation that the user wants to edit
     *
     * @param translation The translation object which the user just selected
     */
    fun selectTranslation(translation: Translation) {
        // Disable live translation so we can select the provided translation properly
        this.toggleLiveTranslation(false)
        this.getSelectedLanguages(translation.fromLanguageId, translation.toLanguageId)
        this.selectedTranslation.apply {
            value = translation
        }
        this.textToTranslate.apply {
            value = translation.originalText
        }
        this.translatedText.apply {
            value = TranslateResponse(translation.translatedText, null)
        }
    }

    /**
     * Select a fromLanguage and a toLanguage
     * The params are nullable. If no ID has been provided we will get the last selected
     * language from the db
     *
     * @param fromLanguageId    the id of the fromLanguage which we want to select
     * @param toLanguageId      the id of the toLanguage which we want to select
     *
     */
    fun getSelectedLanguages(fromLanguageId: Int? = null, toLanguageId: Int? = null) {
        val selectedFromLanguage =
            if (fromLanguageId == null) this.getSelectedFromLanguage() else this.languageRepository.getLanguageById(
                fromLanguageId
            )

        fromLanguage.addSource(selectedFromLanguage) { fromLanguage ->
            if (fromLanguage == null) {
                // couldn't fetch the fromLanguage. Try it again..
                this@TranslatorViewModel.fromLanguage.removeSource(selectedFromLanguage)
                this@TranslatorViewModel.getSelectedLanguages()
            } else {
                this@TranslatorViewModel.fromLanguage.apply {
                    value = fromLanguage
                }
                val selectedToLanguage =
                    if (toLanguageId == null) this.getSelectedToLanguage() else this.languageRepository.getLanguageById(
                        toLanguageId
                    )
                toLanguage.addSource(selectedToLanguage) { toLanguage ->
                    this@TranslatorViewModel.toLanguage.apply {
                        value = toLanguage
                    }
                    this@TranslatorViewModel.toggleLiveTranslation(true)
                    this@TranslatorViewModel.toLanguage.removeSource(selectedToLanguage)
                }
                this@TranslatorViewModel.fromLanguage.removeSource(selectedFromLanguage)
            }
        }
    }

    /**
     * Disable or enable live translation
     *
     * @param enable should or shoudln't live translation be enabled
     */
    fun toggleLiveTranslation(enable: Boolean = true) {
        // Automatically translate the text after the user enters some text
        if (enable && !liveTranslation){
            liveTranslation = true
            translatedText.addSource(textToTranslate) {
                translate().addOnCompleteListener(
                    processTranslation
                )
            }
            translatedText.addSource(fromLanguage) {
                if (this.textToTranslate.value != "") {
                    translate().addOnCompleteListener(processTranslation)
                }
            }
            translatedText.addSource(toLanguage) {
                if (this.textToTranslate.value != "") {
                    translate().addOnCompleteListener(processTranslation)
                }
            }
        } else if(!enable && liveTranslation) {
            liveTranslation = false
            translatedText.removeSource(textToTranslate)
            translatedText.removeSource(fromLanguage)
            translatedText.removeSource(toLanguage)
        }

    }

    /**
     * Try to create a new translation. If it already exists update the existing one
     *
     * @param translation The translation that should be created or updated
     * if translation is null we will create a new one using the values set in the viewModel
     */
    fun createOrUpdateTranslation(translation: Translation? = null) {
        var translationToAddOrUpdate = translation
        if (this.selectedTranslation.value != null && translationToAddOrUpdate == null) {
            translationToAddOrUpdate = this.selectedTranslation.value!!
            translationToAddOrUpdate.fromLanguageId = this.fromLanguage.value!!.languageId
            translationToAddOrUpdate.toLanguageId = this.toLanguage.value!!.languageId
            translationToAddOrUpdate.originalText = this.textToTranslate.value!!
            translationToAddOrUpdate.translatedText = this.translatedText.value!!.result!!
            this.selectedTranslation.value = null
        }

        else if (translationToAddOrUpdate == null && this.fromLanguage.value != null && this.toLanguage.value != null &&
            this.textToTranslate.value != null &&
            this.translatedText.value?.error == null &&
            this.translatedText.value?.result != ""
        ) {
            translationToAddOrUpdate = Translation(
                this.fromLanguage.value!!.languageId,
                this.toLanguage.value!!.languageId,
                this.textToTranslate.value!!,
                this.translatedText.value!!.result!!
            )
        }
        if (translationToAddOrUpdate != null) {
            CoroutineScope(Dispatchers.Main).async {
                this@TranslatorViewModel.translationRepository.insertTranslation(
                    translationToAddOrUpdate
                ).await()
                this@TranslatorViewModel.getTranslationHistory()
            }
        }
    }

    /**
     * Get the translation history
     */
    private fun getTranslationHistory() {
        val translations = this.translationRepository.getTranslations()
        translationHistory.addSource(translations) { translationHistory ->
            this.translationHistory.apply {
                value = translationHistory
            }
            this.translationHistory.removeSource(translations)
        }
    }

    /**
     * Delete a translation
     */
    fun deleteTranslation(translation: Translation) {
        CoroutineScope(Dispatchers.Main).async {
            this@TranslatorViewModel.translationRepository.deleteTranslation(translation).await()
            this@TranslatorViewModel.getTranslationHistory()
        }
    }

    /**
     * Favorite or unfavorite a translation
     *
     * @param translation The translation which want to favorite/unfavorite
     */
    fun favoriteTranslation(translation: Translation) {
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