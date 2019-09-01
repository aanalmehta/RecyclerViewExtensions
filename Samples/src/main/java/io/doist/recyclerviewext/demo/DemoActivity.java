package io.doist.recyclerviewext.demo;

import android.annotation.SuppressLint;
import android.content.ClipDescription;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.doist.recyclerviewext.R;
import io.doist.recyclerviewext.animations.WithLayerItemAnimator;
import io.doist.recyclerviewext.choice_modes.MultiSelector;
import io.doist.recyclerviewext.choice_modes.Selector;
import io.doist.recyclerviewext.choice_modes.SingleSelector;
import io.doist.recyclerviewext.dividers.DividerItemDecoration;
import io.doist.recyclerviewext.dragdrop.CombiningDragListener;
import io.doist.recyclerviewext.dragdrop.PlaceholderDragListener;
import io.doist.recyclerviewext.dragdrop.PositionedDragShadowBuilder;
import io.doist.recyclerviewext.dragdrop.ScrollerDragListener;
import io.doist.recyclerviewext.dragdrop.VisibilityDragListener;
import io.doist.recyclerviewext.flippers.ProgressEmptyRecyclerFlipper;
import io.doist.recyclerviewext.pinch_zoom.PinchZoomItemTouchListener;
import io.doist.recyclerviewext.sticky_headers.StickyHeadersLinearLayoutManager;

public class DemoActivity extends AppCompatActivity
        implements PinchZoomItemTouchListener.PinchZoomListener {
    private ViewGroup mContainer;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private DemoAdapter mAdapter;
    private ProgressEmptyRecyclerFlipper mProgressEmptyRecyclerFlipper;

    private Selector mSelector;

    private int mDataCount = 0;
    private int mSelectorCount = 0;
    private int mLayoutCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        mContainer = findViewById(R.id.container);
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new WithLayerItemAnimator(true));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, R.drawable.divider_light, true));
        mRecyclerView.addOnItemTouchListener(new PinchZoomItemTouchListener(this, this));

        mProgressEmptyRecyclerFlipper = new ProgressEmptyRecyclerFlipper(
                mContainer, R.id.recycler_view, R.id.empty, R.id.loading);

        setLayout(mLayoutCount);

        final View fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setNextAdapterItems();
            }
        });
        fab.setOnDragListener(new VisibilityDragListener(fab));
        fab.setOnTouchListener(new View.OnTouchListener() {
            int downX;
            int downY;
            boolean dragging;
            int touchSlop = ViewConfiguration.get(fab.getContext()).getScaledTouchSlop();

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = (int) event.getX();
                        downY = (int) event.getY();
                        dragging = false;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (!dragging && Math.max(
                                Math.abs(event.getRawX() - downX), Math.abs(event.getRawY() - downY)) > touchSlop) {
                            dragging = true;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                v.startDragAndDrop(
                                        null, new PositionedDragShadowBuilder(fab, downX, downY), null,
                                        View.DRAG_FLAG_OPAQUE);
                            } else {
                                v.startDrag(null, new PositionedDragShadowBuilder(fab, downX, downY), null, 0);
                            }
                        }
                }
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_demo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_load:
                mProgressEmptyRecyclerFlipper.setLoading(true);
                mContainer.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mProgressEmptyRecyclerFlipper.setLoading(false);
                    }
                }, 2000);
                return true;

            case R.id.action_selector:
                setNextSelector();
                return true;

            case R.id.action_layout:
                setLayout(++mLayoutCount);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPinchZoom(int position) {
        Toast.makeText(this, "Pinched " + position, Toast.LENGTH_SHORT).show();
    }

    private static final Object[][] ITEMS = {
            {1, "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", 2, 3, "k", "l", 4, "m", "n", "o", "p", "q", "r", "s",
             "t", "u", "v", "w", "x", "y", "z", 5},
            {"e", "b", "f", "a", "c", "d"},
            {"f", "g", "h", "e"},
            {1, "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", 2, 3, "k", "l", 4, "m", "n", "o", "p", "q", "r", "s",
             "t", "u", "v", "w", "x", "y", "z"},
            {2, "c", "f", "b"},
            {"c", "e", 2, "b"},
            {1, "a", 2, 3, "c", "b", "d", 4, "f", "e", "h", "j", "i", "g"},
            {"d", "c", "b", "a", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o"},
            {}
    };

    public void setNextAdapterItems() {
        mDataCount++;
        mAdapter.setDataset(getAdapterItems());
    }

    public List<Object> getAdapterItems() {
        return Arrays.asList(ITEMS[mDataCount % ITEMS.length]);
    }

    public void setNextSelector() {
        mSelector = getSelector();
        mAdapter.setSelector(mSelector);
        mAdapter.notifyDataSetChanged();
        mSelectorCount++;
    }

    public Selector getSelector() {
        if (mSelectorCount % 2 == 0) {
            return new SingleSelector(mRecyclerView, mAdapter);
        } else {
            return new MultiSelector(mRecyclerView, mAdapter);
        }
    }

    public void setLayout(int layout) {
        @RecyclerView.Orientation int orientation;
        boolean reverse;
        switch (layout % 4) {
            case 0:
                orientation = LinearLayoutManager.VERTICAL;
                reverse = false;
                break;
            case 1:
                orientation = LinearLayoutManager.VERTICAL;
                reverse = true;
                break;
            case 3:
                orientation = LinearLayoutManager.HORIZONTAL;
                reverse = true;
                break;
            default:
                orientation = LinearLayoutManager.HORIZONTAL;
                reverse = false;
                break;
        }
        mAdapter = new DemoAdapter(orientation);
        mAdapter.setDataset(getAdapterItems());
        if (mSelector != null) {
            mAdapter.setSelector(mSelector);
        }
        mLayoutManager = new StickyHeadersLinearLayoutManager(this, orientation, reverse);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mProgressEmptyRecyclerFlipper.monitor(mAdapter);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setOnDragListener(new CombiningDragListener(
                new ScrollerDragListener(
                        new ScrollerDragListener.Callback() {
                            @Override
                            public boolean onDragStarted(ClipDescription description) {
                                return true;
                            }

                            @Override
                            public void onDragEnded(ClipDescription description) {

                            }
                        }, mRecyclerView),
                new PlaceholderDragListener(mAdapter, mRecyclerView)));
    }
}
