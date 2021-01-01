package liste.tobiasfraenzel.de.liste.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

import liste.tobiasfraenzel.de.liste.ListEntry;
import liste.tobiasfraenzel.de.liste.utils.Constants;
import liste.tobiasfraenzel.de.liste.utils.Utilities;

import static android.content.Context.ALARM_SERVICE;

public class AlarmHandler {
    /**
     * Register a new alarm with the system alarm service
     * @param context Current context
     * @param time Time when the alarm will go off
     * @param title Title of the ListEntry for which the alarm is set
     * @param id ID of the ListEntry for which the alarm is set
     */
    public static void setAlarm(final Context context, final Calendar time, final String title, final int id) {
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        final PendingIntent pi = createPendingIntentForAlarm(context, title, id);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pi);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pi);
        }
        Log.d(Utilities.getLogTag(), "Alarm set for ListEntry with title: " + title);
    }

    /**
     * Cancels a registered alarm with the system alarm service
     * @param context Current context
     * @param entry The entry for which the reminder is canceled
     */
    public static void cancelAlarm(final Context context, final ListEntry entry) {
        cancelAlarm(context, entry.getTitle(), entry.getId());
    }

    public static void cancelAlarm(final Context context, final String title, final int id) {
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManager.cancel(createPendingIntentForAlarm(context, title, id));
        Log.d(Utilities.getLogTag(), "Alarm canceled for ListEntry with title: " + title);
    }

    /**
     * Creates a new PendingIntent for setting or canceling alarms
     * @param context Current context
     * @param title Title of the ListEntry for which the PendingIntent is created
     * @param id ID of the ListEntry for which the PendingIntent is created
     * @return The new PendingIntent
     */
    private static PendingIntent createPendingIntentForAlarm(final Context context, final String title, final int id) {
        final Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(Constants.ALARM_ACTION);
        intent.putExtra(Constants.INTENT_KEY_TITLE, title);
        intent.putExtra(Constants.INTENT_KEY_ID, id);
        Log.d(Utilities.getLogTag(), "AlarmHandler created intent for ListEntry with title: " + title);
        return PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
