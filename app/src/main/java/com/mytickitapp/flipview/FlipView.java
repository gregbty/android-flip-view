package net.gregbeaty.flipview;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class FlipView extends FrameLayout {
    public static final int SCROLL_STATE_DRAGGING = RecyclerView.SCROLL_STATE_DRAGGING;
    public static final int SCROLL_STATE_SETTLING = RecyclerView.SCROLL_STATE_SETTLING;
    public static final int SCROLL_STATE_IDLE = RecyclerView.SCROLL_STATE_IDLE;
    public static final int DISTANCE_PER_POSITION = FlipLayoutManager.DISTANCE_PER_POSITION;
    public static final int HORIZONTAL = FlipLayoutManager.HORIZONTAL;
    public static final int VERTICAL = FlipLayoutManager.VERTICAL;
    public static final int NO_POSITION = RecyclerView.NO_POSITION;

    private RecyclerView recyclerView;
    private final List<OnPositionChangeListener> onPositionChangeListeners = new ArrayList<>();
    private final List<OnScrollListener> onScrollListeners = new ArrayList<>();
    private FlipLayoutManager layoutManager;

    public FlipView(@NonNull Context context) {
        this(context, null);
    }

    public FlipView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public FlipView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.FlipView, defStyleAttr, 0);
        int orientation = a.getInteger(R.styleable.FlipView_android_orientation, VERTICAL);
        a.recycle();

        LayoutInflater inflate = LayoutInflater.from(context);
        View view = inflate.inflate(R.layout.flip_view, this, true);
        recyclerView = view.findViewById(R.id.flip_view_recycler_view);

        recyclerView.setHasFixedSize(true);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                FlipView.this.onScrollStateChanged(newState);

                for (OnScrollListener listener : onScrollListeners) {
                    listener.onScrollStateChanged(FlipView.this, newState);
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                FlipView.this.onScrolled(dx, dy);

                for (OnScrollListener listener : onScrollListeners) {
                    listener.onScrolled(FlipView.this, dx, dy);
                }
            }
        });

        layoutManager = new FlipLayoutManager(recyclerView, orientation);
        layoutManager.setOnPositionListener(new FlipLayoutManager.OnPositionChangeListener() {
            @Override
            public void onPositionChange(FlipLayoutManager flipLayoutManager, int position) {
                for (OnPositionChangeListener listener : onPositionChangeListeners) {
                    listener.onPositionChange(FlipView.this, position);
                }
            }
        });

        recyclerView.setLayoutManager(layoutManager);
    }

    public RecyclerView.Adapter getAdapter() {
        return recyclerView.getAdapter();
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        recyclerView.setAdapter(adapter);
    }

    public int getScrollState() {
        return recyclerView.getScrollState();
    }

    public int getAngle() {
        return layoutManager.getAngle();
    }

    public int getScrollDistance() {
        return layoutManager.getScrollDistance();
    }

    public int getPosition() {
        return layoutManager.getCurrentPosition();
    }

    public void addOnPositionChangeListener(OnPositionChangeListener listener) {
        if (listener != null) {
            onPositionChangeListeners.add(listener);
        }
    }

    public void removeOnPositionChangeListener(OnPositionChangeListener listener) {
        if (listener != null) {
            onPositionChangeListeners.remove(listener);
        }
    }

    public void clearOnPositionChangeListeners() {
        onPositionChangeListeners.clear();
    }

    public void addOnScrollListener(OnScrollListener listener) {
        if (listener != null) {
            onScrollListeners.add(listener);
        }
    }

    public void removeOnScrollListener(OnScrollListener listener) {
        if (listener != null) {
            onScrollListeners.remove(listener);
        }
    }

    public void clearOnScrollListener() {
        onScrollListeners.clear();
    }

    protected void onScrollStateChanged(int newState) {
    }

    protected void onScrolled(int dx, int dy) {
    }

    public void smoothScrollToPosition(int position) {
        recyclerView.smoothScrollToPosition(position);
    }

    public void smoothScrollBy(int scrollX, int scrollY) {
        recyclerView.smoothScrollBy(scrollX, scrollY);
    }

    @Override
    public void scrollBy(int x, int y) {
        recyclerView.scrollBy(x, y);
    }

    @Override
    public void scrollTo(int x, int y) {
        recyclerView.scrollTo(x, y);
    }

    public void scrollToPosition(int position) {
        recyclerView.scrollToPosition(position);
    }

    public interface OnScrollListener {
        void onScrolled(FlipView flipView, int dx, int dy);

        void onScrollStateChanged(FlipView flipView, int newState);
    }

    public interface OnPositionChangeListener {
        void onPositionChange(FlipView flipView, int position);
    }
}
