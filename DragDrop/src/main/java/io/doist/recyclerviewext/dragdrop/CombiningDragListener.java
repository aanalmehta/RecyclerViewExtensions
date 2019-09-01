package io.doist.recyclerviewext.dragdrop;

import android.view.DragEvent;
import android.view.View;

/**
 * A {@link android.view.View.OnDragListener} that combines multiple {@link android.view.View.OnDragListener}.
 *
 * The semantics of the result of {@link #onDrag(View, DragEvent)} are retained, meaning listeners that return false
 * for {@link DragEvent#ACTION_DRAG_STARTED} won't receive further events.
 */
public class CombiningDragListener implements View.OnDragListener {
    private View.OnDragListener[] onDragListeners;
    private boolean[] dragStartedResults;

    public CombiningDragListener(View.OnDragListener... onDragListeners) {
        this.onDragListeners = onDragListeners;
        dragStartedResults = new boolean[onDragListeners.length];
    }

    @Override
    public boolean onDrag(View view, DragEvent event) {
        boolean handled = false;
        for (int i = 0; i < onDragListeners.length; i++) {
            if (event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                dragStartedResults[i] = onDragListeners[i].onDrag(view, event);
                handled |= dragStartedResults[i];
            } else if (dragStartedResults[i]) {
                handled |= onDragListeners[i].onDrag(view, event);
            }
        }
        return handled;
    }
}
