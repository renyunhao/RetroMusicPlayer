package code.name.monkey.retromusic.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.net.toUri
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.extensions.showToast
import code.name.monkey.retromusic.model.Song
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

object RingtoneManager {
    fun setRingtone(context: Context, song: Song) {
        val uri = Uri.fromFile(File(song.data))
        val resolver = context.contentResolver

        try {
            Settings.System.putString(resolver, Settings.System.RINGTONE, uri.toString())
            val message = context
                .getString(R.string.x_has_been_set_as_ringtone, song.title)
            context.showToast(message)
        } catch (ignored: SecurityException) {
        }
    }

    fun requiresDialog(context: Context): Boolean {
        if (!Settings.System.canWrite(context)) {
            return true
        }
        return false
    }

    fun showDialog(context: Context) {
        return MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialogTheme)
            .setTitle(R.string.dialog_title_set_ringtone)
            .setMessage(R.string.dialog_message_set_ringtone)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = ("package:" + context.applicationContext.packageName).toUri()
                context.startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create().show()
    }
}
