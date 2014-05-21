/**
 * 
 */
package org.jocean.android.bitmap;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jocean.android.bitmap.BitmapTrash.RecyclePolicy;
import org.jocean.idiom.AbstractReferenceCounted;
import org.jocean.idiom.Propertyable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
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
            LoggerFactory.getLogger(CompositeBitmap.class);

    private static BitmapTrash _TRASH = new BitmapTrash(RecyclePolicy.PRE_HONEYCOMB_ONLY);
    
    public static void initBitmapTrash(final BitmapTrash trash) {
        _TRASH = trash;
    }
    
    CompositeBitmap(final Bitmap bitmap, final Map<String, Object> props) {
        this._width = bitmap.getWidth();
        this._height = bitmap.getHeight();
        this._bitmap = bitmap;
//        this._blocks = new ArrayList<Ref<BitmapBlock>>(blocks.size());
        if ( null != props ) {
            this._properties.putAll(props);
        }
        
//        ReferenceCounted.Utils.copyAllAndRetain(blocks, this._blocks);

        this._sizeInBytes = bitmap.getRowBytes() * bitmap.getHeight();
//                calcSizeInBytes( this._blocks );
        
        final int totalSize = _TOTAL_SIZE.addAndGet( this._sizeInBytes );
        
        if ( LOG.isTraceEnabled() ) {
            LOG.trace("({}) created, Total CompositeBitmap cost:({})Kbytes.", 
                this, 
                totalSize / 1024.0f);
        }
    }

    Bitmap getBitmap() {
        return this._bitmap;
    }
    
    /*
    @JSONCreator
    private CompositeBitmap(
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
    */
    
    public int sizeInBytes() {
        return this._sizeInBytes;
    }
    
    /**
     * @return
     */
//    private static int calcSizeInBytes(final Collection<Ref<BitmapBlock>> blocks) {
//        int total = 0;
//        for ( Ref<BitmapBlock> block : blocks ) {
//            total += block.object().rawSizeInBytes();
//        }
//        return total;
//    }

    @SuppressWarnings("unchecked")
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

    public int getWidth() {
        return this._width;
    }

    public int getHeight() {
        return this._height;
    }    
    
