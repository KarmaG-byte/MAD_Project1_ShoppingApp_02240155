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
        // Use a unique filename so an old/cached object reference cannot interfere
        // with a newly selected image. The download URL is requested from the
        // exact StorageReference returned by the completed upload task.
        val fileName = "product_${productId}_${System.currentTimeMillis()}.jpg"
        val ref = storage.reference.child("products/$productId/$fileName")

        ref.putFile(imageUri)
            .addOnSuccessListener { snapshot ->
                snapshot.storage.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        onSuccess(downloadUri.toString())
                    }
                    .addOnFailureListener { error ->
                        onError(
                            "Image uploaded, but its download link could not be created: " +
                                (error.localizedMessage ?: "unknown Storage error")
                        )
                    }
            }
            .addOnFailureListener { error ->
                onError(
                    "Image upload failed: " +
                        (error.localizedMessage ?: "check Firebase Storage setup and rules")
                )
            }
    }
}
