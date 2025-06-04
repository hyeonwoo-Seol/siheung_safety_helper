package utils

import com.example.myapplication.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val CHANNEL_SOUND = "channel_sound"
    private const val CHANNEL_SILENT_VIB = "channel_silent_vib"
    private const val CHANNEL_SILENT_NO_VIB = "channel_silent_novib"
    private const val PREFS_NAME = "setting"

    fun send(context: Context, title: String, message: String, notificationId: Int = 1) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isSoundOff = prefs.getBoolean("sound_enabled", false)
        val isVibrationOn = prefs.getBoolean("vibration_enabled", false)

        val channelId = when {
            !isSoundOff -> CHANNEL_SOUND
            isVibrationOn -> CHANNEL_SILENT_VIB
            else -> CHANNEL_SILENT_NO_VIB
        }

        createChannels(context) // 채널은 항상 보장

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.page)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (isSoundOff) {
            builder.setSound(null)
            if (isVibrationOn) {
                builder.setVibrate(longArrayOf(0, 400, 200, 400))
            } else {
                builder.setVibrate(null)
            }
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 소리 O
            if (manager.getNotificationChannel(CHANNEL_SOUND) == null) {
                val soundChannel = NotificationChannel(
                    CHANNEL_SOUND,
                    "소리 알림",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                manager.createNotificationChannel(soundChannel)
            }

            // 무음 + 진동
            if (manager.getNotificationChannel(CHANNEL_SILENT_VIB) == null) {
                val vibChannel = NotificationChannel(
                    CHANNEL_SILENT_VIB,
                    "무음+진동 알림",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    setSound(null, null)
                    enableVibration(true)
                }
                manager.createNotificationChannel(vibChannel)
            }

            // 무음 + 진동 없음
            if (manager.getNotificationChannel(CHANNEL_SILENT_NO_VIB) == null) {
                val noVibChannel = NotificationChannel(
                    CHANNEL_SILENT_NO_VIB,
                    "무음+진동없음 알림",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                }
                manager.createNotificationChannel(noVibChannel)
            }
        }
    }
}