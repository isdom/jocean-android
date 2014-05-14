package org.jocean.android.view;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jocean.android.bitmap.BitmapAgent;
import org.jocean.android.bitmap.BitmapAgent.BitmapReactor;
import org.jocean.android.bitmap.BitmapAgent.BitmapTransaction;
import org.jocean.android.bitmap.BitmapAgent.PropertiesInitializer;
import org.jocean.android.bitmap.CompositeBitmap;
import org.jocean.android.bitmap.CompositeBitmapDrawable;
import org.jocean.idiom.ArgsHandler;
import org.jocean.idiom.Detachable;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.ExectionLoop;
import org.jocean.idiom.InterfaceUtils;
import org.jocean.rosa.api.TransactionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.drawable.Drawable;


public class SIVLoader {
    private static final Logger LOG = LoggerFactory.getLogger(
            SIVLoader.class);

    private static final String _PROPERTY_SOURCE_URI = "_src_uri";
    private static final long DELAY_FOR_NEXT_LOAD = 100;
        
    private static final int MAX_RETRYCOUNT = 5;
    private int _currentRetryCount = 0;

    private final BitmapAgent _bitmapAgent;

    protected final ExectionLoop _uiloop;


    public SIVLoader(final BitmapAgent bitmapAgent, final ExectionLoop uiloop) {

        this._bitmapAgent = bitmapAgent;
        this._uiloop = uiloop;
    }

    public boolean checkAndTryLoadBitmapFromMemory(final SIVHolder holder) {
        final Drawable currentDrawable = holder._view.getDrawable();
        String currenturl = null;
        if ( null !=  currentDrawable) {
            if ( currentDrawable instanceof CompositeBitmapDrawable ) {
                currenturl = ((CompositeBitmapDrawable)currentDrawable).compositeBitmap().getProperty(
                        _PROPERTY_SOURCE_URI);
                if ( holder._url.equals(currenturl) ) {
                    if ( LOG.isTraceEnabled() ) {
                        LOG.trace("imageView({})/target url({})'s drawable link property is {}, match target url"
                                ,holder._view, holder._url, currenturl);
                    }
                    return true;
                }
                else {
                    if ( LOG.isTraceEnabled() ) {
                        LOG.trace("imageView({})/target url({})'s drawable is CompositeBitmapDrawable,BUT link property is {}, !MISMATCHED!"
                                ,holder._view, holder._url, currenturl);
                    }
                }
            }
            else {
                if ( LOG.isTraceEnabled() ) {
                    LOG.trace("imageView({})/target url({})'s drawable is {}, NOT CompositeBitmapDrawable"
                            ,holder._view, holder._url, currentDrawable);
                }
            }
        }
        else {
            if ( LOG.isTraceEnabled() ) {
                LOG.trace("imageView({})/target url({})'s drawable is null.",holder._view, holder._url);
            }
        }
        CompositeBitmap bitmap = null;
        try {
            bitmap = this._bitmapAgent.tryRetainFromMemorySync(new URI(holder._url));
            if ( null != bitmap ) {
                holder._view.replaceDrawable(new CompositeBitmapDrawable(bitmap));
                return true;
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        finally {
            if ( null != bitmap ) {
                bitmap.release();
            }
        }
        return false;
    }
    
    public void addView(final SmartImageView view) {
        if(!this._viewsToLoad.contains(view)){
            this._viewsToLoad.add(view);
        }
    }

    public void loadBitmaps(){
        if (this._needLoadBitmaps.compareAndSet(false, true)) {
            this._uiloop.submit(new Runnable() {
                @Override
                public void run() {
                    if (_needLoadBitmaps.compareAndSet(true, false)) {
                        doLoadBitmaps();
                    }
                }
            });
        }
    }
    
    private void doLoadBitmaps() {
        if (null == this._currentTask) {
            SmartImageView view = this._viewsToLoad.poll();
            while (null == this._currentTask
                    && null != view) {
                this._currentTask = startLoadBitmap(view);
                if (null != this._currentTask) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("start load pending image for uri:{}",
                                SIVHolder.view2url(view));
                    }
                }
                else {
                    view = this._viewsToLoad.poll();
                }
            }
        }
    }
    
