package com.example.translator.ui.favoriteTranslations

import android.app.Application
import androidx.lifecycle.*
import com.example.translator.database.translation.TranslationRepository
import com.example.translator.model.Translation

class FavoriteTranslationsViewModel(application: Application) : AndroidViewModel(application) {
    private val translationRepository =
        TranslationRepository(application.applicationContext)

    val favoriteTranslations: LiveData<List<Translation>> = translationRepository.getFavoriteTranslations()
    val filteredFavoriteTranslations: MutableLiveData<ArrayList<Translation>> = MutableLiveData()

    fun filterTranslations(query: String?) {
        if(query!!.isNotEmpty()) {
            var newFilteredLanguages = ArrayList<Translation>()

            favoriteTranslations.value?.forEach {
                if(it.originalText.contains(query, true) || it.translatedText.contains(query, true)) {
                    newFilteredLanguages.add(it)
                }
            }
            filteredFavoriteTranslations.apply {
                value = newFilteredLanguages
            }
        }
        else {
            this.filteredFavoriteTranslations.value!!.clear()
            this.filteredFavoriteTranslations.apply {
                value = ArrayList(this@FavoriteTranslationsViewModel.favoriteTranslations.value!!)
            }
        }
    }

}