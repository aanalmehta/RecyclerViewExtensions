package io.doist.recyclerviewext.dragdrop;

import android.view.DragEvent;
import android.view.View;

import java.lang.ref.WeakReference;

public class VisibilityDragListener implements View.OnDragListener {
    private WeakReference<View> viewRef;
    private float x;
    private float y;

    public VisibilityDragListener(View view) {
        this.viewRef = new WeakReference<>(view);
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                View view = viewRef.get();
                if (view != null) {
                    view.setAlpha(0.5f);
                }
                return true;
            case DragEvent.ACTION_DROP:
            case DragEvent.ACTION_DRAG_ENDED:
                view = viewRef.get();
                if (view != null) {
                    view.animate().alpha(1f);
                }
                return true;
            default:
                return true;
        }
    }
}
