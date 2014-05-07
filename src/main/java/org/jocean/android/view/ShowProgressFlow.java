/**
 * 
 */
package org.jocean.android.view;

import java.net.URI;

import org.jocean.android.bitmap.BitmapBlock;
import org.jocean.android.bitmap.BitmapsPool;
import org.jocean.android.bitmap.CompositeBitmap;
import org.jocean.event.api.AbstractFlow;
import org.jocean.event.api.BizStep;
import org.jocean.event.api.annotation.OnEvent;
import org.jocean.idiom.pool.ObjectPool.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/**
 * @author isdom
 *
 */
public class ShowProgressFlow extends AbstractFlow<ShowProgressFlow> {
	private static final Logger LOG = LoggerFactory
			.getLogger(ShowProgressFlow.class);

    public final BizStep START = new BizStep("showprogress.START")
            .handler(selfInvoker("onStartLoadFromDisk"))
            .handler(selfInvoker("onStartDownload"))
            .handler(selfInvoker("onBitmapCached"))
            .freeze();
    
    private final BizStep LOADLOCAL = new BizStep("showprogress.LOADLOCAL")
            .handler(selfInvoker("onDrawOnLoadLocal"))
            .handler(selfInvoker("onBitmapCached"))
            //  从local disk 获取Bitmap可能失败(解码错误, IO错误等), 
            //  因此仍然可能需要从远端网络下载Bitmap数据
            .handler(selfInvoker("onStartDownload"))
            .freeze();
    
    private final BizStep STARTDOWNLOADING = new BizStep("showprogress.STARTDOWNLOADING")
			.handler(selfInvoker("onTransportActived"))
			.handler(selfInvoker("onDrawOnConnecting"))
			.handler(selfInvoker("onTransactionFailure"))
			.freeze();

	private final BizStep CONNECTED = new BizStep("showprogress.CONNECTED")
			.handler(selfInvoker("onProgress"))
			.handler(selfInvoker("onDrawOnNONProgress"))
            .handler(selfInvoker("onTransportInactived"))
			.handler(selfInvoker("onTransactionFailure"))
			.freeze();

	private final BizStep RECVCONTENT = new BizStep("showprogress.RECVCONTENT")
			.handler(selfInvoker("onProgress"))
			.handler(selfInvoker("onDrawOnProgress"))
            .handler(selfInvoker("onTransportInactived"))
			.handler(selfInvoker("onTransactionFailure"))
            .handler(selfInvoker("onBitmapReceived"))
			.freeze();

	@OnEvent(event = "onStartLoadFromDisk")
    public BizStep onStartLoadFromDisk(final View view)
            throws Exception {
        view.invalidate();
	    return this.LOADLOCAL;
    }
    
	@OnEvent(event = "onStartDownload")
    public BizStep onStartDownload(final View view) 
            throws Exception {
        view.invalidate();
	    return this.STARTDOWNLOADING;
    }
    
    @OnEvent(event = "onBitmapCached")
    private BizStep onBitmapCached(final View view, final CompositeBitmap bitmap, final boolean inMemoryCache)
            throws Exception {
        if ( LOG.isDebugEnabled() ) {
            LOG.debug("transaction for {} succeed.", this._uri);
        }
        return null;
    }
    
