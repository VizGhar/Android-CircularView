package sk.kandrac.circularview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This view is intended to display Circular view composed of 2 circles. Inner circle to show
 * standard content cropped into circle, and outer to display PieChart-like portions of added
 * items. (see addItem() methods and/or sample application).
 * <p/>
 * TODO: Add animations
 * <p/>
 * Created by VizGhar on 2.6.2014.
 */
public class CircularView extends ViewGroup {

    // list of items percentage of which will be displayed in outer circle
    private HashMap<Object, ItemDescriptor> items = new HashMap<Object, ItemDescriptor>();

    // width of outer circle
    private int outerWidth;

    // bounds of whole view (substracted by half of width of outer circle)
    private Rect innerBounds;

    // rectangular bounds of child view
    private RectF outerBounds;

    // paint used only for layout preview
    private Paint defaultPaint;

    // color used mainly for layout preview or when no item is presented
    private int defaultColor;

    // clip path where child should be placed
    private Path clipPath;

    // holds whether view is scrolling or not
    private boolean mIsScrolling = false;

    private int mTouchSlop;

    private int scroll;

    private float centerX;
    private float centerY;
    private float innerRadius;
    private float outerRadius;

    private CircularGestureListener mGestureListener;
    private GestureDetector gestureDetector;

    //////////////////////////////////////////////
    // INIT PART (constructors and initalizers) //
    //////////////////////////////////////////////
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
    }


    /**
     * Parse the attributes passed to the view from the XML
     *
     * @param attrs the attributes to parse
     */
    private void parseAttributes(TypedArray attrs) {
        outerWidth = (int) attrs.getDimension(R.styleable.CircularView_outer_width, 50);
        defaultColor = attrs.getColor(R.styleable.CircularView_inner_color, Color.BLACK);
        attrs.recycle();
    }

    private void init() {
        innerBounds = new Rect();
        outerBounds = new RectF();
        clipPath = new Path();
        defaultPaint = new Paint();
        defaultPaint.setColor(defaultColor);
        defaultPaint.setStrokeWidth(outerWidth);
        defaultPaint.setStyle(Paint.Style.STROKE);

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        ViewConfiguration vc = ViewConfiguration.get(getContext());
        mTouchSlop = vc.getScaledTouchSlop();

        mGestureListener = new CircularGestureListener();
        gestureDetector = new GestureDetector(getContext(), mGestureListener);
    }

    //////////////////////////////////////////////
    // OUTER CIRCLE ITEM PROCESSING PART        //
    //////////////////////////////////////////////
    public class ItemDescriptor implements Parcelable {

        private float score;
        private Paint paint;

        public ItemDescriptor(float score, int color) {
            this.score = score;
            this.paint = new Paint();

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(color);
            paint.setStrokeWidth(outerWidth);
            paint.setAntiAlias(true);
        }

        public ItemDescriptor(Parcel parcel) {
            this(parcel.readFloat(), parcel.readInt());
        }

        public float getScore() {
            return score;
        }

        public void setScore(float score) {
            this.score = score;
        }

        public Paint getPaint() {
            return paint;
        }

        public void setPaint(Paint paint) {
            this.paint = paint;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(paint.getColor());
            parcel.writeFloat(score);
        }
    }

    /**
     * @return count of items
     */
    public int getItemCount() {
        return items.size();
    }

    /**
     * Adds all items from collection into layout via {@link #addView(android.view.View)} method,
     * see the method for detailed information.
     *
     * @param items to be added to outer cycle
     */
    public void addItems(Collection<Object> items) {
        for (Object item : items)
            addItem(item);
    }


    /**
     * Same as {@link #addItem(Object, float, int)} <br/>
     * with 0 score and {@link android.graphics.Color#BLACK} color
     *
     * @param item to be added
     */
    public void addItem(Object item) {
        addItem(item, 0, Color.BLACK);
    }

    /**
     * Same as {@link #addItem(Object, float, int)} <br/>
     * with 0 score
     *
     * @param item to be added
     */
    public void addItem(Object item, int color) {
        addItem(item, 0, color);
    }

    /**
     * Same as {@link #addItem(Object, float, int)} <br/>
     * with {@link android.graphics.Color#BLACK} color
     *
     * @param item to be added
     */
    public void addItem(Object item, float score) {
        addItem(item, score, 0);
    }

    /**
     * Adds new item with information to outer view. Item should be any object and mustn't be
     * same with other items. In resulted output there will be Arc displayed for each item based
     * on its current score. Therefore is needed to input item score and item color so the user
     * can distinguish differences between items.
     *
     * @param item  to be added
     * @param score initial score of item
     * @param color color representing item
     */
    public void addItem(Object item, float score, int color) {
        ItemDescriptor itemDescriptor = new ItemDescriptor(score, color);
        items.put(item, itemDescriptor);
    }

    /**
     * @return Sum of scores of all items
     */
    private int getSum() {
        int sum = 0;
        for (Map.Entry<Object, ItemDescriptor> item : items.entrySet()) {
            sum += item.getValue().getScore();
        }
        return sum;
    }

    /**
     * Deprecated: use get/set ItemScore/ItemColor instead
     *
     * @param item of which descriptor should be obtained
     * @return the descriptor of the item, or null if item not presented.
     */
    @Deprecated
    public ItemDescriptor getDescriptor(Object item) {
        return items.get(item);
    }

    /**
     * @param item added to outer view to obtain score from
     * @return score of item
     */
    public float getItemScore(Object item) {
        return items.get(item).getScore();
    }

    /**
     * @param item  added to outer view to set score to
     * @param score to set
     */
    public void setItemScore(Object item, float score) {
        items.get(item).setScore(score);
        postInvalidate();
    }

    /**
     * @param item  added to outer view to add score to
     * @param score addition
     */
    public void addItemScore(Object item, float score) {
        ItemDescriptor descriptor = items.get(item);
        descriptor.setScore(descriptor.getScore() + score);
        postInvalidate();
    }

    /**
     * @param item added to outer view to obtain color from
     * @return color of item
     */
    public int getItemColor(Object item) {
        return items.get(item).getPaint().getColor();
    }

    /**
     * @param item  added to outer view to set color to
     * @param color to set
     */
    public void setItemColor(Object item, int color) {
        items.get(item).getPaint().setColor(color);
        postInvalidate();
    }

    //////////////////////////////////////////////
    // SINGLE CHILD RESTRICTION                 //
    //////////////////////////////////////////////
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
    public void addView(@SuppressWarnings("NullableProblems") View child, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0)
            throw new IllegalStateException("CircularView can host only one direct child");
        super.addView(child, params);
    }

    @Override
    public void addView(@SuppressWarnings("NullableProblems") View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0)
            throw new IllegalStateException("CircularView can host only one direct child");
        super.addView(child, index, params);
    }

    //////////////////////////////////////////////
    // LAYING DOWN THE VIEW                     //
    //////////////////////////////////////////////
    // TODO: implement pading
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // whole view width and height
        int size = Math.min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));

        // compute outer cycle bounds (need to cut width of outer cycle because of drawArc method)
        outerBounds.left = outerWidth / 2;
        outerBounds.top = outerWidth / 2;
        outerBounds.right = size - outerWidth / 2;
        outerBounds.bottom = size - outerWidth / 2;

        // compute inner cycle bounds
        innerBounds.left = outerWidth - 2;
        innerBounds.top = outerWidth - 2;
        innerBounds.right = size - outerWidth + 2;
        innerBounds.bottom = size - outerWidth + 2;

        centerX = (outerBounds.left + outerBounds.right) / 2;
        centerY = (outerBounds.bottom + outerBounds.top) / 2;
        innerRadius = (innerBounds.right - innerBounds.left) / 2;
        outerRadius = (outerBounds.right - outerBounds.left + outerWidth) / 2;

        // compute clip path for inner view (added 2 pixels so the child seems antialliased)
        float center = (innerBounds.right + innerBounds.left) / 2;
        if (!clipPath.isEmpty()) clipPath.reset();
        clipPath.addCircle(center, center, center - outerWidth + 5, Path.Direction.CW);

        // measure down the view(s)
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child != null)
                measureChild(child, size - outerWidth, size - outerWidth);
        }

        setMeasuredDimension(resolveSize(size, widthMeasureSpec), resolveSize(size, widthMeasureSpec));
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        LayoutParams params = (LayoutParams) child.getLayoutParams();
        if (params == null) {
            params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }
        params.x = innerBounds.left;
        params.y = innerBounds.top;
        params.width = (int) innerRadius * 2;
        params.height = (int) innerRadius * 2;
        child.setLayoutParams(params);
        child.measure(parentWidthMeasureSpec, parentHeightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean b, int left, int top, int right, int bottom) {
        // lay down view(s) into inner bounds
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child != null) {
                LayoutParams params = (LayoutParams) child.getLayoutParams();
                child.layout(params.x, params.y, params.x + params.width, params.x + params.height);
            }
        }
    }

    /**
     * This method is responsible for drawing child(s) into clip path. For inspiration special thanks
     * to <a href="http://stackoverflow.com/a/24040115/2316926">budius</a>
     * <p/>
     * After child is drawn outer cycle will have to be drawn. This is not implemented in onDraw method,
     * because the child is not antialiased and outer cycle will cover the bad looking edges
     *
     * @param canvas      The canvas on which to draw the child
     * @param child       Who to draw
     * @param drawingTime The time at which draw is occurring
     * @return True if an invalidate() was issued
     */
    @Override
    protected boolean drawChild(@SuppressWarnings("NullableProblems") Canvas canvas, @SuppressWarnings("NullableProblems") View child, long drawingTime) {
        // draw inner circle
        canvas.save();
        canvas.clipPath(clipPath, Region.Op.REPLACE);
        boolean result = super.drawChild(canvas, child, drawingTime);
        canvas.restore();

        // draw outer circle
        if (outerWidth <= 0) return result;
        float beg = 0;
        float end;
        if (items != null && items.size() > 0 && getSum() != 0)
            for (Map.Entry<Object, ItemDescriptor> item : items.entrySet()) {
                end = (item.getValue().getScore() / getSum()) * 360;
                canvas.drawArc(outerBounds, beg + scroll, end, false, item.getValue().getPaint());
                beg += end;
            }
        else {
            canvas.drawArc(outerBounds, beg, 360, false, defaultPaint);
        }

        return result;
    }

    /**
     * Whole layout drawing is placed into {@link #drawChild(android.graphics.Canvas, android.view.View, long)}
     * because child layout is placed below outer cycle. This is needed because of suppressed possibilty
     * to set clip bounds as antialiased
     *
     * @param canvas to draw layout to
     */
    @Override
    public void onDraw(Canvas canvas) {
    }

    //////////////////////////////////////
    //  Layout parameters Processing    //
    //////////////////////////////////////
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p.width, p.height);
    }

    /**
     * This layout parameters holds on addition position of child item.
     */
    public static class LayoutParams extends ViewGroup.LayoutParams {
        int x;
        int y;

        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public LayoutParams(int w, int h) {
            super(w, h);
        }
    }

    //////////////////////////////////////////////
    // STATE SAVING AND RESTORATION             //
    //////////////////////////////////////////////
    static class SavedState extends BaseSavedState {
        int scroll;
        HashMap items;


        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.scroll = in.readInt();
            this.items = in.readHashMap(Object.class.getClassLoader());
        }

        @Override
        public void writeToParcel(@SuppressWarnings("NullableProblems") Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.scroll);
            out.writeMap(this.items);
        }

        //required field that makes Parcelables from a Parcel
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.scroll = this.scroll;
        ss.items = this.items;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        this.scroll = ss.scroll;
        this.items = (HashMap<Object, ItemDescriptor>) ss.items;
        this.mGestureListener.setScroll(scroll);
    }

    //////////////////////////////
    //  Tracking item click     //
    //////////////////////////////
    private float startX;
    private float startY;
    private boolean shouldScroll;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mIsScrolling = false;
                shouldScroll = false;
                // intercept if ondown is outside radius
                float distance = getDistanceFromCenter(ev.getX(), ev.getY());
                if (distance > innerRadius && distance <= outerRadius) {
                    startX = ev.getX();
                    startY = ev.getY();
                    shouldScroll = true;
                    return true;
                }
                else if (distance > outerRadius) return true; // cannot scroll but child cannot proceed too
                break;
            case MotionEvent.ACTION_MOVE: {
                if (!shouldScroll) break;
                if (mIsScrolling) {
                    return true;
                }
                final float xDiff = Math.max(
                        Math.abs(ev.getX() - startX),
                        Math.abs(ev.getY() - startY));
                if (xDiff > mTouchSlop) {
                    mIsScrolling = true;
                    return true;
                }
                break;
            }
        }
        return false;
    }

    private float getDistanceFromCenter(float x, float y) {
        float xDist = x - centerX;
        float yDist = y - centerY;
        return (float) Math.sqrt(xDist * xDist + yDist * yDist);
    }

    @Override
    public boolean onTouchEvent(@SuppressWarnings("NullableProblems") MotionEvent event) {
        return shouldScroll && gestureDetector.onTouchEvent(event);
    }

    /**
     * Special gesture listener to track events in circle divided into 4 parts (quadrants). In
     * every quadrant is event processed differently (for example to move clockwise in top left
     * fragment X must increase and Y decrease). Result scroll is computed as average of X and
     * Y scroll values.
     */
    private class CircularGestureListener extends GestureDetector.SimpleOnGestureListener {
        private final Pair TOP_LEFT_QUADRANT = new Pair(true, false);
        private final Pair TOP_RIGHT_QUADRANT = new Pair(true, true);
        private final Pair BOTTOM_RIGHT_QUADRANT = new Pair(false, true);
        private final Pair BOTTOM_LEFT_QUADRANT = new Pair(false, false);

        private Scroller mScroller;
        private boolean resetScroll = true;

        private Pair quadrant;

        public CircularGestureListener() {
            mScroller = new Scroller(getContext());
        }

        private class Pair {
            public static final int POSITIVE = -1;
            public static final int NEGATIVE = 1;

            int x;
            int y;

            private Pair(boolean x, boolean y) {
                this.x = x ? POSITIVE : NEGATIVE;
                this.y = y ? POSITIVE : NEGATIVE;
            }
        }

        public void setScroll(int scroll) {
            mScroller.setFinalX(scroll);
            mScroller.setFinalY(scroll);
        }

        private Pair getQuadrant(float lastX, float lastY) {
            float centerX = (outerBounds.right - outerBounds.left) / 2;
            float centerY = (outerBounds.bottom - outerBounds.top) / 2;
            if (lastX <= centerX && lastY <= centerY)
                return TOP_LEFT_QUADRANT;
            if (lastX >= centerX && lastY <= centerY)
                return TOP_RIGHT_QUADRANT;
            if (lastX >= centerX && lastY >= centerY)
                return BOTTOM_RIGHT_QUADRANT;
            if (lastX <= centerX && lastY >= centerY)
                return BOTTOM_LEFT_QUADRANT;
            else
                throw new UnknownError();
        }

        @Override
        public boolean onDown(MotionEvent event) {
            mScroller.forceFinished(true);
            quadrant = getQuadrant(event.getX(), event.getY());
            ViewCompat.postInvalidateOnAnimation(CircularView.this);
            resetScroll = true;
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            mScroller.forceFinished(true);
            mScroller.fling(
                    mScroller.getFinalX(),
                    mScroller.getFinalY(),
                    (int) -((quadrant.x) * velocityX),
                    (int) -((quadrant.y) * velocityY),
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE);
            ViewCompat.postInvalidateOnAnimation(CircularView.this);
            quadrant = getQuadrant(event2.getX(), event2.getY());
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (resetScroll) {
                distanceX = 0;
                distanceY = 0;
                resetScroll = false;
            }
            mScroller.startScroll(
                    mScroller.getFinalX(),
                    mScroller.getFinalY(),
                    (int) ((quadrant.x) * distanceX),
                    (int) ((quadrant.y) * distanceY), 0);
            ViewCompat.postInvalidateOnAnimation(CircularView.this);
            quadrant = getQuadrant(e2.getX(), e2.getY());
            return true;
        }

        public boolean computeScroll() {
            boolean result = mScroller.computeScrollOffset();
            scrollTo((mScroller.getCurrX() + mScroller.getCurrY()) / 2);
            return result;
        }
    }

    /**
     * Scroll circle to defined angle. This is always computed as modulo 360
     *
     * @param angle to scroll to
     */
    public void scrollTo(int angle) {
        scroll = angle % 360;
    }

    @Override
    public void scrollTo(int x, int y) {
        throw new IllegalStateException("Method not supported, use scrollTo(int) instead");
    }

    @Override
    public void computeScroll() {
        if (mGestureListener.computeScroll()) ViewCompat.postInvalidateOnAnimation(this);
    }
}