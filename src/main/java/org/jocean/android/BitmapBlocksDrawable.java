package org.jocean.android;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jocean.idiom.Detachable;
import org.jocean.idiom.Propertyable;
import org.jocean.idiom.ReferenceCounted;
import org.jocean.idiom.pool.ObjectPool.Ref;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class BitmapBlocksDrawable extends Drawable 
    implements Propertyable<BitmapBlocksDrawable>, Detachable {

    public BitmapBlocksDrawable(final int w, final int h, 
            final Collection<Ref<BitmapBlock>> blocks, final Map<String, Object> props) {
        this._width = w;
        this._height = h;
        this._blocks = new ArrayList<Ref<BitmapBlock>>(blocks.size());
        if ( null != props ) {
            this._properties.putAll(props);
        }
        
        ReferenceCounted.Utils.copyAllAndRetain(blocks, _blocks);
    }

    @Override
    public <V> V getProperty(final String key) {
        return (V)this._properties.get(key);
    }

    @Override
    public <V> BitmapBlocksDrawable setProperty(final String key, final V obj) {
        this._properties.put(key, obj);
        return this;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(this._properties);
    }
    
    @Override
    public void detach() {
        ReferenceCounted.Utils.releaseAllAndClear(this._blocks);
    }

//    public RawImage rawImage() {
//        return this._img;
//    }

    // public void setTargetDensity(DisplayMetrics metrics) {
    // mTargetDensity = metrics.densityDpi;
    // computeBitmapSize();
    // }
    //
    // private void computeBitmapSize() {
    // this._width = mBitmap.getScaledWidth(mTargetDensity);
    // this._height = mBitmap.getScaledHeight(mTargetDensity);
    // }

    @Override
    protected void onBoundsChange(final Rect bounds) {
        this._bounds.set(bounds);
        this._sx = (float) bounds.width() / this._width;
        this._sy = (float) bounds.height() / this._height;
    }

    @Override
    public void draw(final Canvas canvas) {
        final int saveCount = canvas.save();

        try {
            canvas.scale(this._sx, this._sy, this._bounds.left,
                    this._bounds.top);
            for ( Ref<BitmapBlock> block : this._blocks ) {
                block.object().draw(canvas, this._bounds.left, this._bounds.top, null);
            }
        } finally {
            canvas.restoreToCount(saveCount);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getOpacity() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getIntrinsicWidth() {
        return this._width;
    }

    @Override
    public int getIntrinsicHeight() {
        return this._height;
    }

    protected final Rect _bounds = new Rect();
    protected float _sx = 1.0f;
    protected float _sy = 1.0f;
    
    private final int _width;
    private final int _height;
    private final List<Ref<BitmapBlock>> _blocks;
    private final Map<String, Object> _properties = new HashMap<String, Object>();

    // private int mTargetDensity;
}
