/**
 * 
 */
package org.jocean.android.bitmap;

import org.jocean.idiom.pool.AbstractCachedObjectPool;
import org.jocean.idiom.pool.CachedObjectPool;
import org.jocean.idiom.pool.IntsPool;
import org.jocean.idiom.pool.Pools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;

/**
 * @author isdom
 *
 */
class CachedBitmapsPool extends AbstractCachedObjectPool<BitmapBlock> 
    implements BitmapsPool, CachedObjectPool<BitmapBlock> {

    private static final Logger LOG = 
            LoggerFactory.getLogger(CachedBitmapsPool.class);
    
    public CachedBitmapsPool(final int w, final int h, final Bitmap.Config config) {
        super(LOG);
        if ( w <= 0 || h <= 0) {
            throw new IllegalArgumentException("w or h for CachedBitmapsPool must more than zero.");
        }
        this._bitmapConfig = config;
        this._widthPerBlock = w;
        this._heightPerBlock = h;
        this._blockSize = w * h * 4;
        this._clspixels = new int[w * h];
        this._intsPool = Pools.createCachedIntsPool(w*h);
    }
    
    @Override
    protected BitmapBlock createObject() {
        return new BitmapBlock( this._widthPerBlock, this._heightPerBlock, this._bitmapConfig, this._clspixels);
    }

    @Override
    public Ref<int[]> borrowBlockSizeInts() {
        return this._intsPool.retainObject();
    }
    
    @Override
    public int getTotalCachedSizeInByte() {
        return this.getCachedCount() * this._blockSize;
    }

    @Override
    public int getTotalRetainedSizeInByte() {
        return this.getRetainedCount() * this._blockSize;
    }
    
    @Override
    public int getTotalSizeInByte() {
        return (this.getCachedCount() + this.getRetainedCount() ) * this._blockSize;
    }
    
    @Override
    public int getBlockSize() {
        return this._blockSize;
    }
    
    @Override
    public int getWidthPerBlock() {
        return this._widthPerBlock;
    }
    
    @Override
    public int getHeightPerBlock() {
        return this._heightPerBlock;
    }
    
    @Override
    public Bitmap.Config getBitmapConfig() {
        return this._bitmapConfig;
    }
    
    private final IntsPool _intsPool;
    private final int _blockSize;
    private final int _widthPerBlock;
    private final int _heightPerBlock;
    private final Bitmap.Config _bitmapConfig;
    private final int[] _clspixels;
}
