package net.gregbeaty.flipview;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import timber.log.Timber;

public class FlipRecyclerView extends RecyclerView {
    private static final int MAX_SHADOW_ALPHA = 180;
    private static final int MAX_SHADE_ALPHA = 130;
    private static final int MAX_SHINE_ALPHA = 100;

    private final Rect topClippingRect = new Rect();
    private final Rect bottomClippingRect = new Rect();
    private final Rect rightClippingRect = new Rect();
    private final Rect leftClippingRect = new Rect();

    private Camera camera = new Camera();
    private Matrix matrix = new Matrix();

    private final Paint shadowPaint = new Paint();
    private final Paint shadePaint = new Paint();
    private final Paint shinePaint = new Paint();

    public FlipRecyclerView(@NonNull Context context) {
        super(context);
    }

    public FlipRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public FlipRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        Timber.tag(getClass().getCanonicalName());

        setItemAnimator(new DefaultItemAnimator());
    }

    @Override
    public int getOverScrollMode() {
        return View.OVER_SCROLL_NEVER;
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (getLayoutManager() == null) {
            return super.onTouchEvent(e);
        }

        return getScrollState() != RecyclerView.SCROLL_STATE_SETTLING && super.onTouchEvent(e);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        topClippingRect.top = 0;
        topClippingRect.left = 0;
        topClippingRect.right = getWidth();
        topClippingRect.bottom = getHeight() / 2;

        bottomClippingRect.top = getHeight() / 2;
        bottomClippingRect.left = 0;
        bottomClippingRect.right = getWidth();
        bottomClippingRect.bottom = getHeight();

        leftClippingRect.top = 0;
        leftClippingRect.left = 0;
        leftClippingRect.right = getWidth() / 2;
        leftClippingRect.bottom = getHeight();

        rightClippingRect.top = 0;
        rightClippingRect.left = getWidth() / 2;
        rightClippingRect.right = getWidth();
        rightClippingRect.bottom = getHeight();

        super.onLayout(changed, l, t, r, b);
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

        final boolean isVerticalScrolling = getLayoutManager().getOrientation() == FlipLayoutManager.VERTICAL;
        final int angle = getLayoutManager().getAngle();
        final int currentPosition = getLayoutManager().getCurrentPosition();
        boolean layoutOnlyCurrentPosition = !getLayoutManager().isScrolling() && !getLayoutManager().requiresSettling();

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

        if (previousView != null) {
            previousView.setVisibility(layoutOnlyCurrentPosition ? GONE : VISIBLE);
        }

        if (nextView != null) {
            nextView.setVisibility(layoutOnlyCurrentPosition ? GONE : VISIBLE);
        }

        if (layoutOnlyCurrentPosition) {
            drawChild(canvas, currentView, 0);
            return;
        }

        //draw previous half
        canvas.save();
        canvas.clipRect(isVerticalScrolling ? topClippingRect : leftClippingRect);
        final View previousHalf = angle >= 90 ? previousView : currentView;
        if (previousHalf != null) {
            drawChild(canvas, previousHalf, 0);
        }

        if (angle > 90) {
            final int alpha = (int) (((angle - 90) / 90f) * MAX_SHADOW_ALPHA);
            shadowPaint.setAlpha(alpha);
            canvas.drawPaint(shadowPaint);
        }

        canvas.restore();

        //draw next half
        canvas.save();
        canvas.clipRect(isVerticalScrolling ? bottomClippingRect : rightClippingRect);
        final View nextHalf = angle >= 90 ? currentView : nextView;

        if (nextHalf != null) {
            drawChild(canvas, nextHalf, 0);
        }

        if (angle < 90) {
            final int alpha = (int) ((Math.abs(angle - 90) / 90f) * MAX_SHADOW_ALPHA);
            shadowPaint.setAlpha(alpha);
            canvas.drawPaint(shadowPaint);
        }

        canvas.restore();

        //draw flipping half
        canvas.save();
        camera.save();

        if (angle > 90) {
            canvas.clipRect(isVerticalScrolling ? topClippingRect : leftClippingRect);
            if (isVerticalScrolling) {
                camera.rotateX(angle - 180);
            } else {
                camera.rotateY(180 - angle);
            }
        } else {
            canvas.clipRect(isVerticalScrolling ? bottomClippingRect : rightClippingRect);
            if (isVerticalScrolling) {
                camera.rotateX(angle);
            } else {
                camera.rotateY(-angle);
            }
        }

        camera.getMatrix(matrix);

        matrix.preScale(0.25f, 0.25f);
        matrix.postScale(4.0f, 4.0f);
        matrix.preTranslate(-getWidth() / 2, -getHeight() / 2);
        matrix.postTranslate(getWidth() / 2, getHeight() / 2);

        canvas.concat(matrix);

        drawChild(canvas, currentView, 0);

        if (angle < 90) {
            final int alpha = (int) ((angle / 90f) * MAX_SHINE_ALPHA);
            shinePaint.setAlpha(alpha);
            canvas.drawRect(isVerticalScrolling ? bottomClippingRect : rightClippingRect, shinePaint);
        } else {
            final int alpha = (int) ((Math.abs(angle - 180) / 90f) * MAX_SHADE_ALPHA);
            shadePaint.setAlpha(alpha);
            canvas.drawRect(isVerticalScrolling ? topClippingRect : leftClippingRect, shadePaint);
        }

        camera.restore();
        canvas.restore();
    }
}
