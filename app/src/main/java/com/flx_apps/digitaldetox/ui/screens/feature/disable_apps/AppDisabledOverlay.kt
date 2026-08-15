package com.flx_apps.digitaldetox.ui.screens.feature.disable_apps

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flx_apps.digitaldetox.examgate.ExamGateStore
import com.flx_apps.digitaldetox.system_integration.OverlayContent
import com.flx_apps.digitaldetox.system_integration.OverlayService
import com.flx_apps.digitaldetox.ui.theme.DetoxDroidTheme

class AppDisabledOverlayService : OverlayService(OverlayContent { AppDisabledOverlay() }) {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        if (ExamGateStore.isUnlocked(this, runningAppPackageName)) {
            dismissOverlay()
        }
        return result
    }
}

/**
 * ExamGate overlay. A selected app remains covered until the learner answers a question correctly.
 * Correct answers grant a short per-app unlock and dismiss this overlay without navigating home.
 */
@Preview
@Composable
fun AppDisabledOverlay() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val service = context as? AppDisabledOverlayService
    val question = remember { ExamGateStore.nextQuestion(context) }
    var feedback by remember { mutableStateOf<String?>(null) }

    DetoxDroidTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Quick exam question",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = question.topic,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 6.dp, bottom = 28.dp),
            )
            Text(
                text = question.prompt,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            question.choices.forEachIndexed { index, choice ->
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    onClick = {
                        if (index == question.correctIndex) {
                            val packageName = service?.runningAppPackageName.orEmpty()
                            ExamGateStore.grantUnlock(context, packageName)
                            service?.dismissOverlay()
                        } else {
                            feedback = if (question.explanation.isBlank()) {
                                "Not quite. Try again."
                            } else {
                                "Not quite. ${question.explanation}"
                            }
                        }
                    },
                ) {
                    Text(text = choice, textAlign = TextAlign.Center)
                }
            }

            feedback?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 18.dp),
                )
            }

            Spacer(modifier = Modifier.padding(10.dp))
            Button(onClick = { service?.closeOverlay() }) {
                Text("Leave app")
            }
            Text(
                text = "Correct answer unlocks this app for 10 minutes.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}
