package ca.mcgill.hs.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An LRU cache, based on <code>LinkedHashMap</code>.<br>
 * This cache has a fixed maximum number of elements (<code>cacheSize</code>).
 * If the cache is full and another entry is added, the LRU (least recently
 * used) entry is dropped.
 * <p>
 * This class is thread-safe. All methods of this class are synchronized.<br>
 * Author: Christian d'Heureuse (<a
 * href="http://www.source-code.biz">www.source-code.biz</a>)<br>
 * License: <a href="http://www.gnu.org/licenses/lgpl.html">LGPL</a>.
 * 
 * @author Christian d'Heureuse
 */
public class LRUCache<K, V> {

	private static final float hashTableLoadFactor = 0.75f;

	private final LinkedHashMap<K, V> map;
	private final int cacheSize;

	/**
	 * Creates a new LRU cache.
	 * 
	 * @param cacheSize
	 *            the maximum number of entries that will be kept in this cache.
	 */
	public LRUCache(final int cacheSize) {
		this.cacheSize = cacheSize;
		final int hashTableCapacity = (int) Math.ceil(cacheSize
				/ hashTableLoadFactor) + 1;
		map = new LinkedHashMap<K, V>(hashTableCapacity, hashTableLoadFactor,
				true) {
			// (an anonymous inner class)
			private static final long serialVersionUID = 1;

			@Override
			protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
				return size() > LRUCache.this.cacheSize;
			}
		};
	}

	/**
	 * Clears the cache.
	 */
	public synchronized void clear() {
		map.clear();
	}

	/**
	 * Retrieves an entry from the cache.<br>
	 * The retrieved entry becomes the MRU (most recently used) entry.
	 * 
	 * @param key
	 *            the key whose associated value is to be returned.
	 * @return the value associated to this key, or null if no value with this
	 *         key exists in the cache.
	 */
	public synchronized V get(final K key) {
		return map.get(key);
	}

	/**
	 * Returns a <code>Collection</code> that contains a copy of all cache
	 * entries.
	 * 
	 * @return a <code>Collection</code> with a copy of the cache content.
	 */
	public synchronized Collection<Map.Entry<K, V>> getAll() {
		return new ArrayList<Map.Entry<K, V>>(map.entrySet());
	}

	/**
	 * Adds an entry to this cache. The new entry becomes the MRU (most recently
	 * used) entry. If an entry with the specified key already exists in the
	 * cache, it is replaced by the new entry. If the cache is full, the LRU
	 * (least recently used) entry is removed from the cache.
	 * 
	 * @param key
	 *            the key with which the specified value is to be associated.
	 * @param value
	 *            a value to be associated with the specified key.
	 */
	public synchronized void put(final K key, final V value) {
		map.put(key, value);
	}

	/**
	 * Returns the number of used entries in the cache.
	 * 
	 * @return the number of entries currently in the cache.
	 */
	public synchronized int usedEntries() {
		return map.size();
	}

} // end class LRUCache
