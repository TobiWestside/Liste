package liste.tobiasfraenzel.de.liste.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import liste.tobiasfraenzel.de.liste.MyList;
import liste.tobiasfraenzel.de.liste.ListEntry;
import liste.tobiasfraenzel.de.liste.R;
import liste.tobiasfraenzel.de.liste.database.model.ListTable;
import liste.tobiasfraenzel.de.liste.database.model.ListEntryTable;
import liste.tobiasfraenzel.de.liste.utils.Constants;
import liste.tobiasfraenzel.de.liste.utils.Utilities;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "list_db";
    final private Context context;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ListEntryTable.CREATE_TABLE);
        db.execSQL(ListTable.CREATE_TABLE);

        // Insert default list
        final ContentValues values = new ContentValues();
        values.put(ListTable.COLUMN_NAME, context.getString(R.string.default_list_name));
        db.insert(ListTable.TABLE_NAME, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {
            // Alarms added in DB version 2
            if (newVersion >= 2 && oldVersion == 1) {
                db.execSQL("ALTER TABLE " + ListEntryTable.TABLE_NAME + " ADD COLUMN " +
                        ListEntryTable.COLUMN_ALARMDATE + " DATETIME DEFAULT NULL");
            }
        }
    }

    /*
     * ListEntry methods
     */

    /**
     * Insert ListEntry data into the ListEntryTable
     * @param title Title of the ListEntry
     * @param description Description of the ListEntry
     * @param listId ID of the List that the entry belongs to
     * @param alarmDateString Alarm date as a String
     * @return ID of the inserted entry
     */
    final public long insertEntry(final String title, final String description, final int listId,
                                  final String alarmDateString) {
        final SQLiteDatabase db = this.getWritableDatabase();

        final ContentValues values = new ContentValues();
        // ID and timestamp will be inserted automatically
        values.put(ListEntryTable.COLUMN_TITLE, title);
        values.put(ListEntryTable.COLUMN_DESCRIPTION, description);
        values.put(ListEntryTable.COLUMN_GROUPID, listId);
        values.put(ListEntryTable.COLUMN_ALARMDATE, alarmDateString);
        // Use -1 as the order index, to make it 0 when moving all entries back (explained below)
        values.put(ListEntryTable.COLUMN_ORDERINDEX, -1);

        final long id = db.insert(ListEntryTable.TABLE_NAME, null, values);
        // Add 1 to the order index of all entries. Thereby, the newly inserted entry gets
        // index 0 and all other entries get higher numbers. I.e., the new entry will be at the top.
        moveAllEntriesBack();
        db.close();
        // Return the ID of the inserted entry
        return id;
    }

    /**
     * Read values of a ListEntry from the database and return them as a ListEntry object
     * @param id ID of the ListEntry
     * @return The new ListEntry
     */
    final public ListEntry getEntry(final long id) {
        final SQLiteDatabase db = this.getReadableDatabase();

        final Cursor cursor = db.query(ListEntryTable.TABLE_NAME,
                new String[]{ListEntryTable.COLUMN_ID, ListEntryTable.COLUMN_TITLE,
                        ListEntryTable.COLUMN_DESCRIPTION, ListEntryTable.COLUMN_TIMESTAMP,
                        ListEntryTable.COLUMN_GROUPID, ListEntryTable.COLUMN_ORDERINDEX,
                        ListEntryTable.COLUMN_ALARMDATE}, ListEntryTable.COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);

        assert cursor != null;
        ListEntry entry = null;
        if (cursor.moveToFirst()) {
            entry = getListEntryFromCursor(cursor);
        }
        cursor.close();
        db.close();
        return entry;
    }

    /**
     * Return a list of all entries, ordered by a specific column and direction
     * @param column Name of the column that the ListEntries will be ordered by
     * @param direction Direction of the order ("ASC" or "DESC")
     * @return Ordered List of all ListEntries
     */
    private List<ListEntry> getAllEntriesOrderedBy(final String column, final String direction) {
        final List<ListEntry> entries = new ArrayList<>();
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(ListEntryTable.TABLE_NAME, null, null,
                null, null, null, column + " " + direction);
        assert cursor != null;
        // Add the returned entries to the list
        if (cursor.moveToFirst()) {
            do {
                final ListEntry entry = getListEntryFromCursor(cursor);
                entries.add(entry);
            } while (cursor.moveToNext());
        }
        db.close();
        cursor.close();
        return entries;
    }

    /**
     * Return a list of all ListEntries, ordered by a specific column in ASC direction
     * @param column Name of the column that the ListEntries will be ordered by
     * @return Ordered List of all ListEntries
     */
    @SuppressWarnings("SameParameterValue")
    private List<ListEntry>getAllEntriesOrderedBy(final String column) {
        return getAllEntriesOrderedBy(column, Constants.DIRECTION_ASCENDING);
    }

    /**
     * Return a list of all entries, ordered ascending by the order index
     * @return Ordered List of all ListEntries
     */
    final public List<ListEntry> getAllEntries() {
        return getAllEntriesOrderedBy(ListEntryTable.COLUMN_ORDERINDEX);
    }

    /**
     * Return a list of all entries, ordered descending by the last modified timestamp
     * @return Ordered List of all ListEntries
     */
    final public List<ListEntry> getAllEntriesOrderedByLastModified() {
        return getAllEntriesOrderedBy(ListEntryTable.COLUMN_TIMESTAMP, Constants.DIRECTION_DESCENDING);
    }

    /**
     * Return a list of all entries that belong to a specific list,
     * ordered ascending by the order index
     * @param listId ID of the list
     * @return Ordered List of all ListEntries that belong to the list
     */
    final public List<ListEntry> getEntriesOfList(final int listId) {
        final List<ListEntry> entries = new ArrayList<>();

        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(ListEntryTable.TABLE_NAME, null,
                ListEntryTable.COLUMN_GROUPID + " = " + listId,
                null, null, null,
                ListEntryTable.COLUMN_ORDERINDEX + " " + Constants.DIRECTION_ASCENDING);
        assert cursor != null;
        // Add the returned entries to the list
        if (cursor.moveToFirst()) {
            do {
                final ListEntry entry = getListEntryFromCursor(cursor);
                entries.add(entry);
            } while (cursor.moveToNext());
        }
        db.close();
        cursor.close();
        return entries;
    }

    /**
     * Get the number of entries that belong to a specific list
     * @param listId ID of the list
     * @return Number of entries that belong to the list
     */
    final public int getEntryCountForList(final int listId) {
        final SQLiteDatabase db = this.getReadableDatabase();
        final int entryCount = (int)DatabaseUtils.queryNumEntries(db, ListEntryTable.TABLE_NAME,
                ListEntryTable.COLUMN_GROUPID + " = " + listId);
        db.close();
        return entryCount;
    }

    /**
     * Get the number of ListEntries in the database
     * @return The number of ListEntries in the database
     */
    final public int getEntriesCount() {
        return getCountFromTable(ListEntryTable.TABLE_NAME);
    }

    /**
     * Update a ListEntry with new values
     * @param entry ListEntry to be updated
     * @param setTimestamp Whether to set a new "last modified" date
     * @return The number of affected rows (should be 1)
     */
    private int updateEntry(final ListEntry entry, final boolean setTimestamp) {
        final SQLiteDatabase db = this.getWritableDatabase();
        final ContentValues values = new ContentValues();
        values.put(ListEntryTable.COLUMN_TITLE, entry.getTitle());
        values.put(ListEntryTable.COLUMN_DESCRIPTION, entry.getDescription());
        if (setTimestamp) {
            values.put(ListEntryTable.COLUMN_TIMESTAMP, getCurrentTimestamp());
        }
        values.put(ListEntryTable.COLUMN_ORDERINDEX, entry.getOrderIndex());
        values.put(ListEntryTable.COLUMN_GROUPID, entry.getListId());
        values.put(ListEntryTable.COLUMN_ALARMDATE, entry.getAlarmDate());
        Log.d(Utilities.getLogTag(), "Updated Alarm Date: " + entry.getAlarmDate());
        final int affectedRows = db.update(ListEntryTable.TABLE_NAME, values,
                ListEntryTable.COLUMN_ID + " = ?", new String[]{String.valueOf(entry.getId())});
        db.close();
        return affectedRows;
    }

    /**
     * Update a ListEntry with new values and set new 'last modified' timestamp
     * @param entry ListEntry to be updated
     * @return The number of affected rows (should be 1)
     */
    @SuppressWarnings("UnusedReturnValue")
    final public int updateEntry(final ListEntry entry) {
        return updateEntry(entry, true);
    }

    /**
     * Update a ListEntry with new values and keep old 'last modified' timestamp
     * @param entry ListEntry to be updated
     * @return The number of affected rows (should be 1)
     */
    final public int updateEntryWithoutTimestamp(final ListEntry entry) {
        return updateEntry(entry, false);
    }

    /**
     * Get formatted current timestamp
     * @return Formatted current timestamp
     */
    private String getCurrentTimestamp() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(
                Constants.TIMESTAMP_DATE_FORMAT, Locale.getDefault()
        );
        final Date date = new Date();
        return dateFormat.format(date);
    }

    /**
     * Delete a ListEntry from the database
     * @param entry ListEntry to be deleted
     */
    final public void deleteEntry(final ListEntry entry) {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.delete(ListEntryTable.TABLE_NAME, ListEntryTable.COLUMN_ID + " = ?",
                new String[]{String.valueOf(entry.getId())});
        db.close();
    }

    /**
     * Delete the alarm date of a ListEntry
     * @param id ID of the ListEntry
     * @return The number of affected rows (should be 1)
     */
    @SuppressWarnings("UnusedReturnValue")
    final public int removeAlarmForEntryId(final int id) {
        Log.d(Utilities.getLogTag(), "Alarm removed for ListEntry with ID: " + id);
        final ListEntry entry = getEntry(id);
        entry.setAlarmDate(null);
        return updateEntryWithoutTimestamp(entry);
    }

    /**
     * Increase the order index of all ListEntries by 1
     */
    private void moveAllEntriesBack() {
        final List<ListEntry> allEntries = getAllEntries();
        final SQLiteDatabase db = this.getWritableDatabase();

        for (final ListEntry e : allEntries) {
            e.setOrderIndex(e.getOrderIndex() + 1);
            final ContentValues allEntriesValues = new ContentValues();
            allEntriesValues.put(ListEntryTable.COLUMN_ORDERINDEX, e.getOrderIndex());
            db.update(ListEntryTable.TABLE_NAME, allEntriesValues, ListEntryTable.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(e.getId())});
        }
        db.close();
    }

    /*
     * List methods
     */

    /**
     * Insert a new list into the database
     * @param name Name of the list
     * @return ID of the list
     */
    final public long insertList(final String name) {
        final SQLiteDatabase db = this.getWritableDatabase();

        final ContentValues values = new ContentValues();
        values.put(ListTable.COLUMN_NAME, name);
        // Use -1 as the order index, to make it 0 when moving all lists back by 1 (explained below)
        values.put(ListTable.COLUMN_ORDERINDEX, -1);

        final long id = db.insert(ListTable.TABLE_NAME, null, values);
        // Add 1 to the order index of all lists. Thereby, the newly inserted list gets
        // index 0 and all other lists get higher numbers. I.e., the new list will be at the top.
        moveAllListsBack();
        db.close();

        // Return the ID of the newly inserted list
        return id;
    }

    /**
     * Read values of a list from the database and return them as a MyList object
     * @param id ID of the list
     * @return The MyList object
     */
    final public MyList getList(final long id) {
        final SQLiteDatabase db = this.getReadableDatabase();

        final Cursor cursor = db.query(ListTable.TABLE_NAME,
                new String[]{ListTable.COLUMN_ID, ListTable.COLUMN_NAME, ListTable.COLUMN_ORDERINDEX},
                ListTable.COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);

        assert cursor != null;
        MyList list = null;
        if (cursor.moveToFirst()) {
            list = getListFromCursor(cursor);
        }
        db.close();
        cursor.close();
        return list;
    }

    final public String getListTitle(final long id) {
        final SQLiteDatabase db = this.getReadableDatabase();

        final Cursor cursor = db.query(ListTable.TABLE_NAME,
                new String[]{ListTable.COLUMN_NAME}, ListTable.COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);

        assert cursor != null;
        String title = "";
        if (cursor.moveToFirst()) {
            title = cursor.getString(cursor.getColumnIndex(ListTable.COLUMN_NAME));
        }
        db.close();
        cursor.close();
        return title;
    }

    /**
     * Return a List of all lists, ordered ascending by the order index
     * @return The ordered List
     */
    final public List<MyList> getAllLists() {
        final List<MyList> lists = new ArrayList<>();

        final SQLiteDatabase db = this.getWritableDatabase();
        final Cursor cursor = db.query(ListTable.TABLE_NAME, null, null,
                null, null, null,
                ListTable.COLUMN_ORDERINDEX + " " + Constants.DIRECTION_ASCENDING);
        assert cursor != null;
        // Add the returned lists to the list
        if (cursor.moveToFirst()) {
            do {
                final MyList list = getListFromCursor(cursor);
                lists.add(list);
            } while (cursor.moveToNext());
        }

        db.close();
        cursor.close();
        return lists;
    }

    /**
     * Update a list with new values
     * @param list MyList object with the new values
     * @return The number of affected rows (should be 1)
     */
    @SuppressWarnings("UnusedReturnValue")
    final public int updateList(final MyList list) {
        final SQLiteDatabase db = this.getWritableDatabase();

        final ContentValues values = new ContentValues();
        values.put(ListTable.COLUMN_NAME, list.getTitle());
        values.put(ListTable.COLUMN_ORDERINDEX, list.getOrderIndex());

        final int affectedRows = db.update(ListTable.TABLE_NAME, values, ListTable.COLUMN_ID + " = ?",
                new String[]{String.valueOf(list.getId())});
        db.close();
        return affectedRows;
    }

    /**
     * Delete a list from the database
     * @param list The list to be deleted
     */
    final public void deleteList(final MyList list) {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.delete(ListTable.TABLE_NAME, ListTable.COLUMN_ID + " = ?",
                new String[]{String.valueOf(list.getId())});
        db.close();
    }

    /**
     * Increase the order index of all lists by 1
     */
    private void moveAllListsBack() {
        final List<MyList> allLists = getAllLists();
        final SQLiteDatabase db = this.getWritableDatabase();

        for (MyList g : allLists) {
            g.setOrderIndex(g.getOrderIndex() + 1);
            final ContentValues allListsValues = new ContentValues();
            allListsValues.put(ListTable.COLUMN_ORDERINDEX, g.getOrderIndex());
            db.update(ListTable.TABLE_NAME, allListsValues, ListTable.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(g.getId())});
        }
        db.close();
    }

    /*
     * Helper
     */

    /**
     * Creates a ListEntry object with the values returned by the cursor
     * @param c Cursor from which the data is returned
     * @return The new ListEntry
     */
    private ListEntry getListEntryFromCursor(final Cursor c) {
        final int groupId = c.getInt(c.getColumnIndex(ListEntryTable.COLUMN_GROUPID));

        // Create ListEntry object with the data read from the database
        ListEntry entry = new ListEntry(
                c.getInt(c.getColumnIndex(ListEntryTable.COLUMN_ID)),
                c.getString(c.getColumnIndex(ListEntryTable.COLUMN_TITLE)),
                c.getString(c.getColumnIndex(ListEntryTable.COLUMN_DESCRIPTION)),
                c.getString(c.getColumnIndex(ListEntryTable.COLUMN_TIMESTAMP)),
                c.getInt(c.getColumnIndex(ListEntryTable.COLUMN_ORDERINDEX)),
                groupId);
        entry.setAlarmDate(c.getString(c.getColumnIndex(ListEntryTable.COLUMN_ALARMDATE)));
        Log.d(Utilities.getLogTag(), "Got entry with title: " + entry.getTitle());

        return entry;
    }

    /**
     * Creates a MyList object with the values returned by the cursor
     * @param c Cursor from which the data is returned
     * @return The new list
     */
    private MyList getListFromCursor(final Cursor c) {
        final MyList list = new MyList(
                c.getInt(c.getColumnIndex(ListTable.COLUMN_ID)),
                c.getString(c.getColumnIndex(ListTable.COLUMN_NAME)),
                c.getInt((c.getColumnIndex(ListTable.COLUMN_ORDERINDEX))));
        Log.d(Utilities.getLogTag(), "Got list with title: " + list.getTitle());
        return list;
    }

    /**
     * Return the number of rows in a table
     * @param table Name of the table
     * @return Number of rows in the table
     */
    @SuppressWarnings("SameParameterValue")
    private int getCountFromTable(final String table) {
        final SQLiteDatabase db = this.getReadableDatabase();
        final int count = (int)DatabaseUtils.queryNumEntries(db, table);
        db.close();
        return count;
    }

    /**
     * Read all data from the database and return it as a List of two lists
     * @return A List of two lists, first list contains all ListEntries, second list contains all lists
     */
    final public List<List<?>> getFullDatabaseContent() {
        final List<List<?>> data = new ArrayList<>();
        final List<ListEntry> entries = getAllEntries();
        final List<MyList> lists = getAllLists();

        data.add(entries);
        data.add(lists);

        return data;
    }

    /**
     * Delete and recreate database tables
     */
    final public void cleanTables() {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(ListEntryTable.DELETE_TABLE);
        db.execSQL(ListTable.DELETE_TABLE);
        db.execSQL(ListEntryTable.CREATE_TABLE);
        db.execSQL(ListTable.CREATE_TABLE);
        db.close();
    }

    /**
     * Takes a nested ArrayList of strings that represent list entry data,
     * parses it and inserts it into the database
     * @param entries List of Maps containing the values of the entries
     * @return True if all entries were successfully restored
     */
    final public boolean parseAndStoreEntries(final List<Map<String, Object>> entries) {
        final SQLiteDatabase db = this.getWritableDatabase();
        int insertedEntries = 0;

        for(final Map<String, Object> entry : entries) {
            final ContentValues values = new ContentValues();

            if (entry.containsKey(Constants.DESCRIPTION_KEY) && entry.get(Constants.DESCRIPTION_KEY) != null) {
                values.put(ListEntryTable.COLUMN_DESCRIPTION,
                        String.valueOf(entry.get(Constants.DESCRIPTION_KEY)));
            }

            if (entry.containsKey(Constants.LISTID_KEY) && entry.get(Constants.LISTID_KEY) != null) {
                values.put(ListEntryTable.COLUMN_GROUPID,
                            (int)(Float.parseFloat(String.valueOf(entry.get(Constants.LISTID_KEY)))));
            }
            if (entry.containsKey(Constants.TITLE_KEY) && entry.get(Constants.TITLE_KEY) != null) {
                values.put(ListEntryTable.COLUMN_TITLE,
                           String.valueOf(entry.get(Constants.TITLE_KEY)));
            }
            if (entry.containsKey(Constants.ID_KEY) && entry.get(Constants.ID_KEY) != null) {
                values.put(ListEntryTable.COLUMN_ID,
                        (int)(Float.parseFloat(String.valueOf(entry.get(Constants.ID_KEY)))));
            }
            if (entry.containsKey(Constants.MODIFICATIONDATE_KEY) && entry.get(Constants.MODIFICATIONDATE_KEY) != null) {
                values.put(ListEntryTable.COLUMN_TIMESTAMP,
                           String.valueOf(entry.get(Constants.MODIFICATIONDATE_KEY)));
            }
            if (entry.containsKey(Constants.ORDERINDEX_KEY) && entry.get(Constants.ORDERINDEX_KEY) != null) {
                values.put(ListEntryTable.COLUMN_ORDERINDEX,
                        (int)(Float.parseFloat(String.valueOf(entry.get("orderIndex")))));
            }
            final long insertedId = db.insert(ListEntryTable.TABLE_NAME, null, values);
            if (insertedId > -1) {
                insertedEntries++;
            }
            Log.d(Utilities.getLogTag(), "Restored ListEntry with ID: " +
                    values.getAsString(ListEntryTable.COLUMN_ID) + " and title: " +
                    values.getAsString(ListEntryTable.COLUMN_TITLE));
        }
        db.close();
        return insertedEntries == entries.size();
    }

    /**
     * Takes a nested ArrayList of strings that represent list data,
     * parses it and inserts it into the database
     * @param lists List of Maps containing the values of the lists
     * @return True if all entries were successfully restored
     */
    final public boolean parseAndRestoreLists(final List<Map<String, Object>> lists) {
        final SQLiteDatabase db = this.getWritableDatabase();
        int insertedLists = 0;

        for(final Map<String, Object> list : lists) {
            final ContentValues values = new ContentValues();

            if (list.containsKey(Constants.ID_KEY) && list.get(Constants.ID_KEY) != null) {
                values.put(ListTable.COLUMN_ID,
                        (int)(Float.parseFloat(String.valueOf(list.get(Constants.ID_KEY)))));
            }
            if (list.containsKey(Constants.TITLE_KEY) && list.get(Constants.TITLE_KEY) != null) {
                values.put(ListTable.COLUMN_NAME,
                           String.valueOf(list.get(Constants.TITLE_KEY)));
            }
            if (list.containsKey(Constants.ORDERINDEX_KEY) && list.get(Constants.ORDERINDEX_KEY) != null) {
                values.put(ListEntryTable.COLUMN_ORDERINDEX,
                        (int)(Float.parseFloat(String.valueOf(list.get(Constants.ORDERINDEX_KEY)))));
            }

            final long insertedId = db.insert(ListTable.TABLE_NAME, null, values);
            if (insertedId > -1) {
                insertedLists++;
            }

            Log.d(Utilities.getLogTag(), "Restored list with ID: " +
                    values.getAsString(ListTable.COLUMN_ID) + " and title: " +
                    values.getAsString(ListTable.COLUMN_NAME));
        }
        db.close();
        return insertedLists == lists.size();
    }
}