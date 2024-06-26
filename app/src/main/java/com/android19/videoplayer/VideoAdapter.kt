package com.android19.videoplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.android19.videoplayer.databinding.VideoViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

class VideoAdapter(private val context: Context, private var videoList: ArrayList<Video>,
                   private val isFolder:Boolean=false) : RecyclerView.Adapter<VideoAdapter.MyHolder>() {
    class MyHolder(binding: VideoViewBinding) : RecyclerView.ViewHolder(binding.root) {
        var title = binding.videoName
        val folder = binding.folderName
        val duration = binding.duration
        val image = binding.videoImg
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(VideoViewBinding.inflate(LayoutInflater.from(context), parent , false))
    }

    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.title.text = videoList[position].title
        holder.folder.text = videoList[position].folderName
        holder.duration.text = DateUtils.formatElapsedTime(videoList[position].duration/1000)
        Glide.with(context)
            .asBitmap()
            .load(videoList[position].artUri)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .apply(RequestOptions().placeholder(R.mipmap.ic_video_player).centerCrop())
            .into(holder.image)
        holder.root.setOnClickListener {
            when{
                 videoList[position].id == PlayerActivity.nowPlayingId -> {
                     sendIntent(pos = position, ref = "NowPlaying")
                }
                isFolder -> {
                    PlayerActivity.pipStatus = 1
                    sendIntent(pos = position, ref = "FolderActivity")
                }
                MainActivity.search -> {
                    PlayerActivity.pipStatus = 2
                    sendIntent(pos = position, ref = "SearchedVideos")
                }
                else->
                {
                    PlayerActivity.pipStatus = 3
                    sendIntent(pos = position, ref = "AllVideos")
                }
            }

        }
    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    @OptIn(UnstableApi::class)
    private fun sendIntent(pos: Int, ref:String){
        PlayerActivity.position = pos
        val intent = Intent(context,PlayerActivity::class.java)
        intent.putExtra("class",ref)
        ContextCompat.startActivity(context,intent,null)

    }
    @SuppressLint("NotifyDataSetChanged")
    fun updateList(searchList: ArrayList<Video>){
        videoList = ArrayList()
        videoList.addAll(searchList)
        notifyDataSetChanged()
    }
}