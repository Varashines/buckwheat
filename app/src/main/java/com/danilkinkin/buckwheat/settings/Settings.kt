package com.danilkinkin.buckwheat.settings

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.danilkinkin.buckwheat.LocalWindowInsets
import com.danilkinkin.buckwheat.R
import com.danilkinkin.buckwheat.base.AppToast
import com.danilkinkin.buckwheat.base.ButtonRow
import com.danilkinkin.buckwheat.base.LocalBottomSheetScrollState
import com.danilkinkin.buckwheat.data.ExtendCurrency
import com.danilkinkin.buckwheat.data.SpendsViewModel
import com.danilkinkin.buckwheat.export.rememberExportCSV
import com.danilkinkin.buckwheat.ui.BuckwheatTheme
import java.time.LocalDate

/** Refuse absurdly large documents instead of risking an OOM on a mistyped export. */
private fun readBackup(context: android.content.Context, uri: android.net.Uri): String? =
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().use { reader ->
                val buffer = CharArray(8192)
                val text = StringBuilder()

                while (true) {
                    val read = reader.read(buffer)
                    if (read < 0) break

                    text.append(buffer, 0, read)
                    if (text.length > 16 * 1024 * 1024) return@runCatching null
                }

                text.toString()
            }
        }
    }.getOrNull()

@Composable
fun Settings(
    onTriedWidget: () -> Unit = {},
    onOpenCurrency: () -> Unit = {},
    onOpenCategories: () -> Unit = {},
    onOpenSubscriptions: () -> Unit = {},
    activityResultRegistryOwner: ActivityResultRegistryOwner? = null,
    spendsViewModel: SpendsViewModel = hiltViewModel(),
) {
    val localBottomSheetScrollState = LocalBottomSheetScrollState.current
    val exportCSVLaunch = rememberExportCSV(
        activityResultRegistryOwner = activityResultRegistryOwner
    )
    val context = LocalContext.current
    val backupExported = stringResource(R.string.backup_exported)
    val backupRestored = stringResource(R.string.backup_restored)
    val backupFailed = stringResource(R.string.backup_failed)

    var pendingBackup by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<String?>(null) }
    var confirmRestore by remember { mutableStateOf(false) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingBackup
        pendingBackup = null

        if (uri != null && json != null) {
            val success = runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(json)
                }
            }.isSuccess

            AppToast.show(if (success) backupExported else backupFailed)
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val json = uri?.let { selected -> readBackup(context, selected) }

        if (json == null) {
            AppToast.show(backupFailed)
        } else {
            pendingRestore = json
            confirmRestore = true
        }
    }

    val currency by spendsViewModel.currency.collectAsStateWithLifecycle()
    val currencyCaption = when (currency.type) {
        ExtendCurrency.Type.NONE -> stringResource(R.string.currency_none)
        else -> currency.value.orEmpty()
    }

    val navigationBarHeight = androidx.compose.ui.unit.max(
        LocalWindowInsets.current.calculateBottomPadding(),
        16.dp,
    )

    Surface(Modifier.padding(top = localBottomSheetScrollState.topPadding)) {
        Column {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = navigationBarHeight)
            ) {
                SettingsSection(stringResource(R.string.settings_appearance)) {
                    ThemeSwitcher()
                    LangSwitcher()
                    ButtonRow(
                        icon = painterResource(R.drawable.ic_currency),
                        text = stringResource(R.string.in_currency_label),
                        endCaption = currencyCaption,
                        onClick = onOpenCurrency,
                    )
                }

                SettingsSection(stringResource(R.string.settings_organise)) {
                    ButtonRow(
                        icon = painterResource(R.drawable.ic_label),
                        text = stringResource(R.string.categories_title),
                        onClick = onOpenCategories,
                    )
                    ButtonRow(
                        icon = painterResource(R.drawable.ic_balance_wallet),
                        text = stringResource(R.string.subscriptions_title),
                        onClick = onOpenSubscriptions,
                    )
                }

                SettingsSection(stringResource(R.string.settings_data)) {
                    TryWidget(onTried = {
                        onTriedWidget()
                    })
                    ButtonRow(
                        icon = painterResource(R.drawable.ic_file_download),
                        text = stringResource(R.string.export_to_csv),
                        onClick = { exportCSVLaunch() },
                    )
                    ButtonRow(
                        icon = painterResource(R.drawable.ic_backup),
                        text = stringResource(R.string.export_backup),
                        onClick = {
                            spendsViewModel.exportBackup { json ->
                                pendingBackup = json
                                exportBackupLauncher.launch("eva-backup-${LocalDate.now()}.json")
                            }
                        },
                    )
                    ButtonRow(
                        icon = painterResource(R.drawable.ic_restore),
                        text = stringResource(R.string.restore_backup),
                        onClick = {
                            importBackupLauncher.launch(
                                arrayOf(
                                    "application/json",
                                    "text/plain",
                                    "application/octet-stream",
                                )
                            )
                        },
                    )
                }

                Spacer(Modifier.height(8.dp))
                About(
                    Modifier.padding(start = 16.dp, end = 16.dp),
                )
            }
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = {
                confirmRestore = false
                pendingRestore = null
            },
            title = { Text(stringResource(R.string.restore_backup_title)) },
            text = { Text(stringResource(R.string.restore_backup_description)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val json = pendingRestore
                        confirmRestore = false
                        pendingRestore = null

                        if (json != null) {
                            spendsViewModel.restoreBackup(json) { backup ->
                                AppToast.show(
                                    if (backup != null) {
                                        context.resources.getString(
                                            R.string.backup_restored_summary,
                                            backup.transactions.size,
                                            backup.subscriptions.size,
                                            backup.categories.size,
                                        )
                                    } else {
                                        backupFailed
                                    }
                                )
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.restore_backup))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmRestore = false
                        pendingRestore = null
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsSection(
    label: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 6.dp),
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(content = content)
        }
    }
}

@Preview(name = "Default")
@Composable
private fun PreviewDefault() {
    BuckwheatTheme {
        Settings()
    }
}

@Preview(name = "Night mode", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PreviewNightMode() {
    BuckwheatTheme {
        Settings()
    }
}
