package adt;

import java.util.Iterator;

public class DoublyLinkedHashMap<K, V> implements MapInterface<K, V> {

  private static class Node<K, V> {
    private K key;
    private V value;
    private Node<K, V> next; // Bucket collision chain pointer

    // Doubly-linked pointers
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

  private Node<K, V> head = null; // Most Recently Used
  private Node<K, V> tail = null; // Least Recently Used

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
    this.initialBucketCount = initialBucketCount > 0 ? initialBucketCount : DEFAULT_BUCKET_COUNT;

    this.buckets = (Node<K, V>[]) new Node[this.initialBucketCount];
    this.numberOfEntries = 0;
    this.maxCapacity = maxCapacity;
    this.loadFactorThreshold =
        loadFactorThreshold > 0 ? loadFactorThreshold : LOAD_FACTOR_THRESHOLD;
    this.enableLru = enableLru;
  }

  public boolean isLruEnabled() {
    return enableLru;
  }

  public void setLruEnabled(boolean enableLru) {
    if (this.enableLru == enableLru) {
      return;
    }

    this.enableLru = enableLru;

    if (enableLru) {
      rebuildAccessList();
    } else {
      clearAccessList();
    }
  }

  @Override
  public boolean put(K key, V value) {
    if (key == null) return false;

    int index = getBucketIndex(key, buckets.length);
    Node<K, V> current = buckets[index];

    // Key already exists.
    while (current != null) {
      if (current.key.equals(key)) {
        current.value = value;

        if (enableLru) {
          moveToHead(current);
        }

        return false;
      }

      current = current.next;
    }

    // A capacity of zero cannot accept any entries.
    if (hasLimit() && maxCapacity == 0) {
      return false;
    }

    // Evict the least recently used entry when capacity is reached.
    if (hasLimit() && numberOfEntries >= maxCapacity) {
      if (!enableLru) {
        return false;
      }

      evictLru();
    }

    // Resize before inserting if the new entry would exceed
    // the configured load factor.
    if ((double) (numberOfEntries + 1) / buckets.length > loadFactorThreshold) {

      resize();
      index = getBucketIndex(key, buckets.length);
    }

    Node<K, V> newNode = new Node<>(key, value, buckets[index]);
    buckets[index] = newNode;
    numberOfEntries++;

    if (enableLru) {
      addToHead(newNode);
    }

    return true;
  }

  @Override
  public V get(K key) {
    if (key == null || isEmpty()) return null;

    int index = getBucketIndex(key, buckets.length);
    Node<K, V> current = buckets[index];

    while (current != null) {
      if (current.key.equals(key)) {

        if (enableLru) {
          moveToHead(current);
        }

        return current.value;
      }

      current = current.next;
    }

    return null;
  }

  @Override
  public V remove(K key) {
    if (key == null || isEmpty()) return null;

    int index = getBucketIndex(key, buckets.length);

    Node<K, V> current = buckets[index];
    Node<K, V> previous = null;

    while (current != null) {

      if (current.key.equals(key)) {

        // Remove from hash-table collision chain.
        if (previous == null) {
          buckets[index] = current.next;
        } else {
          previous.next = current.next;
        }

        numberOfEntries--;

        // Remove from LRU list.
        if (enableLru) {
          removeFromAccessList(current);
        }

        return current.value;
      }

      previous = current;
      current = current.next;
    }

    return null;
  }

  @Override
  public boolean containsKey(K key) {
    if (key == null || isEmpty()) {
      return false;
    }

    int index = getBucketIndex(key, buckets.length);
    Node<K, V> current = buckets[index];

    while (current != null) {

      if (current.key.equals(key)) {
        return true;
      }

      current = current.next;
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
    if (tail == null) {
      return;
    }

    remove(tail.key);
  }

  private void clearAccessList() {
    Node<K, V> current = head;

    while (current != null) {
      Node<K, V> next = current.accessNext;

      current.accessPrev = null;
      current.accessNext = null;

      current = next;
    }

    head = null;
    tail = null;
  }

  private void rebuildAccessList() {
    head = null;
    tail = null;

    for (Node<K, V> bucketHead : buckets) {
      Node<K, V> current = bucketHead;

      while (current != null) {
        current.accessPrev = null;
        current.accessNext = null;

        addToHead(current);

        current = current.next;
      }
    }
  }

  private boolean hasLimit() {
    return maxCapacity >= 0;
  }

  private int getBucketIndex(K key, int bucketCount) {
    int hash = key.hashCode();

    // Spread high bits into lower bits.
    hash ^= (hash >>> 16);

    return Math.floorMod(hash, bucketCount);
  }

  @SuppressWarnings("unchecked")
  private void resize() {
    Node<K, V>[] oldBuckets = buckets;

    buckets = (Node<K, V>[]) new Node[oldBuckets.length * 2];

    for (Node<K, V> bucketHead : oldBuckets) {

      Node<K, V> current = bucketHead;

      while (current != null) {

        Node<K, V> next = current.next;

        int index = getBucketIndex(current.key, buckets.length);

        current.next = buckets[index];
        buckets[index] = current;

        current = next;
      }
    }
  }

  private class KeyIterator implements Iterator<K> {

    private Node<K, V> currentNode;
    private int currentBucketIndex;

    private KeyIterator() {

      if (enableLru) {
        currentNode = head;
      } else {
        // Normal hash-table iteration.
        currentBucketIndex = 0;
        currentNode = null;

        advanceToNextNode();
      }
    }

    private void advanceToNextNode() {

      while (currentBucketIndex < buckets.length) {

        if (buckets[currentBucketIndex] != null) {
          currentNode = buckets[currentBucketIndex];
          currentBucketIndex++;
          return;
        }

        currentBucketIndex++;
      }

      currentNode = null;
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
          advanceToNextNode();
        }
      }

      return key;
    }
  }
}
