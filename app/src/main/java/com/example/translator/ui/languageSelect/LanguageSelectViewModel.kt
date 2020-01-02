package com.example.translator.ui.languageSelect

import android.app.Application
import androidx.lifecycle.*
import com.example.translator.database.language.LanguageRepository
import com.example.translator.model.Language

class LanguageSelectViewModel(application: Application) : AndroidViewModel(application) {
    private val languageRepository =
        LanguageRepository(application.applicationContext)

    val languages: LiveData<List<Language>> = languageRepository.getLanguages()
    val filteredLanguages: MutableLiveData<ArrayList<Language>> = MutableLiveData()

    fun filterLanguages(query: String?) {
        if(query!!.isNotEmpty()) {
            var newFilteredLanguages = ArrayList<Language>()

            languages.value?.forEach {
                if(it.name.contains(query, true)) {
                    newFilteredLanguages.add(it)
                }
            }
            filteredLanguages.apply {
                value = newFilteredLanguages
            }
        }
        else {
            this.filteredLanguages.value!!.clear()
            this.filteredLanguages.apply {
                value = ArrayList(this@LanguageSelectViewModel.languages.value!!)
            }
        }
    }
}