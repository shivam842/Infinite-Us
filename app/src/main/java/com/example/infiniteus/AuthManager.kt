package com.example.infiniteus

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("User creation failed")
        
        // Initialize user profile
        val userRef = database.getReference("users").child(user.uid)
        userRef.setValue(mapOf(
            "email" to email,
            "createdAt" to System.currentTimeMillis(),
            "pairingCode" to generatePairingCode(),
            "sessionId" to null,
            "partnerId" to null
        )).await()
        
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        Result.success(result.user ?: throw Exception("Sign in failed"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun signOut() {
        auth.signOut()
    }

    suspend fun getPairingCode(userId: String): String? = try {
        val snapshot = database.getReference("users").child(userId).child("pairingCode").get().await()
        snapshot.value as? String
    } catch (e: Exception) {
        null
    }

    suspend fun pairWithPartner(userId: String, partnerPairingCode: String): Result<String> = try {
        // Find partner by pairing code
        val usersRef = database.getReference("users")
        val snapshot = usersRef.orderByChild("pairingCode").equalTo(partnerPairingCode).get().await()
        
        if (!snapshot.hasChildren()) {
            return Result.failure(Exception("Partner not found"))
        }
        
        val partnerId = snapshot.children.first().key ?: throw Exception("Invalid partner data")
        
        // Create a unique session ID
        val sessionId = generateSessionId(userId, partnerId)
        
        // Update both users with session ID and partner ID
        val updates = mapOf(
            "users/$userId/sessionId" to sessionId,
            "users/$userId/partnerId" to partnerId,
            "users/$partnerId/sessionId" to sessionId,
            "users/$partnerId/partnerId" to userId,
            "sessions/$sessionId/user_1" to userId,
            "sessions/$sessionId/user_2" to partnerId,
            "sessions/$sessionId/createdAt" to System.currentTimeMillis()
        )
        
        database.reference.updateChildren(updates).await()
        Result.success(sessionId)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun generatePairingCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun generateSessionId(userId1: String, userId2: String): String {
        val combined = listOf(userId1, userId2).sorted().joinToString("_")
        return "session_${combined.hashCode().toString(16)}_${System.currentTimeMillis()}"
    }
}
