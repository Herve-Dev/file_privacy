package com.hervedev.fileprivacy.ui.utils

import java.text.CharacterIterator
import java.text.SimpleDateFormat
import java.text.StringCharacterIterator
import java.util.Date
import java.util.Locale

fun humanReadableByteCountSI(bytes: Long): String {
    if (-1000 < bytes && bytes < 1000) {
        return "$bytes B"
    }
    val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
    var b = bytes
    while (b <= -999_950 || b >= 999_950) {
        b /= 1000
        ci.next()
    }
    return String.format(Locale.getDefault(), "%.1f %cB", b / 1000.0, ci.current())
}

fun formatLastModified(timestamp: Long): String {
    if (timestamp <= 0) return "Inconnue"
    val sdf = SimpleDateFormat("dd MMMM yyyy 'à' HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun getFileExtension(filename: String): String {
    val lastDot = filename.lastIndexOf('.')
    if (lastDot <= 0 || lastDot == filename.length - 1) return "Aucune"
    return filename.substring(lastDot + 1).lowercase(Locale.getDefault())
}
