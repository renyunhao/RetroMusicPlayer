package code.name.monkey.retromusic.appwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import code.name.monkey.appthemehelper.util.MaterialValueHelper
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.activities.MainActivity
import code.name.monkey.retromusic.appwidgets.base.BaseAppWidget
import code.name.monkey.retromusic.extensions.getTintedDrawable
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.model.lyrics.AbsSynchronizedLyrics
import code.name.monkey.retromusic.model.lyrics.Lyrics
import code.name.monkey.retromusic.service.MusicService
import code.name.monkey.retromusic.service.MusicService.Companion.ACTION_DELETE_SONG
import code.name.monkey.retromusic.service.MusicService.Companion.ACTION_PREVIOUS
import code.name.monkey.retromusic.service.MusicService.Companion.ACTION_SKIP
import code.name.monkey.retromusic.service.MusicService.Companion.ACTION_TOGGLE_PAUSE
import code.name.monkey.retromusic.service.MusicService.Companion.CYCLE_REPEAT
import code.name.monkey.retromusic.service.MusicService.Companion.TOGGLE_SHUFFLE
import code.name.monkey.retromusic.util.LyricUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.RetroUtil
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition

class AppWidgetBig : BaseAppWidget() {
    private var target: Target<Bitmap>? = null
    private var lyrics: Lyrics? = null
    private var currentSongId: Long = -1
    private var lastBgColor: Int = 0
    private var lastPrimaryTextColor: Int = 0
    private var lastSecondaryTextColor: Int = 0

    override fun defaultAppWidget(context: Context, appWidgetIds: IntArray) {
        val appWidgetView = RemoteViews(
            context.packageName, R.layout.app_widget_big
        )

        appWidgetView.setViewVisibility(R.id.media_titles, View.INVISIBLE)
        appWidgetView.setTextViewText(R.id.lyrics_line1, "")
        appWidgetView.setTextViewText(R.id.lyrics_line2, "")
        appWidgetView.setImageViewResource(R.id.image, R.drawable.default_audio_art)

        val btnColor = MaterialValueHelper.getPrimaryTextColor(context, false)
        appWidgetView.setImageViewBitmap(
            R.id.button_next, context.getTintedDrawable(
                R.drawable.ic_skip_next, btnColor
            ).toBitmap()
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_prev, context.getTintedDrawable(
                R.drawable.ic_skip_previous, btnColor
            ).toBitmap()
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_toggle_play_pause, context.getTintedDrawable(
                R.drawable.ic_play_arrow_white_32dp, btnColor
            ).toBitmap()
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_delete, context.getTintedDrawable(
                R.drawable.ic_delete, btnColor
            ).toBitmap()
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_repeat, context.getTintedDrawable(
                R.drawable.ic_repeat, btnColor
            ).toBitmap()
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_shuffle, context.getTintedDrawable(
                R.drawable.ic_shuffle, btnColor
            ).toBitmap()
        )

        linkButtons(context, appWidgetView)
        pushUpdate(context, appWidgetIds, appWidgetView)
    }

