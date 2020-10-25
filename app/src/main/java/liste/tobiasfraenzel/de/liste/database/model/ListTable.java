package liste.tobiasfraenzel.de.liste.database.model;

public class ListTable {
    public static final String TABLE_NAME = "groups";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    @SuppressWarnings("SpellCheckingInspection")
    public static final String COLUMN_ORDERINDEX = "order_index";

    // Create table SQL
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_NAME + " TEXT,"
                    + COLUMN_ORDERINDEX + " INTEGER"
                    + ")";

    // Delete table SQL
    public static final String DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
}