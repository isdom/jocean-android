/**
 * 
 */
package org.jocean.android;

import java.io.InputStream;

import org.jocean.idiom.pool.BytesPool;
import org.jocean.idiom.pool.IntsPool;

import android.graphics.BitmapFactory;

/**
 * @author isdom
 *
 */
public class BitmapUtils {
    
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
}
