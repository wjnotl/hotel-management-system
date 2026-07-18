package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Hash map using separate chaining, where each bucket is one of our own LinkedList<Entry<K,V>>
 * instances (from ListInterface) instead of hand-rolled next-pointer chains. Buckets array grows
 * (doubles) once load factor passes 0.75, same idea as before - no hard capacity ceiling.
 */
public class HashMap<K, V> implements MapInterface<K, V> {

  private static class Entry<K, V> {
    K key;
    V value;

    Entry(K key, V value) {
      this.key = key;
      this.value = value;
    }
  }

  private ListInterface<Entry<K, V>>[] buckets;
  private int numberOfEntries;

  private static final int DEFAULT_BUCKET_COUNT = 16;
  private static final double LOAD_FACTOR_LIMIT = 0.75;

  public HashMap() {
    this(DEFAULT_BUCKET_COUNT);
  }

  public HashMap(int initialBucketCount) {
    buckets = createBucketArray(initialBucketCount);
    numberOfEntries = 0;
  }

  private ListInterface<Entry<K, V>>[] createBucketArray(int size) {
    // Java won't let us write new ListInterface<Entry<K,V>>[size] directly -
    // generics can't be used in array creation. This raw-array-then-cast
    // is the standard workaround; it triggers an "unchecked" compiler
    // warning (not an error) since the JVM can't verify it at runtime,
    // but it's safe here because this array is private and only ever
    // touched through the methods in this class.
    return (ListInterface<Entry<K, V>>[]) new ListInterface[size];
  }

  @Override
  public boolean put(K key, V value) {
    if (key == null) return false;

    int index = bucketIndex(key, buckets.length);
    if (buckets[index] == null) {
      buckets[index] = new LinkedList<>();
    }
    ListInterface<Entry<K, V>> chain = buckets[index];

    // walk this bucket's list to see if the key already exists
    for (int i = 1; i <= chain.getNumberOfEntries(); i++) {
      Entry<K, V> existing = chain.getEntry(i);
      if (existing.key.equals(key)) {
        existing.value = value; // update in place
        return false;
      }
    }

    chain.add(new Entry<>(key, value));
    numberOfEntries++;

    if (loadFactor() > LOAD_FACTOR_LIMIT) {
      resize();
    }
    return true;
  }

  @Override
  public V get(K key) {
    if (key == null) return null;

    ListInterface<Entry<K, V>> chain = buckets[bucketIndex(key, buckets.length)];
    if (chain == null) return null;

    for (int i = 1; i <= chain.getNumberOfEntries(); i++) {
      Entry<K, V> entry = chain.getEntry(i);
      if (entry.key.equals(key)) return entry.value;
    }
    return null;
  }

  @Override
  public V remove(K key) {
    if (key == null) return null;

    ListInterface<Entry<K, V>> chain = buckets[bucketIndex(key, buckets.length)];
    if (chain == null) return null;

    for (int i = 1; i <= chain.getNumberOfEntries(); i++) {
      Entry<K, V> entry = chain.getEntry(i);
      if (entry.key.equals(key)) {
        chain.remove(i);
        numberOfEntries--;
        return entry.value;
      }
    }
    return null;
  }

  @Override
  public boolean containsKey(K key) {
    if (key == null) return false;

    ListInterface<Entry<K, V>> chain = buckets[bucketIndex(key, buckets.length)];
    if (chain == null) return false;

    for (int i = 1; i <= chain.getNumberOfEntries(); i++) {
      if (chain.getEntry(i).key.equals(key)) return true;
    }
    return false;
  }

  @Override
  public int size() {
    return numberOfEntries;
  }

  @Override
  public boolean isEmpty() {
    return numberOfEntries == 0;
  }

  @Override
  public void clear() {
    buckets = createBucketArray(DEFAULT_BUCKET_COUNT);
    numberOfEntries = 0;
  }

  @Override
  public Iterator<K> getKeyIterator() {
    return new KeyIterator();
  }

  // ---------- internal helpers ----------

  private int bucketIndex(K key, int bucketCount) {
    return (key.hashCode() & 0x7fffffff) % bucketCount;
  }

  private double loadFactor() {
    return (double) numberOfEntries / buckets.length;
  }

  private void resize() {
    ListInterface<Entry<K, V>>[] oldBuckets = buckets;
    buckets = createBucketArray(oldBuckets.length * 2);

    // re-insert every existing entry - bucket index changes because
    // bucketCount changed
    for (ListInterface<Entry<K, V>> chain : oldBuckets) {
      if (chain == null) continue;
      for (int i = 1; i <= chain.getNumberOfEntries(); i++) {
        Entry<K, V> entry = chain.getEntry(i);
        int newIndex = bucketIndex(entry.key, buckets.length);
        if (buckets[newIndex] == null) {
          buckets[newIndex] = new LinkedList<>();
        }
        buckets[newIndex].add(entry);
      }
    }
  }

  private class KeyIterator implements Iterator<K> {
    private int bucketPosition = 0;
    private int entryPosition = 1; // 1-based, matches ListInterface
    private K nextKey = null;

    KeyIterator() {
      advance();
    }

    // finds the next available key across buckets, or leaves nextKey
    // null if we've run out
    private void advance() {
      nextKey = null;
      while (bucketPosition < buckets.length) {
        ListInterface<Entry<K, V>> chain = buckets[bucketPosition];
        if (chain != null && entryPosition <= chain.getNumberOfEntries()) {
          nextKey = chain.getEntry(entryPosition).key;
          entryPosition++;
          return;
        }
        bucketPosition++;
        entryPosition = 1;
      }
    }

    @Override
    public boolean hasNext() {
      return nextKey != null;
    }

    @Override
    public K next() {
      if (!hasNext()) throw new NoSuchElementException();
      K key = nextKey;
      advance();
      return key;
    }
  }
}
