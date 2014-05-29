/**
 * 
 */
package org.jocean.android.view;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.jocean.android.bitmap.CompositeBitmap;
import org.jocean.event.api.AbstractFlow;
import org.jocean.event.api.BizStep;
import org.jocean.event.api.DelayEvent;
import org.jocean.event.api.annotation.OnEvent;
import org.jocean.idiom.Detachable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.view.View;

/**
 * @author isdom
 *
 */
public class ShowProgress2Flow extends AbstractFlow<ShowProgress2Flow> {
    
	private static final int[] CIRCLE_PROGRESS_COLORS = new int[]{
	    Color.GREEN,Color.GRAY,Color.MAGENTA,Color.RED,Color.WHITE};

    private static final Logger LOG = LoggerFactory
			.getLogger(ShowProgress2Flow.class);

    public final BizStep START = new BizStep("showprogress2.START")
            .handler(selfInvoker("onDrawAsCircleProgress"))
            .handler(selfInvoker("onStartLoadFromDisk"))
            .handler(selfInvoker("onStartDownload"))
            .handler(selfInvoker("onBitmapCached"))
            .freeze();
    
    private final BizStep LOADLOCAL = new BizStep("showprogress2.LOADLOCAL")
            .handler(selfInvoker("onDrawAsCircleProgress"))
            .handler(selfInvoker("onBitmapCached"))
            //  从local disk 获取Bitmap可能失败(解码错误, IO错误等), 
            //  因此仍然可能需要从远端网络下载Bitmap数据
            .handler(selfInvoker("onStartDownload"))
            .freeze();
    
    private final BizStep STARTDOWNLOADING = new BizStep("showprogress2.STARTDOWNLOADING")
            .handler(selfInvoker("onDrawAsCircleProgress"))
			.handler(selfInvoker("onTransportActived"))
			.handler(selfInvoker("onTransactionFailure"))
			.freeze();

	private final BizStep CONNECTED = new BizStep("showprogress2.CONNECTED")
            .handler(selfInvoker("onDrawAsCircleProgress"))
			.handler(selfInvoker("onFirstProgress"))
            .handler(selfInvoker("onTransportInactived"))
			.handler(selfInvoker("onTransactionFailure"))
			.freeze();

	private final BizStep RECVCONTENT = new BizStep("showprogress2.RECVCONTENT")
            .handler(selfInvoker("onDrawOnProgress"))
	        .handler(selfInvoker("onRepeatProgress"))
            .handler(selfInvoker("onTransportInactived"))
			.handler(selfInvoker("onTransactionFailure"))
            .handler(selfInvoker("onBitmapReceived"))
			.freeze();

    public void setRadioRatio(final float ratio) {
        this._radioRatio = ratio;
    }

    public void setLeftPaddingRatio(final float ratio) {
        this._leftPaddingRatio = ratio;
    }
    
    public void setRightPaddingRatio(final float ratio) {
        this._rightPaddingRatio = ratio;
    }
    
    public void setTopPaddingRatio(final float ratio) {
        this._topPaddingRatio = ratio;
    }
    
    public void setBottomPaddingRatio(final float ratio) {
        this._bottomPaddingRatio = ratio;
    }
    
    public void fireAndPush(final DelayEvent delayEvent) {
        this.fireDelayEventAndAddTo(delayEvent, this._timers);
    }
    
	public DelayEvent generateProgressEvent(
	        final BizStep templateBizStep, 
	        final View view) {
	    final DelayEvent delayEvent = templateBizStep.makeDelayEvent(
                    selfInvoker("onUpdateCircleProgress"), this._interval)
                .args(templateBizStep, view);
	    ((BizStep)delayEvent.owner()).freeze();
	    return delayEvent;
    }
	
    private void drawCircleProgress(final View view, final Canvas canvas, final float rotateDegrees) {
        
        final int leftPadding = (int)(this._leftPaddingRatio * view.getWidth());
        final int rightPadding = (int)(this._rightPaddingRatio * view.getWidth());
        final int topPadding = (int)(this._topPaddingRatio * view.getHeight());
        final int bottomPadding = (int)(this._bottomPaddingRatio * view.getHeight());
        
        final int halfWidth = (view.getWidth() - leftPadding - rightPadding) / 2;
        final int centerx = halfWidth + leftPadding;
        final int halfHeight = (view.getHeight() - topPadding - bottomPadding) / 2;
        final int centery = halfHeight + topPadding;
        final int radios = (int)(Math.min(halfWidth, halfHeight) * this._radioRatio);
        
        // 绘制圆环
        this._paint.setStyle(Paint.Style.STROKE); // 绘制空心圆
        
        final int saveCount = canvas.save();
        
        try {
            canvas.rotate(rotateDegrees, centerx, centery);
            this._paint.setStrokeWidth(dip2px(this._context, 5));
            this._paint.setShader(new SweepGradient(centerx, centery, 
                    CIRCLE_PROGRESS_COLORS, null));
            canvas.drawCircle(centerx, centery, radios, this._paint);
        }
        finally {
            canvas.restoreToCount(saveCount);
        }
    }
    
