/*******************************************************************************
 * Copyright (c) 2013 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.jocean.android.bitmap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

class SDK11 {
    static boolean isSDKVersionEqualsOrOlderThanHONEYCOMB() {
        return Build.VERSION.SDK_INT >=  11;
    }
    
    static void addInBitmapOption(final BitmapFactory.Options opts, final Bitmap inBitmap) {
        if ( isSDKVersionEqualsOrOlderThanHONEYCOMB() ) {
            opts.inBitmap = inBitmap;
        }
    }

    static void setMutable(final BitmapFactory.Options opts){
        if ( isSDKVersionEqualsOrOlderThanHONEYCOMB() ) {
            opts.inMutable = true;
        }
    }
}
