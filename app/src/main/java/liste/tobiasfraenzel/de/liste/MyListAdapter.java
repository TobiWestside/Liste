package liste.tobiasfraenzel.de.liste;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import liste.tobiasfraenzel.de.liste.database.DatabaseHelper;

// Adapter to display the MyLists
public class MyListAdapter extends RecyclerView.Adapter<MyListAdapter.MyViewHolder>{

    private List<MyList> listsList;

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        final public TextView title;
        final public TextView entryCount;

        // Get views
        public MyViewHolder(final View view) {
            super(view);
            title = view.findViewById(R.id.name);
            entryCount = view.findViewById(R.id.entryCount);
        }
    }


    public MyListAdapter(final List<MyList> listsList) {
        this.listsList = listsList;
    }

    // Create view objects from layout (inflate)
    @Override
    @NonNull
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.group_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    // Populate the view with data
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        final DatabaseHelper db = new DatabaseHelper(holder.itemView.getContext());
        final MyList list = listsList.get(position);
        // Set the title
        holder.title.setText(list.getTitle());
        // Set the number of entries in this list
        holder.entryCount.setText(String.valueOf(db.getEntryCountForList(list.getId())));
    }

    @Override
    public int getItemCount() {
        return listsList.size();
    }

    /**
     * Delete all MyLists from this adapter
     */
    final protected void clear() {
        listsList.clear();
    }

    /**
     * Get all MyLists currently added to this adapter
     * @return The List of MyLists
     */
    final protected List<MyList> getAll() {
        return listsList;
    }

    /**
     * Add a List of MyLists to this adapter
     * @param listsList The MyLists to be added
     */
    final protected void setAll(final List<MyList> listsList) {
        this.listsList = listsList;
    }

    /**
     * Remove one MyList from the adapter
     * @param position The position of the MyList
     */
    final protected void removeList(final int position) {
        listsList.remove(position);
        // Notify the item removed by position to perform recycler view delete animations
        // NOTE: don't call notifyDataSetChanged()
        notifyItemRemoved(position);
    }

    /**
     * Add one MyList at the specified position to this adapter
     * @param list The MyList to be added
     * @param position The position at which the MyList is added
     */
    final protected void restoreList(final MyList list, final int position) {
        listsList.add(position, list);
        // Notify item added by position
        notifyItemInserted(position);
    }
}