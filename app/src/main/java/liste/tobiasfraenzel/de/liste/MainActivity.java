package liste.tobiasfraenzel.de.liste;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import liste.tobiasfraenzel.de.liste.alarm.AlarmHandler;
import liste.tobiasfraenzel.de.liste.alarm.AlarmReceiver;
import liste.tobiasfraenzel.de.liste.alarm.BootReceiver;
import liste.tobiasfraenzel.de.liste.database.BackupHelper;
import liste.tobiasfraenzel.de.liste.database.DatabaseHelper;
import liste.tobiasfraenzel.de.liste.utils.Constants;
import liste.tobiasfraenzel.de.liste.utils.MyDividerItemDecoration;
import liste.tobiasfraenzel.de.liste.utils.RecyclerTouchListener;
import liste.tobiasfraenzel.de.liste.utils.Utilities;

import static liste.tobiasfraenzel.de.liste.utils.Utilities.allEntriesShown;
import static liste.tobiasfraenzel.de.liste.utils.Utilities.drawSwipeBackground;
import static liste.tobiasfraenzel.de.liste.utils.Utilities.entriesOfListShown;

public class MainActivity extends AppCompatActivity {
    @SuppressWarnings("rawtypes")
    private RecyclerView.Adapter mAdapter;
    final private List<ListEntry> listEntryList = new ArrayList<>();
    final private List<MyList> listsList = new ArrayList<>();
    private CoordinatorLayout coordinatorLayout;
    private RecyclerView recyclerView;
    private TextView noEntriesView;
    private MyList displayedList = null;
    private Snackbar snackbar;
    private BackupHelper.BackupMode backupMode;
    private Constants.displayedType currentlyDisplayed;
    private DatabaseHelper db;
    private int listOfEntry = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannel();
        // Register Alarm and Boot receivers
        registerAlarmAndBootReceivers();
        // Add all lists to be displayed
        db = new DatabaseHelper(this);
        listsList.addAll(db.getAllLists());
        currentlyDisplayed = Constants.displayedType.Lists;

        setupViews();
        // Set adapter for lists or list entries
        mAdapter = getCorrectAdapter();
        recyclerView.setAdapter(mAdapter);

        // Show noNotesView if there is nothing to display
        toggleEmptyNotes();

