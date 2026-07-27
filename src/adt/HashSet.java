package adt;

import java.util.Iterator;

public class HashSet<T> implements SetInterface<T> {
  private static final int DEFAULT_BUCKET_COUNT = 16;
  private static final double LOAD_FACTOR_THRESHOLD = 0.75;

  private final int initialBucketCount;
  private final double loadFactorThreshold;
  private Node<T>[] buckets;
  private int numOfEntires;

  // Single-linked list node for collision resolution via chaining
  private static class Node<T> {
    private T data;
    private Node<T> next;

    private Node(T data) {
      this(data, null);
    }

    private Node(T data, Node<T> next) {
      this.data = data;
      this.next = next;
    }
  }

  public HashSet() {
    this(DEFAULT_BUCKET_COUNT, LOAD_FACTOR_THRESHOLD);
  }

  public HashSet(int initialCapacity) {
    this(initialCapacity, LOAD_FACTOR_THRESHOLD);
  }

  public HashSet(double loadFactorThreshold) {
    this(DEFAULT_BUCKET_COUNT, loadFactorThreshold);
  }

  @SuppressWarnings("unchecked")
  public HashSet(int initialCapacity, double loadFactorThreshold) {
    this.initialBucketCount = initialCapacity <= 0 ? DEFAULT_BUCKET_COUNT : initialCapacity;
    this.loadFactorThreshold = loadFactorThreshold;
    this.buckets = (Node<T>[]) new Node[initialCapacity];
  }

  @Override
  public boolean add(T element) {
    if (element == null) return false;
    if (contains(element)) return false;

    // Expand buckets if load factor exceeds threshold
    if ((double) numOfEntires / buckets.length >= loadFactorThreshold) {
      resize();
    }

    int index = getBucketIndex(element);
    buckets[index] = new Node<T>(element, buckets[index]);
    numOfEntires++;
    return true;
  }

  @Override
  public boolean remove(T element) {
    if (element == null || isEmpty()) return false;

    int index = getBucketIndex(element);
    Node<T> curr = buckets[index];
    Node<T> prev = null;

    while (curr != null) {
      if (curr.data.equals(element)) {
        if (prev == null) {
          buckets[index] = curr.next;
        } else {
          prev.next = curr.next;
        }
        numOfEntires--;
        return true;
      }
      prev = curr;
      curr = curr.next;
    }
    return false;
  }

  @Override
  public boolean contains(T element) {
    if (element == null || isEmpty()) return false;

    int index = getBucketIndex(element);
    Node<T> curr = buckets[index];

    while (curr != null) {
      if (curr.data.equals(element)) return true;
      curr = curr.next;
    }
    return false;
  }

  @Override
  public int size() {
    return numOfEntires;
  }

  @Override
  public boolean isEmpty() {
    return numOfEntires == 0;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void clear() {
    this.buckets = (Node<T>[]) new Node[initialBucketCount];
    this.numOfEntires = 0;
  }

  @Override
  public Iterator<T> getIterator() {
    return new HashSetIterator();
  }

  // --- INTERNAL HELPERS ---

  private int getBucketIndex(T element) {
    int h = element.hashCode();
    h ^= (h >>> 16); // High-bit XOR mixer
    return (h & 0x7FFFFFFF) % buckets.length;
  }

  @SuppressWarnings("unchecked")
  private void resize() {
    Node<T>[] oldBuckets = buckets;
    buckets = (Node<T>[]) new Node[oldBuckets.length * 2];
    numOfEntires = 0; // Will recalculate as we re-add elements

    for (Node<T> head : oldBuckets) {
      Node<T> curr = head;
      while (curr != null) {
        add(curr.data);
        curr = curr.next;
      }
    }
  }

  // --- ITERATOR IMPLEMENTATION ---

  private class HashSetIterator implements Iterator<T> {
    private int currentBucket = 0;
    private Node<T> currentNode = null;

    public HashSetIterator() {
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
    public T next() {
      if (!hasNext()) return null;

      T data = currentNode.data;
      advanceToNextNode();
      return data;
    }
  }
}
