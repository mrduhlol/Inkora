package com.abhishek.inkora.features.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Optional local app lock using the device's own biometric / PIN / pattern —
 * no custom passwords stored anywhere. While locked, no note content is
 * composed at all, so nothing leaks through previews or recents thumbnails.
 */
@Composable
fun rememberAuthenticator(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    return remember(activity) {
        {
            val manager = BiometricManager.from(context)
            val can = manager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            if (can != BiometricManager.BIOMETRIC_SUCCESS) {
                onFailure("Set a screen lock (PIN, pattern or biometrics) to use app lock")
            } else {
                val prompt = BiometricPrompt(
                    activity,
                    ContextCompat.getMainExecutor(context),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            onSuccess()
                        }

                        override fun onAuthenticationError(code: Int, msg: CharSequence) {
                            onFailure(msg.toString())
                        }
                    }
                )
                prompt.authenticate(
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Unlock Inkora")
                        .setSubtitle("Use your device screen lock")
                        .setAllowedAuthenticators(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        )
                        .build()
                )
            }
        }
    }
}

@Composable
fun AppLockGate(
    enabled: Boolean,
    activity: FragmentActivity,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        content()
        return
    }
    var unlocked by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    if (unlocked) {
        content()
        return
    }
    val authenticate = rememberAuthenticator(
        activity = activity,
        onSuccess = { unlocked = true; error = null },
        onFailure = { error = it }
    )
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Lock, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Inkora is locked", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Authenticate with your device screen lock to open your notes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = authenticate) { Text("Unlock") }
    }
}
