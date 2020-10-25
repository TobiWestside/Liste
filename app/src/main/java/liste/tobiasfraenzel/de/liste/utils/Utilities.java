package liste.tobiasfraenzel.de.liste.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import liste.tobiasfraenzel.de.liste.MyList;
import liste.tobiasfraenzel.de.liste.R;

import static java.lang.Math.abs;

// Provide utility functions
public class Utilities {

    @SuppressWarnings("SameReturnValue")
    public static String getLogTag() {
        return "NOTEAPP";
    }

    /**
     * On left swipe, draw red background and show recycling bin icon, to indicate to the user
     * that the item is about to be deleted
     * On right swipe, draw green background and show pencil icon, to indicate to the user
     * that the edit dialog will be opened
     * @param context The context to be used
     * @param c The canvas on which the colors are drawn
     * @param viewHolder The ViewHolder describing the view for which the background is drawn
     * @param dX Horizontal offset by which the item is swiped to a side
     */
    public static void drawSwipeBackground(final Context context, final Canvas c,
                                           final RecyclerView.ViewHolder viewHolder, final float dX) {
        // alpha:    0.0f = invisible
        //           1.0f = looks normal
        // alphaInt:    0 = invisible
        //            255 = looks normal
        final float ALPHA_FULL = 1.0f;
        final int ALPHA_INT_FULL = 255;
        // The ViewHolder expects the alpha value in a range of 0.0f - 1.0f
        final float alpha = ALPHA_FULL - abs(dX) / (float) viewHolder.itemView.getWidth();
        // Paint and icon expect the alpha value in a range of 0 - 255, therefore,
        // it is converted to this range. This process has two steps:
        // Step 1: Convert from floats of 0 through 1 to a scale of ints from -255 through 0
        // *2 is to make the background appear twice as fast as the foreground fades
        int alphaInt = (int)(ALPHA_INT_FULL-(ALPHA_INT_FULL * alpha * 2));
        // Step 2: Convert -255 through 0 to 0 through 255, returning 255 for all values >= 0
        alphaInt = alphaInt < 0 ? ALPHA_INT_FULL - alphaInt * (-1) : ALPHA_INT_FULL;

        final View itemView = viewHolder.itemView;
        // Get the recycling bin icon
        final Drawable binIcon = context.getResources().getDrawable(R.drawable.ic_action_delete);
        // Set color and alpha for red background
        final Paint deletePaint = new Paint();
        deletePaint.setColor(context.getResources().getColor(android.R.color.holo_red_light));
        deletePaint.setAlpha(alphaInt);
        // Get the pencil icon
        final Drawable pencilIcon = context.getResources().getDrawable(R.drawable.ic_action_edit_white);
        // Set color and alpha for green background
        final Paint editPaint = new Paint();
        editPaint.setColor((context.getResources().getColor(R.color.colorPrimary)));
        editPaint.setAlpha((alphaInt));

        if (dX > 0) {
            drawBackgroundForRightSwipe(itemView, dX, editPaint, pencilIcon, alphaInt, c);
        } else if (dX < 0) {
            drawBackgroundForLeftSwipe(itemView, dX, deletePaint, binIcon, alphaInt, c);
        }

        // Fade out the list item as it is swiped out
        viewHolder.itemView.setAlpha(alpha);
        viewHolder.itemView.setTranslationX(dX);
    }

    /**
     * Draw a rectangle on the background, in the area where the item is swiped away to indicate
     * the action that is triggered by the swipe
     * @param itemView The item that is swiped
     * @param dX Horizontal offset by which the item is swiped to a side
     * @param editPaint Color indicating the edit action
     * @param pencilIcon Icon indicating the edit action
     * @param alphaInt How much the background is faded (0-255)
     * @param c The canvas on which the colors are drawn
     */
    private static void drawBackgroundForRightSwipe(final View itemView, final float dX,
                                                    final Paint editPaint, final Drawable pencilIcon,
                                                    final int alphaInt, final Canvas c) {
        // Draw rectangle with varying right side, equal to displacement dX
        c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX,
                (float) itemView.getBottom(), editPaint);
        // Set the icon on the left side, for right swipe
        final int left = itemView.getLeft();
        final int top = itemView.getTop() - (itemView.getTop() - itemView.getBottom()) / 4;
        final int bottom = itemView.getBottom() + (itemView.getTop() - itemView.getBottom()) / 4;
        final int right = itemView.getLeft() + (bottom - top);

