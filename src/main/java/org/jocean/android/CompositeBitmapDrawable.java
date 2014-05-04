package org.jocean.android;

import org.jocean.idiom.Detachable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class CompositeBitmapDrawable extends Drawable 
    implements Detachable {

    private static final Logger LOG = 
            LoggerFactory.getLogger(CompositeBitmapDrawable.class);
    
    public CompositeBitmapDrawable(final CompositeBitmap cb) {
        this._compositeBitmap = cb.retain();
    }

    
    @Override
    public void detach() {
        this._compositeBitmap.release();
    }

    public CompositeBitmap compositeBitmap() {
        return this._compositeBitmap;
    }
    
    // public void setTargetDensity(DisplayMetrics metrics) {
    // mTargetDensity = metrics.densityDpi;
    // computeBitmapSize();
    // }
    //
    // private void computeBitmapSize() {
    // this._width = mBitmap.getScaledWidth(mTargetDensity);
    // this._height = mBitmap.getScaledHeight(mTargetDensity);
    // }

    @Override
    protected void onBoundsChange(final Rect bounds) {
        this._bounds.set(bounds);
        this._sx = (float) bounds.width() / this._compositeBitmap.getWidth();
        this._sy = (float) bounds.height() / this._compositeBitmap.getHeight();
    }

    @Override
    public void draw(final Canvas canvas) {
        final int saveCount = canvas.save();

        try {
            canvas.scale(this._sx, this._sy, this._bounds.left,
                    this._bounds.top);
            this._compositeBitmap.draw(canvas, this._bounds.left, this._bounds.top, null);
        } finally {
            canvas.restoreToCount(saveCount);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getOpacity() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getIntrinsicWidth() {
        return this._compositeBitmap.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return this._compositeBitmap.getHeight();
    }

    protected final Rect _bounds = new Rect();
    protected float _sx = 1.0f;
    protected float _sy = 1.0f;
    
    private final CompositeBitmap _compositeBitmap;
    
    // private int mTargetDensity;
}
