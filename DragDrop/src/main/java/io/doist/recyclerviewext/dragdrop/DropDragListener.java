package io.doist.recyclerviewext.dragdrop;

import android.content.ClipData;
import android.content.ClipDescription;
import android.view.DragEvent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * {@link android.view.View.OnDragListener} for {@link androidx.recyclerview.widget.RecyclerView}
 * that signals when a drag is dropped.
 */
public class DropDragListener implements View.OnDragListener {
    private final Callback callback;
    private final RecyclerView recyclerView;

    public DropDragListener(Callback callback, RecyclerView recyclerView) {
        this.callback = callback;
        this.recyclerView = recyclerView;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                return callback.onDragStarted(event.getClipDescription());
            case DragEvent.ACTION_DROP:
                drop(event.getClipDescription(), event.getClipData(), event.getX(), event.getY());
                break;
            case DragEvent.ACTION_DRAG_ENDED:
                callback.onDragEnded(event.getClipDescription());
                break;
        }
        return true;
    }

    private void drop(ClipDescription description, ClipData data, float x, float y) {
        View view = recyclerView.findChildViewUnder(x, y);
        if (view == null) {
            return;
        }
        RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(view);
        if (holder == null) {
            return;
        }
        int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            callback.onDrop(position, description, data, x, y);
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
         * Signals that the drag was dropped in {@code position}.
         *
         * Only called if {@link #onDragStarted(ClipDescription)} returned {@code true} and the position is valid.
         */
        void onDrop(int position, ClipDescription description, ClipData data, float eventX, float eventY);

        /**
         * Signals that a drag has ended.
         *
         * Only called if {@link #onDragStarted(ClipDescription)} returned {@code true}.
         */
        void onDragEnded(ClipDescription description);
    }
}
