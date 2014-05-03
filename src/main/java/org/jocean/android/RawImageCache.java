/**
 * 
 */
package org.jocean.android;

import org.jocean.image.RawImage;

import android.support.v4.util.LruCache;

/**
 * @author isdom
 * 
 */
public class RawImageCache extends LruCache<String, RawImage> {
    
    public RawImageCache(final int maxSize) {
        super(maxSize);
    }

    @Override
    protected void entryRemoved(
            final boolean evicted, 
            final String key, 
            final RawImage oldValue,
            final RawImage newValue) {
        super.entryRemoved(evicted, key, oldValue, newValue);
        oldValue.release();
    }

    @Override
    protected int sizeOf(final String key, final RawImage img) {
        return img.getSizeInByte();
    }
}
