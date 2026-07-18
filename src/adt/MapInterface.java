package adt;

/**
 * Map ADT interface. Stores key-value pairs, retrieval by key instead of by position or priority.
 */
public interface MapInterface<K, V> {

  /**
   * Adds a new key-value pair, or updates the value if the key already exists.
   *
   * @return true if this was a new key, false if it replaced an existing one
   */
  public boolean put(K key, V value);

  /**
   * @return the value mapped to this key, or null if the key isn't present
   */
  public V get(K key);

  /**
   * Removes the key-value pair for the given key.
   *
   * @return the removed value, or null if the key wasn't present
   */
  public V remove(K key);

  public boolean containsKey(K key);

  public int size();

  public boolean isEmpty();

  public void clear();

  public java.util.Iterator<K> getKeyIterator();
}
