/**
 * 
 */
package org.jocean.android;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
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
    public void draw(final Canvas canvas) {
        this._img.draw(canvas);
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
}
