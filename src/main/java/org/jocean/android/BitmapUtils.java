/**
 * 
 */
package org.jocean.android;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jocean.android.bitmap.BitmapBlock;
import org.jocean.android.bitmap.BitmapsPool;
import org.jocean.android.bitmap.CompositeBitmap;
import org.jocean.idiom.ReferenceCounted;
import org.jocean.idiom.pool.BytesPool;
import org.jocean.idiom.pool.IntsPool;
import org.jocean.idiom.pool.ObjectPool.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

/**
 * @author isdom
 *
 */
public class BitmapUtils {
    
    private static final Logger LOG = 
            LoggerFactory.getLogger(BitmapUtils.class);

    public static final class Context {
        
        public Context(final BytesPool bytesPool, final IntsPool intsPool ) {
            this.bytesPool = bytesPool;
            this.intsPool = intsPool;
        }
        
        final BytesPool bytesPool;
        final IntsPool intsPool;
    }
    
    public static String parseImageMimeType(final InputStream is) throws Exception {
        final BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true; //确保图片不加载到内存
        
        BitmapFactory.decodeStream(is, null, opts);
        
        return opts.outMimeType;
    }
    
    public static BitmapHolder decodeStream(final Context ctx, final InputStream is) 
            throws Exception {
        return decodeStream(ctx, is, null);
    }
        
    public static BitmapHolder decodeStream(final Context ctx, final InputStream is, final BitmapFactory.Options opts) 
        throws Exception {
        if ( !is.markSupported() ) {
            // input stream 不支持mark, 则直接返回 null
            return null;
        }
        
        is.mark(0);
        final String mimeType = parseImageMimeType(is);
        is.reset();
        return decodeStreamByMimeType(ctx, is, mimeType, opts);
    }
    
    public static BitmapHolder decodeStreamByMimeType(final Context ctx, final InputStream is, final String mimeType) 
        throws Exception {
        return decodeStreamByMimeType(ctx, is, mimeType, null);
    }
    
    public static BitmapHolder decodeStreamByMimeType(final Context ctx, final InputStream is, final String mimeType, 
            final BitmapFactory.Options opts) 
        throws Exception {
        /*
        if (  mimeType.equals("image/jpeg") ) {
            LOG.info("using simpleimage.JPEGDecoder");
            
            try {
                final JPEGDecoder decoder = new JPEGDecoder(ctx.bytesPool, new ImageBitsInputStream(is));
                final RawImage rawimg = decoder.decode(ctx.intsPool);
                if ( null != rawimg ) {
                    final Bitmap bitmap = 
                            Bitmap.createBitmap(rawimg.getWidth(), rawimg.getHeight(), Config.ARGB_8888);
                    final ReadableInts ints = IntsBlob.Utils.releaseAndGenReadable(rawimg.);
                    try {
                        for ( int y = 0; y < rawimg.getSecond(); y++) {
                            for ( int x = 0; x < rawimg.getFirst(); x++) {
                                bitmap.setPixel(x, y, ints.read());
                            }
                        }
                        return new DefaultBitmapHolder(bitmap);
                    }
                    finally {
                        ints.close();
                    }
                }
            }
            catch (Exception e) {
                LOG.warn("exception when using simpleimage-lite to decode jpeg, detail: {}", 
                        ExceptionUtils.exception2detail(e));
            }
            return null;
        }
        else*/ {
            LOG.info("try using BitmapFactory.decodeStream");
            
            final Bitmap bitmap = ( null == opts ? BitmapFactory.decodeStream(is) 
                    : BitmapFactory.decodeStream(is, null, opts));
            return (null != bitmap ? new DefaultBitmapHolder(bitmap) : null);
        }
    }
}
