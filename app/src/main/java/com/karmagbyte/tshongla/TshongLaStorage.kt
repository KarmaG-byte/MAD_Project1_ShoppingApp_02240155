package com.karmagbyte.tshongla

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage

object TshongLaStorage {
    private val storage by lazy { FirebaseStorage.getInstance() }

    fun uploadProductImage(
        productId: Int,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val ref = storage.reference.child("products/$productId/main.jpg")
        ref.putFile(imageUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: IllegalStateException("Image upload failed")
                }
                ref.downloadUrl
            }
            .addOnSuccessListener { uri -> onSuccess(uri.toString()) }
            .addOnFailureListener { error ->
                onError(error.localizedMessage ?: "Unable to upload product image")
            }
    }
}
