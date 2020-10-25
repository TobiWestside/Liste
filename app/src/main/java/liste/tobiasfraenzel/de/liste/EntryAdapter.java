package liste.tobiasfraenzel.de.liste;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import liste.tobiasfraenzel.de.liste.database.DatabaseHelper;
import liste.tobiasfraenzel.de.liste.utils.Constants;
import liste.tobiasfraenzel.de.liste.utils.Utilities;

// Adapter to display the ListEntries
public class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.MyViewHolder>{

    final private List<ListEntry> entriesList;
    private boolean showListName = false;

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        final public TextView title;
        final public TextView description;
        final public TextView timestamp;
        final public TextView listName;
        final public TextView alarmDate;
        final public ImageView alarmIcon;

        // Get views
        public MyViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.title);
            description = view.findViewById(R.id.description);
            timestamp = view.findViewById(R.id.timestamp);
            listName = view.findViewById(R.id.listName);
            alarmDate = view.findViewById(R.id.alarmDatePreview);
            alarmIcon = view.findViewById(R.id.alarmIconPreview);
        }
    }

    public EntryAdapter(List<ListEntry> entriesList) {
        this.entriesList = entriesList;
    }

    // Create view objects from layout (inflate)
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.entry_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    // Populate the view with data
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        final ListEntry entry = entriesList.get(position);
        // Show or hide the list name for all ListEntries, depending on what's set in the first entry
        if (position == 0 && entry.isShowListName()) {
            showListName = true;
        }
        if (showListName) {
            holder.listName.setVisibility(View.VISIBLE);
            DatabaseHelper db = new DatabaseHelper(holder.itemView.getContext());
            holder.listName.setText(db.getListTitle(entry.getListId()));
        } else {
            holder.listName.setVisibility(View.GONE);
        }

        // Set title and description
        holder.title.setText(entry.getTitle());
        if (entry.getDescription().isEmpty()) {
            // Hide description to safe space if it's empty
            holder.description.setVisibility(View.GONE);
        } else {
            holder.description.setVisibility(View.VISIBLE);
            holder.description.setText(entry.getDescription());
        }

        // Format and display "last modified" timestamp
        holder.timestamp.setText(formatDate(entry.getModificationDate()));

        if (entry.getAlarmDate() == null || entry.getAlarmDate().isEmpty()) {
            holder.alarmIcon.setVisibility(View.GONE);
            holder.alarmDate.setVisibility(View.GONE);
        } else {
            holder.alarmIcon.setVisibility(View.VISIBLE);
            holder.alarmDate.setVisibility(View.VISIBLE);

            Date parsedAlarmDate;
            try {
                parsedAlarmDate = Utilities.getFullDateFormat().parse(entry.getAlarmDate());

                final String date = Utilities.convertToLocalDateString(parsedAlarmDate);
                final String time = Utilities.convertToLocalTimeString(parsedAlarmDate);

                holder.alarmDate.setText(time + " " + date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onViewAttachedToWindow(MyViewHolder holder) {
        // Only used for debugging
        Log.d(Utilities.getLogTag(), "Title: " + holder.title.getText());
        Log.d(Utilities.getLogTag(), "Show List: " + holder.listName.getVisibility());
        Log.d(Utilities.getLogTag(), "Lines: " + holder.title.getLineCount());
        Log.d(Utilities.getLogTag(), "Width: " + holder.title.getMeasuredWidth());

    }

    @Override
    public int getItemCount() {
        return entriesList.size();
    }

    /**
     * Format timestamp to local date format
     * @param dateStr Date String in the format 2018-02-21 00:15:42
     * @return Formatted date String in the local format, for example Jun 20, 2020 or 20.06.2020
     */
    private String formatDate(final String dateStr) {
        try {
            @SuppressLint("SimpleDateFormat") final SimpleDateFormat fmt = new SimpleDateFormat(Constants.TIMESTAMP_DATE_FORMAT);
            final Date date = fmt.parse(dateStr);
            return Utilities.convertToLocalDateStringShort(date);
        } catch (ParseException e) {
            Log.e(Utilities.getLogTag(), "ParseException on this date: " + dateStr);
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Delete all ListEntries from this adapter
     */
    final protected void clear() {
        entriesList.clear();
    }

    /**
     * Add a List of ListEntries to this adapter
     * @param entries The ListEntries to be added
     */
    final protected void addAll(final List<ListEntry> entries) {
        entriesList.addAll(entries);
    }

    /**
     * Get all ListEntries currently added to this adapter
     * @return The List of ListEntries
     */
    final protected List<ListEntry> getAll() {
        return entriesList;
    }

    /**
     * Remove one ListEntry from the adapter
     * @param position The position of the ListEntry
     */
    final protected void removeItem(final int position) {
        entriesList.remove(position);
        // Notify the item removed by position to perform recycler view delete animations
        // NOTE: don't call notifyDataSetChanged()
        notifyItemRemoved(position);
    }

    /**
     * Add one ListEntry at the specified position to this adapter
     * @param entry The ListEntry to be added
     * @param position The position at which the ListEntry is added
     */
    final protected void restoreItem(final ListEntry entry, final int position) {
        entriesList.add(position, entry);
        // Notify item added by position
        notifyItemInserted(position);
    }
}