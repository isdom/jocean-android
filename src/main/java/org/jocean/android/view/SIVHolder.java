/**
 * 
 */
package org.jocean.android.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author isdom
 * 
 */
public class SIVHolder {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(
            SIVHolder.class);

    public SmartImageView _view;
    public String _url;

    public static String view2url(final SmartImageView view) {
        return ((SIVHolder)view.getTag())._url;
    }
    
    public SIVHolder setImageView(final SmartImageView view) {
        this._view = view;
        this._view.setTag(this);
        return this;
    }
}
