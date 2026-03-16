package com.example.infiniteus

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class CoupleSession(
    val sessionId: String = "",
    val user1: String = "",
    val user2: String = "",
    val createdAt: Long = 0,
    val user1Pulse: Long = 0,
    val user1Position: Position = Position(),
    val user2Pulse: Long = 0,
    val user2Position: Position = Position()
)

data class Position(
    val x: Float = 0f,
    val y: Float = 0f
)

class SessionManager {
    private val database = FirebaseDatabase.getInstance()

    suspend fun getSessionForUser(userId: String): CoupleSession? = try {
        val userSnapshot = database.getReference("users").child(userId).get().await()
        val sessionId = userSnapshot.child("sessionId").value as? String ?: return null
        
        val sessionSnapshot = database.getReference("sessions").child(sessionId).get().await()
        val user1 = sessionSnapshot.child("user_1").value as? String ?: ""
        val user2 = sessionSnapshot.child("user_2").value as? String ?: ""
        val createdAt = sessionSnapshot.child("createdAt").value as? Long ?: 0
        
        CoupleSession(
            sessionId = sessionId,
            user1 = user1,
            user2 = user2,
            createdAt = createdAt
        )
    } catch (e: Exception) {
        null
    }

    fun observeSession(sessionId: String): Flow<CoupleSession> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val user1 = snapshot.child("user_1").value as? String ?: ""
                    val user2 = snapshot.child("user_2").value as? String ?: ""
                    val createdAt = snapshot.child("createdAt").value as? Long ?: 0
                    
                    val user1Pulse = snapshot.child("user_1/pulse").value as? Long ?: 0
                    val user1X = snapshot.child("user_1/position/x").value as? Double ?: 0.0
                    val user1Y = snapshot.child("user_1/position/y").value as? Double ?: 0.0
                    
                    val user2Pulse = snapshot.child("user_2/pulse").value as? Long ?: 0
                    val user2X = snapshot.child("user_2/position/x").value as? Double ?: 0.0
                    val user2Y = snapshot.child("user_2/position/y").value as? Double ?: 0.0
                    
                    val session = CoupleSession(
                        sessionId = snapshot.key ?: "",
                        user1 = user1,
                        user2 = user2,
                        createdAt = createdAt,
                        user1Pulse = user1Pulse,
                        user1Position = Position(user1X.toFloat(), user1Y.toFloat()),
                        user2Pulse = user2Pulse,
                        user2Position = Position(user2X.toFloat(), user2Y.toFloat())
                    )
                    trySend(session)
                } catch (e: Exception) {
                    close(e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        val ref = database.getReference("sessions").child(sessionId)
        ref.addValueEventListener(listener)
        
        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    suspend fun updateUserPosition(sessionId: String, userId: String, position: Position): Result<Unit> = try {
        val userKey = if (userId.endsWith("1")) "user_1" else "user_2"
        database.getReference("sessions").child(sessionId).child(userKey).child("position").setValue(
            mapOf("x" to position.x, "y" to position.y)
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateUserPulse(sessionId: String, userId: String, pulse: Long): Result<Unit> = try {
        val userKey = if (userId.endsWith("1")) "user_1" else "user_2"
        database.getReference("sessions").child(sessionId).child(userKey).child("pulse").setValue(pulse).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteSession(sessionId: String): Result<Unit> = try {
        database.getReference("sessions").child(sessionId).removeValue().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
