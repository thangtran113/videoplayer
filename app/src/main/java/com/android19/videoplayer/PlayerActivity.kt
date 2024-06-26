package com.android19.videoplayer

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import com.android19.videoplayer.databinding.ActivityPlayerBinding
import com.android19.videoplayer.databinding.BoosterBinding
import com.android19.videoplayer.databinding.MoreFeaturesBinding
import com.android19.videoplayer.databinding.SpeedDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.DecimalFormat
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlin.system.exitProcess

@UnstableApi
class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var runnable: Runnable
    private var isSubtitle:Boolean =true
    private var moreTime: Int = 0
    companion object {
        private var timer: Timer? = null

        lateinit var player: ExoPlayer
        lateinit var playerList: ArrayList<Video>
        var position: Int = -1
        var isPlayerInitialized = false
        private var repeat : Boolean = false
        private var isFullScreen: Boolean = false
        private var isLocked: Boolean = false
        var pipStatus: Int = 0
        private lateinit var trackSelector: DefaultTrackSelector
        private lateinit var loudnessEnhancer: LoudnessEnhancer
        private var speed: Float = 1.0f
        //
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        this.enableEdgeToEdge()
        setTheme(R.style.playerActivityTheme)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v: View, insets: WindowInsetsCompat ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initializeLayout()
        initializeBinding()
        binding.forwardFrameBtn.setOnClickListener(DoubleClickListener(callback = object : DoubleClickListener.Callback {
            override fun doubleClicked() {
                binding.playerView.showController()
                binding.forwardButton.visibility = View.VISIBLE
                player.seekTo(player.currentPosition + 10000)
                moreTime = 0
            }
        }))
        binding.backwardFrameBtn.setOnClickListener(DoubleClickListener(callback = object : DoubleClickListener.Callback {
            override fun doubleClicked() {
                binding.playerView.showController()
                binding.backwardButton.visibility = View.VISIBLE
                player.seekTo(player.currentPosition - 10000)
                moreTime = 0
            }
        }))
    }

    @SuppressLint("PrivateResource")
    private fun initializeLayout() {
        when (intent.getStringExtra("class")) {
            "AllVideos" -> {
                playerList = ArrayList(MainActivity.videoList)
            }

            "FolderActivity" -> {
                playerList = ArrayList(FoldersActivity.currentFolderVideo)
            }
            "SearchedVideos" ->{
                playerList = ArrayList()
                playerList = ArrayList(MainActivity.searchList)
                createPlayer()
            }
        }
        createPlayer()
        if(repeat){
            binding.repeatButton.setImageResource(androidx.media3.ui.R.drawable.exo_legacy_controls_repeat_all)
        }
        else binding.repeatButton.setImageResource(androidx.media3.ui.R.drawable.exo_legacy_controls_repeat_off)
    }

    @SuppressLint("PrivateResource")
    private fun initializeBinding() {

        binding.backButton.setOnClickListener {
            finish()
        }
        binding.playButton.setOnClickListener {
            if (player.isPlaying) {
                pauseVideo()
            } else {
                playVideo()
            }
        }
        binding.nextButton.setOnClickListener {
            nextPreviousVideo()
        }
        binding.previousButton.setOnClickListener {
            nextPreviousVideo(isNext = false)
        }
        binding.repeatButton.setOnClickListener {
            if (repeat) {
                repeat = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                binding.repeatButton.setImageResource(androidx.media3.ui.R.drawable.exo_legacy_controls_repeat_off)
            } else {
                repeat = false
                player.repeatMode = Player.REPEAT_MODE_ONE
                binding.repeatButton.setImageResource(androidx.media3.ui.R.drawable.exo_legacy_controls_repeat_all)
            }
        }
        binding.fullscreenButton.setOnClickListener {
            if (isFullScreen) {
                isFullScreen = false
                playInFullScreen(enable = false)
            } else {
                isFullScreen = true
                playInFullScreen(enable = true)
            }
        }
        binding.lockButton.setOnClickListener {
            if (!isLocked) {
                //ẩn
                isLocked = true
                binding.playerView.hideController()
                binding.playerView.useController = false
                binding.lockButton.setImageResource(R.drawable.locked)
            } else {
                //hien
                isLocked = false
                binding.playerView.useController = true
                binding.playerView.showController()
                binding.lockButton.setImageResource(R.drawable.open_lock_icon)
            }
        }
        //da sua
        binding.moreFeaturesBtn.setOnClickListener {
            pauseVideo()
            val customDialog =
                LayoutInflater.from(this).inflate(R.layout.more_features, binding.root, false)
            val bindingMF = MoreFeaturesBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(this).setView(customDialog)
                .setOnCancelListener { playVideo() }
                .setBackground(ColorDrawable(0x803700B3.toInt()))
                .create()
            dialog.show()

            bindingMF.audioTrack.setOnClickListener {
                dialog.dismiss()
                playVideo()
                val audioTrack = ArrayList<String>()
                for (i in 0 until player.currentTrackGroups.length) {
                    if (player.currentTrackGroups.get(i)
                            .getFormat(0).selectionFlags == C.SELECTION_FLAG_DEFAULT
                    ) {
                        audioTrack.add(
                            Locale(
                                player.currentTrackGroups.get(i).getFormat(0).language.toString()
                            ).displayLanguage
                        )
                    }
                }

                val tempTracks = audioTrack.toArray(arrayOfNulls<CharSequence>(audioTrack.size))
                MaterialAlertDialogBuilder(this, R.style.alertDialog)
                    .setTitle("Select language")
                    .setOnCancelListener { playVideo() }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .setItems(tempTracks) { _, position ->
                        Toast.makeText(this, audioTrack[position] + "Selected", Toast.LENGTH_SHORT)
                            .show()
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .setPreferredAudioLanguage(audioTrack[position])
                        )
                    }
                    .create()
                    .show()
            }
            bindingMF.subtitlesBtn.setOnClickListener {
                if (isSubtitle) {
                    trackSelector.parameters =
                        DefaultTrackSelector.ParametersBuilder(this).setRendererDisabled(
                            C.TRACK_TYPE_VIDEO, true
                        ).build()
                    Toast.makeText(this, "Subtitles OFF", Toast.LENGTH_SHORT).show()
                    isSubtitle = false
                } else {
                    trackSelector.parameters =
                        DefaultTrackSelector.ParametersBuilder(this).setRendererDisabled(
                            C.TRACK_TYPE_VIDEO, false
                        ).build()
                    Toast.makeText(this, "Subtitles ON", Toast.LENGTH_SHORT).show()
                    isSubtitle = true
                }
                dialog.dismiss()
                playVideo()
            }
            bindingMF.audioBooster.setOnClickListener {
                dialog.dismiss()
                val customDialogB =
                    LayoutInflater.from(this).inflate(R.layout.booster, binding.root, false)
                val bindingB = BoosterBinding.bind(customDialogB)
                val dialogB = MaterialAlertDialogBuilder(this).setView(customDialogB)
                    .setOnCancelListener { playVideo() }
                    .setPositiveButton("OK") { self, _ ->
                        loudnessEnhancer.setTargetGain(bindingB.verticalBar.progress * 100)
                        playVideo()
                        self.dismiss()
                    }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .create()
                dialogB.show()

                bindingB.verticalBar.progress = loudnessEnhancer.targetGain.toInt() / 100
                bindingB.progressText.text =
                    "AudioBoost\n\n${loudnessEnhancer.targetGain.toInt() / 10}"
                bindingB.verticalBar.setOnProgressChangeListener {
                    bindingB.progressText.text = "AudioBoost\n\n${it * 10}"
                }
            }
            bindingMF.speedBtn.setOnClickListener {
                dialog.dismiss()
                playVideo()
                val customDialogS =
                    LayoutInflater.from(this).inflate(R.layout.speed_dialog, binding.root, false)
                val bindingS = SpeedDialogBinding.bind(customDialogS)
                val dialogS = MaterialAlertDialogBuilder(this).setView(customDialogS)
                    .setCancelable(false)
                    .setPositiveButton("OK") { self, _ ->
                        self.dismiss()
                    }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .create()
                dialogS.show()
                bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                bindingS.minusBtn.setOnClickListener {
                    changeSpeed(isIncrement = false)
                    bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                }
                bindingS.plusBtn.setOnClickListener {
                    changeSpeed(isIncrement = true)
                    bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                }

            }
            bindingMF.sleepTimer.setOnClickListener {
                dialog.dismiss()
                if (timer != null) Toast.makeText(
                    this,
                    "Timer Already Running!!\nClose App to Reset Timer!!",
                    Toast.LENGTH_SHORT
                ).show()
                else {
                    var sleepTime = 15
                    val customDialogS = LayoutInflater.from(this)
                        .inflate(R.layout.speed_dialog, binding.root, false)
                    val bindingS = SpeedDialogBinding.bind(customDialogS)
                    val dialogS = MaterialAlertDialogBuilder(this).setView(customDialogS)
                        .setCancelable(false)
                        .setPositiveButton("OK") { self, _ ->
                            timer = Timer()
                            val task = object : TimerTask() {
                                override fun run() {
                                    moveTaskToBack(true)
                                    exitProcess(1)
                                }
                            }
                            timer!!.schedule(task, sleepTime * 60 * 1000.toLong())
                            self.dismiss()
                            playVideo()
                        }
                        .setBackground(ColorDrawable(0x803700B3.toInt()))
                        .create()
                    dialogS.show()
                    bindingS.speedText.text = "$sleepTime Min"
                    bindingS.minusBtn.setOnClickListener {
                        if (sleepTime > 15) sleepTime -= 15
                        bindingS.speedText.text = "$sleepTime Min"
                    }
                    bindingS.plusBtn.setOnClickListener {
                        if (sleepTime < 120) sleepTime += 15
                        bindingS.speedText.text = "$sleepTime Min"
                    }
                }
            }
            bindingMF.pipModeBtn.setOnClickListener {
                val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                        android.os.Process.myUid(),
                        packageName
                    ) ==
                            AppOpsManager.MODE_ALLOWED
                } else {
                    false
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (status) {
                        this.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                        dialog.dismiss()
                        binding.playerView.hideController()
                        playVideo()
                        pipStatus =0
                    } else {
                        val intent = Intent(
                            "android.settings.PICTURE_IN_PICTURE_SETTINGS",
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    }
                } else {
                    Toast.makeText(this, "Feature Not Supported!!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    playVideo()
                }
            }


        }
    }
    @OptIn(UnstableApi::class)
    private fun createPlayer() {
        try {
            player.release()
        } catch (e: Exception) { }
        speed = 1.0f
        trackSelector = DefaultTrackSelector(this)
        binding.videoTilte.text = playerList[position].title
        binding.videoTilte.isSelected = true
        player = ExoPlayer.Builder(this).setTrackSelector(trackSelector).build()
        binding.playerView.player = player
        val mediaItem = MediaItem.fromUri(playerList[position].artUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        isPlayerInitialized = true
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) nextPreviousVideo()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                setPlayButtonImage(isPlaying)
            }
        })
        playInFullScreen(enable = isFullScreen)
        setVisibility()
        loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
        loudnessEnhancer.enabled = true
    }
    private fun setPlayButtonImage(isPlaying: Boolean) {
        if (isPlaying) {
            binding.playButton.setImageResource(R.drawable.pauseicon)
        } else {
            binding.playButton.setImageResource(R.drawable.play_icon)
        }
    }


    override fun onStart() {
        super.onStart()
        if (!isPlayerInitialized) {
            createPlayer()
        } else {
            setPlayButtonImage(player.isPlaying)
        }
    }
    override fun onStop() {
        super.onStop()
        if (isPlayerInitialized) {
            player.release()
            isPlayerInitialized = false
        }
    }

    private fun playVideo() {
        binding.playButton.setImageResource(R.drawable.pauseicon)
        player.play()
    }

    private fun pauseVideo() {
        binding.playButton.setImageResource(R.drawable.play_icon)
        player.pause()
    }

    private fun nextPreviousVideo(isNext: Boolean = true) {
        if (isNext) setPosition()
        else setPosition(isIncrement = false)
        createPlayer()
    }

    private fun setPosition(isIncrement: Boolean = true) {
        if(!repeat){
            if (isIncrement){
                if(playerList.size -1 == position)
                    position = 0
                else ++position
            }else{
                if(position == 0)
                    position = playerList.size - 1
                else --position
            }
        }
    }
    private fun changeSpeed(isIncrement: Boolean) {
        if (isIncrement) {
            if (speed <= 2.9f) {
                speed += 0.10f //speed = speed + 0.10f
            }
        } else {
            if (speed > 0.20f) {
                speed -= 0.10f
            }
        }
        player.setPlaybackSpeed(speed)
    }
    @SuppressLint("PrivateResource")
    private fun playInFullScreen(enable:Boolean){
        if(enable){
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            binding.fullscreenButton.setImageResource(androidx.media3.ui.R.drawable.exo_ic_fullscreen_exit)
        }else{
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            binding.fullscreenButton.setImageResource(R.drawable.fullscreen_icon)
        }
    }

    private fun setVisibility(){
        runnable = Runnable{
            if(binding.playerView.isControllerFullyVisible) changeVisibility(View.VISIBLE)
            else changeVisibility(View.INVISIBLE)
            Handler(Looper.getMainLooper()).postDelayed(runnable,300)
        }
        Handler(Looper.getMainLooper()).postDelayed(runnable,0)
    }
    private fun changeVisibility(visibility:Int){
        binding.topController.visibility = visibility
        binding.bottomController.visibility = visibility
        binding.playButton.visibility = visibility

        if(isLocked) binding.lockButton.visibility = View.VISIBLE
        else    binding.lockButton.visibility = visibility
        if(moreTime ==2){
            binding.backwardButton.visibility = View.GONE
            binding.forwardButton.visibility = View.GONE
        }else ++moreTime
        //khoá màn hình - ẩn
//        binding.backwardFrameBtn.visibility = visibility
//        binding.forwardFrameBtn.visibility = visibility



    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (pipStatus != 0) {
            finish()
            val intent = Intent(this, PlayerActivity::class.java)
            when (pipStatus) {
                1 -> intent.putExtra("class", "FolderActivity")
                2 -> intent.putExtra("class", "SearchedVideos")
                3 -> intent.putExtra("class", "AllVideos")
            }
            startActivity(intent)
        }
        if (!isInPictureInPictureMode) pauseVideo()

    }
    override fun onDestroy() {
        super.onDestroy()
        if (isPlayerInitialized) {
            player.release()
            isPlayerInitialized = false
        }
    }
}