package com.android19.videoplayer

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android19.videoplayer.databinding.VideoViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

class VideoAdapter(private val context: Context, private var videoList: ArrayList<Video>) : RecyclerView.Adapter<VideoAdapter.MyHolder>() {
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
    }

    override fun getItemCount(): Int {
        return videoList.size
    }
}