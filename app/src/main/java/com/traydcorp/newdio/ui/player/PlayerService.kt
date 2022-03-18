package com.traydcorp.newdio.ui.player

import android.app.Service
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.traydcorp.newdio.utils.SharedPreference
import java.io.IOException


class PlayerService : Service(), MediaPlayer.OnPreparedListener,
    AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnErrorListener {

    var actionPlaying : ActionPlaying? = null
    lateinit var audioManager : AudioManager
    private lateinit var mediaData : String

    var mediaPlayer : MediaPlayer? = null
    private var resumePosition = 0
    val focusLock = Any()
    lateinit var focusRequest : AudioFocusRequest

    var playbackDelayed = false
    var playbackNowAuthorized = false
    var resumeOnFocusGain = false
    var focusGained = false
    var playBtn = false
    var isPlayed = false
    var isPlaying = false

    private val sharedPreferences = SharedPreference()

    private val iBinder: IBinder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder {
        return iBinder
    }

    inner class LocalBinder : Binder() {
        val service: PlayerService
            get() = this@PlayerService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val actionName = intent?.getStringExtra("actionName")

        if (actionName != null) {
            when (actionName) {
                ACTION_PLAY -> { // 재생 & 일시정지
                    if (actionPlaying != null) {
                        actionPlaying!!.playClicked()
                    }
                }

                ACTION_PREV -> { // 이전버튼
                    if (actionPlaying != null) {
                        actionPlaying!!.prevClicked(null)
                    }
                }

                ACTION_NEXT -> { // 다음 버튼
                    if (actionPlaying != null) {
                        actionPlaying!!.nextClicked(null)
                    }
                }

                ACTION_DELETE -> { // 중지
                    if (actionPlaying != null) {
                        actionPlaying!!.stopClicked()
                    }
                }
            }
        }

        return START_STICKY
    }

    fun setCallBack(actionPlaying: ActionPlaying) {
        this.actionPlaying = actionPlaying
    }

    fun startMedia() {
        isPlayed = true
        buildAudioFocusRequest()
        if (mediaPlayer?.isPlaying == false && requestAudioFocus()) {
            mediaPlayer?.start()
            actionPlaying!!.playPrepared(true)
        } else if (!requestAudioFocus()) {
            pauseMedia()
        }
    }


    fun stopMedia() {
        if (mediaPlayer == null) return
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
        }
    }

    fun pauseMedia() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            resumePosition = mediaPlayer!!.currentPosition
        }
    }

    fun resumeMedia() {
        if (!focusGained) {
            buildAudioFocusRequest()
            focusGained = false
        }
        if (mediaPlayer?.isPlaying == false && requestAudioFocus()) {
            mediaPlayer?.seekTo(resumePosition)
            mediaPlayer?.start()
        } else if (!requestAudioFocus()) {
            pauseMedia()
        }
    }


    fun createMediaPlayer(mediaFile : String) {
        mediaData = mediaFile
        if (mediaPlayer != null) {
            mediaPlayer?.release()
        }

        mediaPlayer = MediaPlayer()
        mediaPlayer!!.setOnPreparedListener(this)

        mediaPlayer?.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )

        try {
            mediaPlayer?.setDataSource(mediaFile)
        } catch (e: IOException) {
            e.printStackTrace()
            stopSelf()
            isPlaying = false
            return
        }

        mediaPlayer?.prepareAsync()
        mediaPlayer?.setOnCompletionListener {
            actionPlaying!!.nextClicked("onComplete")
        }
        mediaPlayer?.setOnErrorListener(this)
        isPlaying = true

    }


    private fun buildAudioFocusRequest() {
        Log.d(TAG, "AudioFocus >> called requestAudioFocus() / Build.VERSION: " + Build.VERSION.SDK_INT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                    setAudioAttributes(AudioAttributes.Builder().run {
                        setUsage(AudioAttributes.USAGE_GAME)
                        setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        build()
                    })
                    setAcceptsDelayedFocusGain(true)
                    setOnAudioFocusChangeListener(afChangeListener, handler)
                    setWillPauseWhenDucked(true)
                    build()
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(focusRequest)
        } else {
            audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        synchronized(focusLock) {
            playbackNowAuthorized = when (res) {
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> false
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    true
                }
                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                    playbackDelayed = true
                    false
                }
                else -> false
            }
        }
        return playbackNowAuthorized
    }

    override fun onPrepared(mp: MediaPlayer?) {
        val autoPlayResult = sharedPreferences.getShared(applicationContext, "autoPlay")

        // 재생이 되었던 적이 있으면 다른 기사 선택 시 알림창 업데이트
        if (isPlayed) {
            actionPlaying!!.playPrepared(false)
        }

        if (autoPlayResult == "") {
            startMedia() // 설정 값이 없을 때 자동 재생
        } else {
            when (autoPlayResult) {
                "true" -> startMedia() // 자동 재생
                "false" -> if (playBtn) startMedia() // 자동 재생이 아닐 때 playBtn 선택
            }
        }
        playBtn = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            isPlayed = false
        }
    }

    // 포커스 변경
    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN ->
                if (playbackDelayed || resumeOnFocusGain) { // 포커스 획득 미디어 재시작
                    synchronized(focusLock) {
                        playbackDelayed = false
                        resumeOnFocusGain = false
                    }
                    startMedia()
                    actionPlaying!!.playClicked()
                }
            AudioManager.AUDIOFOCUS_LOSS -> { // 포커스 손실 : 미디어 정지
                synchronized(focusLock) {
                    resumeOnFocusGain = false
                    playbackDelayed = false
                }
                pauseMedia()
                actionPlaying!!.playClicked()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> { // 포커스 손실 : 미디어 정지
                synchronized(focusLock) {
                    resumeOnFocusGain = true
                    playbackDelayed = false
                }
                pauseMedia()
                actionPlaying!!.playClicked()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { // 포커스 손실 : 미디어 정지
                synchronized(focusLock) {
                    resumeOnFocusGain = true
                    playbackDelayed = false
                }
                pauseMedia()
                actionPlaying!!.playClicked()
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (mediaPlayer?.isPlaying == true) //pauseMedia()
                actionPlaying!!.playClicked()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (mediaPlayer?.isPlaying == true) //pauseMedia()
                actionPlaying!!.playClicked()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (mediaPlayer?.isPlaying == true) //pauseMedia()
                actionPlaying!!.playClicked()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (mediaPlayer == null && mediaData != null) {
                    createMediaPlayer(mediaData)
                }
                else if (!mediaPlayer!!.isPlaying) {
                    focusGained = true
                    actionPlaying!!.playClicked()
                }
            }
        }
    }

    // media player 에러 로그
    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        when (what) {
            MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> Log.d(
                "MediaPlayer Error",
                "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK $extra"
            )
            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> Log.d(
                "MediaPlayer Error",
                "MEDIA ERROR SERVER DIED $extra"
            )
            MediaPlayer.MEDIA_ERROR_UNKNOWN -> Log.d(
                "MediaPlayer Error",
                "MEDIA ERROR UNKNOWN $extra"
            )
        }
        return false
    }

    companion object {
        const val ACTION_PLAY = "PLAY"
        const val ACTION_PREV= "PREVIOUS"
        const val ACTION_NEXT = "NEXT"
        const val ACTION_DELETE = "DELETE"
    }



}

