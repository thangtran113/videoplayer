package com.android19.videoplayer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android19.videoplayer.databinding.FragmentFoldersBinding


class FoldersFragment : Fragment() {

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_folders, container, false)
        val binding = FragmentFoldersBinding.bind(view)
        binding.foldersRV.setHasFixedSize(true)
        binding.foldersRV.setItemViewCacheSize(10)
        binding.foldersRV.layoutManager = LinearLayoutManager(requireContext())
        binding.foldersRV.adapter = FoldersAdapter(requireContext(),MainActivity.folderList )
        binding.totalFolders.text = "Total folders: ${MainActivity.folderList.size}"
        return view



    }
}