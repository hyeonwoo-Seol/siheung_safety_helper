package utils

import android.Manifest
import android.content.pm.PackageManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.safetyhelper.R

object NotificationHelper {

    private const val CHANNEL_SOUND = "channel_sound"
    private const val CHANNEL_SILENT_VIB = "channel_silent_vib"
    private const val CHANNEL_SILENT_NO_VIB = "channel_silent_novib"
    private const val PREFS_NAME = "setting"

    // 기본 설정값 기반 알림 전송 함수
    fun send(context: Context, title: String, message: String, notificationId: Int = 1) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isSoundOff = prefs.getBoolean("sound_enabled", false)
        val isVibrationOn = prefs.getBoolean("vibration_enabled", false)
        val isPreviewEnabled = prefs.getBoolean("preview_enabled", true)
        val channelId = when {
            !isSoundOff -> CHANNEL_SOUND
            isVibrationOn -> CHANNEL_SILENT_VIB
            else -> CHANNEL_SILENT_NO_VIB
        }

        createChannels(context)

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
        if (isPreviewEnabled) {
            builder.setContentTitle(title)
                .setContentText(message)
        } else {
            builder.setContentTitle("새 알림이 도착했습니다")
                .setContentText("알림 미리보기는 비활성화되어 있습니다")
        }


        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    // channelId 직접 지정해서 전송할 수 있는 함수 (함수 오버로딩)
    fun send(context: Context, title: String, message: String, channelId: String, notificationId: Int = 1) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.page)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())

    }

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (manager.getNotificationChannel(CHANNEL_SOUND) == null) {
                val soundChannel = NotificationChannel(
                    CHANNEL_SOUND,
                    "소리 알림",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                manager.createNotificationChannel(soundChannel)
            }

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

            if (manager.getNotificationChannel(CHANNEL_SILENT_NO_VIB) == null) {
                val noVibChannel = NotificationChannel(
                    CHANNEL_SILENT_NO_VIB,
                    "무음+진동없음 알림",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                }
                manager.createNotificationChannel(noVibChannel)
            }

        }
    }
}
