package com.android19.videoplayer

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import java.io.File


data class Video(val id: String,
                 var title: String,
                 val duration: Long = 0,
                 val folderName: String,
                 val size: String,
                 var path: String,
                 var artUri: Uri
)

data class Folder(val id: String,
                  val folderName: String
)
@SuppressLint("Range", "SuspiciousIndentation")
fun getAllVideos(context: Context): ArrayList<Video> {
    val sortEditor = context.getSharedPreferences("Sorting", AppCompatActivity.MODE_PRIVATE)
    MainActivity.sortValue = sortEditor.getInt("sortValue", 0)

    val tempList = ArrayList<Video>()
    val tempFolderList = ArrayList<String>()
    val projection = arrayOf(
        MediaStore.Video.Media.TITLE,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Video.Media.DATA,  // Keep DATA to get full path
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.BUCKET_ID
    )
    val cursor = context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        MainActivity.sortList[MainActivity.sortValue]
    )
    if (cursor != null) {
        while (cursor.moveToNext()) {
            val titleC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE))
            val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID))
            val folderC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
            val folderIdC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID))
            val sizeC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE))
            val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA)) // Full path

            try {
                val file = File(pathC)
                val artUri = Uri.fromFile(file)
                val video = Video(
                    title = titleC,
                    id = idC,
                    folderName = folderC,
                    duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION)),
                    size = sizeC,
                    artUri = artUri,
                    path = pathC
                )
                if (file.exists()) tempList.add(video)

                if (!tempFolderList.contains(folderC)) {
                    tempFolderList.add(folderC)
                    MainActivity.folderList.add(Folder(id = folderIdC, folderName = folderC))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        cursor.close()
    }
    return tempList
}
