/**
 * 
 */
package org.jocean.android.bitmap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.fastjson.annotation.JSONField;

/**
 * @author isdom
 *
 */
public class MultiBitmap extends AbstractReferenceCounted<MultiBitmap> 
    implements Propertyable<MultiBitmap> {

    private static final AtomicInteger _TOTAL_SIZE = new AtomicInteger(0);
    
    private static final Logger LOG = 
            LoggerFactory.getLogger(MultiBitmap.class);
    
    MultiBitmap(final BitmapsPool pool, final int w, final int h, 
            final Collection<Ref<BitmapBlock>> blocks, final Map<String, Object> props) {
        this._pool = pool;
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

    @JSONCreator
    private MultiBitmap(
            @JSONField(name="width")
            final int w, 
            @JSONField(name="height")
            final int h, 
            @JSONField(name="blockCount")
            final int blockCount
            ) {
        this._width = w;
        this._height = h;
        
        final Map<String, Object> props = _CURRENT_PROPERTIES.get();
        if ( null != props ) {
            this._properties.putAll(props);
        }
        this._blocks = new ArrayList<Ref<BitmapBlock>>(blockCount);
        
        this._pool = _CURRENT_POOL.get();
        if ( null == this._pool ) {
            throw new RuntimeException("Internal Error: current bitmaps pool is null.");
        }
        for ( int idx = 0; idx < blockCount; idx++) {
            this._blocks.add( this._pool.retainObject());
        }
        this._sizeInBytes = calcSizeInBytes( this._blocks );
        
        final int totalSize = _TOTAL_SIZE.addAndGet( this._sizeInBytes );
        
        if ( LOG.isTraceEnabled() ) {
            LOG.trace("({}) created from InputStream, Total CompositeBitmap cost:({})Kbytes.", 
                this, 
                totalSize / 1024.0f);
        }
    }
    
    public BitmapsPool pool() {
        return this._pool;
    }
    
    @JSONField(serialize = false)
    public int sizeInBytes() {
        return this._sizeInBytes;
    }
    
    /**
     * @return
     */
    private static int calcSizeInBytes(final Collection<Ref<BitmapBlock>> blocks) {
        int total = 0;
        for ( Ref<BitmapBlock> block : blocks ) {
            total += block.object().rawSizeInBytes();
        }
        return total;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V getProperty(final String key) {
        return (V)this._properties.get(key);
    }

    @Override
    public <V> MultiBitmap setProperty(final String key, final V obj) {
        this._properties.put(key, obj);
        return this;
    }

    @JSONField(serialize = false)
    @Override
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(this._properties);
    }

    @JSONField(name = "width")
    public int getWidth() {
        return this._width;
    }

    @JSONField(name = "height")
    public int getHeight() {
        return this._height;
    }    
    
    @JSONField(name = "blockCount")
    public int getBlockCount() {
        return this._blocks.size();
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
    
    public void draw(final Canvas canvas, final int left, final int top, final Paint paint) {
        for ( Ref<BitmapBlock> block : this._blocks ) {
            block.object().draw(canvas, left, top, paint);
        }
    }

    public void encodeTo(final OutputStream os) 
        throws Exception {
        final DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));
        final Ref<int[]> ints = this._pool.borrowBlockSizeInts();

        try {
            dos.writeUTF(JSON.toJSONString(this));
            for ( Ref<BitmapBlock> block : this._blocks ) {
                dos.writeUTF(JSON.toJSONString(block.object()));
                block.object().bitmap().getPixels(ints.object(), 0, block.object().getWidth(), 0, 0, 
                        block.object().getWidth(), block.object().getHeight());
                final int writeSize = block.object().usedIntCount();
                for ( int idx = 0; idx < writeSize; idx++ ) {
                    dos.writeInt(ints.object()[idx]);
                }
            }
            dos.flush();
        }
        finally {
            ints.release();
        }
    }
    
    interface RECT {
        @JSONField(name = "left")
        public int getLeft();
        
        @JSONField(name = "left")
        public void setLeft(int left);

        @JSONField(name = "top")
        public int getTop();

        @JSONField(name = "top")
        public void setTop(int top);
        
        @JSONField(name = "width")
        public int getWidth();

        @JSONField(name = "width")
        public void setWidth(int width);
        
        @JSONField(name = "height")
        public int getHeight();
        
        @JSONField(name = "height")
        public void setHeight(int height);
    };
       
    public static MultiBitmap decodeFrom(final InputStream is, final BitmapsPool pool, final Map<String, Object> toinit) 
            throws Exception {
        
        _CURRENT_POOL.set(pool);
        _CURRENT_PROPERTIES.set(toinit);
        final Ref<int[]> ints = pool.borrowBlockSizeInts();
        
        try {
            final DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
    
            final MultiBitmap bitmap = JSON.parseObject( dis.readUTF(), MultiBitmap.class);
            for ( int idx = 0; idx < bitmap.getBlockCount(); idx++ ) {
                final BitmapBlock block = bitmap._blocks.get(idx).object();
                final String utf = dis.readUTF();
                final RECT rect = JSON.parseObject(utf, RECT.class);
                block.set(rect.getLeft(), rect.getTop(), rect.getWidth(), rect.getHeight());
                decodeTo(block, dis, ints.object());
            }
            return bitmap;
        }
        finally {
            ints.release();
            _CURRENT_POOL.remove();
            _CURRENT_PROPERTIES.remove();
        }
    }
    
    private static void decodeTo(final BitmapBlock block, final DataInputStream dis, final int[] ints) 
        throws Exception {
        final int readSize = block.usedIntCount();
        for ( int idx = 0; idx < readSize; idx++ ) {
            ints[idx] = dis.readInt();
        }
        block.bitmap().setPixels(ints, 0, block.getWidth(), 0, 0, block.getWidth(), block.getHeight());
    }

    @Override
    public String toString() {
        return "CompositeBitmap ["+Integer.toHexString(hashCode()) 
                + ", width=" + _width + ", height="
                + _height + ", sizeInBytes=(" + _sizeInBytes / 1024.0f + ")KBytes, blocks count="
                + _blocks.size() + ", _properties=" + _properties + "]";
    }

    public static ThreadLocal<BitmapsPool> _CURRENT_POOL = new ThreadLocal<BitmapsPool>();
    public static ThreadLocal<Map<String, Object>> _CURRENT_PROPERTIES = new ThreadLocal<Map<String, Object>>();
    
    private final BitmapsPool _pool;
    private final int _width;
    private final int _height;
    private final int _sizeInBytes;
    private final List<Ref<BitmapBlock>> _blocks;
    private final Map<String, Object> _properties = new HashMap<String, Object>();
}
