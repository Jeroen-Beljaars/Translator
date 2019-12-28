package com.example.translator.model

import android.os.Parcelable
import androidx.room.*
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
@Entity(
    tableName = "translationHistoryTable",
    foreignKeys = [
        ForeignKey(
            entity = Language::class,
            parentColumns = ["languageId"],
            childColumns = ["fromLanguageId"]
        ),
        ForeignKey(
            entity = Language::class,
            parentColumns = ["languageId"],
            childColumns = ["toLanguageId"]
        )
    ]
)
data class Translation(
    @ColumnInfo(name = "fromLanguageId")
    var fromLanguageId: Int,

    @ColumnInfo(name = "toLanguageId")
    var toLanguageId: Int,

    @ColumnInfo(name = "originalText")
    var originalText: String,

    @ColumnInfo(name = "translatedText")
    var translatedText: String,

    @ColumnInfo(name = "isFavorite")
    var isFavorite: Boolean = false,

    @ColumnInfo(name = "translationDate")
    val translationDate: Date = Date(),

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "translationId")
    val translationId: Long? = null

) : Parcelable