    override fun performUpdate(service: MusicService, appWidgetIds: IntArray?) {
        val appWidgetView = RemoteViews(
            service.packageName, R.layout.app_widget_big
        )

        val isPlaying = service.isPlaying
        val song = service.currentSong

        if (song == Song.emptySong)
            return;

        if (song.title.isEmpty() && song.artistName.isEmpty()) {
            appWidgetView.setViewVisibility(R.id.media_titles, View.INVISIBLE)
        } else {
            appWidgetView.setViewVisibility(R.id.media_titles, View.VISIBLE)
            appWidgetView.setTextViewText(R.id.title, getSongTitle(song))
        }

        loadLyrics(service, song)

        updateLyricsView(appWidgetView, service.songProgressMillis)

        val p = RetroUtil.getScreenSize(service)
        val widgetImageSize = p.x.coerceAtMost(p.y)
        val appContext = service.applicationContext
        service.runOnUiThread {
            if (target != null) {
                Glide.with(service).clear(target)
            }
            target = Glide.with(appContext)
                .asBitmap()
                .load(RetroGlideExtension.getSongModel(song))
                .into(object : CustomTarget<Bitmap>(widgetImageSize, widgetImageSize) {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?,
                    ) {
                        updateWithBitmap(appContext, appWidgetView, appWidgetIds, resource, service)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        updateWithBitmap(appContext, appWidgetView, appWidgetIds, null, service)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }
    }

    private fun updateWithBitmap(
        context: Context,
        appWidgetView: RemoteViews,
        appWidgetIds: IntArray?,
        bitmap: Bitmap?,
        service: MusicService
    ) {
        val processor = if (bitmap != null) {
            MediaNotificationProcessor(context, bitmap)
        } else {
            MediaNotificationProcessor.errorColor(context)
        }

        lastBgColor = processor.backgroundColor
        lastPrimaryTextColor = processor.primaryTextColor
        lastSecondaryTextColor = processor.secondaryTextColor

        appWidgetView.setInt(R.id.widget_root, "setBackgroundColor", lastBgColor)
        appWidgetView.setTextColor(R.id.title, lastPrimaryTextColor)
        appWidgetView.setTextColor(R.id.text, lastSecondaryTextColor)
        appWidgetView.setTextColor(R.id.lyrics_line1, lastPrimaryTextColor)
        appWidgetView.setTextColor(R.id.lyrics_line2, lastSecondaryTextColor)

        if (bitmap != null) {
            appWidgetView.setImageViewBitmap(R.id.image, bitmap)
        } else {
            appWidgetView.setImageViewResource(R.id.image, R.drawable.default_audio_art)
        }

        val playPauseRes =
            if (service.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow_white_32dp

        appWidgetView.setImageViewBitmap(
            R.id.button_toggle_play_pause,
            service.getTintedDrawable(playPauseRes, lastPrimaryTextColor).toBitmap()
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_next,
            service.getTintedDrawable(R.drawable.ic_skip_next, lastPrimaryTextColor).toBitmap()
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_prev,
            service.getTintedDrawable(R.drawable.ic_skip_previous, lastPrimaryTextColor).toBitmap()
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_delete,
            service.getTintedDrawable(R.drawable.ic_delete, lastSecondaryTextColor).toBitmap()
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_repeat,
            service.getTintedDrawable(getRepeatDrawable(service), lastSecondaryTextColor).toBitmap()
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_shuffle,
            service.getTintedDrawable(getShuffleDrawable(service), lastSecondaryTextColor).toBitmap()
        )

        linkButtons(service, appWidgetView)
        pushUpdate(context, appWidgetIds, appWidgetView)
    }

    fun onSongChanged(service: MusicService) {
        currentSongId = -1
        lyrics = null
        loadLyrics(service, service.currentSong)
    }

    fun getCurrentLyricsLine(service: MusicService, progress: Int): String {
        val syncedLyrics = lyrics as? AbsSynchronizedLyrics
        if (syncedLyrics != null && syncedLyrics.isValid) {
            return syncedLyrics.getLine(progress) ?: ""
        }
        return ""
    }

    fun notifyLyricsLineChanged(service: MusicService, line: String) {
        if (!hasInstances(service)) return

        val appWidgetView = RemoteViews(
            service.packageName, R.layout.app_widget_big
        )

        if (line.isNotEmpty()) {
            val nextLine = getNextLine(service.songProgressMillis)
            appWidgetView.setTextViewText(R.id.lyrics_line1, line)
            appWidgetView.setTextViewText(R.id.lyrics_line2, nextLine)
        } else {
            appWidgetView.setTextViewText(R.id.lyrics_line1, "")
            appWidgetView.setTextViewText(R.id.lyrics_line2, "")
        }

        if (lastBgColor != 0) {
            appWidgetView.setTextColor(R.id.lyrics_line1, lastPrimaryTextColor)
            appWidgetView.setTextColor(R.id.lyrics_line2, lastSecondaryTextColor)
        }

        val appWidgetManager = AppWidgetManager.getInstance(service)
        val ids = appWidgetManager.getAppWidgetIds(
            ComponentName(service, AppWidgetBig::class.java)
        )
        pushUpdate(service, ids, appWidgetView)
    }

    private fun getNextLine(progress: Int): String {
        val syncedLyrics = lyrics as? AbsSynchronizedLyrics ?: return ""
        if (!syncedLyrics.isValid) return ""
        return syncedLyrics.getNextLine(progress) ?: ""
    }

    private fun loadLyrics(service: MusicService, song: Song) {
        if (song.id != currentSongId) {
            currentSongId = song.id
            lyrics = null
            Thread {
                val lrcFile = LyricUtil.getSyncedLyricsFile(song)
                val lrcData = if (lrcFile != null) {
                    LyricUtil.getStringFromLrc(lrcFile)
                } else {
                    LyricUtil.getEmbeddedSyncedLyrics(song.data)
                }
                if (!lrcData.isNullOrEmpty()) {
                    lyrics = Lyrics.parse(song, lrcData)
                }
            }.start()
        }
    }

    private fun updateLyricsView(appWidgetView: RemoteViews, progress: Int) {
        val syncedLyrics = lyrics as? AbsSynchronizedLyrics
        if (syncedLyrics != null && syncedLyrics.isValid) {
            val currentLine = syncedLyrics.getLine(progress)
            val nextLine = getNextLine(progress)
            appWidgetView.setTextViewText(R.id.lyrics_line1, currentLine)
            appWidgetView.setTextViewText(R.id.lyrics_line2, nextLine)
        } else {
            appWidgetView.setTextViewText(R.id.lyrics_line2, "")
            appWidgetView.setTextViewText(R.id.lyrics_line1, "")
        }
    }

    private fun hasInstances(context: Context): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        return appWidgetManager.getAppWidgetIds(
            ComponentName(context, AppWidgetBig::class.java)
        ).isNotEmpty()
    }

    private fun linkButtons(context: Context, views: RemoteViews) {
        val action = Intent(context, MainActivity::class.java)
            .putExtra(
                MainActivity.EXPAND_PANEL,
                PreferenceUtil.isExpandPanel
            )

        val serviceName = ComponentName(context, MusicService::class.java)

        action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        var pendingIntent =
            PendingIntent.getActivity(
                context, 0, action, PendingIntent.FLAG_IMMUTABLE
            )
        views.setOnClickPendingIntent(R.id.clickable_area, pendingIntent)
        views.setOnClickPendingIntent(R.id.image, pendingIntent)

        pendingIntent = buildPendingIntent(context, CYCLE_REPEAT, serviceName)
        views.setOnClickPendingIntent(R.id.button_repeat, pendingIntent)

        pendingIntent = buildPendingIntent(context, ACTION_PREVIOUS, serviceName)
        views.setOnClickPendingIntent(R.id.button_prev, pendingIntent)

        pendingIntent = buildPendingIntent(context, ACTION_TOGGLE_PAUSE, serviceName)
        views.setOnClickPendingIntent(R.id.button_toggle_play_pause, pendingIntent)

        pendingIntent = buildPendingIntent(context, ACTION_SKIP, serviceName)
        views.setOnClickPendingIntent(R.id.button_next, pendingIntent)

        pendingIntent = buildPendingIntent(context, ACTION_DELETE_SONG, serviceName)
        views.setOnClickPendingIntent(R.id.button_delete, pendingIntent)

        pendingIntent = buildPendingIntent(context, TOGGLE_SHUFFLE, serviceName)
        views.setOnClickPendingIntent(R.id.button_shuffle, pendingIntent)
    }

    companion object {
        const val NAME: String = "app_widget_big"
        private var mInstance: AppWidgetBig? = null

        val instance: AppWidgetBig
            @Synchronized get() {
                if (mInstance == null) {
                    mInstance = AppWidgetBig()
                }
                return mInstance!!
            }
    }
}
