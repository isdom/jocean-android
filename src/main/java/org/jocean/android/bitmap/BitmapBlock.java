/**
 * 
 */
package org.jocean.android.bitmap;

import java.util.concurrent.atomic.AtomicReference;

import com.alibaba.fastjson.annotation.JSONField;

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
    public BitmapBlock(int w, int h, Config Config, final int[] clspixels) {
        this._left = 0;
        this._top = 0;
        this._width = w;
        this._height = h;
        this._bitmap = Bitmap.createBitmap(w, h, Config);
        this._src = new Rect(0, 0, w, h);
        this._clspixels = clspixels;
    }
    
    public BitmapBlock set(final int left, final int top, final int w, int h) {
        this._left = left;
        this._top = top;
        this._width = w;
        this._height = h;
        this._src.set(0, 0, w, h);
        return this;
    }

    public void clsBitmap() {
        this._bitmap.setPixels(this._clspixels, 0, this._width, 0, 0, this._width, this._height);
    }
    
    public Bitmap bitmap() {
        return this._bitmap;
    }
    
    public Canvas canvas() {
        
        if ( null == this._canvas.get() ) {
            this._canvas.compareAndSet(null, new Canvas(this._bitmap));
        }
        
        return this._canvas.get();
    }
    
    public int rawSizeInBytes() {
        return  this._bitmap.getRowBytes() * this._bitmap.getHeight();
    }
    
    public void draw(final Canvas canvas, final int left, int top, final Paint paint) {
        this._dest.set(this._left + left, this._top + top, 
                this._left + left + this._width, this._top + top + this._height);
        
        canvas.drawBitmap(this._bitmap, this._src, this._dest, paint);
    }
    
    @JSONField(name = "left")
    public int getLeft() {
        return this._left;
    }
    
    @JSONField(name = "top")
    public int getTop() {
        return this._top;
    }
    
    @JSONField(name = "width")
    public int getWidth() {
        return this._width;
    }
    
    @JSONField(name = "height")
    public int getHeight() {
        return this._height;
    }
    
    public int usedIntCount() {
        return this._width * this._height;
    }
    
    private int _left;
    private int _top;
    
    private int _width;
    private int _height;
    
    private final Rect _src;
    private final Rect _dest = new Rect();
    
    private final Bitmap _bitmap;
    private final AtomicReference<Canvas> _canvas = new AtomicReference<Canvas>();
    private final int[] _clspixels;
}
