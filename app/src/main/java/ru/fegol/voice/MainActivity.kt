package ru.fegol.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private val file by lazy {
        val ouptut = getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
        handler.post {
            Toast.makeText(this, ouptut, Toast.LENGTH_LONG).show()
        }
        File(ouptut, "test_record.pcm")
    }
    private var recorder: AudioRecord? = null
    private var writeAudio: Thread? = null
    private var isRecording = false
    private val rate: Int
        get() {
            return rate_edit_text.text.toString().toInt()
        }

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        record_button.setOnClickListener {
            if (recorder == null) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startRecord()
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO
                        ),
                        100
                    )
                }
            } else {
                stopRecord()
            }
        }
        share_button.setOnClickListener {
            if (file.exists()) {
                val uri: Uri = Uri.parse(file.absolutePath)
                val share = Intent(Intent.ACTION_SEND)
                share.type = "audio/*"
                share.putExtra(Intent.EXTRA_STREAM, uri)
                startActivity(Intent.createChooser(share, "Share Sound File"))
            }
        }
        play_button.setOnClickListener {
            playShortAudioFileViaAudioTrack(file.absolutePath)
        }
    }

    private fun startRecord() {
        record_button.text = "STOP"
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            rate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            2048
        )
        recorder?.apply {
            startRecording()
            isRecording = true
        }
        writeAudio = Thread {
            if (file.exists()) {
                file.delete()
            }
            val buffer = ByteArray(2048)
            val fos = FileOutputStream(file)
            while (isRecording) {
                recorder?.read(buffer, 0, 2048)
                try {
                    fos.write(buffer)
                } catch (e: Exception) {
                }
            }
            try {
                fos.close()
            } catch (e: Exception) {
            }
        }
        writeAudio?.start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            grantResults.forEach {
                if (it != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }
            startRecord()
        }
    }

    private fun stopRecord() {
        record_button.text = "RECORD"
        // stops the recording activity
        recorder?.apply {
            isRecording = false
            stop()
            release()
            recorder = null
            writeAudio = null
        }
    }


    private fun playShortAudioFileViaAudioTrack(filePath: String?) { // We keep temporarily filePath globally as we have only two sample sounds now..
        if (filePath == null) return
        //Reading the file..
        val file =
            File(filePath) // for ex. path= "/sdcard/samplesound.pcm" or "/sdcard/samplesound.wav"
        val byteData = ByteArray(file.length().toInt())
        val `in`: FileInputStream?
        try {
            `in` = FileInputStream(file)
            `in`.read(byteData)
            `in`.close()
        } catch (e: FileNotFoundException) { // TODO Auto-generated catch block
            e.printStackTrace()
        }
        // Set and push to audio track..
        val intSize = AudioTrack.getMinBufferSize(
            rate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val at = AudioTrack(
            AudioManager.STREAM_MUSIC,
            rate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            intSize,
            AudioTrack.MODE_STREAM
        )
        at.play()
        // Write the byte array to the track
        at.write(byteData, 0, byteData.size)
        at.stop()
        at.release()
    }
}
