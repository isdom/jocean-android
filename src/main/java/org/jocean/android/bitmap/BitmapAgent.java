/**
 * 
 */
package org.jocean.android.bitmap;

import java.net.URI;
import java.util.Map;

import org.jocean.idiom.Detachable;
import org.jocean.idiom.Visitor2;
import org.jocean.rosa.api.TransactionPolicy;

/**
 * @author isdom
 *
 */
public interface BitmapAgent {
    
    public static class KEYS {
        public static final String PERSIST_FILENAME = "_persist_filename";
    }
    
    // define fromo 100
    public static final int FAILURE_BITMAP_DECODE_FAILED = 100;
    
    public interface PropertiesInitializer<CTX> extends Visitor2<CTX, Map<String, Object>> {
    }
    
    public interface BitmapTransaction extends Detachable {
        public <CTX> void loadFromMemoryOnly(
                final URI uri, 
                final CTX ctx,
                final BitmapInMemoryReactor<CTX> reactor);
        
        public <CTX> void loadAnyway(
                final URI uri, 
                final CTX ctx,
                final BitmapReactor<CTX> reactor, 
                final PropertiesInitializer<CTX> initializer,
                final TransactionPolicy policy);
    }
    
    public interface BitmapInMemoryReactor<CTX> {
        
        /**
         * load bitmap from memory's result
         * @param ctx
         * @param bitmap  != null means load succeed, otherwise means bitmap not in memory
         *                          maybe in disk cache or should download from network
         * @throws Exception
         */
        public void onLoadFromMemoryResult(final CTX ctx, final CompositeBitmap bitmap)
            throws Exception;
    }
    
    public interface BitmapReactor<CTX> {
        /**
         * notify start load bitmap from local disk by uri
         * @param ctx
         * @throws Exception
         */
        public void onStartLoadFromDisk(final CTX ctx)
                throws Exception;
        
        /**
         * notify start download bitmap from network by uri
         * @param ctx
         * @throws Exception
         */
        public void onStartDownload(final CTX ctx)
                throws Exception;
        
        /**
         * bitmap cached in memory or local disk, so just return soon
         */
        public void onBitmapCached(final CTX ctx, final CompositeBitmap bitmap, final boolean inMemoryCache)
            throws Exception;
        
        /**
         * transport layer actived for this bitmap fetch action
         */
        public void onTransportActived(final CTX ctx) throws Exception;

        /**
         * transport layer inactived for this bitmap fetch action
         * @throws Exception
         */
        public void onTransportInactived(final CTX ctx) throws Exception;

        /**
         * on content-type received, eg: "application/json" ...
         * @param contentType
         * @throws Exception
         */
        public void onContentTypeReceived(final CTX ctx, final String contentType) 
                throws Exception;
        
        /**
         * bitmap fetch action in progress, maybe invoke more than once
         * @param currentByteSize: current fetched bytes
         * @param totalByteSize: total bytes for image
         */
        public void onProgress(final CTX ctx, final long currentByteSize, final long totalByteSize) 
                throws Exception;

        /**
         * bitmap fetched and decode as CompositeBitmap succeed
         * @param bitmap : composite bitmap
         */
        public void onBitmapReceived(final CTX ctx, final CompositeBitmap bitmap) 
                throws Exception;
        
        /**
         * bitmap fetch action failed(timeout or received failed)
         */
        public void onTransactionFailure(final CTX ctx, final int failureReason) 
                throws Exception;

    }
    
	/**
	 * create transaction for bitmap fetch via special uri
	 * 
	 * @param uri: uri for fetch bitmap
	 * @return
	 */
	public BitmapTransaction createBitmapTransaction();

    public CompositeBitmap tryRetainFromMemorySync(final URI uri);
}
