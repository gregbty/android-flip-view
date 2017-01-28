package net.gregbeaty.flipview;

import android.content.Context;
import android.support.v7.widget.LinearSmoothScroller;
import android.util.DisplayMetrics;
import android.view.View;

abstract class FlipScroller extends LinearSmoothScroller {
    FlipScroller(Context context) {
        super(context);
    }

    @Override
    public int calculateDxToMakeVisible(View view, int snapPreference) {
        final FlipLayoutManager layoutManager = (FlipLayoutManager) getLayoutManager();
        if (layoutManager == null || !layoutManager.canScrollHorizontally()) {
            return 0;
        }

        return calculateDeltaToMakeVisible(layoutManager, view);
    }

    @Override
    public int calculateDyToMakeVisible(View view, int snapPreference) {
        final FlipLayoutManager layoutManager = (FlipLayoutManager) getLayoutManager();
        if (layoutManager == null || !layoutManager.canScrollVertically()) {
            return 0;
        }

        return calculateDeltaToMakeVisible(layoutManager, view);
    }

    private int calculateDeltaToMakeVisible(FlipLayoutManager layoutManager, View view) {
        int scrollDistance = layoutManager.getScrollDistance();
        int distanceForPage = layoutManager.getPosition(view) * FlipLayoutManager.DISTANCE_PER_POSITION;
        return scrollDistance - distanceForPage;
    }

    @Override
    protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
        return 200f / displayMetrics.densityDpi;
    }

    @Override
    protected int calculateTimeForScrolling(int dx) {
        return super.calculateTimeForScrolling(dx);
    }
}
