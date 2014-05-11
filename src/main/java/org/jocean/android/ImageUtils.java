/**
 * 
 */
package org.jocean.android;

import java.io.InputStream;
import java.util.Map;

import org.jocean.idiom.block.BlockUtils;
import org.jocean.idiom.block.IntsBlob;
import org.jocean.idiom.block.WriteableInts;
import org.jocean.idiom.pool.BytesPool;
import org.jocean.idiom.pool.IntsPool;
import org.jocean.image.RawImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngReader;

import com.alibaba.simpleimage.codec.jpeg.JPEGDecoder;
import com.alibaba.simpleimage.io.ImageBitsInputStream;

/**
 * @author isdom
 *
 */
public class ImageUtils {
    
    private static final Logger LOG = 
            LoggerFactory.getLogger(ImageUtils.class);

    public static final class Context {
        
        public Context(final BytesPool bytesPool, final IntsPool intsPool) {
            this.bytesPool = bytesPool;
            this.intsPool = intsPool;
            this.mimeType = null;
            this.properties = null;
        }
        
        public Context(final Context ctx, final String mimeType) {
            this.bytesPool = ctx.bytesPool;
            this.intsPool = ctx.intsPool;
            this.properties = ctx.properties;
            this.mimeType = mimeType;
        }
        
        public Context(final Context ctx, final String mimeType, final Map<String, Object> properties) {
            this.bytesPool = ctx.bytesPool;
            this.intsPool = ctx.intsPool;
            this.mimeType = mimeType;
            this.properties = properties;
        }
        
        public Context(final BytesPool bytesPool, final IntsPool intsPool, final String mimeType ) {
            this.bytesPool = bytesPool;
            this.intsPool = intsPool;
            this.mimeType = mimeType;
            this.properties = null;
        }
        
        final BytesPool bytesPool;
        final IntsPool intsPool;
        final String mimeType;
        final Map<String, Object> properties;
    }
    
    public static RawImage decodeStreamAsRawImage(final Context ctx, final InputStream is) 
        throws Exception {
        if ( ctx.mimeType.equals("image/jpeg")) {
            return decodeJPEGStreamAsRawImage(ctx, is);
        }
        else if ( ctx.mimeType.equals("image/png")) {
            return decodePNGStreamAsRawImage(ctx, is);
        }
        else {
            return null;
        }
    }
    
    public static RawImage decodeJPEGStreamAsRawImage(final Context ctx, final InputStream is) 
            throws Exception {
        final JPEGDecoder decoder = new JPEGDecoder(ctx.bytesPool, new ImageBitsInputStream(is));
        return decoder.decode(ctx.intsPool, ctx.properties);
    }
    
    public static RawImage decodePNGStreamAsRawImage(final Context ctx, final InputStream is) 
            throws Exception {
        final PngReader pngr = new PngReader(is);
        final int channels = pngr.imgInfo.channels;
        if (channels < 3 || pngr.imgInfo.bitDepth != 8) {
            LOG.warn("This method is for RGB8/RGBA8 images");
            return null;
        }
        final WriteableInts output = BlockUtils.createWriteableInts(ctx.intsPool);
        
        while(pngr.hasMoreRows()) {
            final IImageLine l1 = pngr.readRow();
            final int[] scanline = ((ImageLineInt) l1).getScanline(); // to save typing
            for (int j = 0; j < pngr.imgInfo.cols; j++) {
                final int R = scanline[j * channels] & 0xff;
                final int G = scanline[j * channels+1] & 0xff;
                final int B = scanline[j * channels+2] & 0xff;
                final int A = pngr.imgInfo.alpha ? (scanline[j * channels+3] & 0xff) : 0xff;
                output.write( (A << 24) | (R << 16) | (G << 8) | B );
            }
        }
        final IntsBlob ints = output.drainToIntsBlob();
        try {
            return new RawImage(pngr.imgInfo.cols, pngr.imgInfo.rows, ints, pngr.imgInfo.alpha, ctx.properties);
        }
        finally {
            ints.release();
            pngr.close();
        }
    }
}
