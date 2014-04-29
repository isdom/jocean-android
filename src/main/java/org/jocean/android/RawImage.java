package org.jocean.android;

import org.jocean.idiom.block.IntsBlob;

import android.graphics.Canvas;

public class RawImage {
    public RawImage(final int w, final int h, final IntsBlob ints) {
        this._width = w;
        this._height = h;
        this._ints = ints.retain();
    }
    
    public int getSizeInByte() {
        return this._ints.length();
    }
    
    public int getWidth() {
        return this._width;
    }

    public int getHeight() {
        return this._height;
    }

    public void draw(final Canvas canvas) {
        int currentx = 0;
        int currenty = 0;
        
        for ( int idx = 0; idx < this._ints.totalBlockCount(); idx++) {
            
            //|         |#############|   <----- top 
            //#########################   <-----+
            //....                              |
            //....                              +-- body
            //....                              |
            //#########################   <-----+
            //#######                     <------ bottom
            final int[] colors = this._ints.getBlockAt(idx);
            int currentoffset = 0;
            int w = 0, h = 0; 
            int restLength = colors.length;
            
            if ( currentx > 0 ) {
                // draw top
                w = Math.min(this._width - currentx, restLength);
                h = 1;
                canvas.drawBitmap(colors, currentoffset, this._width, currentx, currenty, w, h, false, null);
                currentoffset += w;
                restLength -= w;
                currentx += w;
                if ( currentx == this._width ) {
                    // 递进到下一row
                    currentx = 0;
                    currenty++;
                }
            }
            if ( restLength > 0 ) {
                // draw body
                w = Math.min(this._width, restLength);
                h = restLength / w;
                canvas.drawBitmap(colors, currentoffset, this._width, currentx, currenty, w, h, false, null);
                currentoffset += w * h;
                restLength -= w * h;
                if ( h > 1 ) {
                    currentx = 0;
                    currenty += h;
                }
                else {
                    currentx += w;
                    if ( currentx == this._width ) {
                        // 递进到下一row
                        currentx = 0;
                        currenty++;
                    }
                }
            }
            
            if ( restLength > 0 ) {
                // draw bottom
                w = restLength;
                h = 1;
                canvas.drawBitmap(colors, currentoffset, this._width, currentx, currenty, w, h, false, null);
                currentx += w;
                if ( currentx == this._width ) {
                    // 递进到下一row
                    currentx = 0;
                    currenty++;
                }
            }
        }
    }
    
    private final int _width;
    private final int _height;
    private final IntsBlob _ints;
}
