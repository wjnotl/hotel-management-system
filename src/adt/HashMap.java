package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

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

  private final boolean hasLimit;
  private final int maxCapacity;

  private static final int DEFAULT_BUCKET_COUNT = 16;
  private static final double LOAD_FACTOR_LIMIT = 0.75;
  private static final int UNLIMITED = -1;

  public HashMap() {
    this(DEFAULT_BUCKET_COUNT, false, UNLIMITED);
  }

  public HashMap(int initialBucketCount, boolean hasLimit, int maxCapacity) {
    this.buckets = createBucketArray(initialBucketCount);
    this.numberOfEntries = 0;
    this.hasLimit = hasLimit;
    this.maxCapacity = maxCapacity;
  }

  @SuppressWarnings("unchecked")
  private ListInterface<Entry<K, V>>[] createBucketArray(int size) {
    // Workaround for generic array limitations in Java
    return (ListInterface<Entry<K, V>>[]) new ListInterface[size];
  }

  @Override
  public boolean put(K key, V value) {
    if (key == null) return false;

    int index = bucketIndex(key, buckets.length);
    if (buckets[index] == null) {
      buckets[index] = new AList<>(); // Uses O(1) random-access memory array structure
    }
    ListInterface<Entry<K, V>> chain = buckets[index];

    // Scan the chain to check if the incoming key matches an existing entry
    int chainSize = chain.getNumberOfEntries();
    for (int i = 1; i <= chainSize; i++) {
      Entry<K, V> existing = chain.getEntry(i);
      if (existing.key.equals(key)) {
        existing.value = value;
        chain.replace(i, existing); // Notify container layer to reflect update
        return false;
      }
    }

    // Verify hard capacity controls before introducing a brand-new entry block
    if (hasLimit && numberOfEntries >= maxCapacity) {
      throw new IllegalStateException(
          "Map capacity limit exceeded! Maximum allowed entries: " + maxCapacity);
    }

    chain.add(new Entry<>(key, value));
    numberOfEntries++;

    // Evaluate dynamic allocation enlargement boundaries
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

    int chainSize = chain.getNumberOfEntries();
    for (int i = 1; i <= chainSize; i++) {
      Entry<K, V> entry = chain.getEntry(i);
      if (entry.key.equals(key)) {
        return entry.value;
      }
    }
    return null;
  }

  @Override
  public V remove(K key) {
    if (key == null) return null;

    ListInterface<Entry<K, V>> chain = buckets[bucketIndex(key, buckets.length)];
    if (chain == null) return null;

    int chainSize = chain.getNumberOfEntries();
    for (int i = 1; i <= chainSize; i++) {
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

    int chainSize = chain.getNumberOfEntries();
    for (int i = 1; i <= chainSize; i++) {
      if (chain.getEntry(i).key.equals(key)) {
        return true;
      }
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

  // INTERNAL HELPERS

  private int bucketIndex(K key, int bucketCount) {
    // Drop the sign bit to strip out negative integer numbers before running modulo calculation
    return (key.hashCode() & 0x7fffffff) % bucketCount;
  }

  private double loadFactor() {
    return (double) numberOfEntries / buckets.length;
  }

  private void resize() {
    ListInterface<Entry<K, V>>[] oldBuckets = buckets;
    buckets = createBucketArray(oldBuckets.length * 2);

    // Re-hash elements inside a clean expansion bucket list setup
    for (ListInterface<Entry<K, V>> chain : oldBuckets) {
      if (chain == null) continue;

      int chainSize = chain.getNumberOfEntries();
      for (int i = 1; i <= chainSize; i++) {
        Entry<K, V> entry = chain.getEntry(i);
        int newIndex = bucketIndex(entry.key, buckets.length);

        if (buckets[newIndex] == null) {
          buckets[newIndex] = new AList<>();
        }
        buckets[newIndex].add(entry);
      }
    }
  }

  // ITERATOR IMPLEMENTATION

  private class KeyIterator implements Iterator<K> {
    private int bucketPosition = 0;
    private int entryPosition = 1; // Tracks custom 1-based ListInterface indexes
    private K nextKey = null;

    KeyIterator() {
      advance();
    }

    private void advance() {
      nextKey = null;
      while (bucketPosition < buckets.length) {
        ListInterface<Entry<K, V>> chain = buckets[bucketPosition];
        // Ensure execution stays inside current list chain bounds
        if (chain != null && entryPosition <= chain.getNumberOfEntries()) {
          nextKey = chain.getEntry(entryPosition).key;
          entryPosition++;
          return;
        }
        // Jump sideways to inspect adjacent bucket arrays
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
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      K key = nextKey;
      advance();
      return key;
    }
  }
}
