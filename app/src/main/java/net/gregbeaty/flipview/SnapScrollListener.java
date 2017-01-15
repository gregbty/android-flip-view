package net.gregbeaty.flipview;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

class SnapScrollListener extends RecyclerView.OnScrollListener {
    private final FlipLayoutManager mLayoutManager;

    public SnapScrollListener(@NonNull FlipLayoutManager layoutManager) {
        mLayoutManager = layoutManager;
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        if (newState != RecyclerView.SCROLL_STATE_IDLE) {
            return;
        }

        if (!mLayoutManager.requiresSettling()) {
            return;
        }

        recyclerView.smoothScrollToPosition(mLayoutManager.getCurrentPosition());
    }
}
