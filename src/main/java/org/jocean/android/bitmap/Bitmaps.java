package org.jocean.android.bitmap;

import java.io.OutputStream;

import org.jocean.event.api.EventReceiverSource;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.pool.BytesPool;
import org.jocean.rosa.api.BlobAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;

import com.jakewharton.disklrucache.DiskLruCache;
import com.jakewharton.disklrucache.DiskLruCache.Editor;

public abstract class Bitmaps {
    
    private static final Logger LOG = LoggerFactory
            .getLogger(Bitmaps.class);
    
    public static BitmapAgent createBitmapAgent(
            final BytesPool bytesPool,
            final EventReceiverSource source,
            final Bitmap.Config config, 
            final BlobAgent blobAgent,
            final CompositeBitmapCache memoryCache,
            final DiskLruCache diskCache
            ) {
        return new BitmapAgentImpl(bytesPool, source, config, blobAgent, memoryCache, diskCache);
    }
    
    static String saveBitmapToDisk(final String key, final CompositeBitmap bitmap, final DiskLruCache diskCache ) {
        try {
            if ( null != diskCache ) {
                final String diskCacheKey = Md5.encode(key);
                if ( null != diskCache.get(diskCacheKey)) {
                    if ( LOG.isTraceEnabled() ) {
                        LOG.trace("bitmap({}) has already save to disk", bitmap);
                    }
                    return genCacheFilename(diskCache, diskCacheKey);
                }
                else {
                    final Editor editor = diskCache.edit(diskCacheKey);
                    OutputStream os = null;
                    if ( null != editor ) {
                        try {
                            os = editor.newOutputStream(0);
                            bitmap.encodeTo(os, CompressFormat.JPEG, 75);
                            return genCacheFilename(diskCache, diskCacheKey);
                        }
                        finally {
                            editor.commit();
                            if ( null != os ) {
                                os.close();
                            }
                        }
                    }
                }
            }
        }
        catch (final Throwable e) {
            LOG.warn("exception while saving bitmap ({}) to disk, detail:{}", 
                    bitmap, ExceptionUtils.exception2detail(e));
        }
        if ( LOG.isTraceEnabled() ) {
            LOG.trace("failed to save bitmap({}) to disk", bitmap);
        }
        return null;
    }

    private static String genCacheFilename(
            final DiskLruCache diskCache,
            final String diskCacheKey) {
        return diskCache.getDirectory().getAbsolutePath() + "/" + diskCacheKey + ".0";
    }
}
