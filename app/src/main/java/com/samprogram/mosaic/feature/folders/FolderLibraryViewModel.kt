package com.samprogram.mosaic.feature.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.samprogram.mosaic.core.model.FolderAccessStatus
import com.samprogram.mosaic.core.model.MediaItem
import com.samprogram.mosaic.core.model.ScanFailure
import com.samprogram.mosaic.data.repository.AddFolderResult
import com.samprogram.mosaic.data.repository.FolderLibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One row in the folder library. */
data class FolderSummary(
    val id: String,
    val displayName: String,
    val accessStatus: FolderAccessStatus,
    val mediaCount: Int,
    val lastScanFailure: ScanFailure?,
)

/**
 * A user notification. These are typed rather than pre-formatted so the
 * ViewModel stays free of Android resources and the UI localizes them.
 */
sealed interface LibraryMessage {
    data class Added(val count: Int) : LibraryMessage
    data class AlreadyRegistered(val count: Int) : LibraryMessage
    data class UnavailableFolders(val count: Int) : LibraryMessage
    data object AccessDenied : LibraryMessage
    data object InvalidSelection : LibraryMessage
}

data class FolderLibraryUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val folders: List<FolderSummary> = emptyList(),
    val message: LibraryMessage? = null,
)

class FolderLibraryViewModel(
    private val repository: FolderLibraryRepository,
) : ViewModel() {

    private val isLoading = MutableStateFlow(true)
    private val isRefreshing = MutableStateFlow(false)
    private val message = MutableStateFlow<LibraryMessage?>(null)

    val uiState: StateFlow<FolderLibraryUiState> = combine(
        repository.observeFolders(),
        repository.observeMediaCounts(),
        isLoading,
        isRefreshing,
        message,
    ) { folders, counts, loading, refreshing, currentMessage ->
        FolderLibraryUiState(
            isLoading = loading,
            isRefreshing = refreshing,
            folders = folders.map { folder ->
                FolderSummary(
                    id = folder.id,
                    displayName = folder.displayName,
                    accessStatus = folder.accessStatus,
                    mediaCount = counts[folder.id] ?: 0,
                    lastScanFailure = folder.lastScanFailure,
                )
            },
            message = currentMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = FolderLibraryUiState(),
    )

    fun mediaInFolder(folderId: String): Flow<List<MediaItem>> = repository.observeMediaInFolder(folderId)

    /**
     * Rescans every registered folder. Called when the screen starts or resumes
     * and for explicit refreshes. [notifyUnavailable] is set only for refreshes
     * the user asked for, so a resume does not raise a snackbar every time.
     */
    fun refresh(notifyUnavailable: Boolean = false) {
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                val report = repository.refreshAll()
                if (notifyUnavailable && report.unavailableCount > 0) {
                    message.value = LibraryMessage.UnavailableFolders(report.unavailableCount)
                }
            } finally {
                isRefreshing.value = false
                isLoading.value = false
            }
        }
    }

    /** Registers or rescans the folder the picker returned. */
    fun onFolderPicked(treeUri: String, grantedRead: Boolean) {
        viewModelScope.launch {
            message.value = when (val result = repository.addFolder(treeUri, grantedRead)) {
                is AddFolderResult.Added -> LibraryMessage.Added(result.indexedCount)
                is AddFolderResult.AlreadyRegistered -> LibraryMessage.AlreadyRegistered(result.indexedCount)
                AddFolderResult.AccessDenied -> LibraryMessage.AccessDenied
                AddFolderResult.InvalidSelection -> LibraryMessage.InvalidSelection
            }
            isLoading.value = false
        }
    }

    fun removeFolder(folderId: String) {
        viewModelScope.launch { repository.removeFolder(folderId) }
    }

    fun onMessageShown() {
        message.value = null
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(repository: FolderLibraryRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { FolderLibraryViewModel(repository) }
        }
    }
}
