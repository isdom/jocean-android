/**
 * 
 */
package org.jocean.android.bitmap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.jocean.android.bitmap.BitmapAgent.BitmapInCacheReactor;
import org.jocean.android.bitmap.BitmapAgent.BitmapInMemoryReactor;
import org.jocean.android.bitmap.BitmapAgent.BitmapReactor;
import org.jocean.android.bitmap.BitmapAgent.PropertiesInitializer;
import org.jocean.event.api.AbstractFlow;
import org.jocean.event.api.BizStep;
import org.jocean.event.api.EventReceiver;
import org.jocean.event.api.FlowLifecycleListener;
import org.jocean.event.api.annotation.OnEvent;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.block.Blob;
import org.jocean.idiom.block.BlockUtils;
import org.jocean.idiom.pool.BytesPool;
import org.jocean.rosa.api.BlobAgent;
import org.jocean.rosa.api.BlobAgent.BlobReactor;
import org.jocean.rosa.api.BlobAgent.BlobTransaction;
import org.jocean.rosa.api.TransactionConstants;
import org.jocean.rosa.api.TransactionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;

import com.jakewharton.disklrucache.DiskLruCache;
import com.jakewharton.disklrucache.DiskLruCache.Snapshot;

/**
 * @author isdom
 *
 */
class BitmapTransactionFlow extends AbstractFlow<BitmapTransactionFlow> {

	private static final Logger LOG = LoggerFactory
			.getLogger(BitmapTransactionFlow.class);

