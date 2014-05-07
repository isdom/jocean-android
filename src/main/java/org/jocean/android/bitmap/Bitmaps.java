package org.jocean.android.bitmap;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jocean.event.api.EventReceiverSource;
import org.jocean.idiom.ReferenceCounted;
import org.jocean.idiom.pool.ObjectPool.Ref;
import org.jocean.rosa.api.BlobAgent;

import com.jakewharton.disklrucache.DiskLruCache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

public abstract class Bitmaps {
    
    public static BitmapsPool createBitmapsPool(final int w, final int h, final Bitmap.Config config) {
        return new CachedBitmapsPool(w, h, config);
    }

    public static BitmapAgent createBitmapAgent(
            final EventReceiverSource source,
            final BitmapsPool pool, 
            final BlobAgent blobAgent,
            final int maxMemoryCacheSizeInBytes,
            final DiskLruCache diskCache
            ) {
        return new BitmapAgentImpl(source, pool, blobAgent, maxMemoryCacheSizeInBytes, diskCache);
    }
    
    public static CompositeBitmap decodeStreamAsBlocks(
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
            
            return new CompositeBitmap(pool, bitmap.getWidth(), bitmap.getHeight(), blocks, props);
        }
        finally {
            ReferenceCounted.Utils.releaseAllAndClear(blocks);
        }
    }
}
