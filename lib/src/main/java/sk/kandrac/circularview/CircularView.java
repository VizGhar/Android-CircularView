package sk.kandrac.circularview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This view is intended to display Circular view composed of 2 circles. Inner circle to show
 * standard content cropped into circle, and outer to display PieChart-like portions of added
 * items. (see addItem() methods and/or sample application).
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

    // bounds of whole view (substracted by half of width of outer circle)
    private RectF innerBounds;

    // rectangular bounds of child view
    private RectF outerBounds;

    // paint used only for layout preview
    private Paint defaultPaint;

    // color used mainly for layout preview or when no item is presented
    private int defaultColor;

    // clip path where child should be placed
    private Path clipPath;

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
        innerBounds = new RectF();
        outerBounds = new RectF();
        clipPath = new Path();
        defaultPaint = new Paint();
        defaultPaint.setColor(defaultColor);
        defaultPaint.setStrokeWidth(outerWidth);
        defaultPaint.setStyle(Paint.Style.STROKE);

        // TODO: when background is not set, view is not drawing Arc. Don't know why
        setBackgroundColor(Color.TRANSPARENT);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        gestureListener = new MyGestureListener();
    }

    //////////////////////////////////////////////
    // OUTER CIRCLE ITEM PROCESSING PART        //
    //////////////////////////////////////////////

    /**
     * @return count of items
     */
    public int getItemCount() {
        return items.size();
    }

    /**
     * Adds all items from collection into layout via {@link #addView(android.view.View)} method,
     * see the method for detailed information.
     * @param items to be added to outer cycle
     */
    public void addItems(Collection<Object> items) {
        for (Object item : items)
            addItem(item);
    }


    /**
     * Same as {@link #addItem(Object, float, int)} <br/>
     * with 0 score and {@link android.graphics.Color#BLACK} color
     * @param item to be added
     */
    public void addItem(Object item) {
        addItem(item, 0, Color.BLACK);
    }

    /**
     * Same as {@link #addItem(Object, float, int)} <br/>
     * with 0 score
     * @param item to be added
     */
    public void addItem(Object item, int color) {
        addItem(item, 0, color);
    }

    /**
     * Same as {@link #addItem(Object, float, int)} <br/>
     * with {@link android.graphics.Color#BLACK} color
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
     * @param item to be added
     * @param score initial score of item
     * @param color color representing item
     */
    public void addItem(Object item, float score, int color) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(outerWidth);
        paint.setAntiAlias(true);

        ItemDescriptor itemDescriptor = new ItemDescriptor(score, paint);
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
     * @param item of which descriptor should be obtained
     * @return the descriptor of the item, or null if item not presented.
     */
    @Deprecated
    public ItemDescriptor getDescriptor(Object item) {
        return items.get(item);
    }

    public float getItemScore(Object item){
        return items.get(item).getScore();
    }

    public void setItemScore(Object item, float score){
        items.get(item).setScore(score);
        postInvalidate();
    }

    public void addItemScore(Object item, float score){
        ItemDescriptor descriptor = items.get(item);
        descriptor.setScore(descriptor.getScore() + score);
        postInvalidate();
    }

    public int getItemColor(Object item){
        return items.get(item).getPaint().getColor();
    }

    public void setItemColor(Object item, int color){
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

    //////////////////////////////////////////////
    // LAYING DOWN THE VIEW                     //
    //////////////////////////////////////////////
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // whole view width and height
        int size = Math.min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));

        // compute outer cycle bounds (need to cut width of outer cycle because of drawArc method)
        outerBounds.left = outerWidth/2;
        outerBounds.top = outerWidth/2;
        outerBounds.right = size-outerWidth/2;
        outerBounds.bottom = size-outerWidth/2;

        // compute inner cycle bounds
        innerBounds.left = outerWidth;
        innerBounds.top = outerWidth;
        innerBounds.right = size - outerWidth;
        innerBounds.bottom = size - outerWidth;

        // compute clip path for inner view (added 2 pixels so the child seems antialliased)
        float center = (innerBounds.right + innerBounds.left)/2;
        if (!clipPath.isEmpty()) clipPath.reset();
        clipPath.addCircle(center, center, center - outerWidth+2, Path.Direction.CW);

        // measure down the view(s)
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child!=null)
                measureChild(child, size - outerWidth, size - outerWidth);
        }

        // set dimension for whole view
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onLayout(boolean b, int left, int top, int right, int bottom) {
        // lay down view(s) into inner bounds
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child!=null)
                child.layout((int) innerBounds.left, (int) innerBounds.top, (int) innerBounds.right, (int) innerBounds.bottom);
        }
    }

    /**
     * This method is responsible for drawing child(s) into clip path. For inspiration special thanks
     * to <a href="http://stackoverflow.com/a/24040115/2316926">budius</a>
     *
     * After child is drawn outer cycle will have to be drawn. This is not implemented in onDraw method,
     * because the child is not antialiased and outer cycle will cover the bad looking edges
     *
     * @param canvas The canvas on which to draw the child
     * @param child Who to draw
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
        float beg = 0;
        float end;
        if (items != null && items.size() > 0 && getSum() != 0)
            for (Map.Entry<Object, ItemDescriptor> item : items.entrySet()) {
                end = (item.getValue().getScore() / getSum()) * 360;
                canvas.drawArc(outerBounds, beg + gestureListener.getScroll(), end, false, item.getValue().getPaint());
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
     * @param canvas
     */
    @Override
    public void onDraw(Canvas canvas) {
        // view drown in drawChild()
    }

    // TODO: infinite (periodic) scroll and 4 quadrants
    class MyGestureListener extends GestureDetector.SimpleOnGestureListener implements OnTouchListener {

        private Scroller mScroller;
        private GestureDetector gesture;

        private int minScroll = Integer.MIN_VALUE;
        private int maxScroll = Integer.MAX_VALUE;

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
                return mScroller.getCurrX() % 360;
            }
            return mScroller.getFinalX() % 360;
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

        public void setScroll(int scroll) {
            this.mScroller.setFinalX(scroll);
        }
    }

    @Override
    public void computeScroll() {
        gestureListener.getScroll();
    }


    //////////////////////////////////////////////
    // STATE SAVING AND RESTORATION             //
    //////////////////////////////////////////////
    static class SavedState extends BaseSavedState {
        int scroll;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.scroll = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.scroll);
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

        ss.scroll = this.gestureListener.getScroll();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState)state;
        super.onRestoreInstanceState(ss.getSuperState());

        this.gestureListener.setScroll(ss.scroll);
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
