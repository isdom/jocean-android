package org.jocean.android.pool;

public abstract class Bitmaps {
    
    public static BitmapsPool createBitmapsPool(final int w, final int h) {
        return new CachedBitmapsPool(w, h);
    }
}
