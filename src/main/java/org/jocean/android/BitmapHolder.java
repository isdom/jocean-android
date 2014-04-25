package org.jocean.android;

import org.jocean.idiom.ReferenceCounted;

import android.graphics.Bitmap;

public interface BitmapHolder extends ReferenceCounted<BitmapHolder> {
    public Bitmap bitmap();
}
