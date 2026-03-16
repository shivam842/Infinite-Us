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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

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
        
        float3 purple = float3(0.3, 0.0, 0.5);
        float3 gold = float3(1.0, 0.8, 0.0);
        
        float3 color = mix(purple, gold, n);
        
        float d1 = length(fragCoord - u_p1);
        float d2 = length(fragCoord - u_p2);
        
        float glow1 = exp(-d1 * 0.01) * 0.5;
        float glow2 = exp(-d2 * 0.01) * 0.5;
        
        color += gold * glow1;
        color += purple * glow2;
        
        if (u_eclipse > 0.0) {
            float2 mid = (u_p1 + u_p2) * 0.5;
            float dMid = length(fragCoord - mid);
            float eclipseGlow = exp(-dMid * 0.005) * u_eclipse;
            color += float3(1.0, 1.0, 1.0) * eclipseGlow;
        }

        return half4(color, 1.0);
    }
"""

class MainActivityImproved : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val authManager = AuthManager()
        val sessionManager = SessionManager()

        setContent {
            TheInfiniteUsAppImproved(authManager, sessionManager)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun TheInfiniteUsAppImproved(authManager: AuthManager, sessionManager: SessionManager) {
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    var showAuthScreen by remember { mutableStateOf(currentUser == null) }
    var currentSession by remember { mutableStateOf<CoupleSession?>(null) }
    var showPairingScreen by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val session = sessionManager.getSessionForUser(currentUser!!.uid)
            if (session == null) {
                showPairingScreen = true
            } else {
                currentSession = session
                showPairingScreen = false
            }
        }
    }

    when {
        showAuthScreen -> AuthScreen(authManager) { user ->
            currentUser = user
            showAuthScreen = false
        }
        showPairingScreen && currentUser != null -> PairingScreen(authManager, currentUser!!.uid) { session ->
            currentSession = session
            showPairingScreen = false
        }
        currentSession != null -> NebulaScreen(sessionManager, currentUser!!.uid, currentSession!!)
        else -> LoadingScreen()
    }
}

@Composable
fun AuthScreen(authManager: AuthManager, onAuthSuccess: (com.google.firebase.auth.FirebaseUser) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1a0033))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "The Infinite Us",
            fontSize = 32.sp,
            color = Color(0xFFFFD700),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color(0xFF9966CC)
            )
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            type = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color(0xFF9966CC)
            )
        )

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(bottom = 16.dp))
        }

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    val result = if (isSignUp) {
                        authManager.signUpWithEmail(email, password)
                    } else {
                        authManager.signInWithEmail(email, password)
                    }
                    result.onSuccess { user ->
                        onAuthSuccess(user)
                    }.onFailure { exception ->
                        errorMessage = exception.message ?: "Authentication failed"
                    }
                    isLoading = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = !isLoading
        ) {
            Text(if (isSignUp) "Sign Up" else "Sign In")
        }

        TextButton(onClick = { isSignUp = !isSignUp }) {
            Text(
                if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up",
                color = Color(0xFFFFD700)
            )
        }
    }
}

@Composable
fun PairingScreen(authManager: AuthManager, userId: String, onPairingSuccess: (CoupleSession) -> Unit) {
    var pairingCode by remember { mutableStateOf("") }
    var myPairingCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            myPairingCode = authManager.getPairingCode(userId) ?: "ERROR"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1a0033))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Connect with Your Partner",
            fontSize = 28.sp,
            color = Color(0xFFFFD700),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            "Your Pairing Code:",
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            myPairingCode,
            fontSize = 24.sp,
            color = Color(0xFFFFD700),
            modifier = Modifier
                .padding(bottom = 32.dp)
                .background(Color(0xFF2a0055), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .padding(16.dp)
        )

        OutlinedTextField(
            value = pairingCode,
            onValueChange = { pairingCode = it.uppercase() },
            label = { Text("Partner's Pairing Code") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color(0xFF9966CC)
            )
        )

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(bottom = 16.dp))
        }

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    val result = authManager.pairWithPartner(userId, pairingCode)
                    result.onSuccess { sessionId ->
                        val session = CoupleSession(
                            sessionId = sessionId,
                            user1 = userId,
                            user2 = "",
                            createdAt = System.currentTimeMillis()
                        )
                        onPairingSuccess(session)
                    }.onFailure { exception ->
                        errorMessage = exception.message ?: "Pairing failed"
                    }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && pairingCode.length == 6
        ) {
            Text("Connect")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NebulaScreen(sessionManager: SessionManager, userId: String, initialSession: CoupleSession) {
    val scope = rememberCoroutineScope()
    val shader = remember { RuntimeShader(NEBULA_SHADER) }
    val infiniteTransition = rememberInfiniteTransition(label = "nebula")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(100000, easing = LinearEasing)),
        label = "time"
    )

    var myPos by remember { mutableStateOf(initialSession.user1Position) }
    var partnerPos by remember { mutableStateOf(initialSession.user2Position) }
    var myPulse by remember { mutableStateOf(initialSession.user1Pulse) }
    var partnerPulse by remember { mutableStateOf(initialSession.user2Pulse) }
    var eclipseActive by remember { mutableStateOf(false) }
    val eclipseProgress by animateFloatAsState(if (eclipseActive) 1f else 0f, label = "eclipse")

    // Observe session updates
    LaunchedEffect(initialSession.sessionId) {
        sessionManager.observeSession(initialSession.sessionId).collect { session ->
            myPos = session.user1Position
            partnerPos = session.user2Position
            myPulse = session.user1Pulse
            partnerPulse = session.user2Pulse
        }
    }

    // Eclipse logic
    LaunchedEffect(myPulse, partnerPulse) {
        if (abs(myPulse - partnerPulse) < 500 && myPulse != 0L) {
            eclipseActive = true
            triggerHapticFeedback()
            delay(3000)
            eclipseActive = false
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                myPos = Offset(myPos.x + dragAmount.x, myPos.y + dragAmount.y)
                scope.launch {
                    sessionManager.updateUserPosition(initialSession.sessionId, userId, Position(myPos.x, myPos.y))
                    
                    val now = System.currentTimeMillis()
                    if (now - myPulse > 200) {
                        sessionManager.updateUserPulse(initialSession.sessionId, userId, now)
                    }
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

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1a0033)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(0xFFFFD700))
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun triggerHapticFeedback() {
    val vibratorManager = android.app.Application().getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
    val vibrator = vibratorManager.defaultVibrator
    val effect = VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 100), -1)
    vibrator.vibrate(effect)
}
