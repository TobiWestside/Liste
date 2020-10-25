package liste.tobiasfraenzel.de.liste.database.model;

public class ListEntryTable {
    public static final String TABLE_NAME = "entries";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    @SuppressWarnings("SpellCheckingInspection")
    public static final String COLUMN_ORDERINDEX = "order_index";
    @SuppressWarnings("SpellCheckingInspection")
    public static final String COLUMN_GROUPID = "group_id";
    @SuppressWarnings("SpellCheckingInspection")
    public static final String COLUMN_ALARMDATE = "alarmdate";

    // Create table SQL
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_TITLE + " TEXT,"
                    + COLUMN_DESCRIPTION + " TEXT,"
                    + COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + COLUMN_ORDERINDEX + " INTEGER,"
                    + COLUMN_GROUPID + " INTEGER,"
                    + COLUMN_ALARMDATE + " DATETIME DEFAULT NULL"
                    + ")";

    // Delete table SQL
    public static final String DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME + ";";

}
