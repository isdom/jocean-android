package org.jocean.android.pool;

import org.jocean.android.BitmapBlock;
import org.jocean.idiom.pool.BlockPool;

public interface BitmapsPool extends BlockPool<BitmapBlock>{
    
    public int getWidthPerBlock();
    
    public int getHeightPerBlock();
}
