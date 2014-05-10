/**
 * 
 */
package org.jocean.android.bitmap;

import java.net.URI;

import org.jocean.event.api.AbstractFlow;
import org.jocean.event.api.BizStep;
import org.jocean.event.api.EventReceiverSource;
import org.jocean.event.api.annotation.OnEvent;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.rosa.api.BlobAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jakewharton.disklrucache.DiskLruCache;

/**
 * @author isdom
 *
 */
class BitmapAgentImpl implements BitmapAgent {

    private static final Logger LOG = LoggerFactory
            .getLogger(BitmapAgentImpl.class);

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
        return this._memoryCache.getAndTryRetain(uri.toASCIIString());
    }
    
    private final class RemoveFlow extends AbstractFlow<RemoveFlow> {
        final BizStep DO = new BizStep("remove.DO")
            .handler(selfInvoker("onRemove"))
            .freeze();

        @OnEvent(event="remove")
        private BizStep onRemove(final URI uri) throws Exception {
            if ( null != _diskCache ) {
                if ( _diskCache.remove(Md5.encode(uri.toASCIIString())) ) {
                    if ( LOG.isTraceEnabled() ) {
                        LOG.trace("BitmapAgent: remove bitmap for uri({}) from diskcache succeed.", uri);
                    }
                }
                else {
                    LOG.warn("BitmapAgent: remove bitmap for uri({}) from diskcache failed.", uri);
                }
            }
            return null;
        }
    }
    
    public void removeBitmap(final URI uri, final boolean alsoRemoveFromMemory) {
        try {
            final RemoveFlow flow = new RemoveFlow();
            this._source.create(flow, flow.DO).acceptEvent("remove", uri);
        } catch (Throwable e) {
            LOG.warn("exception when exec remove event for uri({}), detail:{}", 
                    uri, ExceptionUtils.exception2detail(e));
        }
        
        if ( alsoRemoveFromMemory ) {
            this._memoryCache.remove(uri.toASCIIString());
        }
    }
    
    private final BitmapsPool _pool;
    private final BlobAgent _blobAgent;
    private final CompositeBitmapCache _memoryCache;
    private final DiskLruCache _diskCache;
    
    private final EventReceiverSource _source;
}
