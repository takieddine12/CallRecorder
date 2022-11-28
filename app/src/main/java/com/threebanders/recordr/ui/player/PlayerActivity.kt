package com.threebanders.recordr.ui.player

import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.chibde.visualizer.LineBarVisualizer
import com.sdsmdg.harjot.crollerTest.Croller
import com.sdsmdg.harjot.crollerTest.Croller.onProgressChangedListener
import com.threebanders.recordr.R
import com.threebanders.recordr.ui.BaseActivity
import com.threebanders.recordr.ui.contact.ContactDetailFragment
import core.threebanders.recordr.CoreUtil
import core.threebanders.recordr.CrLog
import core.threebanders.recordr.data.Recording
import core.threebanders.recordr.player.AudioPlayer
import core.threebanders.recordr.player.PlaybackListenerInterface
import core.threebanders.recordr.player.PlayerAdapter

class PlayerActivity : BaseActivity() {
    var player: AudioPlayer? = null
    var recording: Recording? = null
    var playPause: ImageButton? = null
    var resetPlaying: ImageButton? = null
    lateinit var recordingInfo: TextView
    var playSeekBar: SeekBar? = null
    var playedTime: TextView? = null
    var totalTime: TextView? = null
    var userIsSeeking = false
    var visualizer: LineBarVisualizer? = null
    var audioManager: AudioManager? = null
    var phoneVolume = 0
    lateinit var gainControl: Croller
    lateinit var volumeControl: Croller
    public override fun createFragment(): Fragment? {
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme()
        setContentView(R.layout.player_activity)
        val toolbar = findViewById<Toolbar>(R.id.toolbar_player)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true)
            actionBar.setTitle(R.string.player_title)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }
        recording = intent.getParcelableExtra(ContactDetailFragment.Companion.RECORDING_EXTRA)
        visualizer = findViewById(R.id.visualizer)
        visualizer?.setColor(resources.getColor(R.color.colorAccentLighter))
        visualizer?.setDensity(
            if (resources.configuration.orientation ==
                Configuration.ORIENTATION_PORTRAIT
            ) DENSITY_PORTRAIT else DENSITY_LANDSCAPE.toFloat()
        )
        //crash report nr. 886:
        try {
            visualizer?.setPlayer(AUDIO_SESSION_ID)
        } catch (exc: Exception) {
            CrLog.log(CrLog.ERROR, "Error initializing visualizer.")
            visualizer = null
        }
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        playPause = findViewById(R.id.test_player_play_pause)
        resetPlaying = findViewById(R.id.test_player_reset)
        playSeekBar = findViewById(R.id.play_seekbar)
        playedTime = findViewById(R.id.test_play_time_played)
        totalTime = findViewById(R.id.test_play_total_time)
        playPause?.setOnClickListener(View.OnClickListener { view: View? ->
            if (player!!.playerState == PlayerAdapter.State.PLAYING) {
                player!!.pause()
                playPause?.setBackground(resources.getDrawable(R.drawable.player_play))
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else if (player!!.playerState == PlayerAdapter.State.PAUSED ||
                player!!.playerState == PlayerAdapter.State.INITIALIZED
            ) {
                player!!.play()
                playPause?.setBackground(resources.getDrawable(R.drawable.player_pause))
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        })
        resetPlaying?.setOnClickListener(View.OnClickListener { view: View? ->
            if (player!!.playerState == PlayerAdapter.State.PLAYING) playPause?.setBackground(
                resources.getDrawable(R.drawable.player_play)
            )
            player!!.reset()
        })
        playSeekBar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            var userSelectedPosition = 0
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) userSelectedPosition = progress
                playedTime?.setText(CoreUtil.getDurationHuman(progress.toLong(), false))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                userIsSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                userIsSeeking = false
                player!!.seekTo(userSelectedPosition)
            }
        })
        gainControl = findViewById(R.id.gain_control)
        player?.setGain(25.0F)
        gainControl.progress = 25
        gainControl.setOnProgressChangedListener(
            onProgressChangedListener { progress: Int -> player!!.setGain(progress.toFloat()) }
        )
        volumeControl = findViewById(R.id.volume_control)
        if (audioManager != null) {
            volumeControl.setMax(audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
            phoneVolume = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
            volumeControl.setProgress(phoneVolume)
        }
        volumeControl.setOnProgressChangedListener(
            onProgressChangedListener { progress: Int ->
                audioManager!!.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    progress,
                    0
                )
            }
        )
        recordingInfo = findViewById(R.id.recording_info)
        recordingInfo.setText(
            String.format(
                resources.getString(R.string.recording_info),
                recording!!.name, recording!!.getHumanReadingFormat(applicationContext)
            )
        )

