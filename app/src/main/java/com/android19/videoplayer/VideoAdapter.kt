package com.android19.videoplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.android19.videoplayer.databinding.DetailsViewBinding
import com.android19.videoplayer.databinding.RenameFieldBinding
import com.android19.videoplayer.databinding.VideoMoreFeaturesBinding
import com.android19.videoplayer.databinding.VideoViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

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

    @SuppressLint("NotifyDataSetChanged")
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
                 videoList[position].id == PlayerActivity.keepPlayingId -> {
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
        holder.root.setOnLongClickListener{
            val customDialog =
                LayoutInflater.from(context).inflate(R.layout.video_more_features, holder.root, false)
            val bindingVMF = VideoMoreFeaturesBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(context).setView(customDialog)
                .create()
            dialog.show()

            bindingVMF.renameVideo.setOnClickListener {
                requestPermissionR()
                dialog.dismiss()
                val customDialogRF =
                LayoutInflater.from(context).inflate(R.layout.rename_field, holder.root, false)
                val bindingRF = RenameFieldBinding.bind(customDialogRF)
                val dialogRF = MaterialAlertDialogBuilder(context).setView(customDialogRF)
                    .setCancelable(false)
                    .setPositiveButton("Rename"){self, _ ->
                        val currentFile = File(videoList[position].path)
                        val newName = bindingRF.renameField.text
                        if(newName != null && currentFile.exists() && newName.toString().isNotEmpty()){
                            val newFile = File(currentFile.parentFile,newName.toString()+"."+currentFile.extension)
                            if(currentFile.renameTo(newFile)){
                                MediaScannerConnection.scanFile(context, arrayOf(newFile.toString()), arrayOf("video/*"), null)
                               when{
                                   MainActivity.search ->{
                                       MainActivity.searchList[position].title = newName.toString()
                                       MainActivity.searchList[position].path = newFile.path
                                       MainActivity.searchList[position].artUri = Uri.fromFile(newFile)
                                       notifyItemChanged(position)
                                   }
                                       isFolder ->{
                                       FoldersActivity.currentFolderVideo[position].title = newName.toString()
                                       FoldersActivity.currentFolderVideo[position].path = newFile.path
                                       FoldersActivity.currentFolderVideo[position].artUri = Uri.fromFile(newFile)
                                       notifyItemChanged(position)
                                           MainActivity.dataChanged = true
                                   }
                                       else->{
                                           MainActivity.videoList[position].title = newName.toString()
                                           MainActivity.videoList[position].path = newFile.path
                                           MainActivity.videoList[position].artUri = Uri.fromFile(newFile)
                                           notifyItemChanged(position)
                                       }
                                   }
                               }
                                else{
                                    Toast.makeText(context,"Permission Denied!!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        self.dismiss()
                    }
                    .setNegativeButton("Cancel"){self, _ ->
                        self.dismiss()
                    }
                    .create()
                dialogRF.show()
                bindingRF.renameField.text = SpannableStringBuilder(videoList[position].title)
                dialogRF.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.BLUE)
                dialogRF.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.BLUE)
            }

            bindingVMF.shareVideo.setOnClickListener {
                dialog.dismiss()
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.type = "video/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(videoList[position].path))
                ContextCompat.startActivity(context, Intent.createChooser(shareIntent,"Sharing Video File"), null)
            }

            bindingVMF.infoVideo.setOnClickListener {
              dialog.dismiss()
                val customDialogDF =
                    LayoutInflater.from(context).inflate(R.layout.details_view, holder.root, false)
                val bindingIF = DetailsViewBinding.bind(customDialogDF)
                val dialogIF = MaterialAlertDialogBuilder(context).setView(customDialogDF)
                    .setCancelable(false)
                    .setPositiveButton("OK"){self, _ ->
                        self.dismiss()
                    }

                    .create()
                dialogIF.show()
                val detailText = SpannableStringBuilder()
                    .bold { append("Details\n\nName: ") }.append(videoList[position].title)
                    .bold { append("\n\nDuration: ") }.append(DateUtils.formatElapsedTime(videoList[position].duration/1000))
                    .bold { append("\n\nFile Size: ") }.append(Formatter.formatShortFileSize(context,videoList[position].size.toLong()))
                    .bold { append("\n\nLocation: ") }.append(videoList[position].path)


                bindingIF.detailTV.text = detailText
                dialogIF.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.BLUE)

            }

            bindingVMF.deleteVideo.setOnClickListener {
                requestPermissionR()
                dialog.dismiss()

                val dialogDF = MaterialAlertDialogBuilder(context)
                    .setTitle("Delete Video?")
                    .setMessage(videoList[position].title)
                    .setCancelable(false)
                    .setPositiveButton("YES"){self, _ ->
                        val file = File(videoList[position].path)
                        if(file.exists() && file.delete()){
                            MediaScannerConnection.scanFile(context, arrayOf(file.path), arrayOf("video/*"), null)
                            when{
                                MainActivity.search -> {
                                    MainActivity.dataChanged = true
                                    videoList.removeAt(position)
                                    notifyDataSetChanged()
                                }
                                isFolder -> {
                                    MainActivity.dataChanged = true
                                    FoldersActivity.currentFolderVideo.removeAt(position)
                                    notifyDataSetChanged()
                                }
                                else ->{
                                    MainActivity.videoList.removeAt(position)
                                    notifyDataSetChanged()
                                }
                            }
                        }
                            else{
                                Toast.makeText(context,"Permission Denied!", Toast.LENGTH_SHORT).show()
                        }
                            self.dismiss()
                    }
                    .setNegativeButton("NO"){self, _ ->
                        self.dismiss()
                    }
                    .create()
                dialogDF.show()

                dialogDF.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.BLUE)
                dialogDF.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.BLUE)
            }

            return@setOnLongClickListener true
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




    //Requesting permisson for Android >= 11
    private fun requestPermissionR(){
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if(!Environment.isExternalStorageManager()){
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse("package:${context.applicationContext.packageName}")
                    ContextCompat.startActivity(context,intent, null)
                }
            }
        )
    }
}