    private BitmapTransaction startLoadBitmap(final SmartImageView view) {
        if ( null == view ) {
            return null;
        }
        final String url = SIVHolder.view2url(view);
        try {
            final URI uri = new URI(url);
            final BitmapTransaction transaction = this._bitmapAgent
                    .createBitmapTransaction();
            transaction.loadAnyway(
                    uri,
                    view,
                    genFetchBitmapReactor(uri, transaction),
                    new PropertiesInitializer<SmartImageView> () {
                        @Override
                        public void visit(final SmartImageView view,
                                final Map<String, Object> propertiesToInit) throws Exception {
                            propertiesToInit.put(_PROPERTY_SOURCE_URI, uri.toASCIIString());
                        }},
                    new TransactionPolicy().priority(-1) );
            return transaction;
        } catch (URISyntaxException e) {
            LOG.error("invalid url:{}", url);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private BitmapReactor<SmartImageView> genFetchBitmapReactor(
            final URI uri, 
            final BitmapTransaction transaction) {
        final class BitmapReactorImpl implements BitmapReactor<SmartImageView> {
            
            @Override
            public void onProgress(final SmartImageView view, final long currentByteSize,
                    final long totalByteSize) throws Exception {
                LOG.info("load image uri {} on progress {}/{}", uri,
                        currentByteSize, totalByteSize);
            }

            @Override
            public void onTransportActived(final SmartImageView view) throws Exception {
                LOG.info("load image uri {} 's transport actived", uri);
            }

            @Override
            public void onTransportInactived(final SmartImageView view) throws Exception {
                LOG.info("load image uri {} 's transport inactived", uri);
            }

            @Override
            public void onContentTypeReceived(final SmartImageView view, final String contentType)
                    throws Exception {
                LOG.info("load image uri {} 's contentType:{}", uri, contentType);
            }

            @Override
            public void onBitmapReceived(final SmartImageView view, final CompositeBitmap bitmap) throws Exception {
                // 根据Tag找到相应的ImageView控件，将下载好的图片显示出来。
                setBitmapToViewIfMatchedURI(bitmap, view, uri);
                onBitmapTransactionFinished(true, uri);
            }

            @Override
            public void onTransactionFailure(final SmartImageView view, final int failureReason)
                    throws Exception {
                LOG.info("load image uri {} failed with reason: {}", uri, failureReason);
                if ( BitmapAgent.FAILURE_BITMAP_DECODE_FAILED == failureReason ) {
                    onBitmapDecodeFailed(view, uri);
                }
                onBitmapTransactionFinished(false, uri);
            }

            @Override
            public void onBitmapCached(
                    final SmartImageView view,
                    final CompositeBitmap bitmap, 
                    final boolean inMemoryCache)
                    throws Exception {
                // 根据Tag找到相应的ImageView控件，将下载好的图片显示出来。
                setBitmapToViewIfMatchedURI(bitmap, view, uri);
                onBitmapTransactionFinished(true, uri);
            }

            @Override
            public void onStartLoadFromDisk(
                    final SmartImageView view) throws Exception {
            }

            @Override
            public void onStartDownload(
                    final SmartImageView view) throws Exception {
            }

        }
        return (BitmapReactor<SmartImageView>)InterfaceUtils.genAsyncImpl(
                BitmapReactor.class, new BitmapReactorImpl(), this._uiloop, 
                ArgsHandler.Consts._REFCOUNTED_ARGS_GUARD);
    }
    
    public void onBitmapDecodeFailed(final SmartImageView view, final URI uri) {
        if (this._currentRetryCount++ < MAX_RETRYCOUNT) {
            LOG.warn("onTransactionFailure: uri({}) decode bitmap failed , current retry count {}, try re-load.",
                    uri, this._currentRetryCount);
            if ( !this._viewsToLoad.contains(view)) {
                this._viewsToLoad.addFirst(view);
            }
        } else {
            LOG.error(
                    "onTransactionFailure: uri({}) decode bitmap failed reach max retry count {}, stop load.",
                    uri, this._currentRetryCount);
            // reset retry counter
            this._currentRetryCount = 0;
        }
    }

    private void setBitmapToViewIfMatchedURI(
            final CompositeBitmap bitmap,
            final SmartImageView view, 
            final URI uri) {
        if (view != null && bitmap != null 
                && SIVHolder.view2url(view).equals(uri.toASCIIString())) {
            if ( LOG.isTraceEnabled() ) {
                LOG.trace("setBitmapToView: SmartImageView({}) will be set by bitmap({}) for uri({})", view, bitmap, uri);
            }
            view.replaceDrawable(new CompositeBitmapDrawable(bitmap));
        }
        else {
            if ( LOG.isTraceEnabled() ) {
                LOG.trace("setBitmapToView: CAN'T set bitmap({}) to SmartImageView({})  for uri({})", bitmap, view, uri);
            }
        }
    }

    private void onBitmapTransactionFinished(
            final boolean succeed,
            final URI uri) {
        if ( LOG.isInfoEnabled() ) {
            LOG.info("load image uri {} finished with {}", 
                    uri,
                    succeed ? "succeed" : "failed");
        }
        if ( this._isBusy ) {
            this._currentTask = null;
        }
        else {
            this._currentTask = _uiloop.schedule(new Runnable() {
                @Override
                public void run() {
                    _currentTask = null;
                    doLoadBitmaps();
                }}, DELAY_FOR_NEXT_LOAD);
        }
    }
    
    public void cancelCurrentTasks() {
        if (null != _currentTask) {
            try {
                _currentTask.detach();
            } catch (Exception e) {
                LOG.error(
                        "exception when detach current task, detail:{}",
                        ExceptionUtils.exception2detail(e));
            }
            this._currentTask = null;
        }
    }

    public void enableForceLoadBitmaps() {
        this._forceLoadBitmaps = true;
    }

    public void tryForceLoadBitmapsOnce() {
        if (this._forceLoadBitmaps) {
            if (LOG.isInfoEnabled()) {
                LOG.info("tryForceLoadBitmapsOnce: start to loadBitmaps");
            }
            this._forceLoadBitmaps = false;
            loadBitmaps();
        }
    }
    
    public void markOnBusy() {
        this._isBusy = true;
    }

    public void markOnIdle() {
        this._isBusy = false;
    }

    private volatile boolean _isBusy = false;
    
    private boolean _forceLoadBitmaps = true;
    private final Deque<SmartImageView> _viewsToLoad = 
            new LinkedBlockingDeque<SmartImageView>();
    private AtomicBoolean _needLoadBitmaps = new AtomicBoolean(false);
    private Detachable _currentTask = null;
}
