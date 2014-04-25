/**
 * 
 */
package org.jocean.android;

import org.jocean.idiom.AbstractReferenceCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;

/**
 * @author isdom
 *
 */
public final class DefaultBitmapHolder extends AbstractReferenceCounted<BitmapHolder> 
    implements BitmapHolder {

    private static final Logger LOG = 
            LoggerFactory.getLogger(DefaultBitmapHolder.class);
    
    public DefaultBitmapHolder(final Bitmap bitmap) {
        if ( null == bitmap ) {
            throw new NullPointerException("bitmap can't be null");
        }
        this._bitmap = bitmap;
    }
    
    @Override
    public Bitmap bitmap() {
        return this._bitmap;
    }
    
    @Override
    protected void deallocate() {
        if ( this._bitmap.isRecycled() ) {
            LOG.error("Internal Error: bitmap({}) has already recycled", this._bitmap);
        }
        else {
            this._bitmap.recycle();
        }
    }
    
    private final Bitmap _bitmap;
}
