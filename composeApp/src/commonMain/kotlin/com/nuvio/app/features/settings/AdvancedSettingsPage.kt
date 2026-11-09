package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.DialogButton
import com.nuvio.app.core.ui.DialogButtons
import com.nuvio.app.core.ui.DialogButtonStyle
import com.nuvio.app.core.ui.DialogSurface
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.watchprogress.ContinueWatchingEnrichmentCache
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_cancel
import nuvio.composeapp.generated.resources.settings_advanced_backup_export
import nuvio.composeapp.generated.resources.settings_advanced_backup_export_done
import nuvio.composeapp.generated.resources.settings_advanced_backup_export_failed
import nuvio.composeapp.generated.resources.settings_advanced_backup_export_subtitle
import nuvio.composeapp.generated.resources.settings_advanced_backup_import
import nuvio.composeapp.generated.resources.settings_advanced_backup_import_done
import nuvio.composeapp.generated.resources.settings_advanced_backup_import_failed
import nuvio.composeapp.generated.resources.settings_advanced_backup_import_invalid
import nuvio.composeapp.generated.resources.settings_advanced_backup_import_subtitle
import nuvio.composeapp.generated.resources.settings_advanced_backup_restore_confirm
import nuvio.composeapp.generated.resources.settings_advanced_backup_restore_dialog_subtitle
import nuvio.composeapp.generated.resources.settings_advanced_backup_restore_dialog_title
import nuvio.composeapp.generated.resources.settings_advanced_clear_cw_cache
import nuvio.composeapp.generated.resources.settings_advanced_clear_cw_cache_done
import nuvio.composeapp.generated.resources.settings_advanced_clear_cw_cache_subtitle
import nuvio.composeapp.generated.resources.settings_advanced_section_backup
import nuvio.composeapp.generated.resources.settings_advanced_remember_last_profile
import nuvio.composeapp.generated.resources.settings_advanced_remember_last_profile_description
import nuvio.composeapp.generated.resources.settings_advanced_section_cache
import nuvio.composeapp.generated.resources.settings_advanced_section_diagnostics
import nuvio.composeapp.generated.resources.settings_advanced_section_startup
import nuvio.composeapp.generated.resources.settings_advanced_sentry_reports
import nuvio.composeapp.generated.resources.settings_advanced_sentry_reports_subtitle
import nuvio.composeapp.generated.resources.sentry_disable_dialog_subtitle
import nuvio.composeapp.generated.resources.sentry_disable_dialog_title
import nuvio.composeapp.generated.resources.sentry_enable_dialog_subtitle
import nuvio.composeapp.generated.resources.sentry_enable_dialog_title
import nuvio.composeapp.generated.resources.sentry_help_body
import nuvio.composeapp.generated.resources.sentry_help_title
import nuvio.composeapp.generated.resources.sentry_keep_enabled
import nuvio.composeapp.generated.resources.sentry_not_sent_body
import nuvio.composeapp.generated.resources.sentry_not_sent_title
import nuvio.composeapp.generated.resources.sentry_sent_body
import nuvio.composeapp.generated.resources.sentry_sent_title
import nuvio.composeapp.generated.resources.sentry_turn_off
import nuvio.composeapp.generated.resources.sentry_turn_on
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.advancedSettingsContent(
    isTablet: Boolean,
    rememberLastProfileEnabled: Boolean,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_advanced_section_startup),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_advanced_remember_last_profile),
                    description = stringResource(Res.string.settings_advanced_remember_last_profile_description),
                    checked = rememberLastProfileEnabled,
                    isTablet = isTablet,
                    onCheckedChange = ProfileRepository::setRememberLastProfileEnabled,
                )
            }
        }
    }
    if (SentrySettingsRepository.isSupported) {
        item {
            val sentryEnabledFlow = remember {
                SentrySettingsRepository.ensureLoaded()
                SentrySettingsRepository.enabled
            }
            val sentryEnabled by sentryEnabledFlow.collectAsStateWithLifecycle()
            var showSentryDialog by rememberSaveable { mutableStateOf(false) }

            SettingsSection(
                title = stringResource(Res.string.settings_advanced_section_diagnostics),
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_advanced_sentry_reports),
                        description = stringResource(Res.string.settings_advanced_sentry_reports_subtitle),
                        checked = sentryEnabled,
                        isTablet = isTablet,
                        onCheckedChange = { showSentryDialog = true },
                    )
                }
            }

            if (showSentryDialog) {
                SentrySettingsDialog(
                    enabled = sentryEnabled,
                    onConfirm = {
                        SentrySettingsRepository.setEnabled(!sentryEnabled)
                    },
                    onDismiss = {
                        showSentryDialog = false
                    },
                )
            }
        }
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_advanced_section_cache),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                val scope = rememberCoroutineScope()
                var cleared by rememberSaveable { mutableStateOf(false) }
                SettingsNavigationRow(
                    title = stringResource(Res.string.settings_advanced_clear_cw_cache),
                    description = if (cleared) {
                        stringResource(Res.string.settings_advanced_clear_cw_cache_done)
                    } else {
                        stringResource(Res.string.settings_advanced_clear_cw_cache_subtitle)
                    },
                    isTablet = isTablet,
                    onClick = {
                        if (!cleared) {
                            ContinueWatchingEnrichmentCache.clearAll(ProfileRepository.activeProfileId)
                            cleared = true
                            scope.launch {
                                WatchProgressRepository.clearLocalAndForceSnapshotRefreshFromServer(
                                    ProfileRepository.activeProfileId,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
    if (BackupRestoreRepository.isSupported) {
        item {
            var exportState by rememberSaveable { mutableStateOf(BACKUP_STATE_IDLE) }
            var importState by rememberSaveable { mutableStateOf(BACKUP_STATE_IDLE) }
            var showRestoreDialog by rememberSaveable { mutableStateOf(false) }
            val exportBackup = rememberBackupExportLauncher { result ->
                exportState = if (result.isSuccess) {
                    BACKUP_STATE_DONE
                } else {
                    BACKUP_STATE_FAILED
                }
            }
            val importBackup = rememberBackupImportLauncher { result ->
                importState = when {
                    result.isSuccess -> BACKUP_STATE_DONE
                    result.exceptionOrNull() is BackupFormatException -> BACKUP_STATE_INVALID
                    else -> BACKUP_STATE_FAILED
                }
            }

            SettingsSection(
                title = stringResource(Res.string.settings_advanced_section_backup),
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    SettingsNavigationRow(
                        title = stringResource(Res.string.settings_advanced_backup_export),
                        description = stringResource(
                            when (exportState) {
                                BACKUP_STATE_DONE -> Res.string.settings_advanced_backup_export_done
                                BACKUP_STATE_FAILED -> Res.string.settings_advanced_backup_export_failed
                                else -> Res.string.settings_advanced_backup_export_subtitle
                            },
                        ),
                        isTablet = isTablet,
                        onClick = exportBackup,
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = stringResource(Res.string.settings_advanced_backup_import),
                        description = stringResource(
                            when (importState) {
                                BACKUP_STATE_DONE -> Res.string.settings_advanced_backup_import_done
                                BACKUP_STATE_FAILED -> Res.string.settings_advanced_backup_import_failed
                                BACKUP_STATE_INVALID -> Res.string.settings_advanced_backup_import_invalid
                                else -> Res.string.settings_advanced_backup_import_subtitle
                            },
                        ),
                        isTablet = isTablet,
                        onClick = { showRestoreDialog = true },
                    )
                }
            }

            if (showRestoreDialog) {
                RestoreBackupDialog(
                    onConfirm = {
                        showRestoreDialog = false
                        importBackup()
                    },
                    onDismiss = {
                        showRestoreDialog = false
                    },
                )
            }
        }
    }
}

private const val BACKUP_STATE_IDLE = 0
private const val BACKUP_STATE_DONE = 1
private const val BACKUP_STATE_INVALID = 2
private const val BACKUP_STATE_FAILED = 3

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RestoreBackupDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    DialogSurface(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.settings_advanced_backup_restore_dialog_title),
    ) {
        Text(
            text = stringResource(Res.string.settings_advanced_backup_restore_dialog_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.colors.textMuted,
        )
        DialogButtons {
            DialogButton(
                text = stringResource(Res.string.action_cancel),
                onClick = onDismiss,
            )
            DialogButton(
                text = stringResource(Res.string.settings_advanced_backup_restore_confirm),
                onClick = onConfirm,
                style = DialogButtonStyle.Primary,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SentrySettingsDialog(
    enabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    DialogSurface(
        onDismissRequest = onDismiss,
        title = stringResource(
                        if (enabled) {
                            Res.string.sentry_disable_dialog_title
                        } else {
                            Res.string.sentry_enable_dialog_title
                        },
                    ),
    ) {
        Text(
            text = stringResource(
                if (enabled) {
                    Res.string.sentry_disable_dialog_subtitle
                } else {
                    Res.string.sentry_enable_dialog_subtitle
                },
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.colors.textMuted,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
        ) {
            SentryInfoSection(
                title = stringResource(Res.string.sentry_help_title),
                body = stringResource(Res.string.sentry_help_body),
            )
            SentryInfoSection(
                title = stringResource(Res.string.sentry_sent_title),
                body = stringResource(Res.string.sentry_sent_body),
            )
            SentryInfoSection(
                title = stringResource(Res.string.sentry_not_sent_title),
                body = stringResource(Res.string.sentry_not_sent_body),
            )
        }
        DialogButtons {
            DialogButton(
                text = stringResource(
                        if (enabled) {
                            Res.string.sentry_keep_enabled
                        } else {
                            Res.string.action_cancel
                        },
                    ),
                onClick = onDismiss,
            )
            DialogButton(
                text = stringResource(
                        if (enabled) {
                            Res.string.sentry_turn_off
                        } else {
                            Res.string.sentry_turn_on
                        },
                    ),
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                style = DialogButtonStyle.Primary,
            )
        }
    }
}

@Composable
private fun SentryInfoSection(
    title: String,
    body: String,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s4),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = tokens.colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.colors.textMuted,
        )
    }
}
