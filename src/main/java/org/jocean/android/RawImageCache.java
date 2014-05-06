/**
 * 
 */
package org.jocean.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jocean.android.agent.impl.Md5;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.image.RawImage;
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
public final class RawImageCache<KEY>  {
    
    private static final Logger LOG = 
            LoggerFactory.getLogger(RawImageCache.class);
    
    public RawImageCache(final int maxInMemorySize, final DiskLruCache diskCache ) {
        this._memoryCache = new LruCache<KEY, RawImage>(maxInMemorySize) {
            @Override
            protected void entryRemoved(
                    final boolean evicted, 
                    final KEY key, 
                    final RawImage oldValue,
                    final RawImage newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
                if ( LOG.isTraceEnabled() ) {
                    LOG.trace("entryRemoved key:{}/newValue:{},\r\n release oldValue:{},\r\n now totalSize:({})KBytes", 
                            key, newValue, oldValue, size() / 1024.0f);
                }
                try {
                    saveToDisk(key, oldValue);
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
            protected int sizeOf(final KEY key, final RawImage img) {
                return img.getSizeInByte();
            }
        };
        this._diskCache = diskCache;
    }
    
    public RawImage put( final KEY key, final RawImage image) {
        //  TODO, sync or async record to DiskCache
        return this._memoryCache.put(key, image.retain());
    }
    
    public RawImage get(final KEY key) {
        final RawImage img = this._memoryCache.get(key);
        if ( null == img ) {
            return tryLoadFromDisk(key);
        }
        else {
            return img;
        }
    }
    
    public RawImage tryLoadFromDisk(final KEY key) {
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
                        final RawImage img = RawImage.decodeFrom(is);
                        if ( null != img ) {
                            try {
                                if ( LOG.isTraceEnabled() ) {
                                    LOG.trace("tryLoadFromDisk: load RawImage({}) from disk for key({})", img, key);
                                }
                                return put(key, img);
                            }
                            finally {
                                img.release();
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
    private void saveToDisk(
            final KEY key,
            final RawImage img) throws Exception {
        if ( null != this._diskCache ) {
            final Editor editor = this._diskCache.edit(Md5.encode( key.toString()));
            OutputStream os = null;
            if ( null != editor ) {
                try {
                    os = editor.newOutputStream(0);
                    img.encodeTo(os);
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
    
    public void remove(final KEY key) {
        this._memoryCache.remove(key);
    }

    private final LruCache<KEY, RawImage> _memoryCache;
    private final DiskLruCache _diskCache;
    
}
