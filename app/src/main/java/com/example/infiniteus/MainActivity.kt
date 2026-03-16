package com.example.infiniteus

import android.graphics.RuntimeShader
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

// AGSL Shader for Nebula effect
const val NEBULA_SHADER = """
    uniform float2 u_resolution;
    uniform float u_time;
    uniform float2 u_p1;
    uniform float2 u_p2;
    uniform float u_eclipse;

    float hash(float2 p) {
        return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
    }

    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float a = hash(i);
        float b = hash(i + float2(1.0, 0.0));
        float c = hash(i + float2(0.0, 1.0));
        float d = hash(i + float2(1.0, 1.0));
        float2 u = f * f * (3.0 - 2.0 * f);
        return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
    }

    float fbm(float2 p) {
        float v = 0.0;
        float a = 0.5;
        for (int i = 0; i < 5; i++) {
            v += a * noise(p);
            p *= 2.0;
            a *= 0.5;
        }
        return v;
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / u_resolution.xy;
        float2 p = (fragCoord * 2.0 - u_resolution.xy) / min(u_resolution.x, u_resolution.y);
        
        float n = fbm(p * 2.0 + u_time * 0.1);
        
        // Purple and Gold colors
        float3 purple = float3(0.3, 0.0, 0.5);
        float3 gold = float3(1.0, 0.8, 0.0);
        
        float3 color = mix(purple, gold, n);
        
        // Glow at user positions
        float d1 = length(fragCoord - u_p1);
        float d2 = length(fragCoord - u_p2);
        
        float glow1 = exp(-d1 * 0.01) * 0.5;
        float glow2 = exp(-d2 * 0.01) * 0.5;
        
        color += gold * glow1;
        color += purple * glow2;
        
        // Eclipse effect
        if (u_eclipse > 0.0) {
            float2 mid = (u_p1 + u_p2) * 0.5;
            float dMid = length(fragCoord - mid);
            float eclipseGlow = exp(-dMid * 0.005) * u_eclipse;
            color += float3(1.0, 1.0, 1.0) * eclipseGlow;
        }

        return half4(color, 1.0);
    }
"""

data class UserState(
    val pulse: Long = 0,
    val presence: Boolean = false,
    val x: Float = 0f,
    val y: Float = 0f
)

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = FirebaseDatabase.getInstance()
        val sessionRef = database.getReference("sessions/couple_1")
        val userId = "user_1" // Hardcoded for demo, usually from Auth
        val partnerId = "user_2"

        setContent {
            TheInfiniteUsApp(sessionRef, userId, partnerId)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun TheInfiniteUsApp(sessionRef: com.google.firebase.database.DatabaseReference, userId: String, partnerId: String) {
    val scope = rememberCoroutineScope()
    val shader = remember { RuntimeShader(NEBULA_SHADER) }
    val infiniteTransition = rememberInfiniteTransition(label = "nebula")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(100000, easing = LinearEasing)),
        label = "time"
    )

    var myPos by remember { mutableStateOf(Offset.Zero) }
    var partnerPos by remember { mutableStateOf(Offset.Zero) }
    var myPulse by remember { mutableStateOf(0L) }
    var partnerPulse by remember { mutableStateOf(0L) }
    var eclipseActive by remember { mutableStateOf(false) }
    val eclipseProgress by animateFloatAsState(if (eclipseActive) 1f else 0f, label = "eclipse")

    // Sync with Firebase
    LaunchedEffect(Unit) {
        sessionRef.child(partnerId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val x = snapshot.child("position/x").getValue(Float::class.java) ?: 0f
                val y = snapshot.child("position/y").getValue(Float::class.java) ?: 0f
                partnerPos = Offset(x, y)
                partnerPulse = snapshot.child("pulse").getValue(Long::class.java) ?: 0L
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Eclipse logic
    LaunchedEffect(myPulse, partnerPulse) {
        if (abs(myPulse - partnerPulse) < 500 && myPulse != 0L) {
            eclipseActive = true
            delay(3000)
            eclipseActive = false
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                myPos += dragAmount
                sessionRef.child(userId).child("position").setValue(mapOf("x" to myPos.x, "y" to myPos.y))
                
                val now = System.currentTimeMillis()
                if (now - myPulse > 200) {
                    myPulse = now
                    sessionRef.child(userId).child("pulse").setValue(now)
                }
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            shader.setFloatUniform("u_resolution", size.width, size.height)
            shader.setFloatUniform("u_time", time)
            shader.setFloatUniform("u_p1", myPos.x, myPos.y)
            shader.setFloatUniform("u_p2", partnerPos.x, partnerPos.y)
            shader.setFloatUniform("u_eclipse", eclipseProgress)
            
            drawRect(brush = ShaderBrush(shader))
        }
    }
}