	@OnEvent(event = "onBitmapReceived")
	private BizStep onBitmapReceived(final View view, final CompositeBitmap bitmap)
			throws Exception {
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("transaction for {} succeed.", this._uri);
		}
		return null;
	}

    @OnEvent(event = "onTransactionFailure")
    private BizStep onTransactionFailure(final View view, final int failureReason)
            throws Exception {
        if ( LOG.isDebugEnabled() ) {
            LOG.debug("transaction for {} failed with status {}.", this._uri, failureReason);
        }
        return null;
    }
    
	@OnEvent(event = "onTransportActived")
	private BizStep onTransportActived(final View view) {
		view.invalidate();
		return CONNECTED;
	}

    @OnEvent(event = "onTransportInactived")
    private BizStep onTransportInactived(final View view) {
        view.invalidate();
        return STARTDOWNLOADING;
    }
    
    @OnEvent(event = "drawOnView")
    private BizStep onDrawOnLoadLocal(final View view, final Canvas canvas) {
        
        int center = view.getWidth() / 2;
        int radios = center / 4;

        // 绘制圆环
        this.paint.setStyle(Paint.Style.STROKE); // 绘制空心圆
        this.paint.setColor(Color.LTGRAY);
        this.paint.setStrokeWidth(ringWidth);
        canvas.drawCircle(center, center, radios, this.paint);

        // display _progress %
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(textColor);
        this.paint.setStrokeWidth(0);
        this.paint.setTextSize(textSize);
        this.paint.setTypeface(Typeface.DEFAULT_BOLD);
        textProgress = "^_^";
        final float textWidth = paint.measureText(textProgress);
        drawText(canvas, textProgress, center - textWidth / 2, center + textSize
              / 2, paint);
        
        return this.currentEventHandler();
    }
    
	@OnEvent(event = "drawOnView")
	private BizStep onDrawOnConnecting(final View view, final Canvas canvas) {
		
		int center = view.getWidth() / 2;
		int radios = center / 4;

		// 绘制圆环
		this.paint.setStyle(Paint.Style.STROKE); // 绘制空心圆
		this.paint.setColor(Color.RED);
		this.paint.setStrokeWidth(ringWidth);
		canvas.drawCircle(center, center, radios, this.paint);

		// display _progress %
		this.paint.setStyle(Paint.Style.FILL);
		this.paint.setColor(textColor);
		this.paint.setStrokeWidth(0);
		this.paint.setTextSize(textSize);
		this.paint.setTypeface(Typeface.DEFAULT_BOLD);
		textProgress = "-->";
		final float textWidth = paint.measureText(textProgress);
		drawText(canvas, textProgress, center - textWidth / 2, center + textSize
              / 2, paint);
		
		return this.currentEventHandler();
	}
	
	private void drawText(
	        final Canvas canvas, final String text, final float x, final int y,
            final Paint paint) {
	    if ( !this.drawTextByOffCanvas ) {
	        canvas.drawText(text, x, y, paint);
	    }
	    else {
	        final Ref<BitmapBlock> block = this._pool.retainObject();
	        
	        try {
	            block.object().clsBitmap();
    	        block.object().canvas().drawText(textProgress, 0, paint.getTextSize(), paint);
    	        canvas.drawBitmap(block.object().bitmap(), x, y - paint.getTextSize(), null);
	        }
	        finally {
	            block.release();
	        }
	    }
    }

    @OnEvent(event = "drawOnView")
	private BizStep onDrawOnNONProgress(final View view, final Canvas canvas) {
		
		int center = view.getWidth() / 2;
		int radios = center / 4;

		// 绘制圆环
		this.paint.setStyle(Paint.Style.STROKE); // 绘制空心圆
		this.paint.setColor(Color.GREEN);
		this.paint.setStrokeWidth(ringWidth);
		canvas.drawCircle(center, center, radios, this.paint);

		// display _progress %
		this.paint.setStyle(Paint.Style.FILL);
		this.paint.setColor(textColor);
		this.paint.setStrokeWidth(0);
		this.paint.setTextSize(textSize);
		this.paint.setTypeface(Typeface.DEFAULT_BOLD);
		textProgress = "<--";
		final float textWidth = paint.measureText(textProgress);
        drawText(canvas, textProgress, center - textWidth / 2, center + textSize
                / 2, paint);
		
		return this.currentEventHandler();
	}
	
	@OnEvent(event = "onProgress")
	private BizStep onProgress(final View view, final long currentByteSize, final long totalByteSize) {
		this._progress = currentByteSize;
		this._contentLength = totalByteSize;
		view.invalidate();
		return RECVCONTENT;
	}

	@OnEvent(event = "drawOnView")
	private BizStep onDrawOnProgress(final View view, final Canvas canvas) {
		
		final long max = Math.max(this._contentLength, this._progress);
		LOG.info("draw uri {} progress with {}/{}", new Object[]{ this._uri, this._progress, this._contentLength}); 
		

		int center = view.getWidth() / 2;
		int radios = center / 4;

		// 绘制圆环
		this.paint.setStyle(Paint.Style.STROKE); // 绘制空心圆
		this.paint.setColor(ringColor);
		this.paint.setStrokeWidth(ringWidth);
		canvas.drawCircle(center, center, radios, this.paint);

		// draw arc
		final RectF oval = new RectF(center - radios, center - radios, center
				+ radios, center + radios);

		this.paint.setColor(progressColor);
		canvas.drawArc(oval, 90, 360 * this._progress / max, false, paint);

		// display _progress %
		this.paint.setStyle(Paint.Style.FILL);
		this.paint.setColor(textColor);
		this.paint.setStrokeWidth(0);
		this.paint.setTextSize(textSize);
		this.paint.setTypeface(Typeface.DEFAULT_BOLD);
		textProgress = (int) (1000 * (this._progress / (10.0 * max))) + "%";
		float textWidth = paint.measureText(textProgress);
        drawText(canvas, textProgress, center - textWidth / 2, center + textSize
                / 2, paint);
		
		return this.currentEventHandler();
	}
	
	public ShowProgressFlow(final BitmapsPool pool, final Context context, final URI uri) {
		this._uri = uri;
		this._pool = pool;
		
		this.paint.setAntiAlias(true); // 消除锯齿

		this.ringWidth = dip2px(context, 3); // 设置圆环宽度;
		this.ringColor = Color.BLACK;// 黑色进度条背景
		this.progressColor = Color.WHITE;// 白色进度条
		this.textColor = Color.BLACK;// 黑色数字显示进度;
		this.textSize = 15;// 默认字体大小
	}

	private final URI _uri;
	private long _contentLength = -1;
	private long _progress = 0;
	private final BitmapsPool _pool;
	private final boolean drawTextByOffCanvas = (android.os.Build.VERSION.SDK_INT >= 11);
	
	/**
	 * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
	 */
	public static int dip2px(final Context context, final float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	private final Paint paint = new Paint();

	private int ringWidth;

	// 圆环的颜色
	private int ringColor;

	// 进度条颜色
	private int progressColor;
	// 字体颜色
	private int textColor;
	// 字的大小
	private int textSize;
	private String textProgress;
	
}
