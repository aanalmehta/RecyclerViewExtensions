package io.doist.recyclerviewext.demo;

import android.annotation.SuppressLint;
import android.content.ClipDescription;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.doist.recyclerviewext.R;
import io.doist.recyclerviewext.animations.AnimatedAdapter;
import io.doist.recyclerviewext.choice_modes.Selector;
import io.doist.recyclerviewext.dragdrop.PlaceholderDragListener;
import io.doist.recyclerviewext.dragdrop.PositionedDragShadowBuilder;
import io.doist.recyclerviewext.sticky_headers.StickyHeaders;

public class DemoAdapter extends AnimatedAdapter<BindableViewHolder>
        implements StickyHeaders, PlaceholderDragListener.Callback {
    @RecyclerView.Orientation
    private final int mOrientation;

    private Selector mSelector;

    private List<Object> mDataset;

    private int mDraggedPosition;

    DemoAdapter(int orientation) {
        super();
        mOrientation = orientation;
    }

    void setDataset(List<Object> dataset) {
        mDataset = new ArrayList<>(dataset);
        animateDataSetChanged();
    }

    void setSelector(Selector selector) {
        mSelector = selector;
    }

    @NonNull
    @Override
    public BindableViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == 0) {
            return new DemoItemViewHolder(inflater.inflate(
                    mOrientation == LinearLayoutManager.VERTICAL ?
                    R.layout.item : R.layout.item_horizontal, parent, false));
        } else {
            return new DemoSectionViewHolder(inflater.inflate(
                    mOrientation == LinearLayoutManager.VERTICAL ?
                    R.layout.section : R.layout.section_horizontal, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull BindableViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.contains(Selector.PAYLOAD_SELECT)) {
            if (mSelector != null) {
                mSelector.bind(holder, false);
            }
        } else if (payloads.isEmpty()) {
            if (mSelector != null) {
                mSelector.bind(holder, true);
            }
            holder.bind(mDataset.get(position));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull BindableViewHolder holder, int position) {
        throw new RuntimeException(
                "Use onBindViewHolder(ProjectViewHolder, int, List<Object>) instead");
    }

    @Override
    public long getItemId(int position) {
        return mDataset.get(position).hashCode();
    }

    @Override
    public long getItemContentHash(int position) {
        return 0L;
    }

    @Override
    public int getItemCount() {
        return mDataset != null ? mDataset.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return mDataset.get(position) instanceof String ? 0 : 1;
    }

    @Override
    public boolean isStickyHeader(int position) {
        return mDataset.get(position) instanceof Integer;
    }

    @Override
    public int onDragStarted(ClipDescription description, Object localState) {
        return mDraggedPosition;
    }

    @Override
    public int onSwap(int position, int newPosition, float eventX, float eventY) {
        mDataset.add(newPosition, mDataset.remove(position));
        notifyItemMoved(position, newPosition);
        mDraggedPosition = newPosition;
        return mDraggedPosition;
    }

    @Override
    public void onDragEnded(ClipDescription description, Object localState) {
        mDraggedPosition = RecyclerView.NO_POSITION;
    }

    public class DemoItemViewHolder extends BindableViewHolder
            implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener {
        final TextView textView1;
        final TextView textView2;

        private int downX;
        private int downY;

        DemoItemViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            itemView.setOnTouchListener(this);

            this.textView1 = itemView.findViewById(android.R.id.text1);
            this.textView2 = itemView.findViewById(android.R.id.text2);
        }

        @Override
        public void bind(Object object) {
            textView1.setText(String.valueOf(object));
        }

        @Override
        public void onClick(View v) {
            if (mSelector != null) {
                mSelector.toggleSelected(getItemId());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            mDraggedPosition = getAdapterPosition();
            Drawable background = v.getBackground();
            v.setBackgroundColor(Color.WHITE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                v.startDragAndDrop(
                        null, new PositionedDragShadowBuilder(v, downX, downY), null, View.DRAG_FLAG_OPAQUE);
            } else {
                v.startDrag(null, new PositionedDragShadowBuilder(v, downX, downY), null, 0);
            }
            v.setBackground(background);
            v.setVisibility(View.INVISIBLE);
            return true;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downX = (int) event.getX();
                downY = (int) event.getY();
            }
            return false;
        }
    }

    public class DemoSectionViewHolder extends BindableViewHolder implements View.OnClickListener {
        final TextView textView;

        DemoSectionViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);
            textView = itemView.findViewById(android.R.id.text1);
        }

        @Override
        public void bind(Object object) {
            textView.setText(String.valueOf(object));
        }

        @Override
        public void onClick(View itemView) {
            textView.animate().rotationBy(360).setDuration(500).start();
        }
    }
}
