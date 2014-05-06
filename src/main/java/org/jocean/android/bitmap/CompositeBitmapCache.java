/**
 * 
 */
package org.jocean.android.bitmap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jocean.idiom.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.support.v4.util.LruCache;

import com.jakewharton.disklrucache.DiskLruCache;
import com.jakewharton.disklrucache.DiskLruCache.Editor;
import com.jakewharton.disklrucache.DiskLruCache.Snapshot;

/**
 * @author isdom
 * 
 */
public final class CompositeBitmapCache<KEY>  {
    
    private static final Logger LOG = 
            LoggerFactory.getLogger(CompositeBitmapCache.class);
    
    public CompositeBitmapCache(final int maxInMemorySize, final BitmapsPool pool, final DiskLruCache diskCache ) {
        this._memoryCache = new LruCache<KEY, CompositeBitmap>(maxInMemorySize) {
            @Override
            protected void entryRemoved(
                    final boolean evicted, 
                    final KEY key, 
                    final CompositeBitmap oldValue,
                    final CompositeBitmap newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
                if ( LOG.isTraceEnabled() ) {
                    LOG.trace("entryRemoved key:{}/newValue:{},\r\n release oldValue:{},\r\n now totalSize:({})KBytes", 
                            key, newValue, oldValue, size() / 1024.0f);
                }
                try {
                    trySaveToDisk(key, oldValue);
                }
                catch (Exception e) {
                    LOG.error("exception when put image to disk, detail:{}", 
                            ExceptionUtils.exception2detail(e));
                }
                finally {
                    oldValue.release();
                }
            }

            @Override
            protected int sizeOf(final KEY key, final CompositeBitmap cb) {
                return cb.sizeInBytes();
            }
        };
        this._diskCache = diskCache;
        this._pool = pool;
    }
    
    public CompositeBitmap put( final KEY key, final CompositeBitmap cb) {
        //  TODO, sync or async record to DiskCache
        return this._memoryCache.put(key, cb.retain());
    }
    
    public CompositeBitmap get(final KEY key) {
        final CompositeBitmap bitmap = this._memoryCache.get(key);
        if ( null == bitmap ) {
            return tryLoadFromDisk(key);
        }
        else {
            return bitmap;
        }
    }
    
    public CompositeBitmap tryLoadFromDisk(final KEY key) {
        if ( null == this._diskCache ) {
            return null;
        }
        
        try {
            final String diskKey = Md5.encode( key.toString() );
            final Snapshot snapshot = this._diskCache.get(diskKey);
            if ( null != snapshot ) {
                try {
                    final InputStream is = snapshot.getInputStream(0);
                    if ( null != is ) {
                        final CompositeBitmap bitmap = CompositeBitmap.decodeFrom(is, this._pool);
                        if ( null != bitmap ) {
                            try {
                                if ( LOG.isTraceEnabled() ) {
                                    LOG.trace("tryLoadFromDisk: load CompositeBitmap({}) from disk for key({})", bitmap, key);
                                }
                                put(key, bitmap);
                                return bitmap;
                            }
                            finally {
                                bitmap.release();
                            }
                        }
                    }
                }finally {
                    snapshot.close();
                }
            }
        }
        catch (Exception e) {
            LOG.error("exception when tryLoadFromDisk for key({}), detail:{} ", 
                    key, ExceptionUtils.exception2detail(e));
        }
        
        return null;
    }

    /**
     * @param diskCache
     * @param key
     * @param img
     * @throws IOException
     * @throws Exception
     */
    private void trySaveToDisk(
            final KEY key,
            final CompositeBitmap bitmap) throws Exception {
        if ( null != this._diskCache ) {
            if ( LOG.isTraceEnabled() ) {
                LOG.trace("trySaveToDisk: key({})/bitmap({}) NOT save to disk before, try to save.", key, bitmap);
            }
            final String md5 = Md5.encode( key.toString());
            if ( null == this._diskCache.get(md5) ) {
                final Editor editor = this._diskCache.edit(md5);
                OutputStream os = null;
                if ( null != editor ) {
                    try {
                        os = editor.newOutputStream(0);
                        bitmap.encodeTo(os);
                        if ( LOG.isTraceEnabled() ) {
                            LOG.trace("trySaveToDisk: save key({}) to disk succeed", key);
                        }
                    }
                    finally {
                        editor.commit();
                        if ( null != os ) {
                            os.close();
                        }
                    }
                }
            }
            else {
                if ( LOG.isTraceEnabled() ) {
                    LOG.trace("trySaveToDisk: key({})/bitmap({}) already save to disk cache.", key, bitmap);
                }
            }
        }
    }
    
    public void remove(final KEY key) {
        this._memoryCache.remove(key);
    }

    private final LruCache<KEY, CompositeBitmap> _memoryCache;
    private final DiskLruCache _diskCache;
    private final BitmapsPool _pool;
}
