package com.karmagbyte.tshongla

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class TshongLaUser(
    val uid: String,
    val name: String,
    val email: String,
    val role: String,
    val phone: String = "",
    val phoneVerified: Boolean = false,
    val shopName: String = "",
    val dzongkhag: String = "",
    val address: String = "",
    val cidNumber: String = "",
    val cidImageUri: String = "",
    val verificationStatus: String = "not_required"
) {
    val canPublishProducts: Boolean
        get() = role == "shopkeeper" && phoneVerified && cidNumber.isNotBlank() && cidImageUri.isNotBlank()
}

object TshongLaAuth {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val users = db.collection("users")

    fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        phone: String = "",
        phoneVerified: Boolean = false,
        shopName: String = "",
        dzongkhag: String = "",
        address: String = "",
        cidNumber: String = "",
        cidImageUri: String = "",
        onSuccess: (TshongLaUser) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val firebaseUser = result.user ?: run {
                    onError("Account was created but the user session could not be opened.")
                    return@addOnSuccessListener
                }
                val isShopkeeper = role == "shopkeeper"
                val profile = TshongLaUser(
                    uid = firebaseUser.uid,
                    name = name.trim(),
                    email = email.trim(),
                    role = role,
                    phone = phone.trim(),
                    phoneVerified = phoneVerified,
                    shopName = shopName.trim(),
                    dzongkhag = dzongkhag,
                    address = address.trim(),
                    cidNumber = cidNumber.trim(),
                    cidImageUri = cidImageUri,
                    verificationStatus = if (isShopkeeper && phoneVerified && cidNumber.isNotBlank() && cidImageUri.isNotBlank()) "submitted" else if (isShopkeeper) "incomplete" else if (phoneVerified) "verified" else "incomplete"
                )
                users.document(firebaseUser.uid).set(profileToMap(profile))
                    .addOnSuccessListener { onSuccess(profile) }
                    .addOnFailureListener { error ->
                        firebaseUser.delete()
                        onError(error.localizedMessage ?: "Unable to save account profile.")
                    }
            }
            .addOnFailureListener { onError(it.localizedMessage ?: "Unable to create account.") }
    }

    fun login(email: String, password: String, expectedRole: String, onSuccess: (TshongLaUser) -> Unit, onError: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: run { onError("Unable to open user session."); return@addOnSuccessListener }
                users.document(uid).get().addOnSuccessListener { doc ->
                    if (!doc.exists()) { auth.signOut(); onError("This account does not have a TshongLa profile."); return@addOnSuccessListener }
                    val role = doc.getString("role") ?: ""
                    if (role != expectedRole) {
                        auth.signOut()
                        onError("This account is registered as a ${if (role == "shopkeeper") "Shopkeeper" else "Customer"}. Please use the correct login.")
                        return@addOnSuccessListener
                    }
                    onSuccess(userFromDocument(uid, doc, email.trim()))
                }.addOnFailureListener { error -> auth.signOut(); onError(error.localizedMessage ?: "Unable to load account profile.") }
            }
            .addOnFailureListener { onError(it.localizedMessage ?: "Unable to log in.") }
    }

    fun updateVerification(
        current: TshongLaUser,
        phone: String,
        phoneVerified: Boolean,
        cidNumber: String,
        cidImageUri: String,
        onSuccess: (TshongLaUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val updated = current.copy(
            phone = phone.trim(),
            phoneVerified = phoneVerified,
            cidNumber = if (current.role == "shopkeeper") cidNumber.trim() else "",
            cidImageUri = if (current.role == "shopkeeper") cidImageUri else "",
            verificationStatus = if (current.role == "shopkeeper") {
                if (phoneVerified && cidNumber.isNotBlank() && cidImageUri.isNotBlank()) "submitted" else "incomplete"
            } else if (phoneVerified) "verified" else "incomplete"
        )
        users.document(current.uid).update(mapOf(
            "phone" to updated.phone,
            "phoneVerified" to updated.phoneVerified,
            "cidNumber" to updated.cidNumber,
            "cidImageUri" to updated.cidImageUri,
            "verificationStatus" to updated.verificationStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        )).addOnSuccessListener { onSuccess(updated) }
            .addOnFailureListener { onError(it.localizedMessage ?: "Unable to update verification") }
    }

    private fun profileToMap(profile: TshongLaUser): Map<String, Any> = mapOf(
        "uid" to profile.uid, "name" to profile.name, "email" to profile.email, "role" to profile.role,
        "phone" to profile.phone, "phoneVerified" to profile.phoneVerified,
        "shopName" to profile.shopName, "dzongkhag" to profile.dzongkhag, "address" to profile.address,
        "cidNumber" to profile.cidNumber, "cidImageUri" to profile.cidImageUri,
        "verificationStatus" to profile.verificationStatus,
        "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun userFromDocument(uid: String, doc: com.google.firebase.firestore.DocumentSnapshot, fallbackEmail: String) = TshongLaUser(
        uid = uid, name = doc.getString("name") ?: "", email = doc.getString("email") ?: fallbackEmail,
        role = doc.getString("role") ?: "", phone = doc.getString("phone") ?: "",
        phoneVerified = doc.getBoolean("phoneVerified") ?: false, shopName = doc.getString("shopName") ?: "",
        dzongkhag = doc.getString("dzongkhag") ?: "", address = doc.getString("address") ?: "",
        cidNumber = doc.getString("cidNumber") ?: "", cidImageUri = doc.getString("cidImageUri") ?: "",
        verificationStatus = doc.getString("verificationStatus") ?: "incomplete"
    )

    fun signOut() = auth.signOut()
    fun currentUserId(): String? = auth.currentUser?.uid
}
