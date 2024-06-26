package com.android19.videoplayer

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.android19.videoplayer.databinding.ActivityMainBinding
import com.android19.videoplayer.databinding.ThemesViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var toggle: ActionBarDrawerToggle

//    private var runnable: Runnable? = null

    companion object{
        lateinit var videoList: ArrayList<Video>
        lateinit var folderList: ArrayList<Folder>
        lateinit var searchList: ArrayList<Video>
        var search: Boolean = false
        var themeIndex: Int = 0
        var sortValue:Int = 0
        val themesList = arrayOf(R.style.coolPinkNav, R.style.coolBlue,
            R.style.coolGreen, R.style.holoPurple, R.style.holoblueLight, R.style.darkerGrey, R.style.red, R.style.darkOrange, R.style.yellow)
        var dataChanged : Boolean = false
        var adapterChanged : Boolean = false
        val sortList = arrayOf(
            MediaStore.Video.Media.DATE_ADDED +" DESC",
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.TITLE + " DESC",
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.SIZE + " DESC")
    }

//    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val editor = getSharedPreferences("Themes", MODE_PRIVATE)
        themeIndex = editor.getInt("themeIndex",0)

        setTheme(themesList[themeIndex])
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        //Nav Drawer
        toggle = ActionBarDrawerToggle(this, binding.main, R.string.open, R.string.close)
        binding.main.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.themes->{

                    val customDialog =
                        LayoutInflater.from(this).inflate(R.layout.themes_view, binding.root, false)
                    val bindingTV = ThemesViewBinding.bind(customDialog)
                    val dialog = MaterialAlertDialogBuilder(this).setView(customDialog)
                        .setTitle("Select Theme")
                        .create()
                    dialog.show()
                        when(themeIndex){
                            0 -> bindingTV.themePink.setBackgroundColor(Color.CYAN)
                            1 -> bindingTV.themeCoolBlue.setBackgroundColor(Color.CYAN)
                            2 -> bindingTV.themeCoolGreen.setBackgroundColor(Color.CYAN)
                            3 -> bindingTV.themeHoloPurple.setBackgroundColor(Color.CYAN)
                            4 -> bindingTV.themeHoloBlueLight.setBackgroundColor(Color.CYAN)
                            5 -> bindingTV.themeDarkerGray.setBackgroundColor(Color.CYAN)
                            6 -> bindingTV.themeRed.setBackgroundColor(Color.CYAN)
                            7 -> bindingTV.themeOrange.setBackgroundColor(Color.CYAN)
                            8 -> bindingTV.themeYellow.setBackgroundColor(Color.CYAN)

                        }
                        bindingTV.themePink.setOnClickListener { saveTheme(0)}
                        bindingTV.themeCoolBlue.setOnClickListener { saveTheme(1)}
                        bindingTV.themeCoolGreen.setOnClickListener { saveTheme(2)}
                        bindingTV.themeHoloPurple.setOnClickListener { saveTheme(3)}
                        bindingTV.themeHoloBlueLight.setOnClickListener { saveTheme(4)}
                        bindingTV.themeDarkerGray.setOnClickListener { saveTheme(5)}
                        bindingTV.themeRed.setOnClickListener { saveTheme(6)}
                        bindingTV.themeOrange.setOnClickListener { saveTheme(7)}
                        bindingTV.themeYellow.setOnClickListener { saveTheme(8)}
                }
                R.id.sort ->{
                    val menuItems = arrayOf("Latest","Oldest","Name(A - Z)","Name(Z - A)",
                        "File Size(Smallest)","File Size(Largest)")
                    var value = sortValue

                    val dialog = MaterialAlertDialogBuilder(this)
                        .setTitle("Sort By")
                        .setPositiveButton("OK"){_, _ ->
                            val sortEditor = getSharedPreferences("Sorting", MODE_PRIVATE).edit()
                            sortEditor.putInt("sortValue",value)
                            sortEditor.apply()
                            finish()
                            startActivity(intent)
                        }
                        .setSingleChoiceItems(menuItems, sortValue){_,pos ->
                            value = pos
                        }
                        .create()
                    dialog.show()
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.DKGRAY)
                }
                R.id.exit -> exitProcess(1)
                else -> false
            }
            return@setNavigationItemSelectedListener true
        }

        if (requestRuntimePermission()){
            folderList = ArrayList()
            videoList = getAllVideos()
            setFragment(VideosFragment())

//            runnable = Runnable{
//                if(dataChanged){
//                    videoList = getAllVideos()
//                    dataChanged = false
//                    adapterChanged = true
//                }
//                Log.d("MainActivity", "Runnable executed")
//                Handler(Looper.getMainLooper()).postDelayed(runnable!!,300)
//            }
//            Handler(Looper.getMainLooper()).postDelayed(runnable!!,0)
        }


        binding.bottomNav.setOnItemSelectedListener {

            when(it.itemId){
                R.id.videoView -> setFragment(VideosFragment())
                R.id.folderView -> setFragment(FoldersFragment())
            }
            return@setOnItemSelectedListener true
        }
    }

    private fun setFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentFL, fragment)
        transaction.disallowAddToBackStack()
        transaction.commit()
    }

    private fun requestRuntimePermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), 13)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 13) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                folderList = ArrayList()
                videoList = getAllVideos()
                setFragment(VideosFragment())
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), 13)
            }
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("Range", "SuspiciousIndentation")
    private fun getAllVideos(): ArrayList<Video> {
        val sortEditor = getSharedPreferences("Sorting", MODE_PRIVATE)
        sortValue = sortEditor.getInt("sortValue",0)

        val tempList = ArrayList<Video>()
        val tempFolderList = ArrayList<String>()
        val projection = arrayOf(
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID
        )
        val cursor = this.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortList[sortValue]
        )
        if (cursor != null)
            if (cursor.moveToNext())
                do {
                    val titleC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE))
                    val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID))
                    val folderC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
                    val folderIdC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID))
                    val sizeC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE))
                    val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                    val durationC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION)).toLong()

                    try {
                        val file = File(pathC)
                        val artUri = Uri.fromFile(file)
                        val video = Video(
                            title = titleC,
                            id = idC,
                            folderName = folderC,
                            duration = durationC,
                            size = sizeC,
                            artUri = artUri,
                            path = pathC
                        )
                        if (file.exists()) tempList.add(video)

                        if (!tempFolderList.contains(folderC)) {
                            tempFolderList.add(folderC)
                            folderList.add(Folder(id = folderIdC, folderName = folderC))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } while (cursor.moveToNext())
        cursor?.close()
        return tempList
    }
    private fun saveTheme(index: Int){
        val editor = getSharedPreferences("Themes", MODE_PRIVATE).edit()
        editor.putInt("themeIndex",index)
        editor.apply()
        //for restarting
        finish()
        startActivity(intent)


    }

    override fun onDestroy() {
        super.onDestroy()

    }
}
