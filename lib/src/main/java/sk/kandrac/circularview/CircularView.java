package sk.kandrac.circularview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.OverScroller;
import android.widget.Scroller;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This view is intended to display Circular view composed of 2 circles. Inner circle to show
 * standard content cropped into circle, and outer to display PieChart-like portions of added
 * objects (see addItem() methods and/or sample application).
 *
 * TODO: OnDataChangeListener to animate changes
 * TODO: Improve touch event handling
 *
 * Created by VizGhar on 2.6.2014.
 */
public class CircularView extends ViewGroup {
    private MyGestureListener gestureListener;

    // list of items percentage of which will be displayed in outer circle
    private HashMap<Object, ItemDescriptor> items = new HashMap<Object, ItemDescriptor>();

    // width of outer circle
    private int outerWidth;

    // bounds of whole view
    private RectF mBounds;

    //bounds of child view
    private RectF mBoundsI;

    private Paint defalutPaint;

    private Path clipPath;

    private int color;

    private View child;

    /**
     * Get Count of items for which score will be displayed on outer cycle
     * @return count of items
     */
    public int getItemCount() {
        return items.size();
    }

    /**
     *
     * @param items to be added to outer cycle
     */
    public void addItems(Collection<Object> items) {
        for (Object item : items)
            addItem(item);
    }

    public void addItem(Object item) {
        addItem(item, 0);
    }

    public void addItem(Object item, float score) {
        addItem(item, score, 0);
    }

    public void addItem(Object item, float score, int color) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(outerWidth);
        paint.setAntiAlias(true);

