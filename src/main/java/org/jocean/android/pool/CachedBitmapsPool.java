/**
 * 
 */
package org.jocean.android.pool;

import org.jocean.android.BitmapBlock;
import org.jocean.idiom.pool.AbstractCachedObjectPool;
import org.jocean.idiom.pool.CachedObjectPool;

/**
 * @author isdom
 *
 */
class CachedBitmapsPool extends AbstractCachedObjectPool<BitmapBlock> 
    implements BitmapsPool, CachedObjectPool<BitmapBlock> {

    public CachedBitmapsPool(final int w, int h) {
        if ( w <= 0 || h <= 0) {
            throw new IllegalArgumentException("w or h for CachedBitmapsPool must more than zero.");
        }
        this._widthPerBlock = w;
        this._heightPerBlock = h;
        this._blockSize = w * h * 4;
    }
    
    @Override
    protected BitmapBlock createObject() {
        return new BitmapBlock( this._widthPerBlock, this._heightPerBlock);
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
    
    private final int _blockSize;
    private final int _widthPerBlock;
    private final int _heightPerBlock;
}
