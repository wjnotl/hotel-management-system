package adt;

import java.util.Iterator;

public interface MapInterface<K, V> {

  // Inserts a key-value mapping. Overwrites the value and returns false if the key exists.
  public boolean put(K key, V value);

  // Grabs the value paired with this key, or returns null if it does not exist.
  public V get(K key);

  // Evicts the key-value pair completely and hands back the evicted value.
  public V remove(K key);

  // Checks if a specific key exists anywhere in the map.
  public boolean containsKey(K key);

  // Returns the total number of key-value mappings.
  public int size();

  // Checks if the map is empty.
  public boolean isEmpty();

  // Flushes all key-value entries from the map.
  public void clear();

  // Gives back an iterator to step through all the key-value pairs
  public Iterator<K> getKeyIterator();
}
