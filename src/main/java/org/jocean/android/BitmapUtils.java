/**
 * 
 */
package org.jocean.android;

import java.io.InputStream;

import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

import com.alibaba.simpleimage.codec.jpeg.JPEGDecoder;
import com.alibaba.simpleimage.io.ImageBitsInputStream;

/**
 * @author isdom
 *
 */
public class BitmapUtils {
    
    private static final Logger LOG = 
            LoggerFactory.getLogger(BitmapUtils.class);

    public static String parseImageMimeType(final InputStream is) throws Exception {
        final BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true; //确保图片不加载到内存
        
        BitmapFactory.decodeStream(is, null, opts);
        
        return opts.outMimeType;
    }
    
    public static BitmapHolder decodeStreamByMimeType(final InputStream is, final String mimeType) 
        throws Exception {
        return decodeStreamByMimeType(is, mimeType, null);
    }
    
    public static BitmapHolder decodeStreamByMimeType(final InputStream is, final String mimeType, 
            final BitmapFactory.Options opts) 
        throws Exception {
        if (  mimeType.equals("image/jpeg") ) {
            LOG.info("using simpleimage.JPEGDecoder");
            
            try {
                final JPEGDecoder decoder = new JPEGDecoder(new ImageBitsInputStream(is));
                final Triple<Integer, Integer, int[]> rawimg = decoder.decode();
                if ( null != rawimg ) {
                    return new DefaultBitmapHolder(
                            Bitmap.createBitmap(rawimg.getThird(), rawimg.getFirst(), rawimg.getSecond(), Config.ARGB_8888));
                }
            }
            catch (Exception e) {
                LOG.warn("exception when using simpleimage-lite to decode jpeg, detail: {}", 
                        ExceptionUtils.exception2detail(e));
            }
            return null;
        }
        else {
            LOG.info("try using BitmapFactory.decodeStream");
            
            final Bitmap bitmap = ( null == opts ? BitmapFactory.decodeStream(is) 
                    : BitmapFactory.decodeStream(is, null, opts));
            return (null != bitmap ? new DefaultBitmapHolder(bitmap) : null);
        }
    }
}
