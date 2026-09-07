package com.karmagbyte.tshongla

import android.net.Uri
import com.google.firebase.FirebaseApp
import java.io.File
import java.io.FileOutputStream

/**
 * Local image helper used for the free-tier classroom/demo build.
 *
 * Product images are copied into the app's private internal storage so they
 * continue to work after the gallery picker closes and after the app restarts.
 * The resulting local file URI is stored with the Firestore product document.
 *
 * Limitation: that URI only exists on this device, so another device will not
 * automatically be able to display the same image. The rest of the product
 * data still synchronizes through Firestore normally.
 */
object TshongLaStorage {

    fun uploadProductImage(
        productId: Int,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val context = FirebaseApp.getInstance().applicationContext
            val imageDir = File(context.filesDir, "product_images")
            if (!imageDir.exists() && !imageDir.mkdirs()) {
                onError("Unable to create local image folder")
                return
            }

            val destination = File(imageDir, "product_${productId}.jpg")
            val input = context.contentResolver.openInputStream(imageUri)
            if (input == null) {
                onError("Unable to open the selected image")
                return
            }

            input.use { source ->
                FileOutputStream(destination, false).use { output ->
                    source.copyTo(output)
                }
            }

            onSuccess(Uri.fromFile(destination).toString())
        } catch (error: Exception) {
            onError("Unable to save product image locally: ${error.localizedMessage ?: "unknown error"}")
        }
    }
}
