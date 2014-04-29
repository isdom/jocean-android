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
        this._img = rawimg;
    }
    
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

    private final RawImage _img;
    private final Rect _bounds = new Rect();
}
