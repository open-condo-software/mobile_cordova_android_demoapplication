package ai.doma.core.miniapps.services

import ai.doma.core.miniapps.data.db.entities.BeaconRegionEntity
import ai.doma.miniappdemo.MainActivity
import ai.doma.miniappdemo.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.altbeacon.beacon.Beacon

private const val BEACON_NOTIFICATION_CHANEL = "beacon_notification_chanel"
private const val SINGLE_BEACON_NOTIFICATION_ID = 1

class BeaconNotifier(private val context: Context) {

    private val notificationStates: MutableMap<String, NotificationState> = mutableMapOf()
    private val notificationManager: NotificationManager = context.getSystemService(
        Context.NOTIFICATION_SERVICE
    ) as NotificationManager

    fun didEnterRegion(region: BeaconRegionEntity) {
        val miniappId: String = "SAMPLE_APP_ID"
        if (notificationStates[miniappId] == NotificationState.NOTIFIED) return

        val notification = createNotification()
        notificationManager.notify(SINGLE_BEACON_NOTIFICATION_ID, notification)
        notificationStates[miniappId] = NotificationState.NOTIFIED
    }

    fun didExitRegion(region: BeaconRegionEntity) {
        val miniappId: String = "SAMPLE_APP_ID"
        if (notificationStates[miniappId] == NotificationState.NOTIFIED) {
            notificationManager.cancel(SINGLE_BEACON_NOTIFICATION_ID)
            notificationStates[miniappId] = NotificationState.CANCELED
        }
    }

    fun didRangeBeaconsInRegion(region: BeaconRegionEntity, beacons: Collection<Beacon>) {
        val miniappId: String = "SAMPLE_APP_ID"
        if (beacons.any { it.distance <= region.minAccuracyValue }) {
            if (notificationStates[miniappId] == NotificationState.NOTIFIED) return

            val notification = createNotification()
            notificationManager.notify(SINGLE_BEACON_NOTIFICATION_ID, notification)
            notificationStates[miniappId] = NotificationState.NOTIFIED
        } else {
            if (notificationStates[miniappId] == NotificationState.NOTIFIED) {
                notificationManager.cancel(SINGLE_BEACON_NOTIFICATION_ID)
                notificationStates[miniappId] = NotificationState.CANCELED
            }
        }
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, BEACON_NOTIFICATION_CHANEL)
        } else {
            Notification.Builder(context)
        }

        val pi = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        builder.setContentTitle("Found beacon")
            .setContentText("Show the desired section")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pi)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                /*id*/ BEACON_NOTIFICATION_CHANEL,
                /*name*/ "Beacon notification",
                /*importance*/ NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setDescription("Beacon notification")
            notificationManager.createNotificationChannel(channel)
            builder.setChannelId(channel.getId());
        }

        return builder.build()
    }

    enum class NotificationState {
        NOTIFIED,
        CANCELED
    }

}