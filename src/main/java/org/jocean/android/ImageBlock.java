/**
 * 
 */
package org.jocean.android;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * @author isdom
 *
 */
public class ImageBlock {
    public ImageBlock(final int left, final int top, int w, int h, final Bitmap bitmap) {
        this._left = left;
        this._top = top;
        this._width = w;
        this._height = h;
        this._bitmap = bitmap;
        this._src = new Rect(0, 0, w, h);
    }

    public void draw(final Canvas canvas, final int left, int top, final Paint paint) {
        this._dest.set(this._left + left, this._top + top, 
                this._left + left + this._width, this._top + top + this._height);
        
        canvas.drawBitmap(this._bitmap, this._src, this._dest, paint);
    }
    
    private final int _left;
    private final int _top;
    private final int _width;
    private final int _height;
    
    private final Rect _src;
    private final Rect _dest = new Rect();
    
    private final Bitmap _bitmap;
}
