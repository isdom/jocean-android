/**
 * 
 */
package org.jocean.android;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * @author isdom
 *
 */
public class RawImageDrawable extends Drawable {

    public RawImageDrawable(final RawImage rawimg) {
        this._img = rawimg.retain();
    }
    
    public void recycle() {
        this._img.release();
    }

//    public void setTargetDensity(DisplayMetrics metrics) {
//         mTargetDensity = metrics.densityDpi;
//         computeBitmapSize();
//    }
//    
//    private void computeBitmapSize() {
//        this._width = mBitmap.getScaledWidth(mTargetDensity);
//        this._height = mBitmap.getScaledHeight(mTargetDensity);
//   }
    
    @Override
    protected void onBoundsChange(final Rect bounds) {
        this._bounds.set(bounds);
    }

    @Override
    public void draw(final Canvas canvas) {
        this._img.drawScale(canvas, this._bounds);
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
        return this._img.getWidth(); //this._width;
    }

    @Override
    public int getIntrinsicHeight() {
        return this._img.getHeight(); //this._height;
    }

    private final RawImage _img;
    private final Rect _bounds = new Rect();
//    private int _width;
//    private int _height;
//    private int mTargetDensity;
}
