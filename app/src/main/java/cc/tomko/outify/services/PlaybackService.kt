package cc.tomko.outify.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Binder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import cc.tomko.outify.MainActivity
import cc.tomko.outify.MediaSessionConstants
import cc.tomko.outify.R
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.spirc.SpircWrapper
import cc.tomko.outify.core.model.SpotifyUri
import cc.tomko.outify.data.dao.LikedDao
import cc.tomko.outify.data.metadata.TrackMetadataHelper
import cc.tomko.outify.data.repository.SettingsRepository
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.playback.Player
import cc.tomko.outify.utils.CoilBitmapLoader
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Singleton


@UnstableApi
@Singleton
@AndroidEntryPoint
class PlaybackService : MediaLibraryService(),
    androidx.media3.common.Player.Listener {
    companion object {
        const val ROOT = "root"
        const val TRACK = "track"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"
        const val SEARCH = "search"
        const val LIKED = "liked"
        const val RECENT = "recent"

        const val NOTIFICATION_ID = 4894
        const val CHANNEL_ID = "outify_channel_01"
        const val CHANNEL_NAME = "Media Playback"

        val TAG = PlaybackService::class.simpleName.toString()

        val AUDIO_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder().apply {
            setUsage(C.USAGE_MEDIA)
            setContentType(AUDIO_CONTENT_TYPE_MUSIC)
        }.build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Media playback controls"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private val scope = CoroutineScope(Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    private val offloadScope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    @Inject
    lateinit var player: Player

    @Inject
    lateinit var trackDatabase: TrackMetadataHelper

    @Inject
    lateinit var settings: SettingsRepository

    @Inject
    lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback

    @Inject
    lateinit var playbackStateHolder: PlaybackStateHolder

    @Inject
    lateinit var spirc: SpircWrapper

    @Inject
    lateinit var spClient: SpClient

    @Inject
    lateinit var likedDao: LikedDao

    private var mediaLibrarySession: MediaLibrarySession? = null
    private var keepAlive: Boolean = true
    private val binder = MusicBinder()

    private val becomingNoisyListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action != AudioManager.ACTION_AUDIO_BECOMING_NOISY) return
            if(player.playWhenReady) player.pause()
        }
    }

    override fun onGetSession(controller: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onCreate() {
        Log.i(TAG, "Starting PlaybackService")
        super.onCreate()

        player.setAudioAttributes(AUDIO_ATTRIBUTES, true)

        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Outify")
                .setContentText("Loading...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()
        )

        mediaLibrarySessionCallback.apply {
            service = this@PlaybackService
            toggleLike = ::toggleLike
            toggleStartRadio = ::toggleStartRadio
        }

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, mediaLibrarySessionCallback)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setBitmapLoader(
                CoilBitmapLoader(
                    scope,
                    context = this,
                )
            )
            .build()

        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val browserFuture = MediaBrowser.Builder(this, sessionToken).buildAsync()
        browserFuture.addListener({ browserFuture.get() }, MoreExecutors.directExecutor())

        scope.launch {
            playbackStateHolder.state
                .map { it.currentTrack }
                .distinctUntilChanged()
                .collect { track ->
                    updateNotification()
                }

            settings.keepalive.collect {
                keepAlive = it
            }
        }

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this@PlaybackService,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.app_name
            ).apply {
                setSmallIcon(R.drawable.ic_launcher_foreground)
            }
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                becomingNoisyListener,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                becomingNoisyListener,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            )
        }
    }

    fun toggleLike() {
        val item = player.currentMediaItem ?: return
        val id = item.mediaId.takeIf { it.isNotBlank() } ?: return

        offloadScope.launch {
            try {
                if (likedDao.containsTrack(id)) {
                    spClient.deleteItems(arrayOf("spotify:track:$id"))
                } else {
                    spClient.saveItems(arrayOf("spotify:track:$id"))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Toggle like failed", e)
            }
        }
    }

    fun toggleStartRadio() {
        val item = player.currentMediaItem ?: return
        val id = item.mediaId.takeIf { it.isNotBlank() } ?: return

        offloadScope.launch {
            try {
                spirc.startRadio(SpotifyUri.Track(id), false)
            } catch (e: Exception) {
                Log.w(TAG, "Start radio failed", e)
            }
        }
    }

    fun updateNotification() {
        val session = mediaLibrarySession ?: return
        val track = playbackStateHolder.state.value.currentTrack
        val hasTrack = track != null

        offloadScope.launch {
            val isLiked = if (hasTrack) {
                runCatching { likedDao.containsTrack(track.id) }.getOrDefault(false)
            } else false
            val buttons = listOf(
                CommandButton.Builder(
                    when (player.repeatMode) {
                        REPEAT_MODE_OFF -> CommandButton.ICON_SHUFFLE_OFF
                        REPEAT_MODE_ONE -> CommandButton.ICON_SHUFFLE_ON
                        REPEAT_MODE_ALL -> CommandButton.ICON_SHUFFLE_STAR
                        else -> {
                            Log.w(TAG, "Unknown repeat mode: ${player.repeatMode}")
                            CommandButton.ICON_SHUFFLE_OFF
                        }
                    }
                )
                    .setDisplayName(
                        getString(
                            when (player.repeatMode) {
                                REPEAT_MODE_OFF -> R.string.repeat_mode_off
                                REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                else -> {
                                    Log.w(
                                        TAG,
                                        "Unknown repeat mode for display name: ${player.repeatMode}"
                                    )
                                    R.string.repeat_mode_off
                                }
                            }
                        )
                    )
                    .setSessionCommand(MediaSessionConstants.CommandToggleRepeatMode)
                    .build(),
                CommandButton.Builder(
                    when (isLiked) {
                        true -> CommandButton.ICON_HEART_FILLED
                        false -> CommandButton.ICON_HEART_UNFILLED
                    }
                )
                    .setDisplayName(getString(R.string.like))
                    .setSessionCommand(MediaSessionConstants.CommandToggleLike)
                    .setEnabled(hasTrack)
                    .build(),
                CommandButton.Builder(CommandButton.ICON_RADIO)
                    .setDisplayName(getString(R.string.start_radio))
                    .setSessionCommand(MediaSessionConstants.CommandToggleStartRadio)
                    .setEnabled(hasTrack)
                    .build()
            )
            session.setMediaButtonPreferences(buttons)
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Log.w(TAG, "Player error ${error.errorCode}", error)

        Toast.makeText(
            this@PlaybackService,
            "Playback failed (${error.errorCode})",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPlaybackStateChanged(@androidx.media3.common.Player.State playbackState: Int) {
        if (playbackState == STATE_IDLE) {
            Log.i(TAG, "Playback idling")
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNotification()
        offloadScope.launch {
            settings.setRepeat(repeatMode != REPEAT_MODE_OFF)
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        offloadScope.launch {
            runCatching { spirc.shuffle(shuffleModeEnabled) }
        }
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        if (!keepAlive)
            return

        super.onUpdateNotification(session, startInForegroundRequired)
    }

    override fun onDestroy() {
        Log.i(TAG, "Terminating PlaybackService")

        mediaLibrarySession?.run {
            runCatching { player.stop() }
            runCatching { player.release() }
            runCatching { release() }
            mediaLibrarySession = null
        }
        scope.cancel()
        offloadScope.cancel()
        runCatching { unregisterReceiver(becomingNoisyListener) }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    inner class MusicBinder : Binder() {
        val service: PlaybackService
            get() = this@PlaybackService
    }
}
