package code.name.monkey.retromusic.helper

import android.graphics.PorterDuff
import android.widget.ImageView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.service.MusicService

object ShuffleRepeatUIHelper {

    fun updateShuffleState(
        shuffleButton: ImageView,
        activeColor: Int,
        disabledColor: Int
    ) {
        shuffleButton.setColorFilter(
            when (MusicPlayerRemote.shuffleMode) {
                MusicService.SHUFFLE_MODE_SHUFFLE -> activeColor
                else -> disabledColor
            }, PorterDuff.Mode.SRC_IN
        )
    }

    fun updateRepeatState(
        repeatButton: ImageView,
        activeColor: Int,
        disabledColor: Int
    ) {
        when (MusicPlayerRemote.repeatMode) {
            MusicService.REPEAT_MODE_NONE -> {
                repeatButton.setImageResource(R.drawable.ic_repeat)
                repeatButton.setColorFilter(disabledColor, PorterDuff.Mode.SRC_IN)
            }
            MusicService.REPEAT_MODE_ALL -> {
                repeatButton.setImageResource(R.drawable.ic_repeat)
                repeatButton.setColorFilter(activeColor, PorterDuff.Mode.SRC_IN)
            }
            MusicService.REPEAT_MODE_THIS -> {
                repeatButton.setImageResource(R.drawable.ic_repeat_one)
                repeatButton.setColorFilter(activeColor, PorterDuff.Mode.SRC_IN)
            }
        }
    }
}