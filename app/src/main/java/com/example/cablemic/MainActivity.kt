package com.example.cablemic

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlin.math.max


class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 1000 // なんか任意定数っぽいよな

    private lateinit var audioRecord: AudioRecord
    //private lateinit var audioTrack: AudioTrack
    private val sampleRate = 44100 // Hz
    private val frRate = 10 // fps，毎秒の処理回数
    private val shortPerFrame = sampleRate / frRate // フレーム当たりの音声データ数
    private val bytePerFrame = shortPerFrame * 2 // フレーム当たりの音声データのバイト数
    private val bufSize = max(bytePerFrame * 20, // バッファサイズ，↑よりでかいこと
        AudioRecord.getMinBufferSize(sampleRate, // 端末ごとの規定値よりでかいこと
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT))

    private fun startRec(audioRecord: AudioRecord) {

        audioRecord.positionNotificationPeriod = shortPerFrame // フレーム当たりの処理数を指定
        audioRecord.notificationMarkerPosition = 50000 // マーカー周期を指定
        val audioArray = ShortArray(shortPerFrame) // 音声データを格納する配列

        audioRecord.setRecordPositionUpdateListener(object :
            AudioRecord.OnRecordPositionUpdateListener { // コールバック

            override fun onPeriodicNotification(recorder: AudioRecord) { // フレームごと
                recorder.read(audioArray, 0, shortPerFrame) // 読み込み
                // 再生処理
                Log.v("AudioRecord", "onPeriodicNotification size=${audioArray.size}")

            }

            override fun onMarkerReached(recorder: AudioRecord) { // マーカー周期ごと
                recorder.read(audioArray, 0, shortPerFrame) // 読み込み
                Log.v("AudioRecord", "onMarkerReached size=${audioArray.size}")
                // 処理
            }
        })
        audioRecord.startRecording()
    }

    var permissionBool = false // マイク許可がある場合はtrue

    private fun permissionCheck() { // パーミッション取得チャレンジ 取得出来たらtrue，できなかったらfalse

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6.0以降について
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) { // マイクの使用が許可されている場合
                permissionBool = true // 許可されてるのでtrue
            } else {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),
                    PERMISSION_REQUEST_CODE) // リクエストを送信
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) { // システム召喚獣
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    permissionBool = true
                } else {
                    permissionBool = false
                    AlertDialog.Builder(this) // 許可取ったほうがいいよの説明
                        .setTitle("注意")
                        .setMessage("マイク入力を使用するには権限の許可が必要です。設定からマイクの使用を許可してください。")
                        .setNeutralButton("閉じる") { _, _ -> }
                        .show()
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private var active = false // マイクONのときtrue

    private fun buttonClick(v: View) { // ボタンが押された時の処理
        val aButton = v as ImageButton // 画像ボタンを指定

        if (permissionBool) { // マイク使用許可があったら処理開始
            active = if (!active) { // マイクがOFFのとき

                audioRecord = AudioRecord( // インスタンス召喚の儀
                    MediaRecorder.AudioSource.MIC, // 入力音源はマイク
                    sampleRate, // 44100Hz
                    AudioFormat.CHANNEL_IN_MONO, // モノトラック
                    AudioFormat.ENCODING_PCM_16BIT, // PCM 16bit
                    bufSize) // バッファ

                aButton.setImageResource(R.drawable._0) // マイクONの画像に変更
                startRec(audioRecord) // マイクON
                true // ONにする

            } else { // マイクがONのとき

                aButton.setImageResource(R.drawable.`_`) // マイクOFFの画像に変更
                Log.v("AudioRecord", "stop")
                audioRecord.stop() // マイクOFF
                false // OFFにする
            }
        } else {
            permissionCheck()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imageButton: ImageButton = findViewById(R.id.imageButton)
        imageButton.setOnClickListener {
            buttonClick(imageButton)
        }
        if (!permissionBool) permissionCheck() // マイク許可がなければ要求
    }
}