//    @JSONField(name = "blockCount")
//    public int getBlockCount() {
//        return this._blocks.size();
//    }
    
    @Override
    protected void deallocate() {
//        ReferenceCounted.Utils.releaseAllAndClear(this._blocks);
        _TRASH.recycle(this._bitmap);
        final int totalSize = _TOTAL_SIZE.addAndGet( -this._sizeInBytes );
        if ( LOG.isTraceEnabled() ) {
            LOG.trace("({}) released, Total CompositeBitmap cost:({})Kbytes.", 
                    this, 
                    totalSize / 1024.0f);
        }
    }
    
    public void draw(final Canvas canvas, final int left, final int top, final Paint paint) {
//        for ( Ref<BitmapBlock> block : this._blocks ) {
//            block.object().draw(canvas, left, top, paint);
//        }
        canvas.drawBitmap(this._bitmap, left, top, paint);
    }

    public void encodeTo(final OutputStream os, final CompressFormat format, final int quality) 
        throws Exception {
        this._bitmap.compress(format, quality, os);
        /*
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
        */
    }
    
    public static CompositeBitmap decodeFrom(
            final InputStream is, 
            final Bitmap.Config config, 
            final Map<String, Object> toinit) 
            throws Exception {
        if ( !is.markSupported() ) {
            throw new RuntimeException("CompositeBitmap.decodeFrom's parameter: InputStream must support mark.");
        }
        
        if ( SDK11.isSDKVersionEqualsOrOlderThanHONEYCOMB() ) {
            return deocdeForSDKVersionEqualsOrLaterThanHONEYCOMB(is, config, toinit);
        }
        else {
            return deocdeForSDKVersionEarlierThanHONEYCOMB(is, config, toinit);
        }
        /*
        _CURRENT_POOL.set(pool);
        _CURRENT_PROPERTIES.set(toinit);
        final Ref<int[]> ints = pool.borrowBlockSizeInts();
        
        try {
            final DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
    
            final CompositeBitmap bitmap = JSON.parseObject( dis.readUTF(), CompositeBitmap.class);
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
        */
    }

    private static CompositeBitmap deocdeForSDKVersionEarlierThanHONEYCOMB(
            final InputStream is, 
            final Config config, 
            final Map<String, Object> toinit) {
        final BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = config;
        final Bitmap decoded = BitmapFactory.decodeStream(is, null, opts);
        if ( null != decoded ) {
            //  TODO, copy decoded bitmap to reuse bitmap, and recycle this bitmap?
            return new CompositeBitmap(decoded, toinit);
        }
        else {
            return null;
        }
    }

    /**
     * @param is
     * @param config
     * @param toinit
     * @return
     * @throws Exception
     */
    private static CompositeBitmap deocdeForSDKVersionEqualsOrLaterThanHONEYCOMB(
            final InputStream is,
            final Bitmap.Config config, 
            final Map<String, Object> toinit)
            throws Exception {
        //  对于SDK版本大于等 3.0.x 的尝试重用bitmap
        final BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true; //确保图片不加载到内存
        
        is.mark(0);
        BitmapFactory.decodeStream(is, null, opts);
        is.reset();
        
        opts.inJustDecodeBounds = false;
        final Bitmap reuse = _TRASH.findAndReuse(opts.outWidth, opts.outHeight, config);
        if ( null != reuse ) {
            SDK11.addInBitmapOption(opts, reuse);
        }
        SDK11.setMutable(opts);
        opts.inDensity = 0;
        opts.inTargetDensity = 0;
        opts.inScreenDensity = 0;
        opts.inSampleSize = 1;
        opts.inScaled = false;
        opts.inPreferredConfig = config;
        final Bitmap decoded = BitmapFactory.decodeStream(is, null, opts);
        if ( null != decoded ) {
            if (decoded == reuse) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("CompositeBitmap.decodeFrom: reuse bitmap({}) succeed, and it's detail: w:{}, h:{}, config:{}",
                            reuse, reuse.getWidth(), reuse.getHeight(), reuse.getConfig());
                }
            }
            else if ( null != reuse ){
                if (LOG.isTraceEnabled()) {
                    LOG.trace("CompositeBitmap.decodeFrom: !NOT! reuse bitmap({})/w:{}/h:{}/config:{}, decode create new bitmap({})/w:{}/h:{}/config:{}.",
                            reuse, reuse.getWidth(), reuse.getHeight(), reuse.getConfig(),
                            decoded, decoded.getWidth(), decoded.getHeight(), decoded.getConfig());
                }
                // just recycle reuse bitmap again
                _TRASH.recycle(reuse);
            }
            return new CompositeBitmap(decoded, toinit);
        }
        else {
            // TODO, decoded is null, try re-decode again without reuse
            //  ...
            return null;
        }
    }
    
    /*
    private static void decodeTo(final BitmapBlock block, final DataInputStream dis, final int[] ints) 
        throws Exception {
        final int readSize = block.usedIntCount();
        for ( int idx = 0; idx < readSize; idx++ ) {
            ints[idx] = dis.readInt();
        }
        block.bitmap().setPixels(ints, 0, block.getWidth(), 0, 0, block.getWidth(), block.getHeight());
    }
    */

    @Override
    public String toString() {
        return "CompositeBitmap ["+Integer.toHexString(hashCode()) 
                + ", width=" + _width + ", height="
                + _height + ", sizeInBytes=(" + _sizeInBytes / 1024.0f + ")KBytes, config="
                + this._bitmap.getConfig() + ", _properties=" + _properties + "]";
    }

//    public static ThreadLocal<BitmapsPool> _CURRENT_POOL = new ThreadLocal<BitmapsPool>();
//    public static ThreadLocal<Map<String, Object>> _CURRENT_PROPERTIES = new ThreadLocal<Map<String, Object>>();
    
//    private final BitmapsPool _pool;
    private final int _width;
    private final int _height;
    private final int _sizeInBytes;
//    private final List<Ref<BitmapBlock>> _blocks;
    private final Bitmap _bitmap;
    private final Map<String, Object> _properties = new HashMap<String, Object>();
}
