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
    public static final int HORIZONTAL = OrientationHelper.HORIZONTAL;
    public static final int VERTICAL = OrientationHelper.VERTICAL;
    public static final int DISTANCE_PER_POSITION = 180;
    private final float INTERACTIVE_SCROLL_SPEED = 0.5f;
    private int mScrollState = RecyclerView.SCROLL_STATE_IDLE;
    private int mPositionBeforeScroll = RecyclerView.NO_POSITION;
    private int mScrollVector;
    private int mCurrentPosition;
    private int mScrollDistance;
    private int mDecoratedChildWidth;
    private int mDecoratedChildHeight;
    private OnPositionChangeListener mPositionChangeListener;

    public FlipLayoutManager(Context context, int orientation) {
        super(context, orientation, false);
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
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

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        return scroll(dy, recycler, state);
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        return scroll(dx, recycler, state);
    }

    private int scroll(int delta, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.getItemCount() == 0) {
            return 0;
        }

        int modifiedDelta = delta;
        if (isInteractiveScroll()) {
            modifiedDelta = (int) (modifiedDelta > 0
                    ? Math.max(modifiedDelta * INTERACTIVE_SCROLL_SPEED, 1)
                    : Math.min(modifiedDelta * INTERACTIVE_SCROLL_SPEED, -1));
        }

        int desiredDistance = mScrollDistance + modifiedDelta;

        int desiredPosition = findPositionByScrollDistance(desiredDistance);
        if (desiredPosition < 0 || desiredPosition >= state.getItemCount()) {
            return 0;
        }

        if (mPositionBeforeScroll == RecyclerView.NO_POSITION) {
            mPositionBeforeScroll = getCurrentPosition();
        }

        if (mScrollVector == 0 && modifiedDelta != 0) {
            mScrollVector = modifiedDelta > 0 ? 1 : -1;
        }

        final int maxOverScrollDistance = 70;
        int minDistance = 0;
        int maxDistance = ((getItemCount() - 1) * DISTANCE_PER_POSITION);

        if (desiredDistance < minDistance - maxOverScrollDistance || desiredDistance > maxDistance + maxOverScrollDistance) {
            return 0;
        }

        if (isInteractiveScroll()) {
            minDistance = (mPositionBeforeScroll - 1) * DISTANCE_PER_POSITION;
            if (mScrollVector > 0) {
                minDistance = mPositionBeforeScroll * DISTANCE_PER_POSITION;
            }

            maxDistance = (mPositionBeforeScroll + 1) * DISTANCE_PER_POSITION;
            if (mScrollVector < 0) {
                maxDistance = mPositionBeforeScroll * DISTANCE_PER_POSITION;
            }

            if (desiredDistance < minDistance || desiredDistance > maxDistance) {
                return 0;
            }
        }

        mScrollDistance = desiredDistance;

        int oldPosition = mCurrentPosition;
        mCurrentPosition = desiredPosition;
        notifyOfPositionChange(oldPosition, mCurrentPosition);

        fill(recycler, state);
        return modifiedDelta;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.isPreLayout()) {
            return;
        }

        if (state.getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            setCurrentPosition(RecyclerView.NO_POSITION, false);
            return;
        }

        if (mCurrentPosition == RecyclerView.NO_POSITION) {
            setCurrentPosition(0, false);
        }

        if (mCurrentPosition >= state.getItemCount()) {
            setCurrentPosition(state.getItemCount() - 1, false);
        }

        View scrap = recycler.getViewForPosition(0);
        addView(scrap);
        measureChildWithMargins(scrap, 0, 0);
        removeAndRecycleView(scrap, recycler);

        mDecoratedChildWidth = getDecoratedMeasuredWidth(scrap);
        mDecoratedChildHeight = getDecoratedMeasuredHeight(scrap);
        fill(recycler, state);
    }

    private void fill(RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachAndScrapAttachedViews(recycler);

        boolean layoutOnlyCurrentPosition = !isScrolling() && !requiresSettling();

        if (!layoutOnlyCurrentPosition) {
            addView(mCurrentPosition - 1, recycler, state);
        }

        addView(mCurrentPosition, recycler, state);

        if (!layoutOnlyCurrentPosition) {
            addView(mCurrentPosition + 1, recycler, state);
        }

        recycler.clear();
    }

    private void addView(int position, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (position == RecyclerView.NO_POSITION) {
            return;
        }

        if (position >= state.getItemCount()) {
            return;
        }

        View view = recycler.getViewForPosition(position);
        addView(view);
        measureChildWithMargins(view, 0, 0);
        layoutDecorated(view, 0, 0, mDecoratedChildWidth, mDecoratedChildHeight);
    }

    public int getAngle() {
        return getAngle(mScrollDistance);
    }

    private int getAngle(int distance) {
        float currentDistance = distance % DISTANCE_PER_POSITION;

        if (currentDistance < 0) {
            currentDistance += DISTANCE_PER_POSITION;
        }

        return Math.round((currentDistance / DISTANCE_PER_POSITION) * DISTANCE_PER_POSITION);
    }

    private int findPositionByScrollDistance(float distance) {
        return Math.round(distance / DISTANCE_PER_POSITION);
    }

    public int getCurrentPosition() {
        if (getItemCount() == 0) {
            return RecyclerView.NO_POSITION;
        }

        return mCurrentPosition;
    }

    void setCurrentPosition(int position, boolean requestLayout) {
        int oldPosition = mCurrentPosition;
        mCurrentPosition = position;

        if (mScrollDistance < 0) {
            mScrollDistance = 0;
        } else {
            mScrollDistance = DISTANCE_PER_POSITION * position;
        }

        if (requestLayout) {
            requestLayout();
        }

        notifyOfPositionChange(oldPosition, mCurrentPosition);
    }

    public int getScrollDistance() {
        return mScrollDistance;
    }

    @Override
    public void onScrollStateChanged(int state) {
        mScrollState = state;

        if (!isScrolling()) {
            mScrollVector = 0;
            mPositionBeforeScroll = RecyclerView.NO_POSITION;
        }
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, final RecyclerView.State state, final int position) {
        final FlipScroller smoothScroller = new FlipScroller(recyclerView.getContext()) {
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

    @Override
    public void scrollToPosition(int position) {
        setCurrentPosition(position, true);
    }

    void setPositionChangeListener(OnPositionChangeListener onPositionChangeListener) {
        mPositionChangeListener = onPositionChangeListener;
    }

    public boolean isScrolling() {
        return getScrollState() != RecyclerView.SCROLL_STATE_IDLE;
    }

    public boolean isInteractiveScroll() {
        return getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING;
    }

    public boolean requiresSettling() {
        return getScrollDistance() % DISTANCE_PER_POSITION != 0;
    }

    int getScrollState() {
        return mScrollState;
    }

    private void notifyOfPositionChange(int oldPosition, int newPosition) {
        if (oldPosition != newPosition && mPositionChangeListener != null) {
            mPositionChangeListener.onPositionChange(this, newPosition);
        }
    }

    interface OnPositionChangeListener {
        void onPositionChange(FlipLayoutManager flipLayoutManager, int position);
    }
}
