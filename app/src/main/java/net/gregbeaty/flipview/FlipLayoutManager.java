package net.gregbeaty.flipview;

import android.content.Context;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

public class FlipLayoutManager extends LinearLayoutManager {
    public static final int INVALID_POSITION = -1;
    public static final int HORIZONTAL = OrientationHelper.HORIZONTAL;
    public static final int VERTICAL = OrientationHelper.VERTICAL;
    public static final int DISTANCE_PER_POSITION = 180;
    private final float MANUAL_SCROLL_SPEED = 0.5f;
    private boolean mAllowManualScroll = true;
    private int mScrollState = RecyclerView.SCROLL_STATE_IDLE;
    private int mCurrentPosition;
    private int mScrollDistance;
    private int mDecoratedChildWidth;
    private int mDecoratedChildHeight;
    private int mPositionBeforeScroll;
    private int mScrollVector;

    public FlipLayoutManager(Context context, int orientation) {
        super(context, orientation, false);
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }

    public void allowManualScroll(boolean allowManualScroll) {
        mAllowManualScroll = allowManualScroll;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public boolean canScrollHorizontally() {
        return getOrientation() == HORIZONTAL;
    }

    @Override
    public boolean canScrollVertically() {
        return getOrientation() == VERTICAL;
    }

    /**
     * @return true if manual scrolling is allowed.
     */
    boolean canScroll() {
        if (!isManualScrolling()) {
            return true;
        }

        return (canScrollHorizontally() || canScrollVertically()) && mAllowManualScroll;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        return scroll(dy, recycler, state);
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        return scroll(dx, recycler, state);
    }

    private int scroll(int delta, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }

        if (!canScroll()) {
            return delta;
        }

        int modifiedDelta = delta;
        if (isManualScrolling()) {
            modifiedDelta = (int) (modifiedDelta > 0
                    ? Math.max(modifiedDelta * MANUAL_SCROLL_SPEED, 1)
                    : Math.min(modifiedDelta * MANUAL_SCROLL_SPEED, -1));
        }

        int desiredDistance = mScrollDistance + modifiedDelta;

        int currentPosition = getAdapterPositionFromScrollDistance(desiredDistance);
        if (currentPosition < 0 || currentPosition >= state.getItemCount()) {
            return 0;
        }

        if (mPositionBeforeScroll == INVALID_POSITION) {
            mPositionBeforeScroll = getCurrentPosition();
        }

        if (mScrollVector == 0 && modifiedDelta != 0) {
            mScrollVector = modifiedDelta > 0 ? 1 : -1;
        }

        final int maxOverFlipDistance = 70;
        int minDistance = 0;
        int maxDistance = ((getItemCount() - 1) * DISTANCE_PER_POSITION);

        if (desiredDistance < minDistance - maxOverFlipDistance || desiredDistance > maxDistance + maxOverFlipDistance) {
            return 0;
        }

        if (isManualScrolling()) {
            minDistance = (mPositionBeforeScroll - 1) * DISTANCE_PER_POSITION;
            if (mScrollVector > 0) {
                minDistance = mPositionBeforeScroll * DISTANCE_PER_POSITION;
            }

            maxDistance = (mPositionBeforeScroll + 1) * DISTANCE_PER_POSITION;
            if (mScrollVector < 0) {
                maxDistance = mPositionBeforeScroll * DISTANCE_PER_POSITION;
            }

            if (!allowManualScroll(desiredDistance, mPositionBeforeScroll, minDistance, maxDistance)) {
                return 0;
            }
        }

        mScrollDistance += modifiedDelta;
        fill(recycler, state);
        return delta;
    }

    protected boolean allowManualScroll(int desiredDistance, int positionBeforeScroll, int minDistance, int maxDistance) {
        return desiredDistance >= minDistance && desiredDistance <= maxDistance;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.getItemCount() == 0) {
            return;
        }

        View scrap = recycler.getViewForPosition(0);
        addView(scrap);
        measureChildWithMargins(scrap, 0, 0);

