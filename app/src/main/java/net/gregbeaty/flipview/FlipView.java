package net.gregbeaty.flipview;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class FlipView extends RecyclerView {
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

    private SnapScrollListener mSnapScrollListener;

    public FlipView(Context context) {
        this(context, null);
    }

    public FlipView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlipView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mShadowPaint.setColor(Color.BLACK);
        mShadowPaint.setStyle(Paint.Style.FILL);
        mShadePaint.setColor(Color.BLACK);
        mShadePaint.setStyle(Paint.Style.FILL);
        mShinePaint.setColor(Color.WHITE);
        mShinePaint.setStyle(Paint.Style.FILL);
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
        super.setLayoutManager(layout);
    }

    public void setLayoutManager(FlipLayoutManager layout) {
        if (mSnapScrollListener != null) {
            removeOnScrollListener(mSnapScrollListener);
        }

        mSnapScrollListener = new SnapScrollListener(layout);
        addOnScrollListener(mSnapScrollListener);

        super.setLayoutManager(layout);
    }

    @Override
    public int getOverScrollMode() {
        return OVER_SCROLL_NEVER;
    }

    @Override
    public FlipLayoutManager getLayoutManager() {
        return (FlipLayoutManager) super.getLayoutManager();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int viewCount = getChildCount();
        if (viewCount == 0) {
            return;
        }

        final FlipLayoutManager layoutManager = getLayoutManager();
        if (layoutManager == null) {
            return;
        }

        final boolean isVerticalScrolling = layoutManager.getOrientation() == FlipLayoutManager.VERTICAL;
        final int flipAngle = layoutManager.getFlipAngle();
        final int previousPosition = layoutManager.getPreviousPosition();
        final int currentPosition = layoutManager.getCurrentPosition();
        final int nextPosition = layoutManager.getNextPosition();

        View previousView = null;
        View currentView = null;
        View nextView = null;

        for (int i = 0; i < viewCount; i++) {
            View view = getChildAt(i);
            int viewAdapterPosition = getChildAdapterPosition(view);
            if (viewAdapterPosition == previousPosition) {
                previousView = view;
                continue;
            }

            if (viewAdapterPosition == currentPosition) {
                currentView = view;
                continue;
            }

            if (viewAdapterPosition == nextPosition) {
                nextView = view;
            }
        }

        assert currentView != null;

        currentView.setVisibility(VISIBLE);

        if (!layoutManager.isScrolling() && !layoutManager.requiresSettling()) {
            if (previousView != null) {
                previousView.setVisibility(GONE);
            }

            if (nextView != null) {
                nextView.setVisibility(GONE);
            }

            setDrawWithLayer(currentView, false);
            drawChild(canvas, currentView, 0);

            return;
        }

        if (previousView != null) {
            previousView.setVisibility(VISIBLE);
        }

        if (nextView != null) {
            nextView.setVisibility(VISIBLE);
        }

        //draw previous half
        canvas.save();
        canvas.clipRect(isVerticalScrolling ? mTopClippingRect : mLeftClippingRect);
        final View previousHalf = flipAngle >= 90 ? previousView : currentView;
        if (previousHalf != null) {
            setDrawWithLayer(previousHalf, true);
            drawChild(canvas, previousHalf, 0);
        }

        if (flipAngle > 90) {
            final int alpha = (int) (((flipAngle - 90) / 90f) * MAX_SHADOW_ALPHA);
            mShadowPaint.setAlpha(alpha);
            canvas.drawPaint(mShadowPaint);
        }

        canvas.restore();

        //draw next half
        canvas.save();
        canvas.clipRect(isVerticalScrolling ? mBottomClippingRect : mRightClippingRect);
        final View nextHalf = flipAngle >= 90 ? currentView : nextView;

        if (nextHalf != null) {
            setDrawWithLayer(nextHalf, true);
            drawChild(canvas, nextHalf, 0);
        }

        if (flipAngle < 90) {
            final int alpha = (int) ((Math.abs(flipAngle - 90) / 90f) * MAX_SHADOW_ALPHA);
            mShadowPaint.setAlpha(alpha);
            canvas.drawPaint(mShadowPaint);
        }

        canvas.restore();

        //draw flipping half
        canvas.save();
        mCamera.save();

        if (flipAngle > 90) {
            canvas.clipRect(isVerticalScrolling ? mTopClippingRect : mLeftClippingRect);
            if (isVerticalScrolling) {
                mCamera.rotateX(flipAngle - 180);
            } else {
                mCamera.rotateY(180 - flipAngle);
            }
        } else {
            canvas.clipRect(isVerticalScrolling ? mBottomClippingRect : mRightClippingRect);
            if (isVerticalScrolling) {
                mCamera.rotateX(flipAngle);
            } else {
                mCamera.rotateY(-flipAngle);
            }
        }

        mCamera.getMatrix(mMatrix);

        mMatrix.preScale(0.25f, 0.25f);
        mMatrix.postScale(4.0f, 4.0f);
        mMatrix.preTranslate(-getWidth() / 2, -getHeight() / 2);
        mMatrix.postTranslate(getWidth() / 2, getHeight() / 2);

        canvas.concat(mMatrix);

        setDrawWithLayer(currentView, true);
        drawChild(canvas, currentView, 0);

        if (flipAngle < 90) {
            final int alpha = (int) ((flipAngle / 90f) * MAX_SHINE_ALPHA);
            mShinePaint.setAlpha(alpha);
            canvas.drawRect(isVerticalScrolling ? mBottomClippingRect : mRightClippingRect, mShinePaint);
        } else {
            final int alpha = (int) ((Math.abs(flipAngle - 180) / 90f) * MAX_SHADE_ALPHA);
            mShadePaint.setAlpha(alpha);
            canvas.drawRect(isVerticalScrolling ? mTopClippingRect : mLeftClippingRect, mShadePaint);
        }

        mCamera.restore();
        canvas.restore();
    }

    private void setDrawWithLayer(View view, boolean drawWithLayer) {
        if (!isHardwareAccelerated()) {
            return;
        }

        if (view.getLayerType() != LAYER_TYPE_HARDWARE && drawWithLayer) {
            view.setLayerType(LAYER_TYPE_HARDWARE, null);
        } else if (view.getLayerType() != LAYER_TYPE_NONE && !drawWithLayer) {
            view.setLayerType(LAYER_TYPE_NONE, null);
        }
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        return false;
    }
}
