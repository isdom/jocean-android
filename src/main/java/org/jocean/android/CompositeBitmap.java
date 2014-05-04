/**
 * 
 */
package org.jocean.android;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jocean.idiom.AbstractReferenceCounted;
import org.jocean.idiom.Propertyable;
import org.jocean.idiom.ReferenceCounted;
import org.jocean.idiom.pool.ObjectPool.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * @author isdom
 *
 */
public class CompositeBitmap extends AbstractReferenceCounted<CompositeBitmap> 
    implements Propertyable<CompositeBitmap> {

    private static final AtomicInteger _TOTAL_SIZE = new AtomicInteger(0);
    
    private static final Logger LOG = 
            LoggerFactory.getLogger(CompositeBitmapDrawable.class);
    
    public CompositeBitmap(final int w, final int h, 
            final Collection<Ref<BitmapBlock>> blocks, final Map<String, Object> props) {
        this._width = w;
        this._height = h;
        this._blocks = new ArrayList<Ref<BitmapBlock>>(blocks.size());
        if ( null != props ) {
            this._properties.putAll(props);
        }
        
        ReferenceCounted.Utils.copyAllAndRetain(blocks, this._blocks);

        this._sizeInBytes = calcSizeInBytes( this._blocks );
        
        final int totalSize = _TOTAL_SIZE.addAndGet( this._sizeInBytes );
        
        if ( LOG.isTraceEnabled() ) {
            LOG.trace("({}) created, Total CompositeBitmap cost:({})Kbytes.", 
                this, 
                totalSize / 1024.0f);
        }
    }

    public int sizeInBytes() {
        return this._sizeInBytes;
    }
    
    /**
     * @return
     */
    private static int calcSizeInBytes(final Collection<Ref<BitmapBlock>> blocks) {
        int total = 0;
        for ( Ref<BitmapBlock> block : blocks ) {
            total += block.object().sizeInBytes();
        }
        return total;
    }

    @Override
    public <V> V getProperty(final String key) {
        return (V)this._properties.get(key);
    }

    @Override
    public <V> CompositeBitmap setProperty(final String key, final V obj) {
        this._properties.put(key, obj);
        return this;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(this._properties);
    }

    @Override
    protected void deallocate() {
        ReferenceCounted.Utils.releaseAllAndClear(this._blocks);
        final int totalSize = _TOTAL_SIZE.addAndGet( -this._sizeInBytes );
        if ( LOG.isTraceEnabled() ) {
            LOG.trace("({}) released, Total CompositeBitmap cost:({})Kbytes.", 
                    this, 
                    totalSize / 1024.0f);
        }
    }
    
    public int getWidth() {
        return this._width;
    }

    public int getHeight() {
        return this._height;
    }    
    
    public void draw(final Canvas canvas, final int left, final int top, final Paint paint) {
        for ( Ref<BitmapBlock> block : this._blocks ) {
            block.object().draw(canvas, left, top, paint);
        }
    }

    @Override
    public String toString() {
        return "CompositeBitmap [width=" + _width + ", height="
                + _height + ", sizeInBytes=(" + _sizeInBytes / 1024.0f + ")KBytes, blocks count="
                + _blocks.size() + ", _properties=" + _properties + "]";
    }

    private final int _width;
    private final int _height;
    private final int _sizeInBytes;
    private final List<Ref<BitmapBlock>> _blocks;
    private final Map<String, Object> _properties = new HashMap<String, Object>();
}
