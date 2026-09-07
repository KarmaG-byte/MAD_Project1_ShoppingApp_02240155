package com.karmagbyte.tshongla

import android.net.Uri
import com.google.firebase.FirebaseApp
import java.io.File
import java.io.FileOutputStream

/** Local image helper for the free-tier classroom/demo build. */
object TshongLaStorage {
    fun uploadProductImage(productId: Int, imageUri: Uri, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        saveLocalImage("product_images", "product_${productId}.jpg", imageUri, onSuccess, onError)
    }

    fun saveCidImage(imageUri: Uri, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        saveLocalImage("identity_images", "cid_${System.currentTimeMillis()}.jpg", imageUri, onSuccess, onError)
    }

    fun saveReviewImage(customerId: String, productId: Int, imageUri: Uri, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        saveLocalImage("review_images", "review_${customerId}_${productId}.jpg", imageUri, onSuccess, onError)
    }

    private fun saveLocalImage(folder: String, fileName: String, imageUri: Uri, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        try {
            val context = FirebaseApp.getInstance().applicationContext
            val imageDir = File(context.filesDir, folder)
            if (!imageDir.exists() && !imageDir.mkdirs()) {
                onError("Unable to create local image folder")
                return
            }
            val destination = File(imageDir, fileName)
            val input = context.contentResolver.openInputStream(imageUri)
            if (input == null) {
                onError("Unable to open the selected image")
                return
            }
            input.use { source -> FileOutputStream(destination, false).use { output -> source.copyTo(output) } }
            onSuccess(Uri.fromFile(destination).toString())
        } catch (error: Exception) {
            onError("Unable to save image locally: ${error.localizedMessage ?: "unknown error"}")
        }
    }
}
