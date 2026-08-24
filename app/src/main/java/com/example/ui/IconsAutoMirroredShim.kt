package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector

val Icons.AutoMirrored: AutoMirroredIcons
    get() = AutoMirroredIcons

object AutoMirroredIcons {
    val Filled: FilledIcons = FilledIcons
}

object FilledIcons {
    val ArrowBack: ImageVector
        get() = Icons.Default.ArrowBack
    val List: ImageVector
        get() = Icons.Default.List
}
