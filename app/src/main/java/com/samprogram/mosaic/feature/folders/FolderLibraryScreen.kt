package com.samprogram.mosaic.feature.folders

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samprogram.mosaic.MosaicApplication
import com.samprogram.mosaic.R
import com.samprogram.mosaic.core.model.FolderAccessStatus
import com.samprogram.mosaic.core.model.ScanFailure
import com.samprogram.mosaic.ui.theme.MosaicTheme

/**
 * The folder library: the screen that proves the folder-to-media flow.
 *
 * The same route hosts the library list and the direct-child media list of an
 * open folder. Navigation is a single piece of remembered state rather than a
 * navigation graph, which is all this two-destination flow currently needs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderLibraryRoute(modifier: Modifier = Modifier) {
    val viewModel = rememberFolderLibraryViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var folderPendingRemoval by remember { mutableStateOf<FolderSummary?>(null) }
    var openFolderId by rememberSaveable { mutableStateOf<String?>(null) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        val uri = data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            // Read the flags the picker actually granted instead of assuming.
            val grantedRead = (data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0
            viewModel.onFolderPicked(uri.toString(), grantedRead)
        }
    }

    val addFolder: () -> Unit = {
        folderPicker.launch(
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            },
        )
    }

    // Refresh on app start and on every resume.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    state.message?.let { message ->
        val text = libraryMessageText(message)
        LaunchedEffect(text) {
            snackbarHostState.showSnackbar(text)
            viewModel.onMessageShown()
        }
    }

    val openFolder = openFolderId?.let { id -> state.folders.firstOrNull { it.id == id } }

    BackHandler(enabled = openFolderId != null) { openFolderId = null }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = openFolder?.displayName ?: stringResource(R.string.library_title)) },
                navigationIcon = {
                    if (openFolderId != null) {
                        TextButton(onClick = { openFolderId = null }) {
                            Text(text = stringResource(R.string.action_back))
                        }
                    }
                },
                actions = {
                    if (openFolderId == null) {
                        TextButton(onClick = { viewModel.refresh(notifyUnavailable = true) }) {
                            Text(text = stringResource(R.string.action_refresh))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when {
            openFolderId == null -> LibraryContent(
                state = state,
                onAddFolder = addFolder,
                onOpenFolder = { openFolderId = it },
                onRemoveFolder = { folderPendingRemoval = it },
                modifier = contentModifier,
            )

            openFolder == null -> RemovedFolderContent(modifier = contentModifier)

            else -> FolderMediaContent(
                folder = openFolder,
                mediaFlow = remember(openFolder.id) { viewModel.mediaInFolder(openFolder.id) },
                modifier = contentModifier,
            )
        }
    }

    folderPendingRemoval?.let { folder ->
        RemoveFolderDialog(
            folderName = folder.displayName,
            onConfirm = {
                viewModel.removeFolder(folder.id)
                folderPendingRemoval = null
            },
            onDismiss = { folderPendingRemoval = null },
        )
    }
}

@Composable
private fun rememberFolderLibraryViewModel(): FolderLibraryViewModel {
    val context = LocalContext.current
    val repository = remember(context) {
        (context.applicationContext as MosaicApplication).container.folderLibraryRepository
    }
    return viewModel(factory = FolderLibraryViewModel.factory(repository))
}

@Composable
private fun LibraryContent(
    state: FolderLibraryUiState,
    onAddFolder: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onRemoveFolder: (FolderSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (state.isRefreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = onAddFolder,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(text = stringResource(R.string.action_add_folder))
        }

        when {
            state.isLoading && state.folders.isEmpty() -> LoadingContent()

            state.folders.isEmpty() -> EmptyLibraryContent()

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = state.folders, key = { it.id }) { folder ->
                    FolderRow(
                        folder = folder,
                        onOpen = { onOpenFolder(folder.id) },
                        onRemove = { onRemoveFolder(folder) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun FolderRow(
    folder: FolderSummary,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onOpen),
        headlineContent = { Text(text = folder.displayName) },
        supportingContent = {
            Column {
                Text(
                    text = stringResource(statusLabel(folder.accessStatus)) +
                        " · " +
                        pluralStringResource(
                            R.plurals.folder_media_count,
                            folder.mediaCount,
                            folder.mediaCount,
                        ),
                )
                folder.lastScanFailure?.let { failure ->
                    Text(
                        text = scanFailureText(failure),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        trailingContent = {
            TextButton(onClick = onRemove) {
                Text(text = stringResource(R.string.action_remove))
            }
        },
    )
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.library_loading))
    }
}

@Composable
private fun EmptyLibraryContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.library_empty_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.library_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun RemovedFolderContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.folder_media_removed),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RemoveFolderDialog(
    folderName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.remove_folder_title)) },
        text = { Text(text = stringResource(R.string.remove_folder_message, folderName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.action_remove))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

/** Resolves a typed library message to localized text. */
@Composable
internal fun libraryMessageText(message: LibraryMessage): String = when (message) {
    is LibraryMessage.Added ->
        pluralStringResource(R.plurals.message_added, message.count, message.count)

    is LibraryMessage.AlreadyRegistered ->
        pluralStringResource(R.plurals.message_already_registered, message.count, message.count)

    is LibraryMessage.UnavailableFolders ->
        pluralStringResource(R.plurals.message_unavailable_folders, message.count, message.count)

    LibraryMessage.AccessDenied -> stringResource(R.string.message_access_denied)

    LibraryMessage.InvalidSelection -> stringResource(R.string.message_invalid_selection)
}

@StringRes
internal fun statusLabel(status: FolderAccessStatus): Int = when (status) {
    FolderAccessStatus.AVAILABLE -> R.string.folder_status_available
    FolderAccessStatus.UNAVAILABLE -> R.string.folder_status_unavailable
}

/** Resolves a stored failure reason to localized text. */
@Composable
internal fun scanFailureText(failure: ScanFailure): String = stringResource(
    when (failure) {
        ScanFailure.ACCESS_NOT_GRANTED -> R.string.scan_failure_access_not_granted
        ScanFailure.ACCESS_DENIED -> R.string.scan_failure_access_denied
        ScanFailure.MISSING_DOCUMENT -> R.string.scan_failure_missing_document
        ScanFailure.PROVIDER_ERROR -> R.string.scan_failure_provider_error
        ScanFailure.IO_ERROR -> R.string.scan_failure_io_error
    },
)

@Preview(showBackground = true)
@Composable
private fun EmptyLibraryPreview() {
    MosaicTheme {
        LibraryContent(
            state = FolderLibraryUiState(isLoading = false),
            onAddFolder = {},
            onOpenFolder = {},
            onRemoveFolder = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FolderListPreview() {
    MosaicTheme {
        LibraryContent(
            state = FolderLibraryUiState(
                isLoading = false,
                folders = listOf(
                    FolderSummary(
                        id = "1",
                        displayName = "Camera",
                        accessStatus = FolderAccessStatus.AVAILABLE,
                        mediaCount = 12,
                        lastScanFailure = null,
                    ),
                    FolderSummary(
                        id = "2",
                        displayName = "Screenshots",
                        accessStatus = FolderAccessStatus.UNAVAILABLE,
                        mediaCount = 3,
                        lastScanFailure = ScanFailure.ACCESS_NOT_GRANTED,
                    ),
                ),
            ),
            onAddFolder = {},
            onOpenFolder = {},
            onRemoveFolder = {},
        )
    }
}
