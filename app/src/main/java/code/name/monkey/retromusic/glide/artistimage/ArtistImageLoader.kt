package code.name.monkey.retromusic.glide.artistimage

import android.content.Context
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import java.io.InputStream

class ArtistImageLoader(
    val context: Context
) : ModelLoader<ArtistImage, InputStream> {

    override fun buildLoadData(
        model: ArtistImage,
        width: Int,
        height: Int,
        options: Options
    ): LoadData<InputStream> {
        return LoadData(
            ObjectKey(model.artist.name),
            ArtistImageFetcher(context, model)
        )
    }

    override fun handles(model: ArtistImage): Boolean {
        return true
    }
}

class Factory(
    val context: Context
) : ModelLoaderFactory<ArtistImage, InputStream> {

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<ArtistImage, InputStream> {
        return ArtistImageLoader(context)
    }

    override fun teardown() {}
}
