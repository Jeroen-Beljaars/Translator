package com.example.translator.database.translation

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.translator.model.Language
import com.example.translator.model.Translation

@Dao
interface TranslationDao {

    @Query("SELECT * FROM translationHistoryTable ORDER BY translationDate DESC")
    fun getTranslationHistory(): LiveData<List<Translation>>

    @Update
    suspend fun updateTranslation(translation: Translation)

    @Insert
    suspend fun insertTranslation(translation: Translation)

    @Delete
    suspend fun deleteTranslation(translation: Translation)

}