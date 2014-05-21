package org.jocean.android.bitmap;

import org.jocean.idiom.Detachable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;

public class CompositeBitmapDrawable extends BitmapDrawable 
    implements Detachable {

    @SuppressWarnings("unused")
    private static final Logger LOG = 
            LoggerFactory.getLogger(CompositeBitmapDrawable.class);
    
    public CompositeBitmapDrawable(final Resources resources, final CompositeBitmap cb) {
        super(resources, cb.getBitmap());
        
        this._compositeBitmap = cb.retain();
    }

    
    @Override
    public void detach() {
        this._compositeBitmap.release();
    }

    public CompositeBitmap compositeBitmap() {
        return this._compositeBitmap;
    }
    
    private final CompositeBitmap _compositeBitmap;
}
