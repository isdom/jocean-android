/**
 * 
 */
package org.jocean.android;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * @author isdom
 *
 */
public class BitmapBlock {
    public BitmapBlock(int w, int h) {
        this._left = 0;
        this._top = 0;
        this._width = w;
        this._height = h;
        this._bitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        this._src = new Rect(0, 0, w, h);
    }
    
    public BitmapBlock(final int left, final int top, int w, int h, final Bitmap bitmap) {
        this._left = left;
        this._top = top;
        this._width = w;
        this._height = h;
        this._bitmap = bitmap;
        this._src = new Rect(0, 0, w, h);
    }
    
    public BitmapBlock set(final int left, final int top, final int w, int h) {
        this._left = left;
        this._top = top;
        this._width = w;
        this._height = h;
        this._src.set(0, 0, w, h);
        return this;
    }

    public Bitmap bitmap() {
        return this._bitmap;
    }
    
    public int sizeInBytes() {
        return  this._bitmap.getRowBytes() * this._bitmap.getHeight();
    }
    
    public void draw(final Canvas canvas, final int left, int top, final Paint paint) {
        this._dest.set(this._left + left, this._top + top, 
                this._left + left + this._width, this._top + top + this._height);
        
        canvas.drawBitmap(this._bitmap, this._src, this._dest, paint);
    }
    
    private int _left;
    private int _top;
    
    private int _width;
    private int _height;
    
    private final Rect _src;
    private final Rect _dest = new Rect();
    
    private final Bitmap _bitmap;
}
