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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlin.math.max


class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 1000 // なんか任意定数っぽいよな

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

    var permissionBool = false // マイク許可がある場合はtrue

    private fun permissionCheck() { // パーミッション取得チャレンジ 取得出来たらtrue，できなかったらfalse

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6.0以降について
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) { // マイクの使用が許可されている場合
                permissionBool = true // 許可されてるのでtrue
            } else {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),
                    PERMISSION_REQUEST_CODE) // リクエストを送信
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.RECORD_AUDIO)
                ) { // 拒否かつ "今後表示しない" になっている場合
                    Toast.makeText(this, "マイクを使用するには権限を許可してください。", Toast.LENGTH_SHORT).show()

                    // 許可取ったほうがいいよの説明

                    permissionBool = false // 許可取れなかったのでfalse
                }
            }
        }
        return // ターンエンド
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
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
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

        if (!permissionBool) permissionCheck() // マイク使用許可がなかったら取りに行く

        if (!permissionBool) return // マイク使用許可がなかったら終了

        active = if (!active) { // マイクがOFFのとき

            aButton.setImageResource(R.drawable._0) // マイクONの画像に変更
            startRec() // マイクON
            true // ONにする

        } else { // マイクがONのとき

            aButton.setImageResource(R.drawable.`_`) // マイクOFFの画像に変更
            Log.v("AudioRecord", "stop")
            audioRecord.stop() // マイクOFF
            false // OFFにする
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