package com.hervedev.fileprivacy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.domain.isApk
import com.hervedev.fileprivacy.domain.isAudio
import com.hervedev.fileprivacy.domain.isDocument
import com.hervedev.fileprivacy.domain.isImage
import com.hervedev.fileprivacy.domain.isVideo

@Composable
fun FileItem.getTypeColor(): Color {
    return when {
        isDirectory -> MaterialTheme.colorScheme.primary
        isImage() -> FileTypeBadges.ImageAccent
        isVideo() -> FileTypeBadges.VideoAccent
        isAudio() -> FileTypeBadges.AudioAccent
        isDocument() -> FileTypeBadges.DocumentAccent
        isApk() -> FileTypeBadges.ApkAccent
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
