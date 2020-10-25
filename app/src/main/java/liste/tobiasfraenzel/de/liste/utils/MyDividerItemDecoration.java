package liste.tobiasfraenzel.de.liste.utils;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Provide dividers for the items in the RecyclerView
public class MyDividerItemDecoration extends RecyclerView.ItemDecoration{

    private static final int[] ATTRS = new int[]{
            android.R.attr.listDivider
    };

    final private Drawable mDivider;
    private int mOrientation;
    final private Context context;
    final private int margin;

    public MyDividerItemDecoration(Context context, int orientation, int margin) {
        this.context = context;
        this.margin = margin;
        final TypedArray a = context.obtainStyledAttributes(ATTRS);
        mDivider = a.getDrawable(0);
        a.recycle();
        setOrientation(orientation);
    }

    /**
     * Sets the orientation of the layout
     * @param orientation The orientation
     */
    private void setOrientation(final int orientation) {
        if (orientation != LinearLayoutManager.HORIZONTAL &&
                orientation != LinearLayoutManager.VERTICAL) {
            throw new IllegalArgumentException("invalid orientation");
        }
        mOrientation = orientation;
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent,
                           @NonNull RecyclerView.State state) {
        if (mOrientation == LinearLayoutManager.VERTICAL) {
            drawVertical(c, parent);
        } else {
            drawHorizontal(c, parent);
        }
    }

    /**
     * Draws the dividers with a vertical orientation
     * @param c The canvas on which the layout is drawn
     * @param parent The parent RecyclerView
     */
    private void drawVertical(final Canvas c, final RecyclerView parent) {
        // All dividers have the same left and right borders, but individual top and bottom
        // Start of the divider
        final int left = parent.getPaddingLeft();
        // End of the divider
        final int right = parent.getWidth() - parent.getPaddingRight();

        for (int i = 0; i < parent.getChildCount(); i++) {
            final View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                    .getLayoutParams();
            // Top of the divider
            final int top = child.getBottom() + params.bottomMargin;
            // Bottom of the divider
            final int bottom = top + mDivider.getIntrinsicHeight();
            mDivider.setBounds(left + dpToPx(margin), top, right - dpToPx(margin), bottom);
            mDivider.draw(c);
        }
    }

    /**
     * Draws the dividers with a horizontal orientation
     * @param c The canvas on which the layout is drawn
     * @param parent The parent RecyclerView
     */
    private void drawHorizontal(final Canvas c, final RecyclerView parent) {
        // All dividers have the same top and bottom borders, but individual start and end
        // Top of the divider
        final int top = parent.getPaddingTop();
        // Bottom of the divider
        final int bottom = parent.getHeight() - parent.getPaddingBottom();

        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                    .getLayoutParams();
            // Start of the divider
            final int left = child.getRight() + params.rightMargin;
            // End of the divider
            final int right = left + mDivider.getIntrinsicHeight();
            mDivider.setBounds(left, top + dpToPx(margin), right, bottom - dpToPx(margin));
            mDivider.draw(c);
        }
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (mOrientation == LinearLayoutManager.VERTICAL) {
            outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
        } else {
            outRect.set(0, 0, mDivider.getIntrinsicWidth(), 0);
        }
    }

    /**
     * Convert dp to px, to get display resolution-sensitive paddings
     * @param dp The value in dp
     * @return The value converted to the display-specific amount of px
     */
    private int dpToPx(final int dp) {
        final Resources r = context.getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }
}