package org.jocean.android;

import org.jocean.idiom.Detachable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class ImageBlocksDrawable extends Drawable implements Detachable {

    public ImageBlocksDrawable(final int w, final int h, final ImageBlock[] blocks) {
        this._width = w;
        this._height = h;
        this._blocks = blocks;
    }

    @Override
    public void detach() throws Exception {
//        this._img.release();
    }

//    public RawImage rawImage() {
//        return this._img;
//    }

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
        this._sx = (float) bounds.width() / this._width;
        this._sy = (float) bounds.height() / this._height;
    }

    @Override
    public void draw(final Canvas canvas) {
        final int saveCount = canvas.save();

        try {
            canvas.scale(this._sx, this._sy, this._bounds.left,
                    this._bounds.top);
            for ( ImageBlock block : this._blocks ) {
                block.draw(canvas, this._bounds.left, this._bounds.top, null);
            }
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
        return this._width;
    }

    @Override
    public int getIntrinsicHeight() {
        return this._height;
    }

    protected final Rect _bounds = new Rect();
    protected float _sx = 1.0f;
    protected float _sy = 1.0f;
    
    private final int _width;
    private final int _height;
    private final ImageBlock[] _blocks;

    // private int mTargetDensity;
}
