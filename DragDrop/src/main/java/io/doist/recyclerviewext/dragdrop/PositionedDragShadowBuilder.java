package io.doist.recyclerviewext.dragdrop;

import android.graphics.Canvas;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;

/**
 * Extension of {@link View.DragShadowBuilder} that positions the shadow according to the specified position,
 * typically x and y from {@link MotionEvent#ACTION_DOWN}.
 */
public class PositionedDragShadowBuilder extends View.DragShadowBuilder {
    private final int downX;
    private final int downY;

    public PositionedDragShadowBuilder(View view, int downX, int downY) {
        super(view);
        this.downX = downX;
        this.downY = downY;
    }

    @Override
    public void onProvideShadowMetrics(Point outShadowSize, Point outShadowTouchPoint) {
        final View view = getView();
        if (view != null) {
            outShadowSize.set(view.getWidth(), view.getHeight());
            outShadowTouchPoint.set(downX, downY);
        }
    }

    @Override
    public void onDrawShadow(Canvas canvas) {
        super.onDrawShadow(canvas);
        View view = getView();
        if (view != null) {
            // Toggling visibility ensures the shadow is created without animation artifacts, eg. ripple effects.
            view.setVisibility(View.INVISIBLE);
            view.setVisibility(View.VISIBLE);
            super.onDrawShadow(canvas);
        }
    }
}
