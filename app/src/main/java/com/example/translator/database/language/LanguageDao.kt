package com.example.translator.database.language

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.translator.model.Language

@Dao
interface LanguageDao {

    @Query("SELECT * FROM languageTable ORDER BY name")
    fun getLanguages(): LiveData<List<Language>>

    @Query("SELECT * FROM languageTable WHERE languageId = :languageId")
    fun getLanguageById(languageId: Int): LiveData<Language>

    @Query("SELECT * FROM languageTable WHERE selectedAsFromLanguage = 1")
    fun getSelectedFromLanguage(): LiveData<Language>

    @Query("SELECT * FROM languageTable WHERE selectedAsToLanguage = 1")
    fun getSelectedToLanguage(): LiveData<Language>

    @Update
    suspend fun updateLanguage(language: Language)

    @Insert
    suspend fun insertLanguage(language: Language)

    @Insert
    suspend fun insertAll(languageList: List<Language>): List<Long>
}