        pencilIcon.setBounds(left, top , right, bottom);
        pencilIcon.setAlpha(alphaInt);
        pencilIcon.draw(c);
    }

    /**
     * Draw a rectangle on the background, in the area where the item is swiped away to indicate
     * the action that is triggered by the swipe
     * @param itemView The item that is swiped
     * @param dX Horizontal offset by which the item is swiped to a side
     * @param deletePaint Color indicating the delete action
     * @param binIcon Icon indicating the delete action
     * @param alphaInt How much the background is faded (0-255)
     * @param c The canvas on which the colors are drawn
     */
    private static void drawBackgroundForLeftSwipe(final View itemView, final float dX,
                                                   final Paint deletePaint, final Drawable binIcon,
                                                   final int alphaInt, final Canvas c) {
        // Draw rectangle with varying left side, equal to the item's right side
        // plus negative displacement dX
        c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(),
                (float) itemView.getRight(), (float) itemView.getBottom(), deletePaint);
        // Set the icon on the right side, for left swipe
        final int right = itemView.getRight();
        final int top = itemView.getTop() - (itemView.getTop() - itemView.getBottom()) / 4;
        final int bottom = itemView.getBottom() + (itemView.getTop() - itemView.getBottom()) / 4;
        final int left = itemView.getRight() - (bottom - top);

        binIcon.setBounds(left, top , right, bottom);
        binIcon.setAlpha(alphaInt);
        binIcon.draw(c);
    }

    /**
     * Returns true if the displayed items don't belong to a single list
     * and at the same time not the list of all lists is shown.
     * This is only the case if all list entries are shown.
     * @param displayedList The displayed list
     * @param listsList The list of all displayed lists
     * @return True if all entries are shown, false otherwise
     */
    public static boolean allEntriesShown(final MyList displayedList, final List<MyList> listsList) {
        return (displayedList == null && listsList.isEmpty());
    }

    /**
     * Returns true if the list of lists is not shown, which is only the case if
     * list entries are shown
     * @param listsList The list of all displayed lists
     * @return True if the entries of one list are shown, false otherwise
     */
    public static boolean entriesOfListShown(final List<MyList> listsList) {
        return listsList.isEmpty();
    }

    /**
     * Dismiss the snackbar if one is shown
     * @param snackbar The snackbar to be dismissed
     */
    public static void dismissSnackbar(final Snackbar snackbar) {
        if (snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
        }
    }

    /**
     * Get the long date format with the timezone set
     * @return The date format
     */
    public static SimpleDateFormat getFullDateFormat() {
        final SimpleDateFormat sdf = new SimpleDateFormat(Constants.ALARM_DATE_FORMAT, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf;
    }

    /**
     * Get the date format for only the date with the timezone set
     * @return The date format
     */
    public static SimpleDateFormat getDateFormat() {
        final SimpleDateFormat sdf = new SimpleDateFormat(Constants.ONLY_DATE_FORMAT, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf;
    }

    /**
     * Get the date format for only the time with the timezone set
     * @return The date format
     */
    public static SimpleDateFormat getTimeFormat() {
        final SimpleDateFormat sdf = new SimpleDateFormat(Constants.ONLY_TIME_FORMAT, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf;
    }

    /**
     * Get the medium date format with the timezone set
     * @return The date format
     */
    public static java.text.DateFormat getLocalDateFormat() {
        final java.text.DateFormat df = java.text.DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        df.setTimeZone(TimeZone.getDefault());
        return df;
    }

    /**
     * Get the short date format for only the date with the timezone set
     * @return The date format
     */
    public static java.text.DateFormat getLocalDateFormatShort() {
        final java.text.DateFormat df = java.text.DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
        df.setTimeZone(TimeZone.getDefault());
        return df;
    }

    /**
     * Get the short date format for only the time with the timezone set
     * @return The date format
     */
    public static java.text.DateFormat getLocalTimeFormat() {
        final java.text.DateFormat df = java.text.DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
        df.setTimeZone(TimeZone.getDefault());
        return df;
    }

    /**
     * Format the date to the medium date format with the timezone set
     * @param date The date to be formatted
     * @return The formatted date as a String
     */
    public static String convertToLocalDateString(final Date date) {
        return getLocalDateFormat().format(date);
    }

    /**
     * Parse the medium date format date String to produce a Date object
     * @param date The date String
     * @return The Date object
     * @throws ParseException Exception if the date cannot be parsed
     */
    public static Date parseFromLocalDateString(final String date) throws ParseException {
        return getLocalDateFormat().parse(date);
    }

    /**
     * Format the date to the short date format with the timezone set
     * @param date The date to be formatted
     * @return The formatted date as a String
     */
    public static String convertToLocalDateStringShort(final Date date) {
        return getLocalDateFormatShort().format(date);
    }

    /**
     * Format the date with the short date format for only the time
     * @param date The date to be formatted
     * @return The formatted time as a String
     */
    public static String convertToLocalTimeString(final Date date) {
        return getLocalTimeFormat().format(date);
    }

    /**
     * Parse the short date format time to produce a Date object
     * @param date The date String to be formatted
     * @return The formatted date as a String
     */
    public static Date parseFromLocalTimeString(final String date) throws ParseException {
        return getLocalTimeFormat().parse(date);
    }

    /**
     * Set the icon of the floating action button to the "add entry" icon
     * @param context The current context
     * @param fab The floating action button
     */
    public static void setAddListEntryFAB(final Context context, final FloatingActionButton fab) {
        fab.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_add_white_24dp));
    }

    /**
     * Set the icon of the floating action button to the "add list" icon
     * @param context The current context
     * @param fab The floating action button
     */
    public static void setAddListFAB(final Context context, final FloatingActionButton fab) {
        fab.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_add_list));
    }
}