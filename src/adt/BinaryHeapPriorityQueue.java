package adt;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;

public class BinaryHeapPriorityQueue<T> implements PriorityQueueInterface<T>, Serializable {
  private static final long serialVersionUID = 1L;

  private static final int DEFAULT_CAPACITY = 16;
  private final boolean canExpand;
  private final Comparator<? super T> comparator;

  private PriorityEntry<T>[] array; // starts from index 1
  private int size;
  private long sequenceCounter = 0;

  private static class PriorityEntry<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    T entry;
    long sequenceNumber;

    PriorityEntry(T entry, long sequenceNumber) {
      this.entry = entry;
      this.sequenceNumber = sequenceNumber;
    }
  }

  public BinaryHeapPriorityQueue() {
    this(DEFAULT_CAPACITY, true, null);
  }

  public BinaryHeapPriorityQueue(Comparator<? super T> comparator) {
    this(DEFAULT_CAPACITY, true, comparator);
  }

  public BinaryHeapPriorityQueue(int initialCapacity) {
    this(initialCapacity, true, null);
  }

  @SuppressWarnings("unchecked")
  public BinaryHeapPriorityQueue(
      int initialCapacity, boolean canExpand, Comparator<? super T> comparator) {
    int cap = initialCapacity <= 0 ? DEFAULT_CAPACITY : initialCapacity;
    this.array = (PriorityEntry<T>[]) new PriorityEntry[cap + 1];
    this.canExpand = canExpand;
    this.comparator = comparator;
    this.size = 0;
  }

  @Override
  public boolean enqueue(T newEntry) {
    if (newEntry == null) return false;

    if (isFull()) {
      if (!canExpand) return false;
      grow();
    }

    size++;
    array[size] = new PriorityEntry<>(newEntry, sequenceCounter++);
    siftUp(size);
    return true;
  }

  @Override
  public T dequeue() {
    if (isEmpty() || array[1] == null) return null;

    T top = array[1].entry;

    if (size == 1) {
      array[1] = null;
    } else {
      array[1] = array[size];
      array[size] = null;
      siftDown(1);
    }

    size--;
    return top;
  }

  @Override
  public T peek() {
    return (isEmpty() || array[1] == null) ? null : array[1].entry;
  }

  @Override
  public boolean remove(T entry) {
    int pos = positionOf(entry);
    if (pos == -1) return false;

    if (pos == size) {
      array[size] = null;
      size--;
      return true;
    }

    array[pos] = array[size];
    array[size] = null;
    size--;

    if (pos <= size && array[pos] != null) {
      siftDown(pos);
      siftUp(pos);
    }
    return true;
  }

  @Override
  public T removeAt(int position) {
    if (position < 1 || position > size || array[position] == null) return null;

    T removed = array[position].entry;

    if (position == size) {
      array[size] = null;
      size--;
    } else {
      array[position] = array[size];
      array[size] = null;
      size--;

      if (position <= size && array[position] != null) {
        siftDown(position);
        siftUp(position);
      }
    }
    return removed;
  }

  @Override
  public boolean contains(T entry) {
    return positionOf(entry) != -1;
  }

  @Override
  public int getPosition(T entry) {
    return positionOf(entry);
  }

  @Override
  public int getNumberOfEntries() {
    return size;
  }

  @Override
  public boolean updatePriority(T entry) {
    int pos = positionOf(entry);
    if (pos == -1 || array[pos] == null) return false;

    siftUp(pos);
    siftDown(pos);
    return true;
  }

  @Override
  public boolean isEmpty() {
    return size <= 0 || array[1] == null;
  }

  @Override
  public boolean isFull() {
    return size >= array.length - 1;
  }

  @Override
  public void clear() {
    for (int i = 1; i <= size; i++) {
      array[i] = null;
    }
    size = 0;
  }

  @Override
  public Iterator<T> getIterator() {
    return new PriorityQueueIterator();
  }

  // --- INTERNAL HELPERS ---

  @SuppressWarnings("unchecked")
  private void grow() {
    int newCapacity = (array.length - 1) * 2 + 1;
    PriorityEntry<T>[] newArray = (PriorityEntry<T>[]) new PriorityEntry[newCapacity];
    for (int i = 1; i <= size; i++) {
      newArray[i] = array[i];
    }
    array = newArray;
  }

  private int positionOf(T entry) {
    if (entry == null || isEmpty()) return -1;
    for (int i = 1; i <= size; i++) {
      if (array[i] != null && array[i].entry != null && array[i].entry.equals(entry)) {
        return i;
      }
    }
    return -1;
  }

  @SuppressWarnings("unchecked")
  private boolean hasHigherPriority(PriorityEntry<T> a, PriorityEntry<T> b) {
    if (a == null) return false;
    if (b == null) return true;

    int comp;
    if (comparator != null) {
      comp = comparator.compare(a.entry, b.entry);
    } else {
      comp = ((Comparable<? super T>) a.entry).compareTo(b.entry);
    }

    // Positive means higher priority (Max-Heap order)
    if (comp != 0) {
      return comp > 0;
    }

    // Tie-breaker: earlier insertion sequence wins (FIFO)
    return a.sequenceNumber < b.sequenceNumber;
  }

  private int parent(int i) {
    return i / 2;
  }

  private int left(int i) {
    return 2 * i;
  }

  private int right(int i) {
    return 2 * i + 1;
  }

  private void swap(int a, int b) {
    PriorityEntry<T> temp = array[a];
    array[a] = array[b];
    array[b] = temp;
  }

  private void siftUp(int i) {
    while (i > 1 && array[i] != null && hasHigherPriority(array[i], array[parent(i)])) {
      swap(i, parent(i));
      i = parent(i);
    }
  }

  private void siftDown(int i) {
    while (true) {
      int l = left(i), r = right(i), best = i;

      if (l <= size && array[l] != null && hasHigherPriority(array[l], array[best])) {
        best = l;
      }
      if (r <= size && array[r] != null && hasHigherPriority(array[r], array[best])) {
        best = r;
      }

      if (best == i) break;
      swap(i, best);
      i = best;
    }
  }

  // --- ITERATOR IMPLEMENTATION ---

  private class PriorityQueueIterator implements Iterator<T> {
    private int position = 1;

    @Override
    public boolean hasNext() {
      return position <= size;
    }

    @Override
    public T next() {
      if (!hasNext()) return null;

      T data = array[position] != null ? array[position].entry : null;
      position++;
      return data;
    }
  }
}
