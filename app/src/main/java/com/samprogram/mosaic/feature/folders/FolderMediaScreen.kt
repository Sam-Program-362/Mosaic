package com.samprogram.mosaic.feature.folders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samprogram.mosaic.R
import com.samprogram.mosaic.core.model.FolderAccessStatus
import com.samprogram.mosaic.core.model.MediaItem
import com.samprogram.mosaic.core.model.MediaKind
import com.samprogram.mosaic.ui.theme.MosaicTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The indexed direct-child media of one selected folder.
 *
 * A folder that cannot currently be read still shows its last-known media with
 * an explicit unavailable notice, and neither a provider failure nor a pending
 * read is ever presented as an empty folder.
 */
@Composable
internal fun FolderMediaContent(
    folder: FolderSummary,
    mediaFlow: Flow<List<MediaItem>>,
    modifier: Modifier = Modifier,
) {
    // A null initial value distinguishes "not read yet" from "read and empty".
    val mediaState by mediaFlow.collectAsStateWithLifecycle(initialValue = null)
    val media = mediaState
    val unavailable = folder.accessStatus == FolderAccessStatus.UNAVAILABLE

    Column(modifier = modifier) {
        if (unavailable) {
            UnavailableNotice(
                message = folder.lastScanFailure?.let { scanFailureText(it) }
                    ?: stringResource(R.string.scan_failure_provider_error),
            )
        }

        when {
            media == null -> {
                if (!unavailable) LoadingMediaContent()
            }

            media.isNotEmpty() -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = media, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = { Text(text = item.displayName) },
                        supportingContent = { Text(text = mediaKindText(item.mediaKind)) },
                    )
                    HorizontalDivider()
                }
            }

            // Only claim the folder is empty when the last scan actually succeeded.
            !unavailable -> EmptyMediaContent()
        }
    }
}

@Composable
private fun UnavailableNotice(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun LoadingMediaContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.folder_media_loading))
    }
}

@Composable
private fun EmptyMediaContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.folder_media_empty_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.folder_media_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

/** The human-readable label for an indexed media kind. */
@Composable
internal fun mediaKindText(kind: MediaKind): String = stringResource(
    when (kind) {
        MediaKind.IMAGE -> R.string.media_kind_image
        MediaKind.GIF -> R.string.media_kind_gif
        MediaKind.VIDEO -> R.string.media_kind_video
    },
)

@Preview(showBackground = true)
@Composable
private fun FolderMediaPreview() {
    MosaicTheme {
        FolderMediaContent(
            folder = FolderSummary(
                id = "1",
                displayName = "Camera",
                accessStatus = FolderAccessStatus.AVAILABLE,
                mediaCount = 3,
                lastScanFailure = null,
            ),
            mediaFlow = flowOf(
                listOf(
                    MediaItem(
                        id = "a",
                        folderId = "1",
                        documentId = "a",
                        contentUri = "content://a",
                        displayName = "IMG_0001.jpg",
                        mediaKind = MediaKind.IMAGE,
                    ),
                    MediaItem(
                        id = "b",
                        folderId = "1",
                        documentId = "b",
                        contentUri = "content://b",
                        displayName = "loop.gif",
                        mediaKind = MediaKind.GIF,
                    ),
                    MediaItem(
                        id = "c",
                        folderId = "1",
                        documentId = "c",
                        contentUri = "content://c",
                        displayName = "clip.mp4",
                        mediaKind = MediaKind.VIDEO,
                    ),
                ),
            ),
        )
    }
}
