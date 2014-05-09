/**
 * 
 */
package org.jocean.android.bitmap;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.jocean.android.bitmap.BitmapAgent.BitmapInMemoryReactor;
import org.jocean.android.bitmap.BitmapAgent.BitmapReactor;
import org.jocean.android.bitmap.BitmapAgent.PropertiesInitializer;
import org.jocean.event.api.AbstractFlow;
import org.jocean.event.api.ArgsHandler;
import org.jocean.event.api.ArgsHandlerSource;
import org.jocean.event.api.BizStep;
import org.jocean.event.api.EventReceiver;
import org.jocean.event.api.FlowLifecycleListener;
import org.jocean.event.api.annotation.OnEvent;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.block.Blob;
import org.jocean.rosa.api.BlobAgent;
import org.jocean.rosa.api.BlobReactor;
import org.jocean.rosa.api.BlobTransaction;
import org.jocean.rosa.api.TransactionConstants;
import org.jocean.rosa.api.TransactionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jakewharton.disklrucache.DiskLruCache;
import com.jakewharton.disklrucache.DiskLruCache.Editor;
import com.jakewharton.disklrucache.DiskLruCache.Snapshot;

/**
 * @author isdom
 *
 */
class BitmapTransactionFlow extends AbstractFlow<BitmapTransactionFlow> 
    implements ArgsHandlerSource {

	private static final Logger LOG = LoggerFactory
			.getLogger(BitmapTransactionFlow.class);

    public BitmapTransactionFlow(
            final BitmapsPool pool,
            final BlobAgent blobAgent,
            final CompositeBitmapCache memoryCache,
            final DiskLruCache diskCache) {
        this._bitmapsPool = pool;
        this._blobAgent = blobAgent;
        this._memoryCache = memoryCache;
        this._diskCache = diskCache;
        
        addFlowLifecycleListener(new FlowLifecycleListener<BitmapTransactionFlow>() {

            @Override
            public void afterEventReceiverCreated(
                    final BitmapTransactionFlow flow, final EventReceiver receiver)
                    throws Exception {
            }

            @Override
            public void afterFlowDestroy(final BitmapTransactionFlow flow)
                    throws Exception {
                notifyReactorFailureIfNeeded();
            }} );
    }

    @Override
    public ArgsHandler getArgsHandler() {
        return ArgsHandler.Consts._REFCOUNTED_ARGS_GUARD;
    }
    
	public final BizStep WAIT = new BizStep("bitmap.WAIT")
            .handler(selfInvoker("onLoadFromMemoryOnly"))
			.handler(selfInvoker("onLoadAnyway"))
			.handler(selfInvoker("onDetach"))
			.freeze();
	
	private final BizStep OBTAINING = new BizStep("bitmap.OBTAINING")
			.handler(selfInvoker("onTransportActived"))
			.handler(selfInvoker("onTransportInactived"))
            .handler(selfInvoker("onContentTypeReceived"))
            .handler(selfInvoker("onProgress"))
            .handler(selfInvoker("onBlobReceived"))
            .handler(selfInvoker("onTransactionFailure"))
			.handler(selfInvoker("onDetach"))
			.freeze();

	@OnEvent(event="detach")
	private BizStep onDetach() throws Exception {
		if ( LOG.isTraceEnabled() ) {
			LOG.trace("fetch bitmap for ctx({})/key({}) canceled", this._ctx, this._key);
		}
		safeDetachBlobTransaction();
		this._bitmapReactor = null;
		return null;
	}

    @OnEvent(event = "loadFromMemoryOnly")
    private BizStep onLoadFromMemoryOnly(
            final URI uri,
            final Object ctx,
            final BitmapInMemoryReactor<Object> reactor) {
        final String key = uri.toASCIIString();
        
        final CompositeBitmap bitmap = this._memoryCache.get(key);
        try {
            reactor.onLoadFromMemoryResult(ctx, bitmap);
        }
        catch (Exception e) {
            LOG.warn("exception when ctx({})/key({})'s BitmapInMemoryReactor.onLoadFromMemoryResult, detail:{}", 
                    ctx, key, ExceptionUtils.exception2detail(e));
        }
        if ( null != bitmap ) {
            if ( LOG.isTraceEnabled() ) {
                LOG.trace("onLoadFromMemoryOnly: load CompositeBitmap({}) from memory cache for ctx({})/key({}) succeed.", 
                        bitmap, ctx, key);
            }
        }
        else {
            if ( LOG.isTraceEnabled() ) {
                LOG.trace("onLoadFromMemoryOnly: ctx({})/key({})'s bitmap !NOT! in memory cache.", 
                        ctx, key);
            }
        }
        return null;
	}
	
    @OnEvent(event = "loadAnyway")
	private BizStep onLoadAnyway(
	        final URI uri,
            final Object ctx,
            final BitmapReactor<Object> reactor, 
            final PropertiesInitializer<Object> initializer,
	        final TransactionPolicy policy) 
            throws Exception {

        final String key = uri.toASCIIString();
        
        if ( tryLoadFromMemoryCache(key, ctx, reactor) ) {
            return null;
        }
        
        if ( tryLoadFromDiskCache(key, ctx, reactor, initializer) ) {
            return null;
        }

        // try to load from network
        this._key = key;
        this._ctx = ctx;
        this._bitmapReactor = reactor;
        this._blobTransaction = this._blobAgent.createBlobTransaction();
        this._blobTransaction.start(uri, this.queryInterfaceInstance(BlobReactor.class), policy);
        this._propertiesInitializer = initializer;
        
        try {
            this._bitmapReactor.onStartDownload( this._ctx);
        }
        catch (Exception e) {
            LOG.warn("exception when ctx({})/key({})'s BitmapReactor.onStartDownload, detail:{}", 
                    this._ctx, this._key, ExceptionUtils.exception2detail(e));
        }
        
		return OBTAINING;
	}

    @OnEvent(event = "onTransportActived")
    public BizStep onTransportActived() throws Exception {
        try {
            this._bitmapReactor.onTransportActived(this._ctx);
        }
        catch (Exception e) {
            LOG.warn("exception when ctx({})/key({})'s BitmapReactor.onTransportActived, detail:{}", 
                    this._ctx, this._key, ExceptionUtils.exception2detail(e));
        }
        return this.currentEventHandler();
    }

    @OnEvent(event = "onTransportInactived")
    public BizStep onTransportInactived() throws Exception {
        try {
            this._bitmapReactor.onTransportInactived(this._ctx);
        }
        catch (Exception e) {
            LOG.warn("exception when ctx({})/key({})'s BitmapReactor.onTransportInactived, detail:{}", 
                    this._ctx, this._key, ExceptionUtils.exception2detail(e));
        }
        return this.currentEventHandler();
    }

    @OnEvent(event = "onContentTypeReceived")
    public BizStep onContentTypeReceived(final String contentType)
            throws Exception {
        try {
            this._bitmapReactor.onContentTypeReceived(this._ctx, contentType);
        }
        catch (Exception e) {
            LOG.warn("exception when ctx({})/key({})'s BitmapReactor.onContentTypeReceived, detail:{}", 
                    this._ctx, this._key, ExceptionUtils.exception2detail(e));
        }
        return this.currentEventHandler();
    }

    @OnEvent(event = "onProgress")
    public BizStep onProgress(final long currentByteSize, final long totalByteSize)
            throws Exception {
        try {
            this._bitmapReactor.onProgress(this._ctx, currentByteSize, totalByteSize);
        }
        catch (Exception e) {
            LOG.warn("exception when ctx({})/key({})'s BitmapReactor.onProgress, detail:{}", 
                    this._ctx, this._key, ExceptionUtils.exception2detail(e));
        }
        return this.currentEventHandler();
    }

    @OnEvent(event = "onBlobReceived")
    public BizStep onBlobReceived(final Blob blob) throws Exception {
        final InputStream is = blob.genInputStream();
        try {
            final Map<String, Object> toinit = new HashMap<String, Object>();
            safeInitProperties(this._ctx, this._key, this._propertiesInitializer, toinit);
            final CompositeBitmap bitmap = Bitmaps.decodeStreamAsBlocks(
                    this._bitmapsPool, is, toinit);
            if ( null != bitmap ) {
                try {
                    putToMemoryCache(this._key, bitmap);
                    // save to disk
                    trySaveToDisk(this._key, this._ctx, bitmap);
                }
                catch (Exception e) {
                    LOG.warn("exception when ctx({})/key({})'s bitmap({}) save to disk and put to memorycache, detail:{}", 
                            this._ctx, this._key, bitmap, ExceptionUtils.exception2detail(e));
                }
                    
                final BitmapReactor<Object> reactor = this._bitmapReactor;
                this._bitmapReactor = null;
                try {
                    reactor.onBitmapReceived(this._ctx, bitmap);
                }
                catch (Exception e) {
                    LOG.warn("exception when ctx({})/key({})'s BitmapReactor.onBitmapReceived, detail:{}", 
                            this._ctx, this._key, ExceptionUtils.exception2detail(e));
                }
                finally {
                    bitmap.release();
                }
            }
            else {
                this.setFailureReason(BitmapAgent.FAILURE_BITMAP_DECODE_FAILED);
                LOG.warn("can't deocde valid bitmap for ctx({})/key({})", this._ctx, this._key);
            }
        }
        finally {
            if ( null != is ) {
                is.close();
            }
        }
        return null;
    }

    @OnEvent(event = "onTransactionFailure")
    public BizStep onTransactionFailure(int failureReason)
            throws Exception {
        this.setFailureReason(failureReason);
        return null;
    }
    
    /**
     * @param ctx
     * @param reactor
     * @param key
     */
    private boolean tryLoadFromMemoryCache(
            final String key,
            final Object ctx,
            final BitmapReactor<Object> reactor) {
        final CompositeBitmap bitmap = this._memoryCache.get(key);
        if ( null != bitmap ) {
            try {
                if ( LOG.isTraceEnabled() ) {
                    LOG.trace("onLoadAnyway: load CompositeBitmap({}) from memory cache for ctx({})/key({})", 
                            bitmap, ctx, key);
                }
                reactor.onBitmapCached(ctx, bitmap, true);
            }
            catch (Exception e) {
                LOG.warn("exception when ctx({})/key({})'s BitmapReactor.onBitmapCached, detail:{}", 
                        ctx, key, ExceptionUtils.exception2detail(e));
            }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * @param key
     * @param ctx
     * @param reactor
     * @param initializer 
     * @throws Exception
     */
    private boolean tryLoadFromDiskCache(
            final String key, 
            final Object ctx,
            final BitmapReactor<Object> reactor, 
            final PropertiesInitializer<Object> initializer) 
        throws Exception {
        if ( null != this._diskCache ) {
            final Snapshot snapshot = this._diskCache.get(Md5.encode(key) );
            if ( null != snapshot ) {
                try {
                    final InputStream is = snapshot.getInputStream(0);
                    if ( null != is ) {
                        try {
                            reactor.onStartLoadFromDisk(ctx);
                        }
                        catch (Exception e) {
                            LOG.warn("exception when ctx({})/key({})'s BitmapReactor.onStartLoadFromDisk, detail:{}", 
                                    ctx, key, ExceptionUtils.exception2detail(e));
                        }
                        
                        final Map<String, Object> toinit = new HashMap<String, Object>();
                        safeInitProperties(ctx, key, initializer, toinit);
                        final CompositeBitmap bitmap = CompositeBitmap.decodeFrom(is, this._bitmapsPool, toinit);
                        if ( null != bitmap ) {
                            try {
                                if ( LOG.isTraceEnabled() ) {
                                    LOG.trace("onLoadAnyway: load CompositeBitmap({}) from disk for ctx({})/key({})", 
                                            bitmap, ctx, key);
                                }
                                putToMemoryCache(key, bitmap);
                                try {
                                    reactor.onBitmapCached(ctx, bitmap, false);
                                }
                                catch (Exception e) {
                                    LOG.warn("exception when ctx({})/key({})'s BitmapReactor.onBitmapCached, detail:{}", 
                                            ctx, key, ExceptionUtils.exception2detail(e));
                                }
                                return true;
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
        return false;
    }

    /**
     * @param ctx
     * @param key
     * @param initializer
     * @param toinit
     */
    private void safeInitProperties(
            final Object ctx, final String key,
            final PropertiesInitializer<Object> initializer,
            final Map<String, Object> toinit) {
        if ( null != initializer ) {
            try {
                initializer.visit(ctx, toinit);
            }
            catch (Exception e) {
                LOG.warn("exception when ctx({})/key({})'s initializer({}).visit, detail:{}", 
                        ctx, key, initializer, ExceptionUtils.exception2detail(e));
            }
        }
    }

    /**
     * @param key
     * @param bitmap
     */
    private void putToMemoryCache(final String key, final CompositeBitmap bitmap) {
        this._memoryCache.put(key, bitmap.retain());
    }

    private void trySaveToDisk(
            final String key, 
            final Object ctx,
            final CompositeBitmap bitmap) throws Exception {
        if ( null != this._diskCache ) {
            final String md5 = Md5.encode(key);
            if ( null == this._diskCache.get(md5) ) {
                final Editor editor = this._diskCache.edit(md5);
                OutputStream os = null;
                if ( null != editor ) {
                    try {
                        os = editor.newOutputStream(0);
                        bitmap.encodeTo(os);
                        if ( LOG.isTraceEnabled() ) {
                            LOG.trace("trySaveToDisk: save ctx({})/key({})'s bitmap({}) to disk succeed", 
                                    ctx, key, bitmap);
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
                    LOG.trace("trySaveToDisk: ctx({})/key({})'s bitmap({}) already save to disk cache.", 
                            ctx, key, bitmap);
                }
            }
        }
    }
    
	private void notifyReactorFailureIfNeeded() {
		if ( null != this._bitmapReactor ) {
			try {
				this._bitmapReactor.onTransactionFailure(this._ctx, this._failureReason);
			}
			catch (Exception e) {
				LOG.warn("exception when BitmapReactor.onTransactionFailure for ctx({})/key({}), detail:{}", 
						this._ctx, this._key, ExceptionUtils.exception2detail(e));
			}
		}
	}
	
    private void safeDetachBlobTransaction() {
        if ( null != this._blobTransaction ) {
            try {
                this._blobTransaction.detach();
            }
            catch (Exception e) {
                LOG.warn("exception when detach blob transaction for ctx({})/key({}), detail:{}",
                        this._ctx, this._key, ExceptionUtils.exception2detail(e));
            }
            this._blobTransaction = null;
        }
    }
    
    private void setFailureReason(final int failureReason) {
        this._failureReason = failureReason;
    }
    
    @Override
    public String toString() {
        return "BitmapTransactionFlow ["+Integer.toHexString(hashCode()) 
                + ", memoryCache=" + _memoryCache
                + ", diskCache=" + _diskCache + ", failureReason="
                + _failureReason + ", key=" + _key + ", ctx=" + _ctx
                + ", bitmapReactor=" + _bitmapReactor + ", blobTransaction="
                + _blobTransaction + "]";
    }

    private final BlobAgent _blobAgent;
    private final BitmapsPool _bitmapsPool;
    private final CompositeBitmapCache _memoryCache;
    private final DiskLruCache _diskCache;
    private int _failureReason = TransactionConstants.FAILURE_UNKNOWN;
	
    private String _key;
    private Object _ctx;
	private BitmapReactor<Object> _bitmapReactor = null;
	private PropertiesInitializer<Object> _propertiesInitializer = null;
	private BlobTransaction    _blobTransaction = null;
}
