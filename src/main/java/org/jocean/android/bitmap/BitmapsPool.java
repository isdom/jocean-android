package org.jocean.android.bitmap;

import org.jocean.idiom.pool.BlockPool;

import android.graphics.Bitmap;

public interface BitmapsPool extends BlockPool<BitmapBlock>{
    
    public int getWidthPerBlock();
    
    public int getHeightPerBlock();
    
    public Bitmap.Config getBitmapConfig();
    
    public Ref<int[]> borrowBlockSizeInts();
}