        ItemDescriptor itemDescriptor = new ItemDescriptor(score, paint);
        items.put(item, itemDescriptor);
    }

    public CircularView(Context context) {
        this(context, null, 0);
    }

    public CircularView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircularView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        parseAttributes(context.obtainStyledAttributes(attrs, R.styleable.CircularView));
        init();
        gestureListener = new MyGestureListener();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if (this.child == null) {
            this.child = getChildAt(0);
        }

        int size = Math.min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));

        mBoundsI.left = outerWidth/2;
        mBoundsI.top = outerWidth/2;
        mBoundsI.right = size-outerWidth/2;
        mBoundsI.bottom = size-outerWidth/2;

        mBounds.left = outerWidth;
        //noinspection SuspiciousNameCombination
        mBounds.top = outerWidth;
        mBounds.right = size - outerWidth;
        mBounds.bottom = size - outerWidth;

        float x = (mBounds.right + mBounds.left)/2;
        float y = (mBounds.bottom + mBounds.top)/2;

        if (!clipPath.isEmpty()) clipPath.reset();

        clipPath.addCircle(x, y, x - outerWidth, Path.Direction.CW);

        setMeasuredDimension(size, size);
        measureChild(size - outerWidth, size - outerWidth);
    }

    protected void measureChild(int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        if (child != null)
            super.measureChild(this.child, parentWidthMeasureSpec, parentHeightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean b, int left, int top, int right, int bottom) {
        if (child != null) {
            child.layout(outerWidth, outerWidth, right - left - outerWidth, bottom - top - outerWidth);
        }
    }

    @Override
    public void addView(@SuppressWarnings("NullableProblems") View child) {
        if (getChildCount() > 0)
            throw new IllegalStateException("CircularView can host only one direct child");
        super.addView(child);
    }

    @Override
    public void addView(@SuppressWarnings("NullableProblems") View child, int index) {
        if (getChildCount() > 0)
            throw new IllegalStateException("CircularView can host only one direct child");
        super.addView(child, index);
    }

    @Override
    public void addView(@SuppressWarnings("NullableProblems") View child, int width, int height) {
        if (getChildCount() > 0)
            throw new IllegalStateException("CircularView can host only one direct child");
        super.addView(child, width, height);
    }

    @Override
    public void addView(@SuppressWarnings("NullableProblems") View child, LayoutParams params) {
        if (getChildCount() > 0)
            throw new IllegalStateException("CircularView can host only one direct child");
        super.addView(child, params);
    }

    @Override
    public void addView(@SuppressWarnings("NullableProblems") View child, int index, LayoutParams params) {
        if (getChildCount() > 0)
            throw new IllegalStateException("CircularView can host only one direct child");
        super.addView(child, index, params);
    }

    /**
     * Parse the attributes passed to the view from the XML
     *
     * @param a the attributes to parse
     */
    private void parseAttributes(TypedArray a) {
        outerWidth = (int) a.getDimension(R.styleable.CircularView_outer_width, 50);
        color = a.getColor(R.styleable.CircularView_inner_color, Color.BLACK);
        a.recycle();
    }

    private void init() {
        mBounds = new RectF();
        mBoundsI = new RectF();
        defalutPaint = new Paint();
        defalutPaint.setColor(color);
        defalutPaint.setStrokeWidth(outerWidth);
        defalutPaint.setStyle(Paint.Style.STROKE);

        clipPath = new Path();

        setBackgroundColor(Color.TRANSPARENT);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected boolean drawChild(@SuppressWarnings("NullableProblems") Canvas canvas, @SuppressWarnings("NullableProblems") View child, long drawingTime) {
        canvas.clipPath(clipPath, Region.Op.REPLACE);
        super.drawChild(canvas, child, drawingTime);
        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        float beg = 0;
        float end;
        if (items != null && items.size() > 0 && getSum() != 0)
            for (Map.Entry<Object, ItemDescriptor> item : items.entrySet()) {
                end = (item.getValue().getScore() / getSum()) * 360;
                canvas.drawArc(mBoundsI, beg + gestureListener.getScroll(), end, false, item.getValue().getPaint());
                beg += end;
            }
        else {
            canvas.drawArc(mBoundsI, beg, 360, false, defalutPaint);
        }
    }

    private int getSum() {
        int sum = 0;
        for (Map.Entry<Object, ItemDescriptor> item : items.entrySet()) {
            sum += item.getValue().getScore();
        }
        return sum;
    }

    public ItemDescriptor getDescriptor(Object obj) {
        return items.get(obj);
    }


    // TODO: infinite (periodic) scroll
    class MyGestureListener extends GestureDetector.SimpleOnGestureListener implements OnTouchListener {

        private Scroller mScroller;
        private GestureDetector gesture;

        private int minScroll = 0;
        private int maxScroll = 300;

        public MyGestureListener() {
            mScroller = new Scroller(getContext());
            gesture = new GestureDetector(getContext(), this);
        }

        @Override
        public boolean onDown(MotionEvent event) {
            mScroller.forceFinished(true);
            mScroller.startScroll(mScroller.getCurrX(), 0, 0, 0);
            ViewCompat.postInvalidateOnAnimation(CircularView.this);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            mScroller.forceFinished(true);
            mScroller.fling(getScroll(), 0, (int) velocityX, 0, minScroll, maxScroll, 0, 0);
            ViewCompat.postInvalidateOnAnimation(CircularView.this);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (mScroller.getFinalX() - distanceX > minScroll  && mScroller.getFinalX() - distanceX < maxScroll) {
                mScroller.startScroll(mScroller.getFinalX(), 0,
                        (int) (-distanceX), 0);
            }
            ViewCompat.postInvalidateOnAnimation(CircularView.this);
            return true;
        }

        public int getScroll() {
            if (mScroller.computeScrollOffset()) {
                requestLayout();
                return mScroller.getCurrX();
            }
            return mScroller.getFinalX();
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            int eventAction = motionEvent.getActionMasked();
            switch (eventAction) {
                // on action up scroll back in bounds if needed
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    int x = mScroller.getCurrX();
                    if (x < minScroll || x > maxScroll) {
                        onFling(null, null, 0, 0);
                    }
            }
            return gesture.onTouchEvent(motionEvent);
        }
    }

    @Override
    public void computeScroll() {
        // TODO: remember scroll on display rotation
        gestureListener.getScroll();
    }

    //////////////////////////////
    //  Tracking item click     //
    //////////////////////////////
    @Override
    public boolean onInterceptTouchEvent(MotionEvent __e) {
        return evaluateTouchEvent(__e);
    }

    // Constants
    protected static final float DRAG_THRESHOLD = 10;

    // Properties
    protected boolean isPressed;
    protected float startPressX;
    protected float startPressY;
    protected boolean isDragging;

    // TODO : childview is still square, process touch event only if click event is in the circles
    private boolean evaluateTouchEvent(MotionEvent __e) {
        float dragDeltaX;
        float dragDeltaY;

        switch (__e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isPressed = true;
                startPressX = __e.getX();
                startPressY = __e.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (isPressed && !isDragging) {
                    dragDeltaX = __e.getX() - startPressX;
                    dragDeltaY = __e.getY() - startPressY;

                    if (Math.abs(dragDeltaX) > DRAG_THRESHOLD
                            || Math.abs(dragDeltaY) > DRAG_THRESHOLD) {
                        MotionEvent me = MotionEvent.obtain(__e);
                        me.setAction(MotionEvent.ACTION_DOWN);
                        me.setLocation(__e.getX() - dragDeltaX, __e.getY()
                                - dragDeltaY);
                        gestureListener.onTouch(this, me);
                        me.recycle();
                        isDragging = true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!isPressed)
                    break;
                isPressed = false;
                if (isDragging)
                    isDragging = false;
        }
        return isDragging;
    }

    @Override
    public boolean onTouchEvent(@SuppressWarnings("NullableProblems") MotionEvent __e) {
        evaluateTouchEvent(__e);
        return gestureListener.onTouch(this, __e);
    }
}
