/**
 * 
 */
package org.jocean.android.bitmap;

import java.net.URI;

import org.jocean.event.api.EventReceiverSource;
import org.jocean.rosa.api.BlobAgent;

import android.support.v4.util.LruCache;

import com.jakewharton.disklrucache.DiskLruCache;

/**
 * @author isdom
 *
 */
class BitmapAgentImpl implements BitmapAgent {

    public BitmapAgentImpl(
            final EventReceiverSource source,
            final BitmapsPool pool, 
            final BlobAgent blobAgent,
            final int maxMemoryCacheSizeInBytes,
            final DiskLruCache diskCache
            ) {
        this._source = source;
        this._pool = pool;
        this._blobAgent = blobAgent;
        this._memoryCache = new LruCache<String, CompositeBitmap>(maxMemoryCacheSizeInBytes) {
            @Override
            protected void entryRemoved(
                    final boolean evicted, 
                    final String key, 
                    final CompositeBitmap oldValue,
                    final CompositeBitmap newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
                oldValue.release();
            }

            @Override
            protected int sizeOf(final String key, final CompositeBitmap cb) {
                return cb.sizeInBytes();
            }
        };
        this._diskCache = diskCache;
    }
    
    @Override
    public BitmapTransaction createBitmapTransaction() {
        final BitmapTransactionFlow flow = 
            new BitmapTransactionFlow(
                    this._pool, this._blobAgent, 
                    this._memoryCache, this._diskCache);
        _source.create(flow, flow.WAIT);
        
        return flow.queryInterfaceInstance(BitmapTransaction.class);
    }

    @Override
    public CompositeBitmap tryRetainFromMemorySync(final URI uri) {
        final CompositeBitmap bitmap = this._memoryCache.get(uri.toASCIIString());
        if ( null != bitmap ) {
            return bitmap.tryRetain();
        }
        else {
            return null;
        }
    }
    
    private final BitmapsPool _pool;
    private final BlobAgent _blobAgent;
    private final LruCache<String, CompositeBitmap> _memoryCache;
    private final DiskLruCache _diskCache;
    
    private final EventReceiverSource _source;
}
