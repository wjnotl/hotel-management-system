package adt;

import java.util.Iterator;

public class BinaryHeapPriorityQueue<T> implements PriorityQueueInterface<T> {
  private static final int DEFAULT_CAPACITY = 16;
  private final boolean canExpand;
  private boolean isMaxHeap;

  private PriorityEntry<T>[] array; // start from index 1 instead of index 0
  private int size;
  private long sequenceCounter = 0;

  private static class PriorityEntry<T> {
    T entry;
    int priority;
    long sequenceNumber;

    PriorityEntry(T entry, int priority, long sequenceNumber) {
      this.entry = entry;
      this.priority = priority;
      this.sequenceNumber = sequenceNumber;
    }
  }

  public BinaryHeapPriorityQueue() {
    this(DEFAULT_CAPACITY, true, true);
  }

  public BinaryHeapPriorityQueue(int initialCapacity) {
    this(initialCapacity, true, true);
  }

  public BinaryHeapPriorityQueue(boolean isMaxHeap) {
    this(DEFAULT_CAPACITY, true, isMaxHeap);
  }

  public BinaryHeapPriorityQueue(boolean canExpand, boolean isMaxHeap) {
    this(DEFAULT_CAPACITY, canExpand, isMaxHeap);
  }

  @SuppressWarnings("unchecked")
  public BinaryHeapPriorityQueue(int initialCapacity, boolean canExpand, boolean isMaxHeap) {
    int cap = initialCapacity <= 0 ? DEFAULT_CAPACITY : initialCapacity;
    this.array = (PriorityEntry<T>[]) new PriorityEntry[cap + 1];
    this.canExpand = canExpand;
    this.isMaxHeap = isMaxHeap;
    this.size = 0;
  }

  @Override
  public boolean enqueue(T newEntry, int priority) {
    if (newEntry == null) {
      return false;
    }
    if (isFull()) {
      if (!canExpand) return false;
      grow();
    }

    size++;
    array[size] = new PriorityEntry<>(newEntry, priority, sequenceCounter++);
    siftUp(size);
    return true;
  }

  @Override
  public T dequeue() {
    if (isEmpty()) return null;

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
    return isEmpty() ? null : array[1].entry;
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

    siftDown(pos);
    siftUp(pos);
    return true;
  }

  @Override
  public T removeAt(int position) {
    if (position < 1 || position > size) return null;

    T removed = array[position].entry;

    if (position == size) {
      array[size] = null;
      size--;
    } else {
      array[position] = array[size];
      array[size] = null;
      size--;
      siftDown(position);
      siftUp(position);
    }
    return removed;
  }

  @Override
  public boolean contains(T entry) {
    return positionOf(entry) != -1;
  }

  @Override
  public int getNumberOfEntries() {
    return size;
  }

  @Override
  public int getPriority(T entry) {
    int pos = positionOf(entry);
    if (pos == -1) return -1;

    return array[pos].priority;
  }

  @Override
  public boolean changePriority(T entry, int newPriority) {
    int pos = positionOf(entry);
    if (pos == -1) return false;

    int oldPriority = array[pos].priority;
    array[pos].priority = newPriority;

    if (hasHigherPriority(
        array[pos], new PriorityEntry<>(null, oldPriority, array[pos].sequenceNumber))) {
      siftUp(pos);
    } else if (newPriority != oldPriority) {
      siftDown(pos);
    }

    return true;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public boolean isFull() {
    return size == array.length - 1;
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

  public boolean isMaxHeap() {
    return isMaxHeap;
  }

  public void setMaxHeap(boolean isMaxHeap) {
    if (this.isMaxHeap == isMaxHeap) return;
    this.isMaxHeap = isMaxHeap;
    rebuildHeap();
  }

  private void rebuildHeap() {
    for (int i = size / 2; i >= 1; i--) {
      siftDown(i);
    }
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
    if (entry == null) return -1;
    for (int i = 1; i <= size; i++) {
      if (array[i].entry.equals(entry)) return i;
    }
    return -1;
  }

  private boolean hasHigherPriority(PriorityEntry<T> a, PriorityEntry<T> b) {
    if (a.priority != b.priority) {
      return isMaxHeap ? a.priority > b.priority : a.priority < b.priority;
    }

    // if have same priority then use sequence number (earlier sequence number takes precedence)
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
    while (i > 1 && hasHigherPriority(array[i], array[parent(i)])) {
      swap(i, parent(i));
      i = parent(i);
    }
  }

  private void siftDown(int i) {
    while (true) {
      int l = left(i), r = right(i), best = i;

      if (l <= size && hasHigherPriority(array[l], array[best])) {
        best = l;
      }
      if (r <= size && hasHigherPriority(array[r], array[best])) {
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

      T data = array[position].entry;
      position++;
      return data;
    }
  }
}
