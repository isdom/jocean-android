/**
 * 
 */
package org.jocean.android;

import org.jocean.image.RawImage;
import org.jocean.image.RawImage.PixelArrayDrawer;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * @author isdom
 *
 */
public class RawImageDrawable extends Drawable {

    private static final PixelArrayDrawer<Canvas> _DRAWER = new PixelArrayDrawer<Canvas>() {

        @Override
        public void drawPixelArray(Canvas canvas, int[] colors, int offset,
                int stride, float x, float y, int width, int height,
                boolean hasAlpha) {
            canvas.drawBitmap(colors, offset, stride, x, y, width, height, hasAlpha, null);
        }
    };
    
    public RawImageDrawable(final RawImage rawimg) {
        this._img = rawimg.retain();
    }
    
    public void recycle() {
        this._img.release();
    }
    
    public RawImage rawImage() {
        return this._img;
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
        this._sx = (float)bounds.width() / this._img.getWidth();
        this._sy = (float)bounds.height() / this._img.getHeight();
    }

    @Override
    public void draw(final Canvas canvas) {
        canvas.save();
        
        try {
            canvas.scale(this._sx, this._sy, this._bounds.left, this._bounds.top);
            this._img.drawDirect(_DRAWER, canvas, this._bounds.left, this._bounds.top);
        }
        finally {
            canvas.restore();
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
        return this._img.getWidth(); //this._width;
    }

    @Override
    public int getIntrinsicHeight() {
        return this._img.getHeight(); //this._height;
    }

    protected final RawImage _img;
    protected final Rect _bounds = new Rect();
    protected float _sx = 1.0f;
    protected float _sy = 1.0f;

//    private int _width;
//    private int _height;
//    private int mTargetDensity;
}