    @OnEvent(event = "drawOnView")
    private BizStep onDrawAsCircleProgress(final View view, final Canvas canvas) {
        
        drawCircleProgress(view, canvas, this._rotateDegrees);
        
        return this.currentEventHandler();
    }

    @SuppressWarnings("unused")
    private BizStep onUpdateCircleProgress(
            final BizStep bizStep, 
            final View view) {
        //  remove below line will cause memory leak
        this.removeAndCancelAllDealyEvents(this._timers);
        this._rotateDegrees += this._deltaDegree;
        view.invalidate();
        return this.fireDelayEventAndAddTo( generateProgressEvent(bizStep, view), this._timers);
    }
    
    private BizStep changeToBizStepOf(final BizStep bizStep, final View view) {
        this.removeAndCancelAllDealyEvents(this._timers);
        return  this.fireDelayEventAndAddTo( generateProgressEvent(bizStep, view),this._timers);
    }
    

    @OnEvent(event = "onStartLoadFromDisk")
    public BizStep onStartLoadFromDisk(final View view)
            throws Exception {
        return changeToBizStepOf(this.LOADLOCAL, view);
    }

	@OnEvent(event = "onStartDownload")
    public BizStep onStartDownload(final View view) 
            throws Exception {
	    return changeToBizStepOf(this.STARTDOWNLOADING, view);
    }
    
    @OnEvent(event = "onBitmapCached")
    private BizStep onBitmapCached(final View view, final CompositeBitmap bitmap, final boolean inMemoryCache)
            throws Exception {
        if ( LOG.isDebugEnabled() ) {
            LOG.debug("transaction for {} succeed.", this._uri);
        }
        this.removeAndCancelAllDealyEvents(this._timers);
        return null;
    }
    
	@OnEvent(event = "onBitmapReceived")
	private BizStep onBitmapReceived(final View view, final CompositeBitmap bitmap)
			throws Exception {
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("transaction for {} succeed.", this._uri);
		}
        this.removeAndCancelAllDealyEvents(this._timers);
		return null;
	}

    @OnEvent(event = "onTransactionFailure")
    private BizStep onTransactionFailure(final View view, final int failureReason)
            throws Exception {
        if ( LOG.isDebugEnabled() ) {
            LOG.debug("transaction for {} failed with status {}.", this._uri, failureReason);
        }
        this.removeAndCancelAllDealyEvents(this._timers);
        return null;
    }
    
	@OnEvent(event = "onTransportActived")
	private BizStep onTransportActived(final View view) {
		return changeToBizStepOf(this.CONNECTED, view);
	}

    @OnEvent(event = "onTransportInactived")
    private BizStep onTransportInactived(final View view) {
        return changeToBizStepOf(this.STARTDOWNLOADING, view);
    }
    
	@OnEvent(event = "onProgress")
	private BizStep onFirstProgress(final View view, final long currentByteSize, final long totalByteSize) {
        this.removeAndCancelAllDealyEvents(this._timers);
		this._progress = currentByteSize;
		this._contentLength = totalByteSize;
		view.invalidate();
		return this.RECVCONTENT;
	}

    @OnEvent(event = "onProgress")
    private BizStep onRepeatProgress(final View view, final long currentByteSize, final long totalByteSize) {
        this._progress = currentByteSize;
        this._contentLength = totalByteSize;
        view.invalidate();
        return this.currentEventHandler();
    }
    
	@OnEvent(event = "drawOnView")
	private BizStep onDrawOnProgress(final View view, final Canvas canvas) {
		
		final long max = Math.max(this._contentLength, this._progress);

		this._downloadProgressDrawer.setMax((int)max);
		this._downloadProgressDrawer.setProgress((int)this._progress);
		this._downloadProgressDrawer.setPaddingTop(this._topPaddingRatio);
        this._downloadProgressDrawer.setPaddingBottom(this._bottomPaddingRatio);
		
		this._downloadProgressDrawer.drawOnView(view, canvas);
		
		return this.currentEventHandler();
	}
	
    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(final Context context, final float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

	public ShowProgress2Flow(final Context context, final URI uri, final long interval, final float deltaDegree) {
		this._uri = uri;
		this._context = context;
		this._paint.setAntiAlias(true); // 消除锯齿
		this._interval = interval;
		this._deltaDegree = deltaDegree;
		this._downloadProgressDrawer = new NumberProgressDrawer(context);
	}

	private final List<Detachable> _timers = new ArrayList<Detachable>();
	
	private final URI _uri;
	private long _contentLength = -1;
	private long _progress = 0;
//	private final boolean drawTextByOffCanvas = false; //(android.os.Build.VERSION.SDK_INT >= 11);
	private final NumberProgressDrawer _downloadProgressDrawer;
	private float _radioRatio = 0.2f;
	private float _bottomPaddingRatio = 0;
    private float _topPaddingRatio = 0;
    private float _leftPaddingRatio = 0;
    private float _rightPaddingRatio = 0;
	
    private float _rotateDegrees;
	private final Context _context;
	private final Paint _paint = new Paint();
    private final long _interval;
    private final float _deltaDegree;
}
