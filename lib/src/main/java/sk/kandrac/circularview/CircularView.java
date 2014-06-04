package sk.kandrac.circularview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This view is intended to display Circular view composed of 2 circles. Inner circle to show
 * standard content cropped into circle, and outer to display PieChart-like portions of added
 * objects (see addItem() methods and/or sample application).
 *
 * TODO: OnDataChangeListener to animate changes
 * TODO: Touch event handling
 *
 * Created by VizGhar on 2.6.2014.
 */
public class CircularView extends ViewGroup {

    // list of items percentage of which will be displayed in outer circle
    private HashMap<Object, ItemDescriptor> items = new HashMap<Object, ItemDescriptor>();

    // fragment placed inside
    private Fragment innerFragment;

    // width of outer circle
    private int outerWidth;

    // bounds of whole view
    private RectF mBounds;

    //bounds of child view
    private Rect mBoundsI;

    private Paint defalutPaint;

    private Paint childPaint;

    private int color;

    private View child;

    public int getItemCount() {
        return items.size();
    }

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
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if (this.child == null) {
            this.child = getChildAt(0);
            if (child != null) child.setDrawingCacheEnabled(true);
        }

        int size = Math.min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));

        mBoundsI.left = 0;
        mBoundsI.top = 0;
        mBoundsI.right = size - 2 * outerWidth;
        mBoundsI.bottom = size - 2 * outerWidth;

        mBounds.left = outerWidth / 2;
        mBounds.top = outerWidth / 2;
        mBounds.right = size - outerWidth / 2;
        mBounds.bottom = size - outerWidth / 2;

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
            child.getDrawingCache();
            child.layout(outerWidth, outerWidth, right - left - outerWidth, bottom - top - outerWidth);
        }
    }

    @Override
    public void addView(View child) {
        if (getChildCount() > 0)
            throw new IllegalStateException("CircularView can host only one direct child");
        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        if (getChildCount() > 0)
            throw new IllegalStateException("CircularView can host only one direct child");
        super.addView(child, index);
    }

    @Override
    public void addView(View child, int width, int height) {
        if (getChildCount() > 0)
            throw new IllegalStateException("CircularView can host only one direct child");
        super.addView(child, width, height);
    }

    @Override
    public void addView(View child, LayoutParams params) {
        if (getChildCount() > 0)
            throw new IllegalStateException("CircularView can host only one direct child");
        super.addView(child, params);
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
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
        mBoundsI = new Rect();
        defalutPaint = new Paint();
        defalutPaint.setColor(color);
        defalutPaint.setStrokeWidth(outerWidth);
        defalutPaint.setStyle(Paint.Style.STROKE);

        childPaint = new Paint();
        childPaint.setAntiAlias(true);
        childPaint.setColor(Color.WHITE);

        setBackgroundColor(Color.TRANSPARENT);
        setWillNotCacheDrawing(false);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (child != null) {
            canvas.drawBitmap(getCroppedBitmap(child.getDrawingCache()), outerWidth, outerWidth, new Paint());
            return true;
        }
        return true;
    }

    public Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        childPaint.setXfermode(null);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, childPaint);
        childPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, mBoundsI, mBoundsI, childPaint);
        return output;
    }

    @Override
    public void onDraw(Canvas canvas) {
        float beg = 0;
        float end;
        if (items != null && items.size() > 0 && getSum() != 0)
            for (Map.Entry<Object, ItemDescriptor> item : items.entrySet()) {
                end = (item.getValue().getScore() / getSum()) * 360;
                Log.d("jano", "drawing from " + beg + " to " + (beg + end));
                canvas.drawArc(mBounds, beg, end, false, item.getValue().getPaint());
                beg += end;
            }
        else {
            canvas.drawArc(mBounds, beg, 360, false, defalutPaint);
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
}