        mDecoratedChildWidth = getDecoratedMeasuredWidth(scrap);
        mDecoratedChildHeight = getDecoratedMeasuredHeight(scrap);
        detachAndScrapView(scrap, recycler);

        detachAndScrapAttachedViews(recycler);
        fill(recycler, state);
    }

    private void fill(RecyclerView.Recycler recycler, RecyclerView.State state) {
        int viewCount = getChildCount();
        SparseArray<View> viewCache = new SparseArray<>(viewCount);

        for (int i = 0; i < viewCount; i++) {
            final View child = getChildAt(i);
            int position = getPosition(child);
            viewCache.put(position, child);
        }

        for (int i = 0; i < viewCache.size(); i++) {
            detachView(viewCache.valueAt(i));
        }

        mCurrentPosition = getAdapterPositionFromScrollDistance(mScrollDistance);

        addView(getPreviousPosition(), viewCache, recycler);
        addView(getCurrentPosition(), viewCache, recycler);
        addView(getNextPosition(), viewCache, recycler);

        for (int i = 0; i < viewCache.size(); i++) {
            final View removingView = viewCache.valueAt(i);
            recycler.recycleView(removingView);
        }
    }

    private void addView(int position, SparseArray<View> viewCache, RecyclerView.Recycler recycler) {
        if (position == INVALID_POSITION) {
            return;
        }

        View view = viewCache.get(position);
        if (view == null) {
            view = recycler.getViewForPosition(position);
            addView(view);
            measureChildWithMargins(view, 0, 0);
            layoutDecorated(view, 0, 0, mDecoratedChildWidth, mDecoratedChildHeight);
        } else {
            attachView(view);
            viewCache.remove(position);
        }
    }

    public int getScrollState() {
        return mScrollState;
    }

    public int getFlipAngle() {
        return getFlipAngle(mScrollDistance);
    }

    protected int getFlipAngle(int distance) {
        float currentDistance = distance % DISTANCE_PER_POSITION;

        if (currentDistance < 0) {
            currentDistance += DISTANCE_PER_POSITION;
        }

        return Math.round((currentDistance / DISTANCE_PER_POSITION) * DISTANCE_PER_POSITION);
    }

    private int getAdapterPositionFromScrollDistance(float distance) {
        return Math.round(distance / DISTANCE_PER_POSITION);
    }

    public int getPreviousPosition() {
        if (getCurrentPosition() - 1 < 0) {
            return INVALID_POSITION;
        }

        return mCurrentPosition - 1;
    }

    public int getCurrentPosition() {
        if (getItemCount() == 0) {
            return INVALID_POSITION;
        }

        return mCurrentPosition;
    }

    public int getNextPosition() {
        if (getCurrentPosition() + 1 >= getItemCount()) {
            return INVALID_POSITION;
        }

        return mCurrentPosition + 1;
    }

    public int getScrollDistance() {
        return mScrollDistance;
    }

    @Override
    public void onScrollStateChanged(int state) {
        mScrollState = state;

        if (!isScrolling()) {
            mScrollVector = 0;
            mPositionBeforeScroll = FlipLayoutManager.INVALID_POSITION;
        }
    }

    public boolean isScrolling() {
        return mScrollState != RecyclerView.SCROLL_STATE_IDLE;
    }

    protected boolean isManualScrolling() {
        return mScrollState == RecyclerView.SCROLL_STATE_DRAGGING;
    }

    boolean requiresSettling() {
        return getScrollDistance() % DISTANCE_PER_POSITION != 0;
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, final RecyclerView.State state, final int position) {
        final FlipSmoothScroller smoothScroller = new FlipSmoothScroller(recyclerView.getContext()) {
            @Nullable
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                if (position < 0) {
                    throw new IllegalArgumentException("position can't be less then 0. position is : " + position);
                }
                if (position >= state.getItemCount()) {
                    throw new IllegalArgumentException("position can't be great then adapter items count. position is : " + position);
                }

                return FlipLayoutManager.this.computeScrollVectorForPosition(targetPosition);
            }
        };

        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }
}