//        Log.wtf(TAG, "Available width: " + getResources().getDisplayMetrics().widthPixels);
//        Log.wtf(TAG, "Density: " + getResources().getDisplayMetrics().density);
//        Log.wtf(TAG, "Density dpi: " + getResources().getDisplayMetrics().densityDpi);
//        Log.wtf(TAG, "Density scaled: " + getResources().getDisplayMetrics().scaledDensity);
    }

    //necesar pentru că dacă apăs pur și simplu pe săgeata back îmi apelează onCreate al activității contactdetail
    //fără un obiect Contact valid.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else super.onOptionsItemSelected(item)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (visualizer != null) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) visualizer!!.setDensity(
                DENSITY_LANDSCAPE.toFloat()
            ) else visualizer!!.setDensity(DENSITY_PORTRAIT.toFloat())
        }
    }

    override fun onStart() {
        super.onStart()
        //        if(player.getPlayerState() == PlayerAdapter.State.UNINITIALIZED ||
//                player.getPlayerState() == PlayerAdapter.State.STOPPED) {
        player = AudioPlayer(PlaybackListener())
        playedTime!!.text = "00:00"
        if (!player!!.loadMedia(recording!!.path)) return
        totalTime!!.text = CoreUtil.getDurationHuman(player!!.totalDuration.toLong(), false)
        player!!.setGain(gainControl!!.progress.toFloat())
        //        }
        val pref = prefs
        val currentPosition = pref!!.getInt(CURRENT_POS, 0)
        val isPlaying = pref.getBoolean(IS_PLAYING, true)
        if (!player!!.setMediaPosition(currentPosition)) {
            return
        }
        if (isPlaying) {
            playPause!!.background = resources.getDrawable(R.drawable.player_pause)
            player!!.play()
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            playPause!!.background = resources.getDrawable(R.drawable.player_play)
            player!!.playerState = PlayerAdapter.State.PAUSED
        }
    }

    override fun onStop() {
        super.onStop()
        val pref = prefs
        val editor = pref!!.edit()
        editor.putInt(CURRENT_POS, player!!.currentPosition)
        editor.putBoolean(IS_PLAYING, player!!.playerState == PlayerAdapter.State.PLAYING)
        editor.apply()
        player!!.stopPlayer()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) //e necesar?
    }

    override fun onDestroy() {
        super.onDestroy()
        val pref = prefs
        val editor = pref!!.edit()
        editor.remove(IS_PLAYING)
        editor.remove(CURRENT_POS)
        editor.apply()
        if (visualizer != null) visualizer!!.release()
        if (audioManager != null) audioManager!!.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            phoneVolume,
            0
        )
    }

    internal inner class PlaybackListener : PlaybackListenerInterface {
        override fun onDurationChanged(duration: Int) {
            playSeekBar!!.max = duration
        }

        override fun onPositionChanged(position: Int) {
            if (!userIsSeeking) {
                if (Build.VERSION.SDK_INT >= 24) playSeekBar!!.setProgress(
                    position,
                    true
                ) else playSeekBar!!.progress = position
            }
        }

        override fun onPlaybackCompleted() {
            //a trebuit să folosesc asta pentru că în lolipop crăpa zicînd că nu am voie să updatez UI din thread secundar.
            playPause!!.post {
                playPause!!.background = resources.getDrawable(R.drawable.player_play)
            }
            player!!.reset()
        }

        override fun onError() {
            playPause!!.background = resources.getDrawable(R.drawable.player_play)
            playPause!!.isEnabled = false
            resetPlaying!!.isEnabled = false
            totalTime!!.text = "00:00"
            playSeekBar!!.isEnabled = false
            recordingInfo!!.text = resources.getString(R.string.player_error)
            recordingInfo!!.setTextColor(resources.getColor(R.color.red))
            volumeControl!!.isEnabled = false
            gainControl!!.isEnabled = false
        }

        override fun onReset() {
            player = AudioPlayer(PlaybackListener())
            if (player!!.loadMedia(recording!!.path)) player!!.setGain(gainControl!!.progress.toFloat())
        }
    }

    companion object {
        const val AUDIO_SESSION_ID = 0
        const val IS_PLAYING = "is_playing"
        const val CURRENT_POS = "current_pos"
        const val DENSITY_PORTRAIT = 70f
        const val DENSITY_LANDSCAPE = 150
    }
}