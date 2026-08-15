package com.flx_apps.digitaldetox.ui.screens.feature.disable_apps

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.flx_apps.digitaldetox.examgate.ExamGateStore
import com.flx_apps.digitaldetox.ui.widgets.SimpleListTile

@Composable
fun ExamQuestionBankTile() {
    val context = LocalContext.current
    val status = remember { mutableStateOf("${ExamGateStore.questionCount(context)} questions loaded") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        kotlin.runCatching {
            val json = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: error("Could not read the selected file")
            ExamGateStore.importQuestionBank(context, json)
        }.onSuccess { count ->
            status.value = "$count questions imported"
        }.onFailure { error ->
            status.value = "Import failed: ${error.message ?: "invalid question bank"}"
        }
    }

    SimpleListTile(
        titleText = "Import exam questions",
        subtitleText = "Choose a JSON question bank generated from your own notes or course content.",
        trailing = { Text(status.value) },
        onClick = { launcher.launch(arrayOf("application/json", "text/json", "text/plain")) },
    )
}
