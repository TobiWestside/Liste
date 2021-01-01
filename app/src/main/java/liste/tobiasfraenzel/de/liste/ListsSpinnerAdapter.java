package liste.tobiasfraenzel.de.liste;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import liste.tobiasfraenzel.de.liste.utils.Utilities;

public class ListsSpinnerAdapter extends ArrayAdapter<MyList> {

    final private Context context;
    // The List of lists that are added to the adapter
    final private List<MyList> lists;

    public ListsSpinnerAdapter(final Context context, final int textViewResourceId,
                               final List<MyList> lists) {
        super(context, textViewResourceId, lists);
        this.context = context;
        // Additional list that is added to provide a "add new list" functionality from the Spinner
        final MyList addNewList = new MyList();
        addNewList.setTitle(context.getString(R.string.add_new_list));
        lists.add(addNewList);
        this.lists = lists;
    }

    @Override
    public int getCount() {
        return lists.size();
    }

    @Override
    public MyList getItem(int position) {
        return lists.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // Configure the view for the "passive" state of the spinner
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        TextView label = (TextView) super.getView(position, convertView, parent);
        label.setTextColor(Color.BLACK);
        label.setText(lists.get(position).getTitle());

        return label;
    }

    // Configure the view for when the "chooser" is popped up
    @Override
    public View getDropDownView(int position, View convertView,
                                @NonNull ViewGroup parent) {
        TextView label = (TextView) super.getDropDownView(position, convertView, parent);
        label.setBackground(context.getDrawable(R.drawable.dropdown_divider));
        label.setTextColor(Color.BLACK);
        label.setText(lists.get(position).getTitle());

        return label;
    }

    /**
     * Delete all MyLists from this adapter
     */
    final public void clear() {
        lists.clear();
    }

    /**
     * Add a List of MyLists to this adapter
     * @param lists The lists to be added
     */
    final public void addAll(final List<MyList> lists) {
        Log.d(Utilities.getLogTag(), "addAll");
        final MyList addNewList = new MyList();
        addNewList.setTitle(context.getString(R.string.add_new_list));
        lists.add(addNewList);
        this.lists.addAll(lists);
    }
}