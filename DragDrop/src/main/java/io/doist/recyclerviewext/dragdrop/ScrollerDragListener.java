package io.doist.recyclerviewext.dragdrop;

import android.content.ClipDescription;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.DragEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * {@link android.view.View.OnDragListener} for {@link androidx.recyclerview.widget.RecyclerView}
 * that scrolls when near the edges.
 *
 * This class provides no way of handling {@link DragEvent#ACTION_DROP} and other events.
 * Use {@link DropDragListener} or create your own {@link View.OnDragListener},
 * and combine it using {@link CombiningDragListener} to do so.
 */
public class ScrollerDragListener implements View.OnDragListener, Runnable {
    /**
     * Distance from the edge in dips that triggers scrolling at maximum velocity.
     */
    private static final float SCROLL_MAX_SLOP = 24;

    /**
     * Distance from  in dips that triggers scrolling, gradually accelerating it.
     */
    private static final float SCROLL_SLOP = 48;

    /**
     * Max scroll speed in dips per second.
     */
    private static final float SCROLL_SPEED_MAX = 1000;

    private static final Interpolator SCROLL_INTERPOLATOR = new AccelerateInterpolator();

    private final Callback callback;
    private final RecyclerView recyclerView;
    private final LinearLayoutManager layoutManager;

    private final float scrollSlop;
    private final float scrollMaxSlop;

    /**
     * Maximum scroll speed. Initialized lazily as it depends on the display's refresh rate,
     * which is not available until the view is attached.
     */
    private Float scrollSpeedMax = null;

    /**
     * Whether scrolling is enabled. When disabled while scrolling, it is slowed down before disengaging.
     */
    private boolean started;

    /**
     * X position of the event that is triggering the scroll, to calculate maximum speed.
     */
    private float x;

    /**
     * Y position of the event that is triggering the scroll, to calculate maximum speed.
     */
    private float y;

    /**
     * Current scroll speed.
     */
    private float scrollSpeed;

    public ScrollerDragListener(@NonNull Callback callback, @NonNull RecyclerView recyclerView) {
        this.callback = callback;
        this.recyclerView = recyclerView;
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (!(layoutManager instanceof LinearLayoutManager)) {
            throw new IllegalStateException("ScrollerDragListener requires LinearLayoutManager or a subclass");
        }
        this.layoutManager = (LinearLayoutManager) layoutManager;

        DisplayMetrics metrics = recyclerView.getResources().getDisplayMetrics();

        scrollSlop = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, SCROLL_SLOP, metrics);
        scrollMaxSlop = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, SCROLL_MAX_SLOP, metrics);
    }

    @Override
    public boolean onDrag(View view, DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                return callback.onDragStarted(event.getClipDescription());
            case DragEvent.ACTION_DRAG_LOCATION:
                scroll(event.getX(), event.getY());
                break;
            case DragEvent.ACTION_DRAG_EXITED:
                stop();
                break;
            case DragEvent.ACTION_DRAG_ENDED:
                stop();
                callback.onDragEnded(event.getClipDescription());
                break;
        }
        return true;
    }

    /**
     * Scrolls the {@link RecyclerView} when {@code x} and/or {@code y} are near the edges, with increasing speed.
     */
    private void scroll(float x, float y) {
        started = true;
        this.x = x;
        this.y = y;
        recyclerView.removeCallbacks(this);
        recyclerView.postOnAnimation(this);
    }

    /**
     * Stops scrolling, by decelerating.
     */
    private void stop() {
        started = false;
    }

    @Override
    public void run() {
        int scrollByX = 0;
        int scrollByY = 0;
        int direction = 0;
        float fraction = 0;

        if (layoutManager.getOrientation() == LinearLayoutManager.VERTICAL) {
            if (y < scrollMaxSlop + scrollSlop) {
                direction = -1;
                fraction = Math.min(1, 1 - (y - scrollMaxSlop) / scrollSlop);
            } else if (y > recyclerView.getHeight() - scrollMaxSlop - scrollSlop) {
                direction = 1;
                fraction = Math.min(1, 1 - (recyclerView.getHeight() - y - scrollMaxSlop) / scrollSlop);
            }
            if (started && canScroll(LinearLayoutManager.VERTICAL, direction)) {
                updateScrollSpeed(fraction);
            } else if (scrollSpeed > 0) {
                updateScrollSpeed(0);
            }
            scrollByY = (int) scrollSpeed;
        } else if (layoutManager.getOrientation() == LinearLayoutManager.HORIZONTAL) {
            if (x < scrollMaxSlop + scrollSlop) {
                direction = -1;
                fraction = Math.min(1, 1 - (x - scrollMaxSlop) / scrollSlop);
            } else if (y > recyclerView.getWidth() - scrollMaxSlop - scrollSlop) {
                direction = 1;
                fraction = Math.min(1, 1 - (recyclerView.getWidth() - x - scrollMaxSlop) / scrollSlop);
            }
            if (started && canScroll(LinearLayoutManager.HORIZONTAL, direction)) {
                updateScrollSpeed(fraction);
            } else if (scrollSpeed > 0) {
                updateScrollSpeed(0);
            }
            scrollByX = (int) scrollSpeed;
        }

        recyclerView.removeCallbacks(this);
        if (scrollByX > 0 || scrollByY > 0) {
            recyclerView.scrollBy(scrollByX * direction, scrollByY * direction);
            recyclerView.postOnAnimation(this);
        }
    }

    /**
     * Faster alternative to {@link RecyclerView#canScrollVertically(int)} and
     * {@link RecyclerView#canScrollHorizontally(int)} (depending on the LayoutManager direction)
     * that doesn't compute scroll offsets, ranges or extents, and just relies on the child views.
     */
    private boolean canScroll(@RecyclerView.Orientation int orientation, int direction) {
        int childCount = recyclerView.getChildCount();
        if (childCount == 0) {
            return false;
        }
        if (orientation == LinearLayoutManager.VERTICAL && !layoutManager.canScrollVertically()
                || orientation == LinearLayoutManager.HORIZONTAL && !layoutManager.canScrollHorizontally()) {
            return false;
        }
        if (direction < 0) {
            return layoutManager.findFirstCompletelyVisibleItemPosition() != 0;
        } else if (direction > 0) {
            //noinspection ConstantConditions
            return layoutManager.findLastCompletelyVisibleItemPosition()
                    != recyclerView.getAdapter().getItemCount() - 1;
        } else {
            return false;
        }
    }

    /**
     * Updates the scroll speed for an accelerating motion, up to {@code scrollSpeedMax}.
     *
     * @param fraction How much of the calculated scroll speed should be applied, depending on how close to the
     *                 boundary it is. [0..1].
     */
    private void updateScrollSpeed(float fraction) {
        if (fraction > 0) {
            initScrollSpeedMax();
            scrollSpeed = scrollSpeed * 1.06f;
            scrollSpeed = Math.min(scrollSpeed, scrollSpeedMax * SCROLL_INTERPOLATOR.getInterpolation(fraction));
            scrollSpeed = Math.max(scrollSpeed, 1);
        } else {
            scrollSpeed = scrollSpeed / 1.18f;
            scrollSpeed = scrollSpeed < 0 ? 0 : scrollSpeed;
        }
    }

    private void initScrollSpeedMax() {
        if (scrollSpeedMax == null) {
            float refreshRate = 60;
            Display display;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                    && (display = recyclerView.getDisplay()) != null) {
                refreshRate = display.getRefreshRate();
            }
            scrollSpeedMax = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, SCROLL_SPEED_MAX / refreshRate,
                    recyclerView.getResources().getDisplayMetrics());
        }
    }

    public interface Callback {
        /**
         * Signals that a drag has started. The {@link ClipDescription} can be inspected to determine whether the
         * {@link RecyclerView} should scroll automatically when near the edges, or not.
         *
         * @return true to scroll the {@link RecyclerView} automatically, false otherwise
         */
        boolean onDragStarted(ClipDescription description);

        /**
         * Signals that a drag has ended.
         *
         * Only called if {@link #onDragStarted(ClipDescription)} returned {@code true}.
         */
        void onDragEnded(ClipDescription description);
    }
}
