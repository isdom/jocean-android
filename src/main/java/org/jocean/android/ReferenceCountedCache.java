/**
 * 
 */
package org.jocean.android;

import org.jocean.idiom.ReferenceCounted;

import android.support.v4.util.LruCache;

/**
 * @author isdom
 *
 */
public class ReferenceCountedCache<K, V extends ReferenceCounted<? extends V>> {
    public interface SizeOf<K, V> {
        public int sizeOf(K key, V value);
    }
    
    private static final SizeOf<Object, Object> SIZEOF_COUNT = new SizeOf<Object, Object>() {
        @Override
        public int sizeOf(Object key, Object value) {
            return 1;
        }
    };
    
    private final static class LruCacheImpl<K, V extends ReferenceCounted<? extends V>> 
        extends LruCache<K, V> {

        @SuppressWarnings("unchecked")
        public LruCacheImpl(final int maxSize, final SizeOf<K, V> sizeOf) {
            super(maxSize);
            this._sizeOf = ( null != sizeOf ? sizeOf : (SizeOf<K, V>)SIZEOF_COUNT);
        }
        
        @Override
        protected void entryRemoved(
                final boolean evicted, 
                final K key, 
                final V oldValue,
                final V newValue) {
            super.entryRemoved(evicted, key, oldValue, newValue);
            oldValue.release();
        }

        @Override
        protected int sizeOf(final K key, final V value) {
            return this._sizeOf.sizeOf(key, value);
        }
        
        private final SizeOf<K, V> _sizeOf;
    }
    
    public ReferenceCountedCache(final int maxSize, final SizeOf<K, V> sizeOf) {
        this._cache = new LruCacheImpl<K, V>(maxSize, sizeOf);
    }
    
    public void put(final K key, final V value) {
        this._cache.put(key, value.retain());
    }
    
    public V get(final K key) {
        final V value = this._cache.get(key);
        
        if ( null == value ) {
            return null;
        }
        else {
            return value.tryRetain();
        }
    }
    
    public void remove(final K key) {
        this._cache.remove(key);
    }
    
    private final LruCache<K, V> _cache;
}
