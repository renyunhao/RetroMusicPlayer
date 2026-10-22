package code.name.monkey.retromusic.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Folder(
    val path: String,
    val name: String,
    val songCount: Int
) : Parcelable