package com.android19.videoplayer

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.WindowManager
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
import androidx.media3.exoplayer.SimpleExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.android19.videoplayer.databinding.ActivityPlayerBinding

@UnstableApi
class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var runnable: Runnable
    companion object {
        lateinit var player: SimpleExoPlayer
        lateinit var playerList: ArrayList<Video>
        var position: Int = -1
        var isPlayerInitialized = false
        private var repeat : Boolean = false
        private var isFullScreen: Boolean = false
        private var isLocked: Boolean = false

    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        binding = ActivityPlayerBinding.inflate(layoutInflater)


        this.enableEdgeToEdge()
        //for immersive mode
        setTheme(R.style.playerActivityTheme)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let {
                controller -> controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v: View, insets: WindowInsetsCompat ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeLayout()
        initializeBinding()
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
        binding.nextButton.setOnClickListener{
            nextPreviousVideo()
        }
        binding.previousButton.setOnClickListener{
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
        binding.fullscreenButton.setOnClickListener{
            if(isFullScreen){
                isFullScreen = false
                playInFullScreen(enable = false)
            }else{
                isFullScreen = true
                playInFullScreen(enable = true)
            }
        }
        binding.lockButton.setOnClickListener{
            if(!isLocked){
                //ẩn
                isLocked = true
                binding.playerView.hideController()
                binding.playerView.useController = false
                binding.lockButton.setImageResource(R.drawable.locked)
        }
            else{
                //hien
                isLocked = false
                binding.playerView.useController = true
                binding.playerView.showController()
                binding.lockButton.setImageResource(R.drawable.open_lock_icon)
            }
        }

    }

    @OptIn(UnstableApi::class)
    private fun createPlayer() {
        try {
            player.release()
        }
        catch(e:Exception){}


        binding.videoTilte.text = playerList[position].title
        binding.videoTilte.isSelected = true
        player = SimpleExoPlayer.Builder(this).build()
        binding.playerView.player = player
        val mediaItem = MediaItem.fromUri(playerList[position].artUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        isPlayerInitialized = true
        player.addListener(object : Player.Listener{
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if(playbackState == Player.STATE_ENDED) nextPreviousVideo()
            }
        })
        playInFullScreen(enable = isFullScreen)
        setVisibility()
    }

    override fun onStart() {
        super.onStart()
        if (!isPlayerInitialized) {
            createPlayer()
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
    }
    override fun onDestroy() {
        super.onDestroy()
        if (isPlayerInitialized) {
            player.release()
            isPlayerInitialized = false
        }
    }
}
