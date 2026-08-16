package adt;

import java.util.Iterator;

public class DoublyLinkedHashMap<K, V> implements MapInterface<K, V> {

  private static class Node<K, V> {
    private K key;
    private V value;
    private Node<K, V> next; // Bucket collision chain pointer

    // Doubly-linked pointers for LRU access ordering
    private Node<K, V> accessPrev;
    private Node<K, V> accessNext;

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
  private boolean enableLru;

  // Doubly linked list pointers for LRU access tracking (head = MRU, tail = LRU)
  private Node<K, V> head;
  private Node<K, V> tail;

  public DoublyLinkedHashMap() {
    this(DEFAULT_BUCKET_COUNT, LOAD_FACTOR_THRESHOLD, -1, false);
  }

  public DoublyLinkedHashMap(int initialBucketCount) {
    this(initialBucketCount, LOAD_FACTOR_THRESHOLD, -1, false);
  }

  public DoublyLinkedHashMap(double loadFactorLimit) {
    this(DEFAULT_BUCKET_COUNT, loadFactorLimit, -1, false);
  }

  public DoublyLinkedHashMap(int initialBucketCount, double loadFactorLimit) {
    this(initialBucketCount, loadFactorLimit, -1, false);
  }

  public DoublyLinkedHashMap(int initialBucketCount, double loadFactorThreshold, int maxCapacity) {
    this(initialBucketCount, loadFactorThreshold, maxCapacity, false);
  }

  @SuppressWarnings("unchecked")
  public DoublyLinkedHashMap(
      int initialBucketCount, double loadFactorThreshold, int maxCapacity, boolean enableLru) {
    this.initialBucketCount = initialBucketCount <= 0 ? DEFAULT_BUCKET_COUNT : initialBucketCount;
    this.buckets = (Node<K, V>[]) new Node[this.initialBucketCount];
    this.numberOfEntries = 0;
    this.maxCapacity = maxCapacity;
    this.loadFactorThreshold = loadFactorThreshold;
    this.enableLru = enableLru;
  }

  public boolean isLruEnabled() {
    return enableLru;
  }

  public void setLruEnabled(boolean enableLru) {
    this.enableLru = enableLru;
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
        if (enableLru) {
          moveToHead(curr);
        }
        return false;
      }
      curr = curr.next;
    }

    // Capacity limit handling
    if (hasLimit() && numberOfEntries >= maxCapacity) {
      if (!enableLru) {
        return false; // Reject insertion when full if LRU is disabled
      } else {
        evictLru(); // Evict Least Recently Used entry when full if LRU is enabled
        index = getBucketIndex(key, buckets.length); // Recalculate bucket index
      }
    }

    // Expand buckets before inserting if load factor limit is hit
    if ((double) numberOfEntries / buckets.length > loadFactorThreshold) {
      resize();
      index = getBucketIndex(key, buckets.length);
    }

    // Create new node and insert at head of bucket chain
    Node<K, V> newNode = new Node<>(key, value, buckets[index]);
    buckets[index] = newNode;
    numberOfEntries++;

    // Track LRU access order if enabled
    if (enableLru) {
      addToHead(newNode);
    }

    return true;
  }

  @Override
  public V get(K key) {
    if (key == null || isEmpty()) return null;

    int index = getBucketIndex(key, buckets.length);
    Node<K, V> curr = buckets[index];

    while (curr != null) {
      if (curr.key.equals(key)) {
        if (enableLru) {
          moveToHead(curr);
        }
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

        if (enableLru) {
          removeFromAccessList(curr);
        }
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
    this.head = null;
    this.tail = null;
  }

  @Override
  public Iterator<K> getKeyIterator() {
    return new KeyIterator();
  }

  // --- LRU DOUBLY-LINKED LIST HELPERS ---

  private void addToHead(Node<K, V> node) {
    if (node == null) return;
    node.accessPrev = null;
    node.accessNext = head;

    if (head != null) {
      head.accessPrev = node;
    }
    head = node;

    if (tail == null) {
      tail = node;
    }
  }

  private void removeFromAccessList(Node<K, V> node) {
    if (node == null) return;

    if (node.accessPrev != null) {
      node.accessPrev.accessNext = node.accessNext;
    } else {
      head = node.accessNext;
    }

    if (node.accessNext != null) {
      node.accessNext.accessPrev = node.accessPrev;
    } else {
      tail = node.accessPrev;
    }

    node.accessPrev = null;
    node.accessNext = null;
  }

  private void moveToHead(Node<K, V> node) {
    if (node == null || node == head) return;
    removeFromAccessList(node);
    addToHead(node);
  }

  private void evictLru() {
    if (tail == null) return;
    K lruKey = tail.key;
    remove(lruKey); // Removes from hash bucket and doubly linked list
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

    // Preserve LRU access ordering across resize
    Node<K, V> curr = head;
    while (curr != null) {
      int index = getBucketIndex(curr.key, buckets.length);
      curr.next = buckets[index];
      buckets[index] = curr;
      curr = curr.accessNext;
    }
  }

  // --- ITERATOR IMPLEMENTATION ---

  private class KeyIterator implements Iterator<K> {
    private Node<K, V> currentNode;

    KeyIterator() {
      if (enableLru) {
        currentNode = head;
      } else {
        advanceToFirstBucketNode();
      }
    }

    private int currentBucket = 0;

    private void advanceToFirstBucketNode() {
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
      if (enableLru) {
        currentNode = currentNode.accessNext;
      } else {
        if (currentNode.next != null) {
          currentNode = currentNode.next;
        } else {
          currentNode = null;
          advanceToFirstBucketNode();
        }
      }
      return key;
    }
  }
}
