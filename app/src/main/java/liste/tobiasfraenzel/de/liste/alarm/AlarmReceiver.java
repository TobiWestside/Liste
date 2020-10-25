package liste.tobiasfraenzel.de.liste.alarm;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import liste.tobiasfraenzel.de.liste.MainActivity;
import liste.tobiasfraenzel.de.liste.R;
import liste.tobiasfraenzel.de.liste.database.DatabaseHelper;
import liste.tobiasfraenzel.de.liste.utils.Constants;
import liste.tobiasfraenzel.de.liste.utils.Utilities;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // When an alarm is received, display a notification
        Log.d(Utilities.getLogTag(), "Alarm received");
        showNotification(context, intent);
    }

    /**
     * Displays a notification and opens the MainActivity when the notification is tapped
     * @param context Current context
     * @param intent Intent from the alarm
     */
    private void showNotification(final Context context, final Intent intent) {
        // Create a PendingIntent for the Activity that will be opened
        // when the notification is tapped
        final Intent targetIntent = new Intent(context, MainActivity.class);
        targetIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        targetIntent.setAction(Constants.NOTIFICATION_ACTION);
        final int id = intent.getIntExtra(Constants.INTENT_KEY_ID, -1);
        Log.d(Utilities.getLogTag(), "AlarmReceiver received alarm for ID " + id);
        targetIntent.putExtra(Constants.INTENT_KEY_ENTRY_ID, id);
        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                Constants.CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_notification)
                .setContentTitle(intent.getStringExtra(Constants.INTENT_KEY_TITLE))
                .setContentText(context.getString(R.string.notification_title))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Create the notification
        notificationManager.notify(Constants.NOTIFICATION_ID, builder.build());
        // Delete the alarm from the database, so it won't be triggered again
        if (id > -1) {
            final DatabaseHelper db = new DatabaseHelper(context);
            db.removeAlarmForEntryId(id);
        }
    }
}