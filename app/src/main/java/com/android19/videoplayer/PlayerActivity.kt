package com.android19.videoplayer



import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android19.videoplayer.databinding.ActivityPlayerBinding
import com.android19.videoplayer.databinding.BoosterBinding
import com.android19.videoplayer.databinding.MoreFeaturesBinding
import com.android19.videoplayer.databinding.SpeedDialogBinding
import com.github.vkay94.dtpv.youtube.YouTubeOverlay
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.text.DecimalFormat
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs
import kotlin.system.exitProcess


//@UnstableApi
class PlayerActivity : AppCompatActivity(),AudioManager.OnAudioFocusChangeListener, GestureDetector.OnGestureListener{
    private lateinit var binding: ActivityPlayerBinding

    private var moreTime: Int = 0
    private lateinit var playPauseBtn: ImageButton
    private lateinit var fullScreenBtn: ImageButton
    private lateinit var forwardBtn: ImageButton
    private lateinit var backwardBtn: ImageButton
    private lateinit var videoTilte:TextView
    private lateinit var gestureDetectorCompat: GestureDetectorCompat
    private var minSwipeY: Float = 0f

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
        var playbackPosition: Long = 0
        var keepPlayingId : String =""
//
        private var audioManager: AudioManager? = null
        private var brightness: Int = 0
        private var volume: Int = 0
        ///
    }



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

        videoTilte = findViewById(R.id.videoTilte)
        playPauseBtn = findViewById(R.id.playButton )
        forwardBtn = findViewById(R.id.forwardButton )
        backwardBtn = findViewById(R.id.backwardButton )
        fullScreenBtn = findViewById(R.id.fullscreenButton)

        gestureDetectorCompat = GestureDetectorCompat(this,this)

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

        //Handling file intent
        try{
            if(intent.data?.scheme.contentEquals("content")){
                playerList = ArrayList()
                position = 0
                val cursor = contentResolver.query(intent.data!!, arrayOf(MediaStore.Video.Media.DATA),null,
                    null,null)
                cursor?.let {
                    it.moveToFirst()
                    val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                    val file = File(path)
                    val video = Video(id= "", title = file.name, duration = 0L, artUri = Uri.fromFile(file), path = path, size = ""
                        , folderName = "")
                    playerList.add(video)
                    cursor.close()
                }
                playVideo()
                initializeBinding()
            }
            else{
                initializeLayout()
                initializeBinding()
            }
        }catch(e:Exception){Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show()}

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
            "keepPlaying" ->{

                speed = 1.0f
                videoTilte.text = playerList[position].title
                videoTilte.isSelected = true
                doubleTapEnable()
                playVideo()
                playInFullScreen(enable = isFullScreen)
                seekBarFeature()

            }
        }
        createPlayer()
        if(repeat){
            findViewById<ImageButton>(R.id.repeatButton).setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_all)
        }
        else findViewById<ImageButton>(R.id.repeatButton).setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_off)
    }

    @SuppressLint("PrivateResource", "SourceLockedOrientationActivity")
    private fun initializeBinding() {
        findViewById<ImageButton>(R.id.orientationButton).setOnClickListener {
            requestedOrientation = if(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            else{
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
        }


        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
        playPauseBtn.setOnClickListener {
            if (player.isPlaying) {
                pauseVideo()
            } else {
                playVideo()
            }

        }
        findViewById<ImageButton>(R.id.nextButton).setOnClickListener {
            nextPreviousVideo()
        }
        findViewById<ImageButton>(R.id.previousButton).setOnClickListener {
            nextPreviousVideo(isNext = false)
        }
        findViewById<ImageButton>(R.id.repeatButton).setOnClickListener {
            if (repeat) {
                repeat = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                findViewById<ImageButton>(R.id.repeatButton).setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_all)
            } else {
                repeat = true
                player.repeatMode = Player.REPEAT_MODE_ONE
                findViewById<ImageButton>(R.id.repeatButton).setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_off)
            }
        }
        fullScreenBtn.setOnClickListener {
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
        findViewById<ImageButton>(R.id.moreFeaturesBtn).setOnClickListener {
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
                val audioList = ArrayList<String>()
                for (group in player.currentTracks.groups) {
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        val groupInfo = group.mediaTrackGroup
                        for (i in 0 until groupInfo.length) {
                            audioTrack.add(groupInfo.getFormat(i).language.toString())
                            audioList.add(
                                "${audioList.size + 1}. " + Locale(groupInfo.getFormat(i).language.toString()).displayLanguage
                                        + " (${groupInfo.getFormat(i).label})"
                            )
                        }
                    }
                }

                if (audioList[0].contains("")) audioList[0] = "1. Default Track"

                val tempTracks = audioList.toArray(arrayOfNulls<CharSequence>(audioList.size))
                val audioDialog = MaterialAlertDialogBuilder(this, R.style.alertDialog)
                    .setTitle("Select Language")
                    .setOnCancelListener { playVideo() }
                    .setPositiveButton("Off Audio") { self, _ ->
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters().setRendererDisabled(
                                C.TRACK_TYPE_AUDIO, true
                            )
                        )
                        self.dismiss()
                    }
                    .setItems(tempTracks) { _, position ->
                        Snackbar.make(binding.root, audioList[position] + " Selected", 3000).show()
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .setRendererDisabled(C.TRACK_TYPE_AUDIO, false)
                                .setPreferredAudioLanguage(audioTrack[position])
                        )
                    }
                    .create()
                audioDialog.show()
                audioDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE)
                audioDialog.window?.setBackgroundDrawable(ColorDrawable(0x99000000.toInt()))

            }
            bindingMF.subtitlesBtn.setOnClickListener {
                dialog.dismiss()
                playVideo()
                val subtitles = ArrayList<String>()
                val subtitlesList = ArrayList<String>()
                for (group in player.currentTracks.groups) {
                    if (group.type == C.TRACK_TYPE_TEXT) {
                        val groupInfo = group.mediaTrackGroup
                        for (i in 0 until groupInfo.length) {
                            subtitles.add(groupInfo.getFormat(i).language.toString())
                            subtitlesList.add(
                                "${subtitlesList.size + 1}. " + Locale(groupInfo.getFormat(i).language.toString()).displayLanguage
                                        + " (${groupInfo.getFormat(i).label})"
                            )
                        }
                    }
                }

                val tempTracks =
                    subtitlesList.toArray(arrayOfNulls<CharSequence>(subtitlesList.size))
                val sDialog = MaterialAlertDialogBuilder(this, R.style.alertDialog)
                    .setTitle("Select Subtitles")
                    .setOnCancelListener { playVideo() }
                    .setPositiveButton("Off Subtitles") { self, _ ->
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters().setRendererDisabled(
                                C.TRACK_TYPE_VIDEO, true
                            )
                        )
                        self.dismiss()
                    }
                    .setItems(tempTracks) { _, position ->
                        Snackbar.make(binding.root, subtitlesList[position] + " Selected", 3000)
                            .show()
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .setRendererDisabled(C.TRACK_TYPE_VIDEO, false)
                                .setPreferredTextLanguage(subtitles[position])
                        )
                    }
                    .create()
                sDialog.show()
                sDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE)
                sDialog.window?.setBackgroundDrawable(ColorDrawable(0x99000000.toInt()))
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
                val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
                val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                        Process.myUid(),
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






    private fun createPlayer() {
        try {
            player.release()
        } catch (e: Exception) { }
        speed = 1.0f
        trackSelector = DefaultTrackSelector(this)
        videoTilte.text = playerList[position].title
        videoTilte.isSelected = true
        player = ExoPlayer.Builder(this).setTrackSelector(trackSelector).build()
        doubleTapEnable()
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

        loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
        loudnessEnhancer.enabled = true
        keepPlayingId = playerList[position].id

        seekBarFeature()

        binding.playerView.setControllerVisibilityListener(
            object : PlayerControlView.VisibilityListener {
                override fun onVisibilityChange(visibility: Int) {
                    when {
                        isLocked -> binding.lockButton.visibility = View.VISIBLE
                        binding.playerView.isControllerVisible -> binding.lockButton.visibility = View.VISIBLE
                        else -> binding.lockButton.visibility = View.INVISIBLE
                    }


                }
            }
        )

    }

    private fun setPlayButtonImage(isPlaying: Boolean) {
        if (isPlaying) {
            playPauseBtn.setImageResource(R.drawable.pauseicon)
        } else {
           playPauseBtn.setImageResource(R.drawable.play_icon)
        }
    }


    override fun onStart() {
        super.onStart()

        if (!isPlayerInitialized) {
            playbackPosition = 0
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

        playPauseBtn.setImageResource(R.drawable.pauseicon)
        player.play()
    }


    private fun pauseVideo() {
        playPauseBtn.setImageResource(R.drawable.play_icon)
        player.pause()
    }

    private fun nextPreviousVideo(isNext: Boolean = true) {
        playbackPosition = 0
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

    private fun playInFullScreen(enable: Boolean) {
        if (enable) {
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            fullScreenBtn.setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_ic_fullscreen_exit)
        } else {
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            player.videoScalingMode= C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            fullScreenBtn.setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_ic_fullscreen_enter)
        }
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
        audioManager?.abandonAudioFocus {this}


    }
    override fun onAudioFocusChange(focusChange:Int){
        if(focusChange<=0){
            pauseVideo()
        }
    }


    override fun onResume() {
        super.onResume()
        if (audioManager == null) {
            audioManager =
                getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager!!.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }else{
            playVideo()
        }


        if (brightness != 0) setScreenBrightness(brightness)
    }
        @SuppressLint("ClickableViewAccessibility")
        private fun doubleTapEnable(){

            binding.playerView.player = player
            binding.ytOverlay.performListener(object : YouTubeOverlay.PerformListener{
                override fun onAnimationEnd() {
                    binding.ytOverlay.visibility = View.GONE
                }

                override fun onAnimationStart() {
                    binding.ytOverlay.visibility = View.VISIBLE
                }

            })
            binding.ytOverlay.player(player)
            binding.playerView.setOnTouchListener { _, motionEvent ->
                gestureDetectorCompat.onTouchEvent(motionEvent)
                    if (motionEvent.action == MotionEvent.ACTION_UP) {
                        binding.brightnessIcon.visibility = View.GONE
                        binding.volumeIcon.visibility = View.GONE
                        //for immersive mode
                        WindowCompat.setDecorFitsSystemWindows(window, false)
                        WindowInsetsControllerCompat(window, binding.root).let { controller ->
                            controller.hide(WindowInsetsCompat.Type.systemBars())
                            controller.systemBarsBehavior =
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        }

                }
                return@setOnTouchListener false
            }
        }
        private fun seekBarFeature(){
            findViewById<DefaultTimeBar>(R.id.exo_progress).addListener(object: TimeBar.OnScrubListener{
                override fun onScrubStart(timeBar: TimeBar, position: Long) {
                    pauseVideo()
                }

                override fun onScrubMove(timeBar: TimeBar, position: Long) {
                    player.seekTo(position)
                }

                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                    playVideo()
                }

            })
        }

    override fun onDown(e: MotionEvent): Boolean {
        minSwipeY = 0f
        return false
    }
    override fun onShowPress(e: MotionEvent) = Unit
    override fun onSingleTapUp(e: MotionEvent): Boolean = false
    override fun onLongPress(e: MotionEvent) = Unit
    override fun onFling(
        event1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean = false

    override fun onScroll(
        event1: MotionEvent?,
        event2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        minSwipeY += distanceY

        val sWidth = Resources.getSystem().displayMetrics.widthPixels
        val sHeight = Resources.getSystem().displayMetrics.heightPixels

        val border = 100 * Resources.getSystem().displayMetrics.density.toInt()
        if (event2.x < border || event2.y < border || event2.x > sWidth - border || event2.y > sHeight - border)
            return false

        //minSwipeY for slowly increasing brightness & volume on swipe --> try changing 50 (<50 --> quick swipe & > 50 --> slow swipe

        if (abs(distanceX) < abs(distanceY) && abs(minSwipeY) > 50) {
            if (event2.x < sWidth / 2) {
                //brightness
                binding.brightnessIcon.visibility = View.VISIBLE
                binding.volumeIcon.visibility = View.GONE
                val increase = distanceY > 0
                val newValue = if (increase) brightness + 1 else brightness - 1
                if (newValue in 0..30) brightness = newValue
                binding.brightnessIcon.text = brightness.toString()
                setScreenBrightness(brightness)
            } else {
                //volume
                binding.brightnessIcon.visibility = View.GONE
                binding.volumeIcon.visibility = View.VISIBLE
                val maxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val increase = distanceY > 0
                val newValue = if (increase) volume + 1 else volume - 1
                if (newValue in 0..maxVolume) volume = newValue
                binding.volumeIcon.text = volume.toString()
                audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
            }
            minSwipeY = 0f
        }

        return true
    }

    private fun setScreenBrightness(value: Int){
        val d = 1.0f/30
        val lp = this.window.attributes
        lp.screenBrightness = d * value
        this.window.attributes = lp
    }
}