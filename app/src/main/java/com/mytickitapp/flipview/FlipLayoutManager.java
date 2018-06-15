package net.gregbeaty.flipview;

import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import timber.log.Timber;

public class FlipLayoutManager extends RecyclerView.LayoutManager {
    static final int HORIZONTAL = OrientationHelper.HORIZONTAL;
    static final int VERTICAL = OrientationHelper.VERTICAL;
    static final int DISTANCE_PER_POSITION = 180;
    private final float INTERACTIVE_SCROLL_SPEED = 0.5f;
    private final int orientation;
    private final RecyclerView recyclerView;
    private Integer decoratedChildWidth;
    private Integer decoratedChildHeight;
    private boolean positionChangedForLayout;
    private int positionBeforeScroll = RecyclerView.NO_POSITION;
    private int scrollVector;
    private int scrollDistance;
    private OnPositionChangeListener onPositionChangeListener;

    FlipLayoutManager(final RecyclerView recyclerView, int orientation) {
        Timber.tag(getClass().getSimpleName());

        this.recyclerView = recyclerView;
        this.orientation = orientation;
    }

    public int getOrientation() {
        return orientation;
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
        return scrollBy(dy, recycler, state);
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        return scrollBy(dx, recycler, state);
    }

    private int scrollBy(int delta, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (decoratedChildWidth == null || decoratedChildHeight == null) {
            return 0;
        }

        if (getChildCount() == 0) {
            return 0;
        }

        int modifiedDelta = delta;
        if (isInteractiveScroll()) {
            modifiedDelta = (int) (modifiedDelta > 0
                    ? Math.max(modifiedDelta * INTERACTIVE_SCROLL_SPEED, 1)
                    : Math.min(modifiedDelta * INTERACTIVE_SCROLL_SPEED, -1));
        }

        int desiredDistance = scrollDistance + modifiedDelta;

        int desiredPosition = getPositionByScrollDistance(desiredDistance);
        if (desiredPosition < 0 || desiredPosition >= state.getItemCount()) {
            return 0;
        }

        if (positionBeforeScroll == RecyclerView.NO_POSITION) {
            positionBeforeScroll = getCurrentPosition();
        }

        if (scrollVector == 0 && modifiedDelta != 0) {
            scrollVector = modifiedDelta > 0 ? 1 : -1;
        }

        final int maxOverScrollDistance = 70;
        int minDistance = 0;
        int maxDistance = ((state.getItemCount() - 1) * DISTANCE_PER_POSITION);

        if (desiredDistance < minDistance - maxOverScrollDistance || desiredDistance > maxDistance + maxOverScrollDistance) {
            return 0;
        }

        if (isInteractiveScroll()) {
            minDistance = (positionBeforeScroll - 1) * DISTANCE_PER_POSITION;
            if (scrollVector > 0) {
                minDistance = positionBeforeScroll * DISTANCE_PER_POSITION;
            }

            maxDistance = (positionBeforeScroll + 1) * DISTANCE_PER_POSITION;
            if (scrollVector < 0) {
                maxDistance = positionBeforeScroll * DISTANCE_PER_POSITION;
            }

            if (desiredDistance < minDistance || desiredDistance > maxDistance) {
                return 0;
            }
        }

        int oldPosition = getCurrentPosition();
        scrollDistance = desiredDistance;

        if (oldPosition != desiredPosition) {
            notifyPositionChange(desiredPosition);
        }

        fill(recycler, state);
        return modifiedDelta;
    }

    private int getPositionByScrollDistance(float distance) {
        return Math.round(distance / DISTANCE_PER_POSITION);
    }

    public int getCurrentPosition() {
        return getPositionByScrollDistance(getScrollDistance());
    }

    public int getScrollDistance() {
        return scrollDistance;
    }

    public int getAngle() {
        return getAngle(getScrollDistance());
    }

    private int getAngle(int distance) {
        float currentDistance = distance % DISTANCE_PER_POSITION;

        if (currentDistance < 0) {
            currentDistance += DISTANCE_PER_POSITION;
        }

        return Math.round((currentDistance / DISTANCE_PER_POSITION) * DISTANCE_PER_POSITION);
    }

    public void setOnPositionListener(OnPositionChangeListener listener) {
        onPositionChangeListener = listener;
    }

    public boolean isScrolling() {
        return recyclerView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE;
    }

    public boolean isInteractiveScroll() {
        return recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING;
    }

    public boolean requiresSettling() {
        return getScrollDistance() % FlipLayoutManager.DISTANCE_PER_POSITION != 0;
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        super.onAdapterChanged(oldAdapter, newAdapter);

        removeAllViews();
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        super.onItemsRemoved(recyclerView, positionStart, itemCount);

        if (positionStart + itemCount <= getCurrentPosition()) {
            Timber.d("onItemsRemoved");

            scrollDistance = (getCurrentPosition() -  itemCount) * DISTANCE_PER_POSITION;
            notifyPositionChange(getCurrentPosition());
        }
    }

