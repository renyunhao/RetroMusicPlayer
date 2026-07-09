package code.name.monkey.retromusic.glide.artistimage

import android.content.Context
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import java.io.File
import java.io.FileInputStream
import java.io.InputStream


class ArtistImageFetcher(
    private val context: Context,
    val model: ArtistImage,
) : DataFetcher<InputStream> {

    override fun getDataClass(): Class<InputStream> {
        return InputStream::class.java
    }

    override fun getDataSource(): DataSource {
        return DataSource.LOCAL
    }

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        callback.onDataReady(getFallbackAlbumImage())
    }

    private fun getFallbackAlbumImage(): InputStream? {
        val firstSong = model.artist.safeGetFirstAlbum().safeGetFirstSong()
        if (firstSong.id == -1L) return null
        return try {
            val file = File(firstSong.data)
            if (file.exists()) FileInputStream(file) else null
        } catch (e: Exception) {
            null
        }
    }

    override fun cleanup() {}

    override fun cancel() {}
}
