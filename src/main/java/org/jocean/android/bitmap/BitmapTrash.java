/**
 * 
 */
package org.jocean.android.bitmap;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.os.Build;

/**
 * @author isdom
 *
 */
public class BitmapTrash {
    
    private static final Logger LOG = 
            LoggerFactory.getLogger(BitmapTrash.class);
    /**
     * The recycle policy controls if the {@link android.graphics.Bitmap#recycle()} is automatically
     * called, when it is no longer being used. To set this, use the {@link
     * Builder#setRecyclePolicy(uk.co.senab.bitmapcache.BitmapLruCache.RecyclePolicy)
     * Builder.setRecyclePolicy()} method.
     */
    public static enum RecyclePolicy {
        /**
         * The Bitmap is never recycled automatically.
         */
        DISABLED,

        /**
         * The Bitmap is only automatically recycled if running on a device API v10 or earlier.
         */
        PRE_HONEYCOMB_ONLY,

        /**
         * The Bitmap is always recycled when no longer being used. This is the default.
         */
        ALWAYS;

        @SuppressWarnings("incomplete-switch")
        boolean canInBitmap() {
            switch (this) {
                case PRE_HONEYCOMB_ONLY:
                case DISABLED:
                    return Build.VERSION.SDK_INT >=  11; // Build.VERSION_CODES.HONEYCOMB;
            }
            return false;
        }

        boolean canRecycle() {
            switch (this) {
                case DISABLED:
                    return false;
                case PRE_HONEYCOMB_ONLY:
                    return Build.VERSION.SDK_INT <  11; //Build.VERSION_CODES.HONEYCOMB;
                case ALWAYS:
                    return true;
            }

            return false;
        }
    }
    
    public BitmapTrash(final RecyclePolicy policy) {
        this._recyclePolicy = policy;
        this._reuseBitmaps = policy.canInBitmap()
                ? Collections.synchronizedSet(new HashSet<SoftReference<Bitmap>>())
                : null;
    }
    
    public void recycle(final Bitmap bitmap) {
        if ( null != this._reuseBitmaps && isBitmapValid(bitmap) && isBitmapMutable(bitmap) ) {
            this._reuseBitmaps.add(new SoftReference<Bitmap>(bitmap));
            if ( LOG.isTraceEnabled() ) {
                LOG.trace("BitmapTrash.recycle: add bitmap({})/w:{}/h:{}/cfg:{} to re-used set",
                        bitmap, bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
            }
        }
        else if ( this._recyclePolicy.canRecycle() && isBitmapValid(bitmap) ) {
            // try to recycle
            bitmap.recycle();
            if ( LOG.isTraceEnabled() ) {
                LOG.trace("BitmapTrash.recycle: invoke bitmap({}).recycle", bitmap);
            }
        }
        else {
            if ( LOG.isTraceEnabled() ) {
                LOG.trace("BitmapTrash.recycle: do nothing for bitmap({})", bitmap);
            }
        }
    }
    
    public Bitmap findAndReuse(final int width, final int height, final Bitmap.Config config) {
        if ( null == this._reuseBitmaps) {
            return null;
        }

        synchronized (this._reuseBitmaps) {
            final Iterator<SoftReference<Bitmap>> it = this._reuseBitmaps.iterator();

            while (it.hasNext()) {
                final Bitmap bitmap = it.next().get();

                if ( null != bitmap && isBitmapValid(bitmap) && isBitmapMutable(bitmap)) {
                    if (bitmap.getWidth() == width
                            && bitmap.getHeight() == height
                            && bitmap.getConfig().equals(config)) {
                        it.remove();
                        if ( LOG.isTraceEnabled() ) {
                            LOG.trace("BitmapTrash.findAndReuse: found bitmap({}) match width:{}, height:{}, config:{}, just reuse", 
                                    bitmap, width, height, config);
                        }
                        return bitmap;
                    }
                } else {
                    it.remove();
                    if ( LOG.isTraceEnabled() ) {
                        LOG.trace("BitmapTrash.findAndReuse: bitmap({}) has been gc or invalid, remove from reuse bitmaps", 
                                bitmap);
                    }
                }
            }
            
            if ( LOG.isTraceEnabled() ) {
                LOG.trace("BitmapTrash.findAndReuse: total reuse bitmaps's size:{}", 
                        this._reuseBitmaps.size());
            }
        }

        return null;
    }
    
    private static boolean isBitmapValid(final Bitmap bitmap) {
        return null != bitmap && !bitmap.isRecycled();
    }

    private static boolean isBitmapMutable(final Bitmap bitmap) {
        return null != bitmap && bitmap.isMutable();
    }
    
    private final RecyclePolicy _recyclePolicy;
    private final Set<SoftReference<Bitmap>> _reuseBitmaps;
}
