package com.karmagbyte.tshongla

import androidx.compose.ui.graphics.Color
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Central Firestore data layer for TshongLa.
 * UI code observes this repository instead of keeping permanent data only in memory.
 */
object TshongLaFirestore {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val products = db.collection("products")
    private val bookings = db.collection("bookings")

    fun listenToProducts(
        onChange: (List<Product>) -> Unit,
        onError: (String) -> Unit = {}
    ): ListenerRegistration {
        return products.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.localizedMessage ?: "Unable to load products")
                return@addSnapshotListener
            }
            val result = snapshot?.documents.orEmpty().mapNotNull(::productFromDocument)
                .sortedBy { it.id }
            onChange(result)
        }
    }

    fun listenToBookings(
        onChange: (List<Booking>) -> Unit,
        onError: (String) -> Unit = {}
    ): ListenerRegistration {
        return bookings.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.localizedMessage ?: "Unable to load bookings")
                return@addSnapshotListener
            }
            val result = snapshot?.documents.orEmpty().mapNotNull(::bookingFromDocument)
            onChange(result)
        }
    }

    fun seedProductsIfEmpty(seed: List<Product>) {
        products.limit(1).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    val batch = db.batch()
                    seed.forEach { product ->
                        batch.set(products.document(product.id.toString()), productToMap(product))
                    }
                    batch.commit()
                }
            }
    }

    fun saveProduct(
        product: Product,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        products.document(product.id.toString())
            .set(productToMap(product))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.localizedMessage ?: "Unable to save product") }
    }

    fun deleteProduct(
        product: Product,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        products.document(product.id.toString())
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.localizedMessage ?: "Unable to delete product") }
    }

    /**
     * Creates a booking and decreases stock in one Firestore transaction.
     * This prevents two customers from reserving stock that is no longer available.
     */
    fun createBooking(
        product: Product,
        quantity: Int,
        onSuccess: (Booking) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (quantity <= 0) {
            onError("Quantity must be at least 1")
            return
        }

        val pickupCode = "TSH-${System.currentTimeMillis().toString().takeLast(8)}"
        val bookingRef = bookings.document(pickupCode)
        val productRef = products.document(product.id.toString())
        val booking = Booking(product, quantity, pickupCode, "Reserved")

        db.runTransaction { transaction ->
            val productSnapshot = transaction.get(productRef)
            val currentStock = productSnapshot.getLong("stock")?.toInt() ?: 0
            if (currentStock < quantity) {
                throw IllegalStateException("Only $currentStock item(s) are currently available")
            }

            transaction.update(productRef, "stock", currentStock - quantity)
            transaction.set(bookingRef, bookingToMap(booking))
        }.addOnSuccessListener { onSuccess(booking) }
            .addOnFailureListener { onError(it.localizedMessage ?: "Unable to reserve item") }
    }

    /**
     * Updates reservation status. Cancelling an active booking also restores its stock.
     */
    fun updateBookingStatus(
        booking: Booking,
        newStatus: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val bookingRef = bookings.document(booking.pickupCode)
        val productRef = products.document(booking.product.id.toString())

        db.runTransaction { transaction ->
            val bookingSnapshot = transaction.get(bookingRef)
            val oldStatus = bookingSnapshot.getString("status") ?: booking.status

            if (newStatus == "Cancelled" && oldStatus != "Cancelled" && oldStatus != "Collected") {
                val productSnapshot = transaction.get(productRef)
                if (productSnapshot.exists()) {
                    val currentStock = productSnapshot.getLong("stock")?.toInt() ?: 0
                    transaction.update(productRef, "stock", currentStock + booking.quantity)
                }
            }

            transaction.update(
                bookingRef,
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }.addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.localizedMessage ?: "Unable to update reservation") }
    }

    private fun productToMap(product: Product): Map<String, Any> = mapOf(
        "id" to product.id,
        "name" to product.name,
        "category" to product.category,
        "price" to product.price,
        "rating" to product.rating,
        "seller" to product.seller,
        "dzongkhag" to product.dzongkhag,
        "address" to product.address,
        "stock" to product.stock,
        "discountPercent" to product.discountPercent,
        "symbol" to product.symbol,
        "description" to product.description,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun bookingToMap(booking: Booking): Map<String, Any> = mapOf(
        "pickupCode" to booking.pickupCode,
        "productId" to booking.product.id,
        "productName" to booking.product.name,
        "category" to booking.product.category,
        "unitPrice" to booking.product.price,
        "discountPercent" to booking.product.discountPercent,
        "seller" to booking.product.seller,
        "dzongkhag" to booking.product.dzongkhag,
        "address" to booking.product.address,
        "symbol" to booking.product.symbol,
        "description" to booking.product.description,
        "quantity" to booking.quantity,
        "status" to booking.status,
        "totalPrice" to booking.product.discountedPrice * booking.quantity,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun productFromDocument(document: com.google.firebase.firestore.DocumentSnapshot): Product? {
        val id = document.getLong("id")?.toInt() ?: document.id.toIntOrNull() ?: return null
        val category = document.getString("category") ?: "Crafts"
        return Product(
            id = id,
            name = document.getString("name") ?: "Untitled product",
            category = category,
            price = document.getLong("price")?.toInt() ?: 0,
            rating = document.getDouble("rating") ?: 0.0,
            seller = document.getString("seller") ?: "Local seller",
            dzongkhag = document.getString("dzongkhag") ?: "Thimphu",
            address = document.getString("address") ?: "",
            stock = document.getLong("stock")?.toInt() ?: 0,
            discountPercent = document.getLong("discountPercent")?.toInt() ?: 0,
            symbol = document.getString("symbol") ?: symbolForCategory(category),
            colors = colorsForCategory(category),
            description = document.getString("description") ?: ""
        )
    }

    private fun bookingFromDocument(document: com.google.firebase.firestore.DocumentSnapshot): Booking? {
        val productId = document.getLong("productId")?.toInt() ?: return null
        val category = document.getString("category") ?: "Crafts"
        val product = Product(
            id = productId,
            name = document.getString("productName") ?: "Reserved product",
            category = category,
            price = document.getLong("unitPrice")?.toInt() ?: 0,
            rating = 0.0,
            seller = document.getString("seller") ?: "Local seller",
            dzongkhag = document.getString("dzongkhag") ?: "Thimphu",
            address = document.getString("address") ?: "",
            stock = 0,
            discountPercent = document.getLong("discountPercent")?.toInt() ?: 0,
            symbol = document.getString("symbol") ?: symbolForCategory(category),
            colors = colorsForCategory(category),
            description = document.getString("description") ?: ""
        )
        return Booking(
            product = product,
            quantity = document.getLong("quantity")?.toInt() ?: 1,
            pickupCode = document.getString("pickupCode") ?: document.id,
            status = document.getString("status") ?: "Reserved"
        )
    }

    private fun symbolForCategory(category: String): String = when (category) {
        "Textiles" -> "🧵"
        "Food" -> "🍯"
        "Wellness" -> "🌿"
        "Home" -> "🏠"
        else -> "🎁"
    }

    private fun colorsForCategory(category: String): List<Color> = when (category) {
        "Textiles" -> listOf(Color(0xFFB95C4B), Color(0xFFE9A64A))
        "Wellness" -> listOf(Color(0xFF557A5D), Color(0xFFA7C48C))
        "Home" -> listOf(Color(0xFF8B5A3C), Color(0xFFD69B61))
        "Food" -> listOf(Color(0xFFE09A24), Color(0xFFF7D46B))
        else -> listOf(Color(0xFFAA603E), Color(0xFFF0B36D))
    }
}
