package liste.tobiasfraenzel.de.liste.utils;

import android.Manifest;

public final class Constants {
    // What's shown in the MainActivity
    public enum displayedType {
        Lists, ListEntries
    }

    // Unique notification ID
    public static final int NOTIFICATION_ID = 1;

    // Time formats
    public static final String ONLY_TIME_FORMAT = "HH:mm";
    public static final String ONLY_DATE_FORMAT = "yyyy-MM-dd";
    public static final String ALARM_DATE_FORMAT = "yyyy-MM-dd HH:mm";
    public static final String TIMESTAMP_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    @SuppressWarnings({"SpellCheckingInspection", "unused"})
    public static final String SHORT_DATE_FORMAT = "dd MMMM yy";

    // Backup
    public static final String BACKUP_FILE_NAME = "backup.json";
    // Backup keys
    public static final String DESCRIPTION_KEY = "description";
    @SuppressWarnings("SpellCheckingInspection")
    public static final String LISTID_KEY = "listId";
    public static final String TITLE_KEY = "title";
    public static final String ID_KEY = "id";
    @SuppressWarnings("SpellCheckingInspection")
    public static final String MODIFICATIONDATE_KEY = "modificationDate";
    @SuppressWarnings("SpellCheckingInspection")
    public static final String ORDERINDEX_KEY = "orderIndex";

    // Sort directions
    public static final String DIRECTION_ASCENDING = "ASC";
    public static final String DIRECTION_DESCENDING = "DESC";

    // Channels
    public static final String CHANNEL_NAME = "Reminder channel";
    public static final String CHANNEL_DESCRIPTION = "Channel for scheduled reminders";
    public static final String CHANNEL_ID = "reminder_channel";

    // Actions
    public static final String NOTIFICATION_ACTION = "de.tobiasfraenzel.liste.notification";
    public static final String ALARM_ACTION = "de.tobiasfraenzel.liste.alarm";
    public static final String BOOT_COMPLETE_ACTION = "android.intent.action.BOOT_COMPLETED";

    // Intent Keys
    public static final String INTENT_KEY_ID = "id";
    public static final String INTENT_KEY_TITLE = "title";
    public static final String INTENT_KEY_ENTRY_ID = "entryId";

    // Text type
    public static final String TEXT_PLAIN = "text/plain";

    // Margins
    public static final int VERTICAL_MARGIN = 16;
}

