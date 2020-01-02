package com.example.translator.api

import com.example.translator.App
import com.example.translator.R
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateRemoteModel
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions

object TranslateApi {

    private val context = App.context
    private val modelManager = FirebaseModelManager.getInstance()

    fun translate(fromLanguage: Int, toLanguage: Int, textToTranslate: String): Task<String> {
        val options = FirebaseTranslatorOptions.Builder()
            .setSourceLanguage(fromLanguage)
            .setTargetLanguage(toLanguage)
            .build()
        val translator = FirebaseNaturalLanguage.getInstance().getTranslator(options)
        return translator.downloadModelIfNeeded().continueWithTask {task ->
            if (task.isSuccessful) {
                translator.translate(textToTranslate)
            } else {
                deleteAllModels()
                Tasks.forException<String>(
                    task.exception
                        ?: Exception(context!!.getString(R.string.unexpected_error))
                )
            }
        }
    }

    private fun deleteAllModels(){
        modelManager.getDownloadedModels(FirebaseTranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                models.forEach {
                    modelManager.deleteDownloadedModel(it)
                }
            }

    }
}
