package com.android19.videoplayer

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SecPage : AppCompatActivity() {
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.l2)
        textView = findViewById(R.id.fileContentTextView)


        val fileInfo = intent.getStringExtra("file_info")
        textView.text = fileInfo
    }
}