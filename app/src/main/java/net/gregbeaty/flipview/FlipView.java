package net.gregbeaty.flipview;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class FlipView extends RecyclerView implements OnPositionChangeListener {
    private static final int MAX_SHADOW_ALPHA = 180;
    private static final int MAX_SHADE_ALPHA = 130;
    private static final int MAX_SHINE_ALPHA = 100;

    private final Rect mTopClippingRect = new Rect();
    private final Rect mBottomClippingRect = new Rect();
    private final Rect mRightClippingRect = new Rect();
    private final Rect mLeftClippingRect = new Rect();

    private Camera mCamera = new Camera();
    private Matrix mMatrix = new Matrix();

    private final Paint mShadowPaint = new Paint();
    private final Paint mShadePaint = new Paint();
    private final Paint mShinePaint = new Paint();

    private List<OnPositionChangeListener> mPositionChangeListeners;

    public FlipView(Context context) {
        this(context, null);
    }

    public FlipView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlipView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setItemAnimator(new DefaultItemAnimator());
    }

    @Override
    public int getOverScrollMode() {
        return OVER_SCROLL_NEVER;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mTopClippingRect.top = 0;
        mTopClippingRect.left = 0;
        mTopClippingRect.right = getWidth();
        mTopClippingRect.bottom = getHeight() / 2;

        mBottomClippingRect.top = getHeight() / 2;
        mBottomClippingRect.left = 0;
        mBottomClippingRect.right = getWidth();
        mBottomClippingRect.bottom = getHeight();

        mLeftClippingRect.top = 0;
        mLeftClippingRect.left = 0;
        mLeftClippingRect.right = getWidth() / 2;
        mLeftClippingRect.bottom = getHeight();

        mRightClippingRect.top = 0;
        mRightClippingRect.left = getWidth() / 2;
        mRightClippingRect.right = getWidth();
        mRightClippingRect.bottom = getHeight();

        super.onLayout(changed, l, t, r, b);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (getLayoutManager() == null) {
            return super.onTouchEvent(e);
        }

        return getLayoutManager().getScrollState() != RecyclerView.SCROLL_STATE_SETTLING && super.onTouchEvent(e);
    }

    /**
     * @deprecated Use {@link #setLayoutManager(FlipLayoutManager)} instead. Only {@link FlipLayoutManager} is supported.
     * <p>
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public void setLayoutManager(LayoutManager layout) {
        throw new UnsupportedOperationException("This view does not support customized layout managers.");
    }

    public void setLayoutManager(FlipLayoutManager layoutManager) {
        layoutManager.setPositionChangeListener(this);

        super.setLayoutManager(layoutManager);
    }

    @Override
    public FlipLayoutManager getLayoutManager() {
        return (FlipLayoutManager) super.getLayoutManager();
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        return false;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int viewCount = getChildCount();
        if (viewCount == 0) {
            return;
        }

        FlipLayoutManager layoutManager = getLayoutManager();
        if (layoutManager == null) {
            return;
        }

        final boolean isVerticalScrolling = layoutManager.getOrientation() == FlipLayoutManager.VERTICAL;
        final int angle = layoutManager.getAngle();
        final int currentPosition = layoutManager.getCurrentPosition();

        View previousView = null;
        View currentView = null;
        View nextView = null;

        for (int i = 0; i < viewCount; i++) {
            View view = getChildAt(i);
            int position = getChildAdapterPosition(view);
            if (position == currentPosition - 1) {
                previousView = view;
                continue;
            }

            if (position == currentPosition) {
                currentView = view;
                continue;
            }

            if (position == currentPosition + 1) {
                nextView = view;
            }
        }

        if (currentView == null) {
            return;
        }

        if (!layoutManager.isScrolling() && !layoutManager.requiresSettling()) {
            drawChild(canvas, currentView, 0);
            return;
        }

        //draw previous half
        canvas.save();
        canvas.clipRect(isVerticalScrolling ? mTopClippingRect : mLeftClippingRect);
        final View previousHalf = angle >= 90 ? previousView : currentView;
        if (previousHalf != null) {
            drawChild(canvas, previousHalf, 0);
        }

        if (angle > 90) {
            final int alpha = (int) (((angle - 90) / 90f) * MAX_SHADOW_ALPHA);
            mShadowPaint.setAlpha(alpha);
            canvas.drawPaint(mShadowPaint);
        }

        canvas.restore();

        //draw next half
        canvas.save();
        canvas.clipRect(isVerticalScrolling ? mBottomClippingRect : mRightClippingRect);
        final View nextHalf = angle >= 90 ? currentView : nextView;

        if (nextHalf != null) {
            drawChild(canvas, nextHalf, 0);
        }

        if (angle < 90) {
            final int alpha = (int) ((Math.abs(angle - 90) / 90f) * MAX_SHADOW_ALPHA);
            mShadowPaint.setAlpha(alpha);
            canvas.drawPaint(mShadowPaint);
        }

        canvas.restore();

        //draw flipping half
        canvas.save();
        mCamera.save();

        if (angle > 90) {
            canvas.clipRect(isVerticalScrolling ? mTopClippingRect : mLeftClippingRect);
            if (isVerticalScrolling) {
                mCamera.rotateX(angle - 180);
            } else {
                mCamera.rotateY(180 - angle);
            }
        } else {
            canvas.clipRect(isVerticalScrolling ? mBottomClippingRect : mRightClippingRect);
            if (isVerticalScrolling) {
                mCamera.rotateX(angle);
            } else {
                mCamera.rotateY(-angle);
            }
        }

        mCamera.getMatrix(mMatrix);

        mMatrix.preScale(0.25f, 0.25f);
        mMatrix.postScale(4.0f, 4.0f);
        mMatrix.preTranslate(-getWidth() / 2, -getHeight() / 2);
        mMatrix.postTranslate(getWidth() / 2, getHeight() / 2);

        canvas.concat(mMatrix);

        drawChild(canvas, currentView, 0);

        if (angle < 90) {
            final int alpha = (int) ((angle / 90f) * MAX_SHINE_ALPHA);
            mShinePaint.setAlpha(alpha);
            canvas.drawRect(isVerticalScrolling ? mBottomClippingRect : mRightClippingRect, mShinePaint);
        } else {
            final int alpha = (int) ((Math.abs(angle - 180) / 90f) * MAX_SHADE_ALPHA);
            mShadePaint.setAlpha(alpha);
            canvas.drawRect(isVerticalScrolling ? mTopClippingRect : mLeftClippingRect, mShadePaint);
        }

        mCamera.restore();
        canvas.restore();
    }

    @Override
    public void onPositionChange(FlipLayoutManager layoutManager, int position) {
        if (mPositionChangeListeners == null) {
            return;
        }

        for (OnPositionChangeListener listener : mPositionChangeListeners) {
            listener.onPositionChange(this, position);
        }
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);

        if (state != RecyclerView.SCROLL_STATE_IDLE) {
            return;
        }

        if (!getLayoutManager().requiresSettling()) {
            return;
        }

        smoothScrollToPosition(getLayoutManager().getCurrentPosition());
    }

    public void addOnPositionChangeListener(OnPositionChangeListener listener) {
        if (mPositionChangeListeners == null) {
            mPositionChangeListeners = new ArrayList<>();
        }

        mPositionChangeListeners.add(listener);
    }

    public void removeOnPositionChangeListener(OnPositionChangeListener listener) {
        if (mPositionChangeListeners == null) {
            return;
        }

        mPositionChangeListeners.remove(listener);
    }

    public void clearOnPositionChangeListeners() {
        if (mPositionChangeListeners == null) {
            return;
        }

        mPositionChangeListeners.clear();
    }

    public interface OnPositionChangeListener {
        void onPositionChange(FlipView flipView, int position);
    }
}
