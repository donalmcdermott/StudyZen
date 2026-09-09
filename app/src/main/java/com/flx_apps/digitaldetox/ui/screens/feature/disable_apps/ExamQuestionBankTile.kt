package com.flx_apps.digitaldetox.ui.screens.feature.disable_apps

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.flx_apps.digitaldetox.R
import com.flx_apps.digitaldetox.examgate.ExamGateStore
import com.flx_apps.digitaldetox.ui.widgets.SimpleListTile

@Composable
fun ExamQuestionBankTile() {
    val context = LocalContext.current
    val status = remember {
        mutableStateOf(
            context.getString(
                R.string.feature_disableApps_examGate_status_loaded,
                ExamGateStore.questionCount(context),
            )
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        kotlin.runCatching {
            val json = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: error(context.getString(R.string.feature_disableApps_examGate_status_unreadable))
            ExamGateStore.importQuestionBank(context, json)
        }.onSuccess { count ->
            status.value = context.getString(
                R.string.feature_disableApps_examGate_status_imported, count
            )
        }.onFailure { error ->
            status.value = context.getString(
                R.string.feature_disableApps_examGate_status_failed,
                error.message
                    ?: context.getString(R.string.feature_disableApps_examGate_status_failedGeneric),
            )
        }
    }

    SimpleListTile(
        titleText = stringResource(R.string.feature_disableApps_examGate_importTitle),
        subtitleText = stringResource(R.string.feature_disableApps_examGate_importSubtitle),
        trailing = { Text(status.value) },
        onClick = { launcher.launch(arrayOf("application/json", "text/json", "text/plain")) },
    )
}
