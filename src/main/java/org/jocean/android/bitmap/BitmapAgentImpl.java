/**
 * 
 */
package org.jocean.android.bitmap;

import java.net.URI;

import org.jocean.event.api.EventReceiverSource;
import org.jocean.rosa.api.BlobAgent;

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
            final CompositeBitmapCache memoryCache,
            final DiskLruCache diskCache
            ) {
        if ( null == memoryCache ) {
            throw new NullPointerException("memoryCache must be not null.");
        }
        this._source = source;
        this._pool = pool;
        this._blobAgent = blobAgent;
        this._memoryCache = memoryCache;
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
    private final CompositeBitmapCache _memoryCache;
    private final DiskLruCache _diskCache;
    
    private final EventReceiverSource _source;
}
