package org.jocean.android.bitmap;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.ReferenceCounted;
import org.jocean.idiom.pool.ObjectPool.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import com.jakewharton.disklrucache.DiskLruCache;
import com.jakewharton.disklrucache.DiskLruCache.Editor;

public abstract class MultiBitmaps {
    
    private static final Logger LOG = LoggerFactory
            .getLogger(MultiBitmaps.class);
    
    public static BitmapsPool createBitmapsPool(final int w, final int h, final Bitmap.Config config) {
        return new CachedBitmapsPool(w, h, config);
    }

    public static MultiBitmap decodeStreamAsBlocks(
            final BitmapsPool pool, final InputStream is, final Map<String, Object> props) {
        final List<Ref<BitmapBlock>> blocks = new ArrayList<Ref<BitmapBlock>>();
        
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPurgeable = true;
            opts.inPreferredConfig = pool.getBitmapConfig();
            
            final Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);
            if ( null == bitmap ) {
                return null;
            }
            
            final Ref<int[]> ARGBS = pool.borrowBlockSizeInts();
            try {
                final int BLOCK_W = pool.getWidthPerBlock();
                final int BLOCK_H = pool.getHeightPerBlock();
                
                final Rect rect = new Rect();
                
                for ( int yidx = 0; yidx < bitmap.getHeight(); yidx += BLOCK_H) {
                    final int h = Math.min(BLOCK_H, bitmap.getHeight() - yidx);
                    
                    for ( int xidx = 0; xidx < bitmap.getWidth(); xidx += BLOCK_W) {
                        final int w = Math.min(BLOCK_W, bitmap.getWidth() - xidx);
                        
                        rect.set(xidx, yidx, xidx + w, yidx + h);
                        final Ref<BitmapBlock> block = pool.retainObject();
                        final Bitmap dest = block.object().bitmap();
                        // getPixels & setPixle 中的 stride 含义参见: http://ranlic.iteye.com/blog/1313735
                        bitmap.getPixels(ARGBS.object(), 0, w, xidx, yidx, w, h);
                        dest.setPixels(ARGBS.object(), 0, w, 0, 0, w, h);
                        block.object().set(xidx, yidx, w, h);
                        blocks.add(block);
                    }
                }
            }
            finally {
                ARGBS.release();
                bitmap.recycle();
            }
            
            return new MultiBitmap(pool, bitmap.getWidth(), bitmap.getHeight(), blocks, props);
        }
        finally {
            ReferenceCounted.Utils.releaseAllAndClear(blocks);
        }
    }
    
    static String saveBitmapToDisk(final String key, final MultiBitmap bitmap, final DiskLruCache diskCache ) {
        try {
            if ( null != diskCache ) {
                final String diskCacheKey = Md5.encode(key);
                if ( null != diskCache.get(diskCacheKey)) {
                    if ( LOG.isTraceEnabled() ) {
                        LOG.trace("bitmap({}) has already save to disk", bitmap);
                    }
                    return genCacheFilename(diskCache, diskCacheKey);
                }
                else {
                    final Editor editor = diskCache.edit(diskCacheKey);
                    OutputStream os = null;
                    if ( null != editor ) {
                        try {
                            os = editor.newOutputStream(0);
                            bitmap.encodeTo(os);
                            return genCacheFilename(diskCache, diskCacheKey);
                        }
                        finally {
                            if ( null != os ) {
                                os.close();
                            }
                            editor.commit();
                        }
                    }
                }
            }
        }
        catch (final Throwable e) {
            LOG.warn("exception while saving bitmap ({}) to disk, detail:{}", 
                    bitmap, ExceptionUtils.exception2detail(e));
        }
        if ( LOG.isTraceEnabled() ) {
            LOG.trace("failed to save bitmap({}) to disk", bitmap);
        }
        return null;
    }

    private static String genCacheFilename(
            final DiskLruCache diskCache,
            final String diskCacheKey) {
        return diskCache.getDirectory().getAbsolutePath() + "/" + diskCacheKey + ".0";
    }
}
