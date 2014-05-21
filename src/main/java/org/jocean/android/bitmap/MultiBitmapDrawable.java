package org.jocean.android.bitmap;

import org.jocean.idiom.Detachable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class MultiBitmapDrawable extends Drawable 
    implements Detachable {

    @SuppressWarnings("unused")
    private static final Logger LOG = 
            LoggerFactory.getLogger(MultiBitmapDrawable.class);
    
    public MultiBitmapDrawable(final MultiBitmap mb) {
        this._multiBitmap = mb.retain();
    }

    
    @Override
    public void detach() {
        this._multiBitmap.release();
    }

    public MultiBitmap compositeBitmap() {
        return this._multiBitmap;
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
        this._sx = (float) bounds.width() / this._multiBitmap.getWidth();
        this._sy = (float) bounds.height() / this._multiBitmap.getHeight();
    }

    @Override
    public void draw(final Canvas canvas) {
        final int saveCount = canvas.save();

        try {
            canvas.scale(this._sx, this._sy, this._bounds.left,
                    this._bounds.top);
            this._multiBitmap.draw(canvas, this._bounds.left, this._bounds.top, null);
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
        return this._multiBitmap.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return this._multiBitmap.getHeight();
    }

    protected final Rect _bounds = new Rect();
    protected float _sx = 1.0f;
    protected float _sy = 1.0f;
    
    private final MultiBitmap _multiBitmap;
    
    // private int mTargetDensity;
}
