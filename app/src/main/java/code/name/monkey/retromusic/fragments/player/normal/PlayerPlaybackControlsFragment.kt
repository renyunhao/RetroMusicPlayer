/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package code.name.monkey.retromusic.fragments.player.normal

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import code.name.monkey.appthemehelper.util.ATHUtil
import code.name.monkey.appthemehelper.util.ColorUtil
import code.name.monkey.appthemehelper.util.MaterialValueHelper
import code.name.monkey.appthemehelper.util.TintHelper
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.FragmentPlayerPlaybackControlsBinding
import code.name.monkey.retromusic.extensions.*
import code.name.monkey.retromusic.fragments.base.AbsPlayerControlsFragment
import code.name.monkey.retromusic.fragments.base.goToAlbum
import code.name.monkey.retromusic.fragments.base.goToArtist
import code.name.monkey.retromusic.fragments.base.goToLyrics
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.lyrics.CoverLrcView
import code.name.monkey.retromusic.util.LyricUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerPlaybackControlsFragment :
    AbsPlayerControlsFragment(R.layout.fragment_player_playback_controls) {

    private var _binding: FragmentPlayerPlaybackControlsBinding? = null
    private val binding get() = _binding!!

    override val progressSlider: Slider
        get() = binding.progressSlider

    override val shuffleButton: ImageButton
        get() = binding.shuffleButton

    override val repeatButton: ImageButton
        get() = binding.repeatButton

    override val nextButton: ImageButton
        get() = binding.nextButton

    override val previousButton: ImageButton
        get() = binding.previousButton

    override val songTotalTime: TextView
        get() = binding.songTotalTime

    override val songCurrentProgress: TextView
        get() = binding.songCurrentProgress

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlayerPlaybackControlsBinding.bind(view)

        setUpPlayPauseFab()
        binding.title.isSelected = true
        binding.text.isSelected = true
        binding.title.setOnClickListener {
            goToAlbum(requireActivity())
        }
        binding.text.setOnClickListener {
            goToArtist(requireActivity())
        }
        setupLyricsView()
        if (PreferenceUtil.showLyrics) {
            binding.lyricsView.isVisible = true
        }
    }

    private fun setupLyricsView() {
        binding.lyricsView.apply {
            setDraggable(true) { time ->
                MusicPlayerRemote.seekTo(time.toInt())
                MusicPlayerRemote.resumePlaying()
                true
            }
            setOnClickListener {
                goToLyrics(requireActivity())
            }
        }
    }

    fun updateLyricsVisibility() {
        if (PreferenceUtil.showLyrics) {
            loadLyrics()
            binding.lyricsView.isVisible = true
        } else {
            binding.lyricsView.isVisible = false
            binding.lyricsView.reset()
        }
    }

    private fun loadLyrics() {
        val song = MusicPlayerRemote.currentSong
        lifecycleScope.launch(Dispatchers.IO) {
            val lrcFile = LyricUtil.getSyncedLyricsFile(song)
            val embeddedLyrics = LyricUtil.getEmbeddedSyncedLyrics(song.data)
            withContext(Dispatchers.Main) {
                if (lrcFile != null) {
                    binding.lyricsView.loadLrc(lrcFile)
                } else if (embeddedLyrics != null) {
                    binding.lyricsView.loadLrc(embeddedLyrics)
                } else {
                    binding.lyricsView.reset()
                    binding.lyricsView.setLabel(context?.getString(R.string.no_lyrics_found))
                }
            }
        }
    }

    fun updateTime(time: Long) {
        binding.lyricsView.updateTime(time)
    }

    fun setLyricsViewColors(primaryColor: Int, secondaryColor: Int) {
        binding.lyricsView.apply {
            setCurrentColor(primaryColor)
            setTimeTextColor(primaryColor)
            setTimelineColor(primaryColor)
            setNormalColor(secondaryColor)
            setTimelineTextColor(primaryColor)
        }
    }

    override fun setColor(color: MediaNotificationProcessor) {
        val colorBg = ATHUtil.resolveColor(requireContext(), android.R.attr.colorBackground)
        if (ColorUtil.isColorLight(colorBg)) {
            lastPlaybackControlsColor =
                MaterialValueHelper.getSecondaryTextColor(requireContext(), true)
            lastDisabledPlaybackControlsColor =
                MaterialValueHelper.getSecondaryDisabledTextColor(requireContext(), true)
        } else {
            lastPlaybackControlsColor =
                MaterialValueHelper.getPrimaryTextColor(requireContext(), false)
            lastDisabledPlaybackControlsColor =
                MaterialValueHelper.getPrimaryDisabledTextColor(requireContext(), false)
        }

        val colorFinal = if (PreferenceUtil.isAdaptiveColor) {
            color.primaryTextColor
        } else {
            accentColor()
        }.ripAlpha()

        TintHelper.setTintAuto(
            binding.playPauseButton,
            MaterialValueHelper.getPrimaryTextColor(
                requireContext(),
                ColorUtil.isColorLight(colorFinal)
            ),
            false
        )
        TintHelper.setTintAuto(binding.playPauseButton, colorFinal, true)
        binding.progressSlider.applyColor(colorFinal)
        volumeFragment?.setTintable(colorFinal)
        setLyricsViewColors(Color.CYAN, Color.GRAY)
        updateRepeatState()
        updateShuffleState()
        updatePrevNextColor()
    }

    private fun updateSong() {
        val song = MusicPlayerRemote.currentSong
        binding.title.text = song.title
        binding.text.text = song.artistName

        if (PreferenceUtil.isSongInfo) {
            binding.songInfo.text = getSongInfo(song)
            binding.songInfo.show()
        } else {
            binding.songInfo.hide()
        }
    }

    override fun onServiceConnected() {
        updatePlayPauseDrawableState()
        updateRepeatState()
        updateShuffleState()
        updateSong()
        updateLyricsVisibility()
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        updateSong()
        if (PreferenceUtil.showLyrics) {
            loadLyrics()
        }
    }

    override fun onPlayStateChanged() {
        updatePlayPauseDrawableState()
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        super.onUpdateProgressViews(progress, total)
        if (PreferenceUtil.showLyrics) {
            updateTime(progress.toLong())
        }
    }

    override fun onRepeatModeChanged() {
        updateRepeatState()
    }

    override fun onShuffleModeChanged() {
        updateShuffleState()
    }

    private fun setUpPlayPauseFab() {
        binding.playPauseButton.setOnClickListener {
            if (MusicPlayerRemote.isPlaying) {
                MusicPlayerRemote.pauseSong()
            } else {
                MusicPlayerRemote.resumePlaying()
            }
            it.showBounceAnimation()
        }
    }

    private fun updatePlayPauseDrawableState() {
        if (MusicPlayerRemote.isPlaying) {
            binding.playPauseButton.setImageResource(R.drawable.ic_pause)
        } else {
            binding.playPauseButton.setImageResource(R.drawable.ic_play_arrow)
        }
    }

    public override fun show() {
        binding.playPauseButton.animate()
            .scaleX(1f)
            .scaleY(1f)
            .rotation(360f)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    public override fun hide() {
        binding.playPauseButton.apply {
            scaleX = 0f
            scaleY = 0f
            rotation = 0f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
