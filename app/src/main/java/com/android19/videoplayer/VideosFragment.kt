package com.android19.videoplayer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android19.videoplayer.databinding.FragmentVideosBinding

class VideosFragment : Fragment() {
    lateinit var adapter: VideoAdapter
    private lateinit var binding: FragmentVideosBinding

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireContext().theme.applyStyle(MainActivity.themesList[MainActivity.themeIndex],true)
        val view = inflater.inflate(R.layout.fragment_videos, container, false)
        val binding = FragmentVideosBinding.bind(view)
        binding.videoRV.setHasFixedSize(true)
        binding.videoRV.setItemViewCacheSize(10)
        binding.videoRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = VideoAdapter(requireContext(), MainActivity.videoList)
        binding.videoRV.adapter = VideoAdapter(requireContext(), MainActivity.videoList )
        binding.totalVideos.text = "Total videos: ${MainActivity.folderList.size}"
        return view
    }

}