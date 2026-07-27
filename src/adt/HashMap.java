package adt;

import java.util.Iterator;

public class HashMap<K, V> implements MapInterface<K, V> {

  private static class Node<K, V> {
    private K key;
    private V value;
    private Node<K, V> next;

    private Node(K key, V value, Node<K, V> next) {
      this.key = key;
      this.value = value;
      this.next = next;
    }
  }

  private static final int DEFAULT_BUCKET_COUNT = 16;
  private static final double LOAD_FACTOR_THRESHOLD = 0.75;

  private Node<K, V>[] buckets;
  private int numberOfEntries;

  private final int maxCapacity;
  private final int initialBucketCount;
  private final double loadFactorThreshold;

  public HashMap() {
    this(DEFAULT_BUCKET_COUNT, LOAD_FACTOR_THRESHOLD, -1);
  }

  public HashMap(int initialBucketCount) {
    this(initialBucketCount, -1);
  }

  public HashMap(double loadFactorLimit) {
    this(DEFAULT_BUCKET_COUNT, loadFactorLimit, -1);
  }

  public HashMap(int initialBucketCount, double loadFactorLimit) {
    this(initialBucketCount, loadFactorLimit, -1);
  }

  @SuppressWarnings("unchecked")
  public HashMap(int initialBucketCount, double loadFactorThreshold, int maxCapacity) {
    this.initialBucketCount = initialBucketCount <= 0 ? DEFAULT_BUCKET_COUNT : initialBucketCount;
    this.buckets = (Node<K, V>[]) new Node[initialBucketCount];
    this.numberOfEntries = 0;
    this.maxCapacity = maxCapacity;
    this.loadFactorThreshold = loadFactorThreshold;
  }

  @Override
  public boolean put(K key, V value) {
    if (key == null) return false;

    int index = getBucketIndex(key, buckets.length);
    Node<K, V> curr = buckets[index];

    // Check if key already exists to update value
    while (curr != null) {
      if (curr.key.equals(key)) {
        curr.value = value;
        return false;
      }
      curr = curr.next;
    }

    // Capacity limit safety check
    if (hasLimit() && numberOfEntries >= maxCapacity) {
      return false;
    }

    // Expand buckets before inserting if load factor limit is hit
    if ((double) numberOfEntries / buckets.length > loadFactorThreshold) {
      resize();
      index = getBucketIndex(key, buckets.length); // Recalculate index for new bucket size
    }

    // Insert new node at head of bucket chain
    buckets[index] = new Node<>(key, value, buckets[index]);
    numberOfEntries++;
    return true;
  }

  @Override
  public V get(K key) {
    if (key == null || isEmpty()) return null;

    int index = getBucketIndex(key, buckets.length);
    Node<K, V> curr = buckets[index];

    while (curr != null) {
      if (curr.key.equals(key)) {
        return curr.value;
      }
      curr = curr.next;
    }
    return null;
  }

  @Override
  public V remove(K key) {
    if (key == null || isEmpty()) return null;

    int index = getBucketIndex(key, buckets.length);
    Node<K, V> curr = buckets[index];
    Node<K, V> prev = null;

    while (curr != null) {
      if (curr.key.equals(key)) {
        if (prev == null) {
          buckets[index] = curr.next;
        } else {
          prev.next = curr.next;
        }
        numberOfEntries--;
        return curr.value;
      }
      prev = curr;
      curr = curr.next;
    }
    return null;
  }

  @Override
  public boolean containsKey(K key) {
    return get(key) != null;
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
  @SuppressWarnings("unchecked")
  public void clear() {
    this.buckets = (Node<K, V>[]) new Node[initialBucketCount];
    this.numberOfEntries = 0;
  }

  @Override
  public Iterator<K> getKeyIterator() {
    return new KeyIterator();
  }

  // --- INTERNAL HELPERS ---

  private boolean hasLimit() {
    return maxCapacity > -1;
  }

  private int getBucketIndex(K key, int bucketCount) {
    int h = key.hashCode();
    h ^= (h >>> 16); // High-bit XOR mixer
    return (h & 0x7FFFFFFF) % bucketCount;
  }

  @SuppressWarnings("unchecked")
  private void resize() {
    Node<K, V>[] oldBuckets = buckets;
    buckets = (Node<K, V>[]) new Node[oldBuckets.length * 2];
    numberOfEntries = 0;

    for (Node<K, V> head : oldBuckets) {
      Node<K, V> curr = head;
      while (curr != null) {
        put(curr.key, curr.value);
        curr = curr.next;
      }
    }
  }

  // --- ITERATOR IMPLEMENTATION ---

  private class KeyIterator implements Iterator<K> {
    private int currentBucket = 0;
    private Node<K, V> currentNode = null;

    KeyIterator() {
      advanceToNextNode();
    }

    private void advanceToNextNode() {
      if (currentNode != null && currentNode.next != null) {
        currentNode = currentNode.next;
        return;
      }

      currentNode = null;
      while (currentBucket < buckets.length) {
        if (buckets[currentBucket] != null) {
          currentNode = buckets[currentBucket];
          currentBucket++;
          return;
        }
        currentBucket++;
      }
    }

    @Override
    public boolean hasNext() {
      return currentNode != null;
    }

    @Override
    public K next() {
      if (!hasNext()) return null;

      K key = currentNode.key;
      advanceToNextNode();
      return key;
    }
  }
}
