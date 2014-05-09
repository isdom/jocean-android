package org.jocean.android.bitmap;

import org.jocean.android.ReferenceCountedCache;

public class CompositeBitmapCache extends ReferenceCountedCache<String, CompositeBitmap> {

    private static final SizeOf<String, CompositeBitmap> BITMAP_SIZER = 
            new SizeOf<String, CompositeBitmap>() {
        @Override
        public int sizeOf(final String key, final CompositeBitmap bitmap) {
            return bitmap.sizeInBytes();
        }};
    
    public CompositeBitmapCache(int maxSize) {
        super(maxSize, BITMAP_SIZER);
    }
}
