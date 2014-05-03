/**
 * 
 */
package org.jocean.android;

import org.jocean.image.RawImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.support.v4.util.LruCache;

/**
 * @author isdom
 * 
 */
public final class RawImageCache<KEY>  {
    
    private static final Logger LOG = 
            LoggerFactory.getLogger(RawImageCache.class);
    
    public RawImageCache(final int maxSize) {
        this._impl = new LruCache<KEY, RawImage>(maxSize) {
            @Override
            protected void entryRemoved(
                    final boolean evicted, 
                    final KEY key, 
                    final RawImage oldValue,
                    final RawImage newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
                if ( LOG.isTraceEnabled() ) {
                    LOG.trace("entryRemoved key:{} and release oldValue:{}, and now totalSize is ({})KBytes", 
                            key, oldValue, size() / 1024.0f);
                }
                oldValue.release();
               
            }

            @Override
            protected int sizeOf(final KEY key, final RawImage img) {
                return img.getSizeInByte();
            }
        };
    }
    
    public RawImage put( final KEY key, final RawImage image) {
        return this._impl.put(key, image.retain());
    }
    
    public RawImage get(final KEY key) {
        return this._impl.get(key);
    }

    public void remove(final KEY key) {
        this._impl.remove(key);
    }
    
    private final LruCache<KEY, RawImage> _impl;
}