        setupTouchHandler();
        handleReceivedIntent();
    }

    /**
     * Enables an AlarmReceiver and a BootReceiver in the PackageManager.
     * Both are used for notifications.
     */
    private void registerAlarmAndBootReceivers() {
        final ComponentName alarmReceiver = new ComponentName(MainActivity.this, AlarmReceiver.class);
        final PackageManager pm = MainActivity.this.getPackageManager();

        pm.setComponentEnabledSetting(alarmReceiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        final ComponentName bootReceiver = new ComponentName(MainActivity.this, BootReceiver.class);
        pm.setComponentEnabledSetting(bootReceiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    /**
     * Handle received intents. Intents can be received
     * 1) if text is shared from another app
     * 2) from clicks on notifications
     */
    private void handleReceivedIntent() {
        final Intent intent = getIntent();
        final String type = intent.getType();

        if (Objects.equals(intent.getAction(), Intent.ACTION_SEND) && type != null) {
            // Handle text being shared from another app
            if (Constants.TEXT_PLAIN.equals(type)) {
                handleReceiveText(intent);
            }
        } else if (Objects.equals(intent.getAction(), Constants.NOTIFICATION_ACTION)) {
            // Handle notification click
            final int id = intent.getIntExtra(Constants.INTENT_KEY_ENTRY_ID, -1);
            if (id >= 0) {
                ListEntry entry = db.getEntry(id);
                showEntriesOfList(entry.getListId());
                for(ListEntry tempEntry:listEntryList) {
                    if (tempEntry.getId() == id) {
                        entry = tempEntry;
                    }
                }
                final int position = listEntryList.indexOf(entry);
                showDisplayListEntryDialog(position, null);
            }
        } else {
            // Log info in case of error
            Log.e(Utilities.getLogTag(), "Received Intent extras: " + intent.getExtras());
            Log.e(Utilities.getLogTag(), "Received Intent action: " + intent.getAction());
        }
    }

    /**
     * Create the NotificationChannel, but only on API 26+ because
     * the NotificationChannel class is new and not in the support library
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel channel = new NotificationChannel(Constants.CHANNEL_ID,
                    Constants.CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(Constants.CHANNEL_DESCRIPTION);
            // Register the channel with the system
            final NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Define behaviour on clicks (different for lists and ListEntries), add callbacks,
     * add ItemTouchHelper to RecyclerView
     */
    private void setupTouchHandler() {
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                recyclerView, new RecyclerTouchListener.ClickListener() {
            @SuppressWarnings("unused")
            @Override
            public void onClick(View view, final int position) {
                // If ListEntries are shown in the main view, a single ListEntry
                // was tapped on -> open dialog to show the ListEntry
                if (currentlyDisplayed.equals(Constants.displayedType.ListEntries)) {
                    showDisplayListEntryDialog(position, null);
                } else {
                    // If lists are shown in the main view, a list was tapped on
                    // -> display items of the list
                    Utilities.setAddListEntryFAB(MainActivity.this,
                            (FloatingActionButton) findViewById(R.id.fab));
                    showEntriesOfListAtPosition(position);
                }
            }

            @SuppressWarnings("unused")
            @Override
            public void onLongClick(View view, int position) {
                // Do nothing, because long clicks are for dragging items
            }
        }));

        // Handle drag and drop events (reordering of items)
        final ItemTouchHelper.Callback _ithCallback = new ItemTouchHelper.Callback() {
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                // Swap old and new position
                swapPositions(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                // Update order indices after the drag/drop or swipe event
                updateOrderIndices();
            }

            // Handle swipe events (edit / deletion of items)
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition();
                if (direction == ItemTouchHelper.LEFT) {
                    handleSwipeToDelete(position);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    handleSwipeToEdit(position);
                }
            }

            // Define in which directions items can be moved
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {
                // Items can be moved in all directions
                return makeMovementFlags(ItemTouchHelper.DOWN | ItemTouchHelper.UP |
                                ItemTouchHelper.START | ItemTouchHelper.END,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            }

            // Define how items look during drag/drop and swiping
            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    drawSwipeBackground(MainActivity.this, c, viewHolder, dX);
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        // Add ItemTouchHelper to the RecyclerView, to enable and handle drag/drop and swipe events
        final ItemTouchHelper ith = new ItemTouchHelper(_ithCallback);
        ith.attachToRecyclerView(recyclerView);
    }

    /**
     * Swap positions of two elements in the list, when one is dragged over the other
     * @param oldPosition Previous position of the dragged element
     * @param newPosition New position of the dragged element
     */
    private void swapPositions(final int oldPosition, final int newPosition) {
        // Use the correct adapter depending on what kind of items are shown
        if (currentlyDisplayed.equals(Constants.displayedType.ListEntries)) {
            Collections.swap(((EntryAdapter)mAdapter).getAll(), oldPosition, newPosition);
        } else {
            Collections.swap(((MyListAdapter)mAdapter).getAll(), oldPosition, newPosition);
        }
        // Notify the adapter that its data set has changed
        mAdapter.notifyItemMoved(oldPosition, newPosition);
    }

    /**
     * Reset all order indices of the displayed elements after a drag or swipe action
     */
    private void updateOrderIndices() {
        if (currentlyDisplayed.equals(Constants.displayedType.ListEntries)) {
            final List<ListEntry> newList =
                    ((EntryAdapter) Objects.requireNonNull(recyclerView.getAdapter())).getAll();
            int i = 0;
            for (final ListEntry e : newList) {
                e.setOrderIndex(i);
                db.updateEntryWithoutTimestamp(e);
                i++;
            }
        } else {
            final List<MyList> newList =
                    ((MyListAdapter) Objects.requireNonNull(recyclerView.getAdapter())).getAll();
            int i = 0;
            for (final MyList g : newList) {
                g.setOrderIndex(i);
                db.updateList(g);
                i++;
            }
        }
    }

    /**
     * Handle swipe-to-delete actions
     * @param position Position of the element to be deleted
     */
    private void handleSwipeToDelete(final int position) {
        if (currentlyDisplayed.equals(Constants.displayedType.ListEntries)) {
            // Delete entry
            deleteListEntryWithUndo(position);
        } else {
            // Delete list
            final MyList listToBeDeleted = listsList.get(position);
            final int entriesInList = db.getEntriesOfList(listToBeDeleted.getId()).size();
            if (entriesInList == 0) {
                deleteListWithUndo(position);
            } else {
                // This notification is necessary for the animation of sliding the item back into the view
                mAdapter.notifyItemChanged(position);
                final String reason = getString(R.string.reason_still_entries);
                final String errorMessage = getString(R.string.cant_be_deleted) + " " + reason;
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Handle swipe-to-edit actions
     * @param position Position of the element to be edited
     */
    private void handleSwipeToEdit(final int position) {
        if (currentlyDisplayed.equals(Constants.displayedType.ListEntries)) {
            // Edit dialog for entries
            showDisplayListEntryDialog(position, null);
        } else {
            // Edit dialog for lists
            showListDialog(position, null);
        }
        mAdapter.notifyItemChanged(position);
    }

    @SuppressWarnings("rawtypes")
    private RecyclerView.Adapter getCorrectAdapter() {
        // Return correct adapter depending on the items shown in the activity
        // 1) The entries of a list are shown or all entries are shown
        // 2) All available lists are shown
        if (currentlyDisplayed.equals(Constants.displayedType.ListEntries)) {
            return new EntryAdapter(listEntryList);
        } else {
            return new MyListAdapter(listsList);
        }
    }

    private void setupViews() {
        setContentView(R.layout.activity_main);
        setTitle(R.string.all_lists);
        // Set view for header bar
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get main view element (e.g. for toasts)
        coordinatorLayout = findViewById(R.id.coordinator_layout);
        // Get view that displays the items
        recyclerView = findViewById(R.id.recycler_view);
        // Get view to display if there are no lists / list items to display
        noEntriesView = findViewById(R.id.no_entries_view);

        // Refresh on swipe down
        final SwipeRefreshLayout swipeContainer = findViewById(R.id.swipeContainer);
        setupSwipeToRefresh(swipeContainer);

        // Configure (+) button
        final FloatingActionButton fab = findViewById(R.id.fab);
        setupFAB(fab);

        // Add LayoutManager and animator
        final RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new MyDividerItemDecoration(this,
                LinearLayoutManager.VERTICAL, Constants.VERTICAL_MARGIN));
    }

    /**
     * Setup the floating action button
     * @param fab The floating action button
     */
    private void setupFAB(final FloatingActionButton fab) {
        // Show different icons for different actions
        if (currentlyDisplayed.equals(Constants.displayedType.ListEntries)) {
            Utilities.setAddListEntryFAB(MainActivity.this, fab);
        } else {
            Utilities.setAddListFAB(MainActivity.this, fab);
        }
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentlyDisplayed.equals(Constants.displayedType.ListEntries)) {
                    showDisplayListEntryDialog(-1, null);
                } else {
                    showListDialog( -1, null);
                }
            }
        });
    }

    /**
     * Setup the swipe to refresh action
     */
    private void setupSwipeToRefresh(final SwipeRefreshLayout swipeContainer) {
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Hide "restore" option when refreshing
                Utilities.dismissSnackbar(snackbar);
                refreshList();
                swipeContainer.setRefreshing(false);
            }
        });

        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    /**
     * Open a create list item dialog, using the extra text from the intent
     * as the description (for example when the app is started through the "Share via..." function)
     * @param intent intent from Share via...
     */
    private void handleReceiveText(final Intent intent) {
        final String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            // Set text as description of a new list entry
            showDisplayListEntryDialog(-1, sharedText);
        }
    }

    /**
     * Show entries belonging to the list at position
     * @param position Position of the list
     */
    private void showEntriesOfListAtPosition(final int position) {
        // Get the list
        final int listId = listsList.get(position).getId();
        showEntriesOfList(listId);
    }

    /**
     * Show entries belonging to the list
     * @param listId ID of the list for which entries are shown
     */
    private void showEntriesOfList(final int listId) {
        displayedList = db.getList(listId);
        setTitle(displayedList.getTitle());

        // Add all items belonging to the list to the variable to be displayed
        listEntryList.addAll(db.getEntriesOfList(listId));
        // Clear lists to be displayed (because list items are to be displayed)
        listsList.clear();
        // Add the list of items to the view
        mAdapter = new EntryAdapter(listEntryList);
        recyclerView.setAdapter(mAdapter);

        // Only show list name below the entry title if all entries of all lists are shown
        if (!listEntryList.isEmpty()) {
            if (allEntriesShown(displayedList, listsList)) {
                listEntryList.get(0).setShowListName(true);
            } else {
                listEntryList.get(0).setShowListName(false);
            }
        }

        currentlyDisplayed = Constants.displayedType.ListEntries;

        // Refresh view by notifying the adapter that the data has changed
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Refreshes the displayed items. This can be lists as well as list entries
     */
    private void refreshList() {
        Log.d(Utilities.getLogTag(), "entriesOfListShown: " + entriesOfListShown(listsList));
        Log.d(Utilities.getLogTag(), "displayedList: " + displayedList);
        final FloatingActionButton fab = findViewById(R.id.fab);
        //if(!entriesOfListShown(listsList) && displayedList == null) {
        if (currentlyDisplayed.equals(Constants.displayedType.Lists)) {
            // If all lists are shown, reload them from the DB
            ((MyListAdapter) mAdapter).clear();
            ((MyListAdapter) mAdapter).setAll(db.getAllLists());
            listsList.addAll(db.getAllLists());
            Utilities.setAddListFAB(MainActivity.this, fab);
        } else if (currentlyDisplayed.equals(Constants.displayedType.ListEntries) && displayedList == null) {
            // If ListEntries are shown, but not of one specific list, then display all list items
            ((EntryAdapter) mAdapter).clear();
            ((EntryAdapter) mAdapter).addAll(db.getAllEntriesOrderedByLastModified());
            if (!listEntryList.isEmpty()) {
                listEntryList.get(0).setShowListName(true);
            }
            Utilities.setAddListEntryFAB(MainActivity.this, fab);
        } else {
            // Reload the items of a specific list from the DB
            ((EntryAdapter) mAdapter).clear();
            ((EntryAdapter) mAdapter).addAll(db.getEntriesOfList(displayedList.getId()));
            if (!listEntryList.isEmpty()) {
                listEntryList.get(0).setShowListName(false);
            }
            Utilities.setAddListEntryFAB(MainActivity.this, fab);
        }
        // Refresh view by notifying the adapter that the data has changed
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Insert new ListEntry into the DB and refresh the list
     * @param title Title of the new entry
     * @param description Description of the new entry
     * @param listId ID of the list that the entry belongs to
     * @param alarmDateString String representing the alarm date in the format specified
     *                       in Constants.ALARM_DATE_FORMAT
     * @return The new ListEntry
     */
    private ListEntry insertListEntryInDBAndRefresh(final String title, final String description,
                                                    final int listId, final String alarmDateString) {
        // Insert new ListEntry into the database and get its ID
        final long id = db.insertEntry(title, description, listId, alarmDateString);

        // Load the new ListEntry from the DB
        final ListEntry entry = db.getEntry(id);
        entry.setShowListName(false);

        // Refresh the data in the adapter
        listEntryList.clear();
        listEntryList.addAll(db.getEntriesOfList(listId));
        toggleEmptyNotes();
        mAdapter.notifyDataSetChanged();

        return entry;
    }

    /**
     * Update ListEntry in the database and the View
     * @param title New title of the entry
     * @param description New description of the entry
     * @param listId ID of the new list that the entry belongs to
     * @param position New position of the entry
     * @param alarmDateString String representing the new alarm date in the format specified
     *                        in Constants.ALARM_DATE_FORMAT
     * @return The updated ListEntry
     */
    private ListEntry updateListEntryAndRefresh(final String title, final String description,
                                                final int listId, final int position,
                                                final String alarmDateString) {
        final ListEntry entry = listEntryList.get(position);
        entry.setTitle(title);
        entry.setDescription(description);
        entry.setListId(listId);
        entry.setAlarmDate(alarmDateString);

        // Update in DB
        db.updateEntry(entry);

        // Refresh the view
        listEntryList.set(position, entry);
        mAdapter.notifyItemChanged(position);
        return entry;
    }

    /**
     * Show alert dialog with options to enter / edit a list.
     * If shouldUpdate is true, an existing list's data is
     * displayed and the button text is changed to UPDATE
     * @param position Position of the list to be updated or -1 if a new list is created
     * @param listSpinner If the new list is created from the "Add new list..." entry in the spinner,
     *                    the Spinner object is needed to add the new list to it afterwards
     */
    private void showListDialog(final int position, final Spinner listSpinner) {
        final boolean isUpdate = position > -1;

        // Inflate the layout of the dialog
        final LayoutInflater layoutInflaterAndroid = LayoutInflater.from(getApplicationContext());
        final View view = layoutInflaterAndroid.inflate(R.layout.edit_list_dialog, null);

        // Build dialog with the layout
        final android.app.AlertDialog.Builder alertDialogBuilderUserInput =
                new android.app.AlertDialog.Builder(MainActivity.this);
        alertDialogBuilderUserInput.setView(view);

        // Set texts
        final EditText inputName = view.findViewById(R.id.name_edit);
        final TextView dialogTitle = view.findViewById(R.id.list_dialog_title);
        dialogTitle.setText(isUpdate ? getString(R.string.lbl_edit_list_title) : getString(R.string.lbl_new_list_title));
        // Load existing data if it's an update
        if (isUpdate) {
            inputName.setText(listsList.get(position).getTitle());
            inputName.setSelection(inputName.getText().length());
        }
        // Set buttons
        alertDialogBuilderUserInput
                .setCancelable(false)
                .setPositiveButton(isUpdate ? R.string.update : R.string.save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) {
                        // Use a default name if none is entered
                        if (TextUtils.isEmpty(inputName.getText().toString())) {
                            inputName.setText(getString(R.string.default_name));
                        }
                        // Close dialog
                        dialogBox.dismiss();

                        // If user is updating a list, update it by its ID
                        if (isUpdate) {
                            updateList(inputName.getText().toString(), position);
                        } else {
                            // Create new list if it's not an update
                            insertListInDBAndRefresh(inputName.getText().toString(), listSpinner);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogBox, int id) {
                                dialogBox.cancel();
                            }
                        });

        // Build dialog
        final android.app.AlertDialog alertDialog = alertDialogBuilderUserInput.create();
        // Show dialog and keyboard
        if (inputName.requestFocus()) {
            Objects.requireNonNull(alertDialog.getWindow()).setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        alertDialog.show();
    }

    /**
     * Update list in the database and update item in the view by its position
     * @param title New title of the list
     * @param position Position in the view of the list to be updated
     */
    private void updateList(final String title, final int position) {
        final MyList list = listsList.get(position);
        list.setTitle(title);
        db.updateList(list);
        listsList.set(position, list);
        mAdapter.notifyItemChanged(position);
    }

    /**
     * Insert new list into the database and refresh the view
     * @param title The title of the new list
     * @param listSpinner If the new list is created from the "Add new list..." entry in the spinner,
     *                    the Spinner object is needed to add the new list to it
     */
    private void insertListInDBAndRefresh(final String title, final Spinner listSpinner) {
        Log.d(Utilities.getLogTag(), "Creating list: " + title);
        // Insert list into database and get its id
        final long id = db.insertList(title);
        // Get the newly inserted list from the database
        final MyList list = db.getList(id);
        Log.d(Utilities.getLogTag(), "Got list from DB: " + list.getTitle() + ": " + list.getId());

        // Refresh the displayed lists
        listsList.clear();
        listsList.addAll(db.getAllLists());
        // Refresh the view
        toggleEmptyNotes();
        mAdapter.notifyDataSetChanged();

        Log.d(Utilities.getLogTag(), "Created list: " + title);
        if (listSpinner != null) {
            Log.d(Utilities.getLogTag(), "Updating Spinner entries");
            final ListsSpinnerAdapter adapter = (ListsSpinnerAdapter) listSpinner.getAdapter();
            adapter.clear();
            adapter.addAll(listsList);
            adapter.notifyDataSetChanged();
            int selectIndex = 0;
            for(MyList l : listsList) {
                if (l.getId() == list.getId()) {
                    selectIndex = listsList.indexOf(l);
                }
            }
            listSpinner.setSelection(selectIndex);
            Log.d(Utilities.getLogTag(), "Selected " +
                    Objects.requireNonNull(adapter.getItem(0)).getTitle());
        }
    }

    /**
     * Convert date and time into the expected format for alarm dates
     * @param date String containing the date
     * @param time String containing the time
     * @return Alarm date in format Constants.ALARM_DATE_FORMAT
     */
    private String convertDateAndTimeToAlarmDate(String date, String time) {
        try {
            Log.d(Utilities.getLogTag(), "Date before parsing: " + date);
            Log.d(Utilities.getLogTag(), "Time before parsing: " + time);

            final Date parsedDate = Utilities.parseFromLocalDateString(date);
            final Date parsedTime = Utilities.parseFromLocalTimeString(time);

            final SimpleDateFormat dateFormat = Utilities.getDateFormat();
            final SimpleDateFormat timeFormat = Utilities.getTimeFormat();

            final String newDate = dateFormat.format(parsedDate);
            final String newTime = timeFormat.format(parsedTime);

            final String newAlarmDateString = newDate + " " + newTime;
            Log.d(Utilities.getLogTag(), "newAlarmDateString: " + newAlarmDateString);
            return newAlarmDateString;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Parse alarm date from entry. If there's an exception, return the default date.
     * @param entry The entry
     * @param defaultDate The default date
     * @return The parsed alarm date or the default date
     */
    private Date parseEntryAlarmDate(final ListEntry entry, final Date defaultDate) {
        final Date tempAlarmDate = new Date();
        final SimpleDateFormat fullDateFormat = Utilities.getFullDateFormat();
        try {
            tempAlarmDate.setTime(fullDateFormat.parse(entry.getAlarmDate()).getTime());
        } catch (ParseException | NullPointerException e) {
            Log.e(Utilities.getLogTag(), "Error parsing alarm date");
            tempAlarmDate.setTime(defaultDate.getTime());
        }
        return tempAlarmDate;
    }

    /**
     * Show dialog that displays one list entry
     * @param position Position of the entry to be displayed in the listEntryList
     */
    private void showDisplayListEntryDialog(final int position, final String defaultText) {
        ListEntry tempEntry;
        if (position >= 0) {
            tempEntry = listEntryList.get(position);
            listOfEntry = tempEntry.getListId();
        } else {
            if (displayedList != null) {
                // If a specific list was shown, then preselect it
                listOfEntry = displayedList.getId();
            } else {
                // Else, preselect the first list
                listOfEntry = db.getAllLists().get(0).getId();
            }
            tempEntry = new ListEntry(-1, "", "", null, -1, listOfEntry);
        }
        final ListEntry entry = tempEntry;

        final String oldTitle = entry.getTitle();
        // Get the view
        final LayoutInflater layoutInflaterAndroid = LayoutInflater.from(MainActivity.this);
        final View view = layoutInflaterAndroid.inflate(R.layout.show_dialog, null);

        // Set texts and buttons
        final EditText showTitle = view.findViewById(R.id.view_entry_title);
        final EditText showDescription = view.findViewById(R.id.view_description);
        final ImageButton editButton = view.findViewById(R.id.edit_entry_button);
        final TextView alarmDateTextView = view.findViewById(R.id.alarmDateTextView);
        final TextView alarmTimeTextView = view.findViewById(R.id.alarmTimeTextView);

        // Build the dialog
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(view);
        alertDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ListEntry newEntry;
                if (listOfEntry == -1) {
                    listOfEntry = db.getAllLists().get(0).getId();
                }
                if (position >= 0) {
                    newEntry = updateListEntryAndRefresh(entry.getTitle(), entry.getDescription(), listOfEntry, position, entry.getAlarmDate());
                } else {
                    newEntry = insertListEntryInDBAndRefresh(entry.getTitle(), entry.getDescription(), listOfEntry, entry.getAlarmDate());
                }
                // If the alarm is on
                if (View.VISIBLE == alarmDateTextView.getVisibility()) {
                    AlarmHandler.cancelAlarm(MainActivity.this, oldTitle, newEntry.getId());
                    final Calendar newAlarmDate = Calendar.getInstance();
                    try {
                        //noinspection ConstantConditions
                        newAlarmDate.setTime(Utilities.getFullDateFormat().parse(newEntry.getAlarmDate()));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    AlarmHandler.setAlarm(MainActivity.this, newAlarmDate, newEntry.getTitle(), newEntry.getId());
                } else {
                    // If the alarm is off
                    AlarmHandler.cancelAlarm(MainActivity.this, oldTitle, newEntry.getId());
                }
                refreshList();
            }
        });
        alertDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogBox, int id) {
                listOfEntry = -1;
                dialogBox.dismiss();
            }
        });
        final AlertDialog alertDialog = alertDialogBuilder.create();

        // If this is a new entry, display the keyboard
        if (position == -1) {
            if (showTitle.requestFocus()) {
                Objects.requireNonNull(alertDialog.getWindow()).
                        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }
        }

        alarmTimeTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d(Utilities.getLogTag(), "Time changed: " + s.toString());
                String dateString = alarmDateTextView.getText().toString();
                String timeString = alarmTimeTextView.getText().toString();
                String alarmDate = convertDateAndTimeToAlarmDate(dateString, timeString);
                entry.setAlarmDate(alarmDate);
            }
        });

        alarmDateTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d(Utilities.getLogTag(), "Date changed: " + s.toString());
                String dateString = alarmDateTextView.getText().toString();
                String timeString = alarmTimeTextView.getText().toString();
                String alarmDate = convertDateAndTimeToAlarmDate(dateString, timeString);
                entry.setAlarmDate(alarmDate);
            }
        });

        final ImageView alarmIcon = view.findViewById(R.id.alarmIcon);

        final Date defaultAlarmDate = new Date();
        final String oldAlarmDate = entry.getAlarmDate();
        if (position >= 0) {
            if (oldAlarmDate != null) {
                final SimpleDateFormat fullDateFormat = Utilities.getFullDateFormat();
                try {
                    //noinspection ConstantConditions
                    defaultAlarmDate.setTime(fullDateFormat.parse(oldAlarmDate).getTime());
                    alarmTimeTextView.setVisibility(View.VISIBLE);
                    alarmDateTextView.setVisibility(View.VISIBLE);
                    alarmIcon.setImageDrawable(getDrawable(R.drawable.ic_notification_on));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        alarmIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (View.GONE == alarmTimeTextView.getVisibility()) {
                    alarmDateTextView.setText(Utilities.convertToLocalDateString(defaultAlarmDate));
                    alarmTimeTextView.setText(Utilities.convertToLocalTimeString(defaultAlarmDate));
                    alarmTimeTextView.setVisibility(View.VISIBLE);
                    alarmDateTextView.setVisibility(View.VISIBLE);
                    alarmIcon.setImageDrawable(getDrawable(R.drawable.ic_notification_on));
                    String dateString = alarmDateTextView.getText().toString();
                    String timeString = alarmTimeTextView.getText().toString();
                    String alarmDate = convertDateAndTimeToAlarmDate(dateString, timeString);
                    entry.setAlarmDate(alarmDate);
                } else {
                    alarmTimeTextView.setVisibility(View.GONE);
                    alarmDateTextView.setVisibility(View.GONE);
                    alarmIcon.setImageDrawable(getDrawable(R.drawable.ic_notification_off_grey));
                    entry.setAlarmDate(null);
                }
            }
        });

        alarmDateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show DatePicker
                // Parse date shown in text view
                final Date tempAlarmDate = parseEntryAlarmDate(entry, defaultAlarmDate);
                // If the default date is shown in the text view use it to initialize the DatePicker,
                // if another date is shown, use that one
                if (tempAlarmDate.equals(defaultAlarmDate)) {
                    showDatePickerDialog((TextView) v.getRootView().findViewById(R.id.alarmDateTextView),
                            defaultAlarmDate);
                } else {
                    showDatePickerDialog((TextView) v.getRootView().findViewById(R.id.alarmDateTextView),
                            tempAlarmDate);
                }
            }
        });
        alarmTimeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show TimePicker
                // Parse date shown in text view
                final Date tempAlarmDate = parseEntryAlarmDate(entry, defaultAlarmDate);
                // If the default date is shown in the text view use it to initialize the DatePicker,
                // if another date is shown, use that one
                if (tempAlarmDate.equals(defaultAlarmDate)) {
                    showTimePickerDialog((TextView) v.getRootView().findViewById(R.id.alarmTimeTextView),
                            defaultAlarmDate);
                } else {
                    showTimePickerDialog((TextView) v.getRootView().findViewById(R.id.alarmDateTextView),
                            tempAlarmDate);
                }
            }
        });

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showListSelectionDialog(position);
            }
        });
        if (!entry.getTitle().isEmpty()) {
            showTitle.setText(entry.getTitle());
        } else {
            showTitle.setHint(R.string.hint_enter_title);
        }
        showTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) {
                Log.d(Utilities.getLogTag(), "Title updated: " + editable.toString());
                entry.setTitle(editable.toString());
            }
        });


        if (!entry.getDescription().isEmpty()) {
            showDescription.setText(entry.getDescription());
        } else {
            showDescription.setHint(R.string.hint_enter_description);
        }
        showDescription.setVisibility(View.VISIBLE);
        showDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) {
                Log.d(Utilities.getLogTag(), "Description updated: " + editable.toString());
                entry.setDescription(editable.toString().trim());
            }
        });
        if (defaultText != null) {
            showDescription.setText(defaultText);
        }

        Log.d(Utilities.getLogTag(), "ShowNote: Alarm date: " + entry.getAlarmDate());
        if (entry.getAlarmDate() != null) {
            Date parsedAlarmDate = null;
            try {
                parsedAlarmDate = Utilities.getFullDateFormat().parse(entry.getAlarmDate());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            final String date = Utilities.convertToLocalDateString(parsedAlarmDate);
            final String time = Utilities.convertToLocalTimeString(parsedAlarmDate);
            alarmDateTextView.setText(date);
            alarmTimeTextView.setText(time);
            alarmDateTextView.setVisibility(View.VISIBLE);
            alarmTimeTextView.setVisibility(View.VISIBLE);
            alarmIcon.setVisibility(View.VISIBLE);
        }
        // Show
        alertDialog.show();
    }

    /**
     * Show dialog that displays the date picker
     * @param alarmDateTextView The TextView that shows the alarm date
     * @param date The date object containing the alarm date
     */
    private void showDatePickerDialog(final TextView alarmDateTextView, final Date date) {
        // Get the view
        final LayoutInflater layoutInflaterAndroid = LayoutInflater.from(MainActivity.this);
        final View view = layoutInflaterAndroid.inflate(R.layout.show_datepicker_dialog, null);
        final DatePicker datePicker = view.findViewById(R.id.alarmDatePicker);
        final Calendar c = Calendar.getInstance();
        c.setTime(date);
        datePicker.updateDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        // Build the dialog
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(view);
        alertDialogBuilder
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) { }
                })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogBox, int id) { }
                        });
        final AlertDialog datePickerDialog = alertDialogBuilder.create();
        datePickerDialog.show();

        datePickerDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int day = datePicker.getDayOfMonth();
                final int month = datePicker.getMonth();
                final int year =  datePicker.getYear();
                final Calendar cal = Calendar.getInstance();
                cal.set(year, month, day);
                final String date = Utilities.convertToLocalDateString(cal.getTime());
                alarmDateTextView.setText(date);
                datePickerDialog.dismiss();
            }
        });
        datePickerDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePickerDialog.dismiss();
            }
        });
    }

    /**
     * Show dialog that displays the time picker
     * @param alarmTimeTextView The TextView that shows the alarm time
     * @param date The date object containing the alarm time
     */
    private void showTimePickerDialog(final TextView alarmTimeTextView, final Date date) {
        // Get the view
        final LayoutInflater layoutInflaterAndroid = LayoutInflater.from(MainActivity.this);
        final View view = layoutInflaterAndroid.inflate(R.layout.show_timepicker_dialog, null);
        final TimePicker timePicker = view.findViewById(R.id.alarmTimePicker);
        timePicker.setIs24HourView(DateFormat.is24HourFormat(getApplicationContext()));
        final Calendar c = Calendar.getInstance();
        c.setTime(date);
        timePicker.setCurrentHour(c.get(Calendar.HOUR_OF_DAY));
        timePicker.setCurrentMinute(c.get(Calendar.MINUTE));

        // Build the dialog
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(view);
        alertDialogBuilder
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) { }
                })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogBox, int id) { }
                        });
        final AlertDialog timePickerDialog = alertDialogBuilder.create();
        timePickerDialog.show();

        timePickerDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int hour = timePicker.getCurrentHour();
                final int minute = timePicker.getCurrentMinute();
                final Calendar cal = Calendar.getInstance();
                cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH), hour, minute);
                Log.d(Utilities.getLogTag(), "From time picker: " + hour + ":" + minute);
                final String time = Utilities.convertToLocalTimeString(cal.getTime());
                Log.d(Utilities.getLogTag(), "Converted to local time: " + time);
                alarmTimeTextView.setText(time);
                timePickerDialog.dismiss();
            }
        });
        timePickerDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timePickerDialog.dismiss();
            }
        });
    }

    /**
     * Show dialog to change the list of a list entry.
     * The list of the entry is preselected in the Spinner.
     * @param position Position of the entry to be updated or -1 if a new entry is created
     */
    private void showListSelectionDialog(final int position) {
        final boolean isUpdate = position > -1;
        // Get the view
        final LayoutInflater layoutInflaterAndroid = LayoutInflater.from(getApplicationContext());
        final View view = layoutInflaterAndroid.inflate(R.layout.list_selection_dialog, null);

        // Build the dialog
        final AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(MainActivity.this);
        // Set title text color
        alertDialogBuilderUserInput.setView(view);
        String color;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            color = String.valueOf(getColor(R.color.colorAccent));
        } else {
            color = "#D81B60";
        }
        final String colorHtmlString = "<font color='" + color + "'>" + getString(R.string.select_list) + "</font>";
        alertDialogBuilderUserInput.setTitle(Html.fromHtml(colorHtmlString));

        // Initialize list selection dropdown
        final Spinner listSelection = view.findViewById(R.id.list_selection);
        int tempListId;
        if (isUpdate) {
            tempListId = initializeListSelectionSpinner(listSelection, listEntryList.get(position));
        } else {
            tempListId = initializeListSelectionSpinner(listSelection, null);
        }
        final int oldListId = tempListId;

        alertDialogBuilderUserInput
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) {
                        listOfEntry = ((MyList) listSelection.getSelectedItem()).getId();
                        dialogBox.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogBox, int id) {
                                listOfEntry = oldListId;
                                dialogBox.dismiss();
                            }
                        });

        // Show dialog
        final AlertDialog alertDialog = alertDialogBuilderUserInput.create();
        alertDialog.show();
    }

    /**
     * Determine the index of the list that will be preselected in the Spinner
     * @param entry The entry for which the dialog is opened
     * @param lists List of all lists
     * @return Index of the list that will be preselected in the Spinner
     */
    private int determineListPreselectionIndex(final ListEntry entry, final List<MyList> lists) {
        final boolean isUpdate = entry != null;
        int preselectListIndex = -1;
        // Determine which list name should be preselected in the drop down
        // Determine the index of the list that was shown when the dialog was started or
        // that the current item belongs to
        // First, determine the ID of the list
        int id = -1;
        // Checks if this is an update
        if (!isUpdate) {
            // Checks if a specific list or all list items were shown
            // when the dialog was started. If it was all list items, then
            // simply preselect the first list.
            if (displayedList != null) {
                // If a specific list was shown, then preselect it
                id = displayedList.getId();
            } else {
                preselectListIndex = 0;
            }
        } else {
            // If this is an update, we can get the list ID from the list entry
            id = entry.getListId();
        }
        if (preselectListIndex < 0) {
            int i = 0;
            for (MyList list : lists) {
                // Second, iterate over all lists and stop when we found the right one
                if (list.getId() == id) {
                    preselectListIndex = i;
                    break;
                }
                i++;
            }
        }
        return preselectListIndex;
    }

    /**
     * Populate the Spinner for list selection and
     * preselect the list that the entry belongs to, if possible
     * @param listSelection The Spinner object
     * @param entry The entry to be updated
     */
    private int initializeListSelectionSpinner(final Spinner listSelection, final ListEntry entry) {
        final List<MyList> lists = db.getAllLists();
        final ArrayAdapter<MyList> listAdapter = new ListsSpinnerAdapter(MainActivity.this,
                R.layout.spinner_item, lists);
        listAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        listSelection.setAdapter(listAdapter);
        listSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == parent.getCount() - 1) {
                    showListDialog(-1, listSelection);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        // Preselect the list
        final int preselectListIndex = determineListPreselectionIndex(entry, lists);
        listSelection.setSelection(preselectListIndex);
        return ((MyList)listSelection.getSelectedItem()).getId();
    }

    /**
     * Show lists or list items if there are any or the noNotesView if not
     */
    private void toggleEmptyNotes() {
        if (db.getAllLists().size() > 0) {
            noEntriesView.setVisibility(View.GONE);
        } else {
            noEntriesView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Add the menu items to the action bar
     * @param menu The menu to be inflated
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (noEntriesView.getVisibility() != View.GONE) {
            menu.findItem(R.id.action_show_all).setVisible(false);
        } else {
            menu.findItem(R.id.action_show_all).setVisible(true);
        }
        return true;
    }

    /**
     * Handle taps on the menu items
     * @param item The selected menu item
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        final FloatingActionButton fab = findViewById(R.id.fab);
        switch (id){
            // Show all list entries
            case R.id.action_show_all:
                displayAllEntries();
                Utilities.setAddListEntryFAB(MainActivity.this, fab);
                return true;
            case R.id.action_show_all_lists:
                displayLists();
                Utilities.setAddListFAB(MainActivity.this, fab);
                return true;
            // Show backup dialog
            case R.id.action_backup:
                showBackupDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Display all list entries
     */
    private void displayAllEntries() {
        // Reset because only entries are displayed, regardless of the lists they belong to
        displayedList = null;
        listsList.clear();
        // Add all list items
        listEntryList.clear();
        listEntryList.addAll(db.getAllEntriesOrderedByLastModified());
        // Set correct adapter
        mAdapter = new EntryAdapter(listEntryList);
        recyclerView.setAdapter(mAdapter);
        if (listEntryList.size() > 0) {
            listEntryList.get(0).setShowListName(true);
        }
        currentlyDisplayed = Constants.displayedType.ListEntries;
        // Refresh the view
        mAdapter.notifyDataSetChanged();
        // Set title
        setTitle(R.string.all_entries);
    }

    /**
     * Show a dialog to create or restore a backup
     */
    private void showBackupDialog() {
        Log.d(Utilities.getLogTag(), "showBackupDialog()");
        // Get the view
        final LayoutInflater layoutInflaterAndroid = LayoutInflater.from(getApplicationContext());
        final View view = layoutInflaterAndroid.inflate(R.layout.backup_dialog, null);

        // Build the dialog
        final AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilderUserInput.setView(view);
        final Spinner backupSelection = view.findViewById(R.id.backup_selection);

        // Initialize backup mode dropdown
        final String[] backupModes = {getString(R.string.backup_create), getString(R.string.backup_restore)};
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, backupModes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        backupSelection.setAdapter(adapter);
        backupSelection.setSelection(0);

        alertDialogBuilderUserInput
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) {
                        handleBackupDialogPositiveButton((String)backupSelection.getSelectedItem());
                        dialogBox.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogBox, int id) {
                                dialogBox.dismiss();
                            }
                        });

        // Show dialog
        final AlertDialog alertDialog = alertDialogBuilderUserInput.create();
        Log.d(Utilities.getLogTag(), "Before alertDialog.show()");
        alertDialog.show();
        Log.d(Utilities.getLogTag(), "After alertDialog.show()");
    }

    private void handleBackupDialogPositiveButton(final String backupString) {
        // Determine selected backup mode
        if (backupString.equals(getString(R.string.backup_restore))) {
            backupMode = BackupHelper.BackupMode.RESTORE;
        } else if (backupString.equals(getString(R.string.backup_create))) {
            backupMode = BackupHelper.BackupMode.CREATE;
        }

        Log.d(Utilities.getLogTag(), "Before Toast");
        final BackupHelper backupHelper = new BackupHelper(MainActivity.this);

        if (BackupHelper.BackupMode.CREATE.equals(backupMode)) {
            createBackup(backupHelper);
        } else if (BackupHelper.BackupMode.RESTORE.equals(backupMode)) {
            restoreBackup(backupHelper);
        } else {
            Log.d(Utilities.getLogTag(), "Unknown backup mode");
        }
    }

    private void createBackup(final BackupHelper backupHelper) {
        Log.d(Utilities.getLogTag(), "Create Mode");
        Toast.makeText(getApplicationContext(), R.string.backup_start, Toast.LENGTH_SHORT).show();
        if (backupHelper.createBackup()) {
            Toast.makeText(getApplicationContext(), R.string.backup_end, Toast.LENGTH_LONG).show();
            File backupDirectory = MainActivity.this.getExternalFilesDir(null);
            Toast.makeText(getApplicationContext(),
                    backupDirectory != null ? backupDirectory.getAbsolutePath() : getString(R.string.backup_fail),
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), R.string.backup_fail, Toast.LENGTH_LONG).show();
        }
    }

    private void restoreBackup(final BackupHelper backupHelper) {
        Log.d(Utilities.getLogTag(), "Restore Mode");
        // Backup can only be restored if the file is in the expected location
        File backupDirectory = MainActivity.this.getExternalFilesDir(null);
        if (backupDirectory != null) {
            final File location = new File(backupDirectory.getAbsolutePath(),
                    Constants.BACKUP_FILE_NAME);
            if (backupHelper.restoreBackup(location)) {
                Toast.makeText(getApplicationContext(), R.string.backup_restore_end,
                        Toast.LENGTH_LONG).show();
                // Refresh displayed items
                displayLists();
            } else {
                Toast.makeText(getApplicationContext(), R.string.backup_restore_fail,
                        Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), R.string.backup_restore_fail,
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Handle taps on the back button
     */
    @Override
    public void onBackPressed() {
        // Hide "restore" option when back is pressed
        Utilities.dismissSnackbar(snackbar);

        // Go back to the default - show all lists
        if (currentlyDisplayed.equals(Constants.displayedType.ListEntries)) {
            displayLists();
            final FloatingActionButton fab = findViewById(R.id.fab);
            Utilities.setAddListFAB(MainActivity.this, fab);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Show all lists
     */
    private void displayLists() {
        // Reset, because no list items are displayed
        listEntryList.clear();
        displayedList = null;
        setTitle(R.string.all_lists);
        // Load lists from DB
        listsList.clear();
        listsList.addAll(db.getAllLists());
        mAdapter = new MyListAdapter(listsList);
        recyclerView.setAdapter(mAdapter);
        currentlyDisplayed = Constants.displayedType.Lists;
        // If there are no lists to be displayed, show a note
        toggleEmptyNotes();
        // Refresh the view
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Remove an entry from the view, but only actually delete it after a timeout or if the snackbar
     * was dismissed. This way, it can be restored by a tap on the snackbar.
     * @param position    The position of the entry to be deleted
     */
    private void deleteListEntryWithUndo(final int position) {
        final ListEntry deletedEntry = listEntryList.get(position);
        // Remove the item from the view
        ((EntryAdapter)mAdapter).removeItem(position);
        // Show snackbar with UNDO option
        showUndoSnackbar(position, deletedEntry, Constants.displayedType.ListEntries);
    }

    /**
     * Remove a list from the view, but only actually delete it after a timeout or if the snackbar
     * was dismissed. This way, it can be restored by a tap on the snackbar.
     * @param position    The position of the list to be deleted
     */
    private void deleteListWithUndo(final int position) {
        final MyList deletedList = listsList.get(position);
        // Remove the item from the view
        ((MyListAdapter)mAdapter).removeList(position);
        // Show snackbar with undo option
        showUndoSnackbar(position, deletedList, Constants.displayedType.Lists);
    }

    /**
     * Show snackbar with info which item was deleted and an undo option
     * @param position Position of the deleted item
     * @param deletedItem The deleted item
     * @param itemType Type of the deleted item
     */
    private void showUndoSnackbar(final int position, final ListItem deletedItem,
                                  final Constants.displayedType itemType) {
        // Get the name of the item to be deleted
        String name = "";
        if (itemType.equals(Constants.displayedType.Lists)) {
            name = deletedItem.getTitle();
        } else if (itemType.equals(Constants.displayedType.ListEntries)){
            name = deletedItem.getTitle();
        }
        // Show snackbar
        snackbar = Snackbar.make(coordinatorLayout, name + " " +
                getResources().getString(R.string.deleted), Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Undo is selected, restore the deleted item
                if (itemType.equals(Constants.displayedType.Lists)) {
                    ((MyListAdapter) mAdapter).restoreList((MyList) deletedItem, position);
                } else if (itemType.equals(Constants.displayedType.ListEntries)) {
                    ((EntryAdapter) mAdapter).restoreItem((ListEntry) deletedItem, position);
                }
            }
        });
        // Delete if the snackbar is dismissed
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar sb, int event) {
                handleSnackbarDismiss(event, deletedItem, itemType);
            }
        });
        snackbar.setActionTextColor(Color.YELLOW);
        snackbar.show();
    }

    /**
     * Delete the item when the undo snackbar is dismissed
     * @param event The way the snackbar was dismissed
     * @param deletedItem The deleted item
     * @param itemType Type of the deleted item
     */
    private void handleSnackbarDismiss(final int event, final ListItem deletedItem,
                                       final Constants.displayedType itemType) {
        if (event == Snackbar.Callback.DISMISS_EVENT_SWIPE ||
                event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT ||
                event == Snackbar.Callback.DISMISS_EVENT_CONSECUTIVE ||
                event == Snackbar.Callback.DISMISS_EVENT_MANUAL) {

            // Delete the item from the database
            if (itemType.equals(Constants.displayedType.Lists)) {
                db.deleteList((MyList) deletedItem);
            } else if (itemType.equals(Constants.displayedType.ListEntries)) {
                db.deleteEntry((ListEntry) deletedItem);
            }

            // If the last item was deleted, show a note
            toggleEmptyNotes();
        }
    }
}