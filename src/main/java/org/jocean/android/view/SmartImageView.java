/**
 * 
 */
package org.jocean.android.view;

import org.jocean.idiom.Detachable;
import org.jocean.idiom.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * @author isdom
 * 
 */
public class SmartImageView extends ImageView {

	private static final Logger LOG = LoggerFactory
			.getLogger(SmartImageView.class);
	
	public SmartImageView(Context context) {
		super(context);
	}

	public SmartImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public SmartImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        
        final DrawerOnView drawer = this._drawer;
        if ( null != drawer ) {
            drawer.drawOnView(this, canvas);
        }
	}

	public void setDrawer(final DrawerOnView drawable) {
		this._drawer = drawable;
	}

	public void replaceDrawable(final Drawable drawable) {
        final Drawable oldDrawable = this.getDrawable();
        if ( null != oldDrawable && (oldDrawable instanceof Detachable) ) {
            try {
                ((Detachable)oldDrawable).detach();
            }
            catch (Exception e) {
                LOG.warn("exception when replaceDrawable invoke oldDrawable.detach, detail:{}", 
                        ExceptionUtils.exception2detail(e));
            }
        }
        this.setImageDrawable(drawable);
	}
	
	private volatile DrawerOnView _drawer = null;
}
