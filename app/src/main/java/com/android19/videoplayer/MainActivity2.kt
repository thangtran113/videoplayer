package com.android19.videoplayer

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_2)

        requestRuntimePermission()

        val searchButton = findViewById<Button>(R.id.searchButton)
        searchButton.performClick()
        val searchText = findViewById<EditText>(R.id.searchText)
        val resultTextView = findViewById<TextView>(R.id.resultTextView)

        val videoTitle: String? = intent.getStringExtra("video_title")

            val keyword = intent.getStringExtra("video_title") ?: ""
            val result = searchForFile(keyword)

            resultTextView.text = result


    }

    private fun searchForFile(keyword: String): String {
        val filesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!filesDir.exists()) {
            filesDir.mkdir()
        }

        val files = filesDir.listFiles {
                file -> file.name.contains(keyword) && file.name.endsWith(".txt")
        } ?: return "No files found"

        return if (files.isNotEmpty()) {
            val fileInfo = files.map { file ->
                "File: ${file.name}\nContent:\n${file.readText()}\n"
            }.joinToString("\n")

            val intent = Intent(this@MainActivity2, SecPage::class.java)
            intent.putExtra("file_info", fileInfo)
            startActivity(intent)
            ""
        } else {
            "No files found"
        }
    }
    private fun requestRuntimePermission():Boolean{
        if (ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), 13)
            return false
        }
        return true
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 13)
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Grandted", Toast.LENGTH_SHORT).show()

            }
            else
                ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE),13)
    }

}