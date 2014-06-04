package sk.kandrac.circularview;

import android.graphics.Paint;

/**
 * Created by VizGhar on 2.6.2014.
 */
public class ItemDescriptor {

    private float score;
    private Paint paint;

    public ItemDescriptor(float score, Paint paint) {
        this.score = score;
        this.paint = paint;
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
}
