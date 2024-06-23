package com.android19.videoplayer

import android.os.Build
import android.os.Bundle
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
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.SimpleExoPlayer
import com.android19.videoplayer.databinding.ActivityPlayerBinding

@UnstableApi
class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding

    companion object {
        lateinit var player: SimpleExoPlayer
        lateinit var playerList: ArrayList<Video>
        var position: Int = -1
        var isPlayerInitialized = false
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
    }

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
    }

    @OptIn(UnstableApi::class)
    private fun createPlayer() {
        binding.videoTilte.text = playerList[position].title
        binding.videoTilte.isSelected = true
        player = SimpleExoPlayer.Builder(this).build()
        binding.playerView.player = player
        val mediaItem = MediaItem.fromUri(playerList[position].artUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        isPlayerInitialized = true
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
        binding.playButton.setImageResource(R.drawable.pause_icon)
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

    override fun onDestroy() {
        super.onDestroy()
        if (isPlayerInitialized) {
            player.release()
            isPlayerInitialized = false
        }
    }
}