    @Override
    public void onMeasure(final RecyclerView.Recycler recycler, final RecyclerView.State state, final int widthSpec, final int heightSpec) {
        Timber.d("onMeasure");

        decoratedChildWidth = null;
        decoratedChildHeight = null;

        super.onMeasure(recycler, state, widthSpec, heightSpec);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.isPreLayout()) {
            Timber.d("onLayoutChildren: pre layout");
            return;
        }

        if (state.getItemCount() == 0) {
            Timber.d("onLayoutChildren: no items");
            removeAndRecycleAllViews(recycler);
            scrollDistance = 0;
            notifyPositionChange(-1);
            return;
        }

        if (decoratedChildWidth == null || decoratedChildHeight == null) {
            Timber.d("onLayoutChildren: measuring");
            View view = recycler.getViewForPosition(0);
            addView(view);
            measureChildWithMargins(view, 0, 0);
            decoratedChildWidth = getDecoratedMeasuredWidth(view);
            decoratedChildHeight = getDecoratedMeasuredHeight(view);
            removeAndRecycleView(view, recycler);
        }

        if (getCurrentPosition() < 0) {
            scrollDistance = 0;
            positionChangedForLayout = true;
        } else if (getCurrentPosition() >= state.getItemCount()) {
            scrollDistance = (state.getItemCount() - 1) * DISTANCE_PER_POSITION;
            positionChangedForLayout = true;
        }


        fill(recycler, state);
        Timber.d("onLayoutChildren: added %s views", getItemCount());

        if (positionChangedForLayout) {
            Timber.d("onLayoutChildren: notify position changed to %s", getCurrentPosition());
            positionChangedForLayout = false;
            notifyPositionChange(getCurrentPosition());
        }
    }

    private void fill(RecyclerView.Recycler recycler, RecyclerView.State state) {
        removeAndRecycleAllViews(recycler);

        boolean layoutOnlyCurrentPosition = !isScrolling() && !requiresSettling() && !state.hasTargetScrollPosition();

        if (!layoutOnlyCurrentPosition && getCurrentPosition() - 1 >= 0) {
            addView(getCurrentPosition() - 1, recycler, state);
        }


        if (!layoutOnlyCurrentPosition && getCurrentPosition() + 1 <= state.getItemCount() - 1) {
            addView(getCurrentPosition() + 1, recycler, state);
        }

        addView(getCurrentPosition(), recycler, state);
    }

    private void addView(int position, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (position < 0 || position >= state.getItemCount()) {
            return;
        }

        View view = recycler.getViewForPosition(position);
        addView(view);
        measureChildWithMargins(view, 0, 0);
        layoutDecorated(view, 0, 0, decoratedChildWidth, decoratedChildHeight);
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);

        if (!isScrolling()) {
            scrollVector = 0;
            positionBeforeScroll = RecyclerView.NO_POSITION;
        }

        if (state != RecyclerView.SCROLL_STATE_IDLE || !requiresSettling()) {
            return;
        }

        smoothScrollToPosition(recyclerView, getCurrentPosition());
    }

    public void smoothScrollToPosition(RecyclerView recyclerView, final RecyclerView.State state, final int position) {
        if (state.isPreLayout()) {
            return;
        }

        smoothScrollToPosition(recyclerView, position);
    }

    private void smoothScrollToPosition(RecyclerView recyclerView, final int position) {
        final FlipSmoothScroller smoothScroller = new FlipSmoothScroller(recyclerView.getContext()) {
            @Nullable
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                if (targetPosition < 0) {
                    throw new IllegalArgumentException("position can't be less then 0. position is: " + targetPosition);
                }

                if (targetPosition >= getItemCount()) {
                    throw new IllegalArgumentException("position can't be great then adapter items count. position is: " + targetPosition + " item count is: " + getItemCount());
                }

                if (getChildCount() == 0) {
                    return null;
                }

                final int direction = targetPosition < getCurrentPosition() ? -1 : 1;
                if (getOrientation() == HORIZONTAL) {
                    return new PointF(direction, 0);
                } else {
                    return new PointF(0, direction);
                }
            }
        };

        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }

    @Override
    public void scrollToPosition(int position) {
        scrollDistance = position * DISTANCE_PER_POSITION;
        positionChangedForLayout = true;
        requestLayout();
    }

    public void notifyPositionChange(int position) {
        onPositionChangeListener.onPositionChange(this, position);
    }

    public interface OnPositionChangeListener {
        void onPositionChange(FlipLayoutManager flipLayoutManager, int position);
    }
}