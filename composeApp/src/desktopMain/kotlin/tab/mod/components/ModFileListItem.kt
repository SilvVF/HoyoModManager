package tab.mod.components

import OS
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import tab.mod.state.ModDownloadState
import tab.mod.state.Progress
import ui.AppTheme
import java.time.LocalDateTime
import java.time.ZoneOffset

@Composable
fun ModFileListItem(
    downloadState: ModDownloadState,
    fileName: String,
    description: String,
    fileTags: List<String>,
    downloaded: Boolean,
    downloadProgress: Progress,
    unzipProgress: Progress,
    download: () -> Unit,
    uploadEpochSecond: Int? = null,
    modifier: Modifier = Modifier
) {
    var active by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(if (active) 4.dp else 0.dp)

    val date = remember {
        uploadEpochSecond?.let { OS.getRelativeTimeSpanString(it.toLong()) }.orEmpty()
    }

    Surface(
        tonalElevation = elevation,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .onPointerEvent(PointerEventType.Enter) { active = true }
            .onPointerEvent(PointerEventType.Exit) { active = false }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.fillMaxWidth(0.6f), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(fileName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        description,
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalContentColor.current.copy(alpha = 0.78f)
                    )
                    Spacer(Modifier.height(1.dp))
                    FlowRow {
                        fileTags.fastForEach {
                            val color = if (it == "ok" || it == "clean") {
                                Color(0xff166534)
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .padding(2.dp)
                                    .drawBehind {
                                        drawRoundRect(
                                            cornerRadius = CornerRadius(4f, 4f),
                                            color = color,
                                            size = size,
                                        )
                                    }
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
            Text(
                date,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(0.2f)
            )
            DownloadButton(
                downloadProgress,
                unzipProgress,
                downloadState,
                downloaded,
                download,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DownloadButton(
    downloadProgress: Progress,
    unzipProgress: Progress,
    downloadState: ModDownloadState,
    downloaded: Boolean,
    download: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier, Alignment.CenterEnd) {
        when (downloadState) {
            ModDownloadState.Idle -> {
                if (!downloaded) {
                    IconButton(
                        onClick = download
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddCircle,
                            contentDescription = null
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }
            ModDownloadState.Downloading -> {
                val totalProgress by remember(downloadProgress, unzipProgress) {
                    derivedStateOf {
                        (downloadProgress.frac * 0.25f) + (unzipProgress.frac * 0.75f).coerceIn(0f..1f)
                    }
                }

                val unzipString = remember(unzipProgress) {
                    val complete = OS.humanReadableByteCountBin(unzipProgress.complete)
                    val total = OS.humanReadableByteCountBin(unzipProgress.total)

                    "Unzipping:  $complete / $total"
                }

                val downloadString = remember(downloadProgress) {
                    val complete = OS.humanReadableByteCountBin(downloadProgress.complete)
                    val total = OS.humanReadableByteCountBin(downloadProgress.total)

                    "Downloading:  $complete / $total"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    CircularProgressIndicator(
                        progress = { totalProgress },
                    )
                    Column {
                        Text(downloadString)
                        Text(unzipString)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewModFileListItem() {
    AppTheme {
        Surface {
            LazyColumn(Modifier.fillMaxSize()) {
                items(30) {
                    ModFileListItem(
                        downloadState = ModDownloadState.Idle,
                        fileName = "samplefilename.zip",
                        fileTags = listOf("ok", "clean"),
                        downloaded = false,
                        downloadProgress = Progress.Zero,
                        description = "",
                        unzipProgress = Progress.Zero,
                        uploadEpochSecond = remember { LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toInt() },
                        download = {},
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}