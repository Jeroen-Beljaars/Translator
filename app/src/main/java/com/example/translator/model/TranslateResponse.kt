package com.example.translator.model

data class TranslateResponse(
    var result: String?,
    var error: Exception?
)