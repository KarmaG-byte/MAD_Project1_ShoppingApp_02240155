package com.karmagbyte.tshongla

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class Review(
    val id: String,
    val productId: Int,
    val customerId: String,
    val customerName: String,
    val productRating: Int,
    val businessRating: Int,
    val comment: String,
    val imageUri: String = ""
)

object TshongLaReviews {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val reviews = db.collection("reviews")
    private val products = db.collection("products")

    fun listenToProductReviews(productId: Int, onChange: (List<Review>) -> Unit, onError: (String) -> Unit = {}): ListenerRegistration {
        return reviews.whereEqualTo("productId", productId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.localizedMessage ?: "Unable to load reviews")
                return@addSnapshotListener
            }
            onChange(snapshot?.documents.orEmpty().mapNotNull { doc ->
                val p = doc.getLong("productId")?.toInt() ?: return@mapNotNull null
                Review(
                    id = doc.id,
                    productId = p,
                    customerId = doc.getString("customerId") ?: "",
                    customerName = doc.getString("customerName") ?: "Customer",
                    productRating = doc.getLong("productRating")?.toInt() ?: 0,
                    businessRating = doc.getLong("businessRating")?.toInt() ?: 0,
                    comment = doc.getString("comment") ?: "",
                    imageUri = doc.getString("imageUri") ?: ""
                )
            })
        }
    }

    fun submitReview(
        booking: Booking,
        customerName: String,
        productRating: Int,
        businessRating: Int,
        comment: String,
        imageUri: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (booking.status != "Collected") {
            onError("Only collected purchases can be reviewed.")
            return
        }
        if (productRating !in 1..5 || businessRating !in 1..5) {
            onError("Choose a 1–5 star rating.")
            return
        }
        val reviewId = "${booking.customerId}_${booking.product.id}"
        val reviewRef = reviews.document(reviewId)
        val productRef = products.document(booking.product.id.toString())

        db.runTransaction { tx ->
            val existing = tx.get(reviewRef)
            if (existing.exists()) throw IllegalStateException("You already reviewed this product.")
            val productDoc = tx.get(productRef)
            val oldCount = productDoc.getLong("ratingCount")?.toInt() ?: 0
            val oldSum = productDoc.getDouble("ratingSum") ?: (productDoc.getDouble("rating") ?: 0.0) * oldCount
            val newCount = oldCount + 1
            val newSum = oldSum + productRating
            val newAverage = newSum / newCount

            tx.set(reviewRef, mapOf(
                "productId" to booking.product.id,
                "customerId" to booking.customerId,
                "customerName" to customerName,
                "productRating" to productRating,
                "businessRating" to businessRating,
                "comment" to comment.trim(),
                "imageUri" to imageUri,
                "verifiedPurchase" to true,
                "bookingId" to booking.pickupCode,
                "sellerId" to booking.sellerId,
                "createdAt" to FieldValue.serverTimestamp()
            ))
            if (productDoc.exists()) {
                tx.update(productRef, mapOf(
                    "rating" to newAverage,
                    "ratingCount" to newCount,
                    "ratingSum" to newSum,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
            }
        }.addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.localizedMessage ?: "Unable to submit review") }
    }
}
