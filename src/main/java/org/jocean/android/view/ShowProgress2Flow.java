/**
 * 
 */
package org.jocean.android.view;

import java.net.URI;

import org.jocean.android.bitmap.CompositeBitmap;
import org.jocean.event.api.AbstractFlow;
import org.jocean.event.api.BizStep;
import org.jocean.event.api.DelayEvent;
import org.jocean.event.api.annotation.OnEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
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
            .handler(selfInvoker("onDrawAsCircleProgress"))
	        .handler(selfInvoker("onRepeatProgress"))
            .handler(selfInvoker("onTransportInactived"))
			.handler(selfInvoker("onTransactionFailure"))
            .handler(selfInvoker("onBitmapReceived"))
			.freeze();

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
        
        int centerx = view.getWidth() / 2;
        int centery = view.getHeight() / 2;
        int radios = Math.min(centerx, centery) / 3;
        
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
        this.popAndCancelDealyEvents();
        this._rotateDegrees += this._deltaDegree;
        view.invalidate();
        return this.fireDelayEventAndPush( generateProgressEvent(bizStep, view));
    }
    
    private BizStep changeToBizStepOf(final BizStep bizStep, final View view) {
        this.popAndCancelDealyEvents();
        return  this.fireDelayEventAndPush( generateProgressEvent(bizStep, view));
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
        this.popAndCancelDealyEvents();
        return null;
    }
    
	@OnEvent(event = "onBitmapReceived")
	private BizStep onBitmapReceived(final View view, final CompositeBitmap bitmap)
			throws Exception {
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("transaction for {} succeed.", this._uri);
		}
        this.popAndCancelDealyEvents();
		return null;
	}

    @OnEvent(event = "onTransactionFailure")
    private BizStep onTransactionFailure(final View view, final int failureReason)
            throws Exception {
        if ( LOG.isDebugEnabled() ) {
            LOG.debug("transaction for {} failed with status {}.", this._uri, failureReason);
        }
        this.popAndCancelDealyEvents();
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
    
//	private void drawText(
//	        final Canvas canvas, final String text, final float x, final int y,
//            final Paint paint) {
//	    if ( !this.drawTextByOffCanvas ) {
//	        canvas.drawText(text, x, y, paint);
//	    }
//    }

    @OnEvent(event = "drawOnView")
	private BizStep onDrawOnNONProgress(final View view, final Canvas canvas) {
		
		int center = view.getWidth() / 2;
		int radios = center / 4;
        int cy = view.getHeight() / 2;

		// 绘制圆环
        this._paint.setStyle(Paint.Style.STROKE); // 绘制空心圆
        //this.paint.setColor(Color.RED);
        this._paint.setStrokeWidth(dip2px(this._context, 5));
        this._paint.setShader(new SweepGradient(center, cy, 0, 0x1E90FF));
        canvas.drawCircle(center, center, radios, this._paint);

		return this.currentEventHandler();
	}
	
	@OnEvent(event = "onProgress")
	private BizStep onFirstProgress(final View view, final long currentByteSize, final long totalByteSize) {
		this._progress = currentByteSize;
		this._contentLength = totalByteSize;
		view.invalidate();
		return changeToBizStepOf(this.RECVCONTENT, view);
	}

    @OnEvent(event = "onProgress")
    private BizStep onRepeatProgress(final View view, final long currentByteSize, final long totalByteSize) {
        this._progress = currentByteSize;
        this._contentLength = totalByteSize;
        view.invalidate();
        return this.currentEventHandler();
    }
    
//	@OnEvent(event = "drawOnView")
//	private BizStep onDrawOnProgress(final View view, final Canvas canvas) {
//		
//		final long max = Math.max(this._contentLength, this._progress);
//		LOG.info("draw uri {} progress with {}/{}", new Object[]{ this._uri, this._progress, this._contentLength}); 
//		
//
//		int center = view.getWidth() / 2;
//		int radios = center / 4;
//
//		// 绘制圆环
//		this._paint.setStyle(Paint.Style.STROKE); // 绘制空心圆
//		this._paint.setColor(ringColor);
//		this._paint.setStrokeWidth(ringWidth);
//		canvas.drawCircle(center, center, radios, this._paint);
//
//		// draw arc
//		final RectF oval = new RectF(center - radios, center - radios, center
//				+ radios, center + radios);
//
//		this._paint.setColor(progressColor);
//		canvas.drawArc(oval, 90, 360 * this._progress / max, false, _paint);
//
//		// display _progress %
//		this._paint.setStyle(Paint.Style.FILL);
//		this._paint.setColor(textColor);
//		this._paint.setStrokeWidth(0);
//		this._paint.setTextSize(textSize);
//		this._paint.setTypeface(Typeface.DEFAULT_BOLD);
//		textProgress = (int) (1000 * (this._progress / (10.0 * max))) + "%";
//		float textWidth = _paint.measureText(textProgress);
//        drawText(canvas, textProgress, center - textWidth / 2, center + textSize
//                / 2, _paint);
//		
//		return this.currentEventHandler();
//	}
	
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
	}

	private final URI _uri;
	private long _contentLength = -1;
	private long _progress = 0;
	private final boolean drawTextByOffCanvas = false; //(android.os.Build.VERSION.SDK_INT >= 11);
	
    private float _rotateDegrees;
	private final Context _context;
	private final Paint _paint = new Paint();
    private final long _interval;
    private final float _deltaDegree;
}
