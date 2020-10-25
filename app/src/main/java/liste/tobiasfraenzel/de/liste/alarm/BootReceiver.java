package liste.tobiasfraenzel.de.liste.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import liste.tobiasfraenzel.de.liste.ListEntry;
import liste.tobiasfraenzel.de.liste.database.DatabaseHelper;
import liste.tobiasfraenzel.de.liste.utils.Constants;
import liste.tobiasfraenzel.de.liste.utils.Utilities;

public class BootReceiver extends BroadcastReceiver {
    // Recreate alarms after boot
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Constants.BOOT_COMPLETE_ACTION)) {
            Log.d(Utilities.getLogTag(), "Boot received");
            recreateAlarms(context);
        }
    }

    /**
     * Set the alarms for all ListEntries for which an alarmDate is stored in the database
     * @param context Current context
     */
    final public void recreateAlarms(final Context context) {
        final DatabaseHelper db = new DatabaseHelper(context);
        final List<ListEntry> allEntries = db.getAllEntries();
        for (final ListEntry entry : allEntries) {
            if (entry.getAlarmDate() != null) {
                final Calendar newAlarmDate = Calendar.getInstance();
                final SimpleDateFormat newAlarmDateFormat = new SimpleDateFormat(
                        Constants.ALARM_DATE_FORMAT, Locale.getDefault());
                try {
                    //noinspection ConstantConditions
                    newAlarmDate.setTime(newAlarmDateFormat.parse(entry.getAlarmDate()));
                    AlarmHandler.setAlarm(context, newAlarmDate, entry.getTitle(), entry.getId());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}