    public BitmapTransactionFlow(
            final BytesPool bytesPool,
            final Bitmap.Config config,
            final BlobAgent blobAgent,
            final CompositeBitmapCache memoryCache,
            final DiskLruCache diskCache) {
        this._bytesPool = bytesPool;
        this._config = config;
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

	public final BizStep WAIT = new BizStep("bitmap.WAIT") {
	    @OnEvent(event = "loadFromMemoryOnly")
	    private BizStep onLoadFromMemoryOnly(
	            final URI uri,
	            final Object ctx,
	            final BitmapInMemoryReactor<Object> reactor) {
	        final String key = uri.toASCIIString();
	        
	        final CompositeBitmap bitmap = _memoryCache.getAndTryRetain(key);
	        safeNotifyOnLoadFromMemory(ctx, key, reactor, bitmap);
	        if ( null != bitmap ) {
	            if ( LOG.isTraceEnabled() ) {
	                LOG.trace("onLoadFromMemoryOnly: load CompositeBitmap({}) from memory cache for ctx({})/key({}) succeed.", 
	                        bitmap, ctx, key);
	            }
	            bitmap.release();
	        }
	        else {
	            if ( LOG.isTraceEnabled() ) {
	                LOG.trace("onLoadFromMemoryOnly: ctx({})/key({})'s bitmap !NOT! in memory cache.", 
	                        ctx, key);
	            }
	        }
	        return null;
	    }
	    
	    @OnEvent(event = "loadFromCacheOnly")
	    private BizStep onLoadFromCacheOnly(
	            final URI uri,
	            final Object ctx,
	            final BitmapInCacheReactor<Object> reactor, 
	            final PropertiesInitializer<Object> initializer) {
	        final String key = uri.toASCIIString();
	        CompositeBitmap bitmap = _memoryCache.getAndTryRetain(key);
	        
	        final boolean inMemoryCache = ( null != bitmap );
	        if ( null == bitmap ) {
	            final String diskCacheKey = Md5.encode(key);
	            final Snapshot snapshot = getSnapshotFromDiskCache(diskCacheKey);
	            InputStream is = null, bytesIs = null;
	            if ( null != snapshot ) {
	                try {
	                    is = snapshot.getInputStream(0);
	                    if ( null != is ) {
	                        bytesIs = BlockUtils.inputStream2BytesListInputStream(is, _bytesPool);
	                        if ( null != bytesIs ) {
	                            bitmap = tryRetainFromDiskCache(key, diskCacheKey, ctx, bytesIs, initializer);
	                        }
	                    }
	                }
	                finally {
	                    if ( null != bytesIs ) {
	                        try {
	                            bytesIs.close();
	                        }
	                        catch (Throwable e) {
	                        }
	                    }
	                    if ( null != is ) {
	                        try {
	                            is.close();
	                        }
	                        catch (Throwable e) {
	                        }
	                    }
	                    snapshot.close();
	                }
	            }
	        }
	        
	        try {
	            safeNotifyOnLoadFromCache(ctx, key, reactor, bitmap, inMemoryCache);
	        }
	        finally {
	            if ( null != bitmap ) {
	                bitmap.release();
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
	        _key = key;
	        _ctx = ctx;
	        _bitmapReactor = reactor;
	        _blobTransaction = _blobAgent.createBlobTransaction();
	        _blobTransaction.start(uri, null, genBlobReactor(), policy);
	        _propertiesInitializer = initializer;
	        
	        safeNotifyOnStartDownload(_ctx, _key, _bitmapReactor);
	        
	        return OBTAINING;
	    }

	}
    .handler(handlersOf(this)) // for onDetach
	.freeze();
	
	private final BizStep OBTAINING = new BizStep("bitmap.OBTAINING") {
	    @OnEvent(event = "onTransportActived")
	    public BizStep onTransportActived(final Object obj) throws Exception {
	        try {
	            if (null!=_bitmapReactor) {
	                _bitmapReactor.onTransportActived(_ctx);
	            }
	        }
	        catch (Throwable e) {
	            LOG.warn("exception when ctx({})/key({})'s BitmapReactor.onTransportActived, detail:{}", 
	                    _ctx, _key, ExceptionUtils.exception2detail(e));
	        }
	        return currentEventHandler();
	    }
	    
	    @OnEvent(event = "onTransportInactived")
	    public BizStep onTransportInactived(final Object obj) throws Exception {
	        try {
	            if (null!=_bitmapReactor) {
	                _bitmapReactor.onTransportInactived(_ctx);
	            }
	        }
	        catch (Throwable e) {
	            LOG.warn("exception when ctx({})/key({})'s BitmapReactor.onTransportInactived, detail:{}", 
	                    _ctx, _key, ExceptionUtils.exception2detail(e));
	        }
	        return currentEventHandler();
	    }

	    @OnEvent(event = "onContentTypeReceived")
	    public BizStep onContentTypeReceived(final Object obj, final String contentType)
	            throws Exception {
	        try {
	            if (null!=_bitmapReactor) {
	                _bitmapReactor.onContentTypeReceived(_ctx, contentType);
	            }
	        }
	        catch (Exception e) {
	            LOG.warn("exception when ctx({})/key({})'s BitmapReactor.onContentTypeReceived, detail:{}", 
	                    _ctx, _key, ExceptionUtils.exception2detail(e));
	        }
	        return currentEventHandler();
	    }
	    
	    @OnEvent(event = "onProgress")
	    public BizStep onProgress(final Object obj, final long currentByteSize, final long totalByteSize)
	            throws Exception {
	        try {
	            if (null!=_bitmapReactor) {
	                _bitmapReactor.onProgress(_ctx, currentByteSize, totalByteSize);
	            }
	        }
	        catch (Throwable e) {
	            LOG.warn("exception when ctx({})/key({})'s BitmapReactor.onProgress, detail:{}", 
	                    _ctx, _key, ExceptionUtils.exception2detail(e));
	        }
	        return currentEventHandler();
	    }
	    
	    @OnEvent(event = "onBlobReceived")
	    public BizStep onBlobReceived(final Object obj, final Blob blob) throws Exception {
	        final InputStream is = blob.genInputStream();
	        try {
	            final Map<String, Object> toinit = new HashMap<String, Object>();
	            safeInitProperties(_ctx, _key, _propertiesInitializer, toinit);
	            final CompositeBitmap bitmap = CompositeBitmap.decodeFrom(is, _config, toinit);
	            if ( null != bitmap ) {
	                try {
	                    putToMemoryCache(_key, bitmap);
	                    // save to disk
	                    trySaveToDisk(_key, _ctx, bitmap);
	                }
	                catch (Exception e) {
	                    LOG.warn("exception when ctx({})/key({})'s bitmap({}) save to disk and put to memorycache, detail:{}", 
	                            _ctx, _key, bitmap, ExceptionUtils.exception2detail(e));
	                }
	                    
	                final BitmapReactor<Object> reactor = _bitmapReactor;
	                _bitmapReactor = null;
	                try {
	                    if (null!=reactor) {
	                        reactor.onBitmapReceived(_ctx, bitmap);
	                    }
	                }
	                catch (Exception e) {
	                    LOG.warn("exception when ctx({})/key({})'s BitmapReactor.onBitmapReceived, detail:{}", 
	                            _ctx, _key, ExceptionUtils.exception2detail(e));
	                }
	                finally {
	                    bitmap.release();
	                }
	            }
	            else {
	                setFailureReason(BitmapAgent.FAILURE_BITMAP_DECODE_FAILED);
	                LOG.warn("can't deocde valid bitmap for ctx({})/key({})", _ctx, _key);
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
	    public BizStep onTransactionFailure(final Object obj, int failureReason)
	            throws Exception {
	        setFailureReason(failureReason);
	        return null;
	    }
	}
    .handler(handlersOf(this)) // for onDetach
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
    
    @SuppressWarnings("unchecked")
    private BlobReactor<Object> genBlobReactor() {
        return (BlobReactor<Object>)this.queryInterfaceInstance(BlobReactor.class);
    }

    /**
     * @param key
     * @param ctx
     * @param reactor
     * @param initializer
     * @throws Exception
     * @throws IOException
     */
    private boolean tryLoadFromDiskCache(
            final String key, 
            final Object ctx,
            final BitmapReactor<Object> reactor,
            final PropertiesInitializer<Object> initializer) 
        throws Exception {
        final String diskCacheKey = Md5.encode(key);
        final Snapshot snapshot = getSnapshotFromDiskCache(diskCacheKey);
        InputStream is = null, bytesIs = null;
        if ( null != snapshot ) {
            try {
                is = snapshot.getInputStream(0);
                if ( null != is ) {
                    bytesIs = BlockUtils.inputStream2BytesListInputStream(is, this._bytesPool);
                    if ( null != bytesIs ) {
                        safeNotifyOnStartLoadFromDisk(ctx, key, reactor);
                        
                        final CompositeBitmap bitmap = tryRetainFromDiskCache(key, diskCacheKey, ctx, bytesIs, initializer);
                        
                        if ( null != bitmap ) {
                            try {
                                safeNotifyOnBitmapCached(ctx, key, reactor, bitmap);
                            }
                            finally {
                                bitmap.release();
                            }
                            return true;
                        }
                    }
                }
            }
            finally {
                if ( null != bytesIs ) {
                    try {
                        bytesIs.close();
                    }
                    catch (Throwable e) {
                    }
                }
                if ( null != is ) {
                    try {
                        is.close();
                    }
                    catch (Throwable e) {
                    }
                }
                snapshot.close();
            }
        }
        return false;
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
        final CompositeBitmap bitmap = this._memoryCache.getAndTryRetain(key);
        if ( null != bitmap ) {
            if ( LOG.isTraceEnabled() ) {
                LOG.trace("tryLoadFromMemoryCache: load CompositeBitmap({}) from memory cache for ctx({})/key({})", 
                        bitmap, ctx, key);
            }
            try {
                if (null!=reactor) {
                    reactor.onBitmapCached(ctx, bitmap, true);
                }
            }
            catch (Exception e) {
                LOG.warn("exception when ctx({})/key({})'s BitmapReactor.onBitmapCached, detail:{}", 
                        ctx, key, ExceptionUtils.exception2detail(e));
            }
            finally {
                bitmap.release();
            }
            return true;
        }
        else {
            return false;
        }
    }

    private Snapshot getSnapshotFromDiskCache(final String diskCacheKey) {
        try {
            if ( null != this._diskCache ) {
                return this._diskCache.get(diskCacheKey);
            }
        }
        catch (Throwable e) {
            LOG.warn("exception when LruDiskCache.get Snapshot for diskCacheKey({}), detail:{}",
                    diskCacheKey, ExceptionUtils.exception2detail(e));
        }
        return null;
    }
    
    /**
     * @param key
     * @param ctx
     * @param reactor
     * @param initializer 
     * @throws Exception
     */
    private CompositeBitmap tryRetainFromDiskCache(
            final String key, 
            final String diskCacheKey,
            final Object ctx,
            final InputStream is,
            final PropertiesInitializer<Object> initializer)  {
        final Map<String, Object> toinit = new HashMap<String, Object>();
        safeInitProperties(ctx, key, initializer, toinit);
        
        toinit.put(BitmapAgent.KEYS.PERSIST_FILENAME, 
                this._diskCache.getDirectory().getAbsolutePath() + "/" + diskCacheKey + ".0");
        
        try {
            final CompositeBitmap bitmap = CompositeBitmap.decodeFrom(is, this._config, toinit);
            
            if ( null != bitmap ) {
                if ( LOG.isTraceEnabled() ) {
                    LOG.trace("tryLoadFromDiskCache: load CompositeBitmap({}) from disk for ctx({})/key({})", 
                            bitmap, ctx, key);
                }
                putToMemoryCache(key, bitmap);
            }
            return bitmap;
        }
        catch (Throwable e) {
            LOG.warn("exception when decodeFrom for ctx({})/key({}), detail:{}", 
                ctx, key, ExceptionUtils.exception2detail(e));
        }
        return null;
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
            catch (Throwable e) {
                LOG.warn("exception when ctx({})/key({})'s initializer({}).visit, detail:{}", 
                        ctx, key, initializer, ExceptionUtils.exception2detail(e));
            }
        }
    }

    private void putToMemoryCache(final String key, final CompositeBitmap bitmap) {
        this._memoryCache.retainAndPut(key, bitmap);
    }

    private void trySaveToDisk(
            final String key, 
            final Object ctx,
            final CompositeBitmap bitmap) throws Exception {
        final String cacheFilename = Bitmaps.saveBitmapToDisk(key, bitmap, this._diskCache);
        if ( null != cacheFilename) {
            bitmap.setProperty(BitmapAgent.KEYS.PERSIST_FILENAME, cacheFilename);
            if ( LOG.isTraceEnabled() ) {
                LOG.trace("trySaveToDisk: save ctx({})/key({})'s bitmap({}) to disk succeed", 
                        ctx, key, bitmap);
            }
        }
    }
    
    /**
     * @param ctx
     * @param key
     * @param reactor
     * @param bitmap
     */
    private void safeNotifyOnLoadFromMemory(
            final Object ctx,
            final String key, 
            final BitmapInMemoryReactor<Object> reactor,
            final CompositeBitmap bitmap) {
        try {
            if ( null != reactor) {
                reactor.onLoadFromMemoryResult(ctx, bitmap);
            }
        }
        catch (Exception e) {
            LOG.warn("exception when ctx({})/key({})'s BitmapInMemoryReactor.onLoadFromMemoryResult, detail:{}", 
                    ctx, key, ExceptionUtils.exception2detail(e));
        }
    }
    
    /**
     * @param ctx
     * @param key
     * @param reactor
     * @param bitmap
     * @param inMemoryCache
     */
    private void safeNotifyOnLoadFromCache(
            final Object ctx,
            final String key, 
            final BitmapInCacheReactor<Object> reactor,
            final CompositeBitmap bitmap, 
            final boolean inMemoryCache) {
        try {
            if ( null != reactor ) {
                reactor.onLoadFromCacheResult(ctx, bitmap, inMemoryCache);
            }
        }
        catch (Throwable e) {
            LOG.warn("exception when BitmapInCacheReactor.onLoadFromCacheResult for ctx({})/key({}), detail:{}",
                    ctx, key, ExceptionUtils.exception2detail(e));
        }
    }
    
    /**
     * 
     */
    private void safeNotifyOnStartDownload(
            final Object ctx,
            final String key,
            final BitmapReactor<Object> reactor) {
        try {
            if (null!=reactor) {
                reactor.onStartDownload(ctx);
            }
        }
        catch (Exception e) {
            LOG.warn("exception when ctx({})/key({})'s BitmapReactor.onStartDownload, detail:{}", 
                    ctx, key, ExceptionUtils.exception2detail(e));
        }
    }
    
    /**
     * @param ctx
     * @param key
     * @param reactor
     * @param bitmap
     */
    private void safeNotifyOnBitmapCached(
            final Object ctx, 
            final String key,
            final BitmapReactor<Object> reactor, 
            final CompositeBitmap bitmap) {
        try {
            if (null!= reactor) {
                reactor.onBitmapCached(ctx, bitmap, false);
            }
        }
        catch (Throwable e) {
            LOG.warn("exception when ctx({})/key({})'s BitmapReactor.onBitmapCached, detail:{}", 
                    ctx, key, ExceptionUtils.exception2detail(e));
        }
    }

    /**
     * @param ctx
     * @param key
     * @param reactor
     */
    private void safeNotifyOnStartLoadFromDisk(
            final Object ctx,
            final String key, 
            final BitmapReactor<Object> reactor) {
        try {
            if ( null != reactor ) {
                reactor.onStartLoadFromDisk(ctx);
            }
        }
        catch (Throwable e) {
            LOG.warn("exception when ctx({})/key({})'s BitmapReactor.onStartLoadFromDisk, detail:{}", 
                    ctx, key, ExceptionUtils.exception2detail(e));
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

    private final BytesPool _bytesPool;
    private final BlobAgent _blobAgent;
    private final Bitmap.Config _config;
    private final CompositeBitmapCache _memoryCache;
    private final DiskLruCache _diskCache;
    private int _failureReason = TransactionConstants.FAILURE_UNKNOWN;
	
    private String _key;
    private Object _ctx;
	private BitmapReactor<Object> _bitmapReactor = null;
	private PropertiesInitializer<Object> _propertiesInitializer = null;
	private BlobTransaction    _blobTransaction = null;
}
