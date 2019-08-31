package io.doist.recyclerviewext.dragdrop;

import android.content.ClipDescription;
import android.view.DragEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * {@link android.view.View.OnDragListener} for {@link androidx.recyclerview.widget.RecyclerView}
 * that has the specified placeholder track the drag.
 */
public class PlaceholderDragListener implements View.OnDragListener {
    private final Callback callback;
    private final RecyclerView recyclerView;

    private int position = RecyclerView.NO_POSITION;

    public PlaceholderDragListener(@NonNull Callback callback, @NonNull RecyclerView recyclerView) {
        this.callback = callback;
        this.recyclerView = recyclerView;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                position = callback.onDragStarted(event.getClipDescription());
                return position != RecyclerView.NO_POSITION;
            case DragEvent.ACTION_DRAG_LOCATION:
                swap(event.getX(), event.getY());
                break;
            case DragEvent.ACTION_DRAG_ENDED:
                callback.onDragEnded(event.getClipDescription());
                break;
        }
        return true;
    }

    private void swap(float x, float y) {
        View view = recyclerView.findChildViewUnder(x, y);
        if (view == null) {
            return;
        }
        RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(view);
        if (holder == null) {
            return;
        }
        int newPosition = holder.getAdapterPosition();
        if (newPosition != RecyclerView.NO_POSITION) {
            position = callback.onSwap(position, newPosition, x, y);
        }
    }

    public interface Callback {
        /**
         * Signals that a drag has started. The {@link ClipDescription} can be inspected to determine whether the
         * placeholder's position should track the drag.
         *
         * @return an adapter position that should track the drag, or {@link RecyclerView#NO_POSITION} to skip it.
         */
        int onDragStarted(ClipDescription description);

        /**
         * Swap {@code position} with {@code newPosition} in the adapter.
         *
         * Only called if {@link #onDragStarted(ClipDescription)} returned {@code true}.
         *
         * @return the new position, ie. {@code newPosition} if it was swapped, {@code position} if not,
         * or any other position if it was moved somewhere else.
         */
        int onSwap(int position, int newPosition, float eventX, float eventY);

        /**
         * Signals that a drag has ended.
         * Only called if {@link #onDragStarted(ClipDescription)} returned {@code true}.
         */
        void onDragEnded(ClipDescription description);
    }
}
