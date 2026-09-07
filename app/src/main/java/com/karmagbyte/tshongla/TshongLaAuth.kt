package com.karmagbyte.tshongla

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class TshongLaUser(
    val uid: String,
    val name: String,
    val email: String,
    val role: String,
    val shopName: String = "",
    val dzongkhag: String = "",
    val address: String = ""
)

/** Firebase Authentication + user profile storage for TshongLa. */
object TshongLaAuth {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val users = db.collection("users")

    fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        shopName: String = "",
        dzongkhag: String = "",
        address: String = "",
        onSuccess: (TshongLaUser) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val firebaseUser = result.user
                if (firebaseUser == null) {
                    onError("Account was created but the user session could not be opened.")
                    return@addOnSuccessListener
                }

                val profile = TshongLaUser(
                    uid = firebaseUser.uid,
                    name = name.trim(),
                    email = email.trim(),
                    role = role,
                    shopName = shopName.trim(),
                    dzongkhag = dzongkhag,
                    address = address.trim()
                )

                val data = hashMapOf<String, Any>(
                    "uid" to profile.uid,
                    "name" to profile.name,
                    "email" to profile.email,
                    "role" to profile.role,
                    "shopName" to profile.shopName,
                    "dzongkhag" to profile.dzongkhag,
                    "address" to profile.address,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                users.document(firebaseUser.uid).set(data)
                    .addOnSuccessListener { onSuccess(profile) }
                    .addOnFailureListener { error ->
                        firebaseUser.delete()
                        onError(error.localizedMessage ?: "Unable to save account profile.")
                    }
            }
            .addOnFailureListener { error ->
                onError(error.localizedMessage ?: "Unable to create account.")
            }
    }

    fun login(
        email: String,
        password: String,
        expectedRole: String,
        onSuccess: (TshongLaUser) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    onError("Unable to open user session.")
                    return@addOnSuccessListener
                }

                users.document(uid).get()
                    .addOnSuccessListener { doc ->
                        if (!doc.exists()) {
                            auth.signOut()
                            onError("This account does not have a TshongLa profile.")
                            return@addOnSuccessListener
                        }

                        val role = doc.getString("role") ?: ""
                        if (role != expectedRole) {
                            auth.signOut()
                            val friendlyRole = if (role == "shopkeeper") "Shopkeeper" else "Customer"
                            onError("This account is registered as a $friendlyRole. Please use the correct login.")
                            return@addOnSuccessListener
                        }

                        onSuccess(
                            TshongLaUser(
                                uid = uid,
                                name = doc.getString("name") ?: "",
                                email = doc.getString("email") ?: email.trim(),
                                role = role,
                                shopName = doc.getString("shopName") ?: "",
                                dzongkhag = doc.getString("dzongkhag") ?: "",
                                address = doc.getString("address") ?: ""
                            )
                        )
                    }
                    .addOnFailureListener { error ->
                        auth.signOut()
                        onError(error.localizedMessage ?: "Unable to load account profile.")
                    }
            }
            .addOnFailureListener { error ->
                onError(error.localizedMessage ?: "Unable to log in.")
            }
    }

    fun signOut() = auth.signOut()

    fun currentUserId(): String? = auth.currentUser?.uid
}
