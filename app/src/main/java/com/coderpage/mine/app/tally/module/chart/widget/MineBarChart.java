package com.coderpage.mine.app.tally.module.chart.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.github.mikephil.charting.utils.MPPointD;
import com.github.mikephil.charting.utils.Transformer;

/**
 * @author lc. 2018-09-29 16:22
 * @since 0.6.0
 */

public class MineBarChart extends BarChart {

    private static final long BAR_HOLD_DURATION_MS = 1000L;
    private boolean mDrawMarkOnTop = false;
    private float mDownX;
    private float mDownY;
    private boolean mHoldTriggered;
    private Runnable mPendingBarHold;
    private OnBarHoldListener mOnBarHoldListener;

    public interface OnBarHoldListener {
        void onBarHold(BarEntry entry);
    }

    public MineBarChart(Context context) {
        super(context);
    }

    public MineBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MineBarChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();
        mChartTouchListener = new MineLineChartTouchListener(this, mViewPortHandler.getMatrixTouch(), 3f);
        mRenderer = new MineBarChartRenderer(this, mAnimator, mViewPortHandler);
    }

    public void setDrawMarkOnTop(boolean drawMarkOnTop) {
        this.mDrawMarkOnTop = drawMarkOnTop;
    }

    public void setOnBarHoldListener(OnBarHoldListener listener) {
        mOnBarHoldListener = listener;
    }

    public void cancelPendingBarHold() {
        if (mPendingBarHold != null) {
            removeCallbacks(mPendingBarHold);
            mPendingBarHold = null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 参考 http://www.jianshu.com/p/fe3d109eb27e
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                requestDisallowInterceptTouchEvent(true);
                cancelPendingBarHold();
                mHoldTriggered = false;
                mDownX = event.getX();
                mDownY = event.getY();
                scheduleBarHold(mDownX, mDownY);
                break;
            case MotionEvent.ACTION_MOVE:
                int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                if (Math.abs(event.getX() - mDownX) > slop
                        || Math.abs(event.getY() - mDownY) > slop) {
                    cancelPendingBarHold();
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_CANCEL:
                cancelPendingBarHold();
                break;
            case MotionEvent.ACTION_UP:
                cancelPendingBarHold();
                if (mHoldTriggered) return true;
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private void scheduleBarHold(float x, float y) {
        if (mOnBarHoldListener == null || mData == null) return;
        if (getMarker() instanceof MarkViewMine
                && ((MarkViewMine) getMarker()).getBound().contains(x, y)) return;
        Highlight highlight = getHighlightByTouchPoint(x, y);
        if (highlight == null) return;
        Entry entry = mData.getEntryForHighlight(highlight);
        if (!(entry instanceof BarEntry) || entry.getY() <= 0f
                || !isNearBar((BarEntry) entry, highlight, x, y)) return;
        BarEntry selected = (BarEntry) entry;
        mPendingBarHold = () -> {
            mPendingBarHold = null;
            mHoldTriggered = true;
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            if (mOnBarHoldListener != null) mOnBarHoldListener.onBarHold(selected);
        };
        postDelayed(mPendingBarHold, BAR_HOLD_DURATION_MS);
    }

    private boolean isNearBar(BarEntry entry, Highlight highlight, float x, float y) {
        Transformer transformer = getTransformer(highlight.getAxis());
        float halfBarWidth = getBarData().getBarWidth() / 2f;
        MPPointD top = transformer.getPixelForValues(entry.getX(), entry.getY());
        MPPointD bottom = transformer.getPixelForValues(entry.getX(), 0f);
        MPPointD left = transformer.getPixelForValues(entry.getX() - halfBarWidth, 0f);
        MPPointD right = transformer.getPixelForValues(entry.getX() + halfBarWidth, 0f);
        float padding = getResources().getDisplayMetrics().density * 12f;
        boolean inside = x >= Math.min(left.x, right.x) - padding
                && x <= Math.max(left.x, right.x) + padding
                && y >= Math.min(top.y, bottom.y) - padding
                && y <= Math.max(top.y, bottom.y) + padding;
        MPPointD.recycleInstance(top);
        MPPointD.recycleInstance(bottom);
        MPPointD.recycleInstance(left);
        MPPointD.recycleInstance(right);
        return inside;
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelPendingBarHold();
        super.onDetachedFromWindow();
    }

    @Override
    protected void drawMarkers(Canvas canvas) {
        if (mDrawMarkOnTop) {
            drawTopMarkers(canvas);
        } else {
            super.drawMarkers(canvas);
        }
    }

    private void drawTopMarkers(Canvas canvas) {
        if (mMarker == null) {
            return;
        }

        if (!isDrawMarkersEnabled() || !valuesToHighlight()) {
            return;
        }

        for (int i = 0; i < mIndicesToHighlight.length; i++) {

            Highlight highlight = mIndicesToHighlight[i];

            IDataSet set = mData.getDataSetByIndex(highlight.getDataSetIndex());

            Entry e = mData.getEntryForHighlight(mIndicesToHighlight[i]);
            int entryIndex = set.getEntryIndex(e);

            // make sure entry not null
            if (e == null || entryIndex > set.getEntryCount() * mAnimator.getPhaseX()) {
                continue;
            }

            if (mMarker != null && mMarker instanceof View) {
                View markerView = (View) this.mMarker;

                int measuredHeight = markerView.getHeight();
                int measuredWidth = markerView.getWidth();

                float x = highlight.getDrawX() - measuredWidth / 2;
                if (!mViewPortHandler.isInBoundsLeft(x)) {
                    x = mViewPortHandler.contentLeft();
                }
                if (x + measuredWidth >= mViewPortHandler.contentRight()) {
                    x = mViewPortHandler.contentRight() - measuredWidth;
                }

                float y = mViewPortHandler.contentTop() - measuredHeight;

                // callbacks to update the content
                mMarker.refreshContent(e, highlight);

                // draw the marker
                mMarker.draw(canvas, x, y);
            }
        }
    }
}
