package liste.tobiasfraenzel.de.liste.database;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import liste.tobiasfraenzel.de.liste.utils.Constants;
import liste.tobiasfraenzel.de.liste.utils.Utilities;

public class BackupHelper {
    final private Context context;
    public enum BackupMode {
        CREATE,
        RESTORE
    }

    public BackupHelper(Context context) {
        this.context = context;
    }

    /**
     * Converts a nested List to JSON
     * @param data The nested List to be converted
     * @return The JSON String
     */
    private String convertDataToJson(final List<List<?>> data) {
        final Gson gson = new Gson();
        return gson.toJson(data);
    }

    /**
     * Convert JSON data to a nested List
     * @param data The JSON String
     * @return The nested List containing the data
     */
    private List<List<Map<String, Object>>> convertJsonToData(final String data) {
        final Gson gson = new Gson();
        return gson.fromJson(data, new TypeToken<List<List<Map<String, Object>>>>(){}.getType());
    }

    /**
     * Read all data from the database and save it in a JSON file
     * @return True if the saving was successful, false otherwise
     */
    final public boolean createBackup() {
        final DatabaseHelper db = new DatabaseHelper(context);
        return saveDataToFile(db.getFullDatabaseContent());
    }

    /**
     * Write the data into a JSON file in the public backup storage directory
     * @param data Nested List containing the data to be backed up
     * @return True if the write was successful, false otherwise
     */
    private boolean saveDataToFile(final List<List<?>> data) {
        if (!isExternalStorageWritable()) {
            return false;
        }
        final String jsonData = convertDataToJson(data);

        try (final Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(createPublicBackupStorageDir() + "/" +
                        Constants.BACKUP_FILE_NAME), StandardCharsets.UTF_8))) {
            writer.write(jsonData);
        } catch (IOException ex) {
            return false;
        }
        return true;
    }

    /**
     * Create the public backup storage directory
     * @return The File object of the new directory
     */
    private File createPublicBackupStorageDir() throws IOException {
        // Get the directory
        final File file = context.getExternalFilesDir(null);
        if (file != null) {
            if (!file.mkdirs()) {
                Log.d(Utilities.getLogTag(), "Directory not created, exists: " + file.exists());
            }
        } else {
            throw new IOException("Backup directory creation failed");
        }
        return file;
    }

    /**
     * Read backup from file, delete current DB content, insert data read from backup
     * @param location File object of the backup file
     * @return True if restoring the backup was successful, false otherwise
     */
    final public boolean restoreBackup(final File location) {
        if (!isExternalStorageReadable()) {
            return false;
        }
        // Read from file
        final StringBuilder text = new StringBuilder();
        try {
            final BufferedReader br = new BufferedReader(new FileReader(location));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            return false;
        }

        // Erase current DB entries
        final DatabaseHelper db = new DatabaseHelper(context);
        db.cleanTables();

        // Write to DB
        final String backupDataJson = text.toString();
        final List<List<Map<String, Object>>> backupData = convertJsonToData(backupDataJson);
        Log.d(Utilities.getLogTag(), "ArrayList: " + Arrays.toString(backupData.toArray()));
        final List<Map<String, Object>> entries = backupData.get(0);
        Log.d(Utilities.getLogTag(), "ListEntries: " + Arrays.toString(entries.toArray()));
        final List<Map<String, Object>> lists = backupData.get(1);
        Log.d(Utilities.getLogTag(), "Lists: " + Arrays.toString(lists.toArray()));

        return db.parseAndStoreEntries(entries) && db.parseAndRestoreLists(lists);
    }

    /**
     * Check if external storage is available for read and write
     * @return True if read and write are possible, false otherwise
     */
    private boolean isExternalStorageWritable() {
        final String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /**
     * Check if external storage is available for read only access
     * @return True if read is possible, false otherwise
     */
    private boolean isExternalStorageReadable() {
        final String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}