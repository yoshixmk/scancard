package com.example.scancard.data.ml

import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TextRecognitionManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

    suspend fun recognizeText(imageUri: Uri): String {
        val image = InputImage.fromFilePath(context, imageUri)
        val result = recognizer.process(image).await()
        return result.text
    }
}
