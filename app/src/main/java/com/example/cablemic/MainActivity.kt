package com.example.cablemic

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max


class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_CODE = 1

    private val smplRate = 44100 // Hz
    private val frRate = 10 // fps，毎秒の処理回数
    private val shortPerFrame = smplRate / frRate // フレーム当たりの音声データ数
    private val bytePerFrame = shortPerFrame * 2 // フレーム当たりの音声データのバイト数
    private val bufSize = max(bytePerFrame * 20, // バッファサイズ，↑よりでかいこと
        AudioRecord.getMinBufferSize(smplRate, // 端末ごとの規定値よりでかいこと
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT))

    private val audioRecord = AudioRecord( // インスタンス召喚の儀
        MediaRecorder.AudioSource.MIC, // 入力音源はマイク
        smplRate, // 44100Hz
        AudioFormat.CHANNEL_IN_MONO, // モノトラック
        AudioFormat.ENCODING_PCM_16BIT, // PCM 16bit
        bufSize) // バッファ

    private fun startRec() {

        audioRecord.positionNotificationPeriod = shortPerFrame // フレーム当たりの処理数を指定
        audioRecord.notificationMarkerPosition = 50000 // マーカー周期を指定
        val audioArray = ShortArray(shortPerFrame) // 音声データを格納する配列

        audioRecord.setRecordPositionUpdateListener(object :
            AudioRecord.OnRecordPositionUpdateListener { // コールバック
            override fun onPeriodicNotification(recorder: AudioRecord) { // フレームごと
                recorder.read(audioArray, 0, shortPerFrame) // 読み込み
                Log.v("AudioRecord", "onPeriodicNotification size=${audioArray.size}")
            }

            override fun onMarkerReached(recorder: AudioRecord) { // マーカー周期ごと
                recorder.read(audioArray, 0, shortPerFrame) // 読み込み
                Log.v("AudioRecord", "onMarkerReached size=${audioArray.size}")
                // 好きに処理する
            }
        })
        audioRecord.startRecording()
    }

    private var active = false // マイクONのときtrue

    private fun buttonClick(v: View) { // ボタンが押された時の処理
        val aButton = v as ImageButton // 画像ボタンを指定

        if (!active) { // マイクがOFFのとき

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6.0以降について
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) { // マイクの使用が許可されていない場合
                    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),
                        PERMISSIONS_REQUEST_CODE) // リクエストを送信
                }
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) { // 許可されなかった場合
                        return // とりあえず終了
                }
            }

            aButton.setImageResource(R.drawable._0) // マイクONの画像に変更
            startRec() // マイクON
            active = true // ONにする

        } else { // マイクをOFFにしたとき
            aButton.setImageResource(R.drawable.`_`) // マイクOFFの画像に変更
            Log.v("AudioRecord", "stop")
            audioRecord.stop() // マイクOFF
            active = false // OFFにする
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

            val imageButton: ImageButton = findViewById(R.id.imageButton)
            imageButton.setOnClickListener {
                buttonClick(imageButton)
            }
        }
    }