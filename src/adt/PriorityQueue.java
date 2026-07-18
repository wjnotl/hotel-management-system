package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Heap-based priority queue, built on top of any ListInterface implementation (AList or LinkedList
 * - your choice, passed into the constructor). Can act as a max-heap or min-heap; flip with
 * setMaxHeap(), which triggers a full rebuild since a max-heap layout usually isn't a valid
 * min-heap layout.
 *
 * <p>Position math (1-based, matches ListInterface): parent(i) = i / 2 left(i) = 2 * i right(i) = 2
 * * i + 1
 */
public class PriorityQueue<T> implements PriorityQueueInterface<T> {

  private static class PriorityEntry<T> {
    T entry;
    int priority;

    PriorityEntry(T entry, int priority) {
      this.entry = entry;
      this.priority = priority;
    }
  }

  private final ListInterface<PriorityEntry<T>> list;
  private boolean isMaxHeap;

  // default: max-heap, backed by LinkedList (no capacity ceiling)
  public PriorityQueue() {
    this(new LinkedList<>(), true);
  }

  public PriorityQueue(boolean isMaxHeap) {
    this(new LinkedList<>(), isMaxHeap);
  }

  // plug in whichever ListInterface implementation you want underneath
  public PriorityQueue(ListInterface<PriorityEntry<T>> list, boolean isMaxHeap) {
    this.list = list;
    this.isMaxHeap = isMaxHeap;
  }

  @Override
  public boolean enqueue(T newEntry, int priority) {
    if (newEntry == null || isFull() || positionOf(newEntry) != -1) {
      return false;
    }
    list.add(new PriorityEntry<>(newEntry, priority));
    siftUp(list.getNumberOfEntries());
    return true;
  }

  @Override
  public T dequeue() {
    if (isEmpty()) return null;

    T top = list.getEntry(1).entry;
    int lastPosition = list.getNumberOfEntries();

    if (lastPosition == 1) {
      list.remove(1);
    } else {
      list.replace(1, list.getEntry(lastPosition));
      list.remove(lastPosition);
      siftDown(1);
    }
    return top;
  }

  @Override
  public T peek() {
    return isEmpty() ? null : list.getEntry(1).entry;
  }

  @Override
  public boolean remove(T entry) {
    int pos = positionOf(entry);
    if (pos == -1) return false;

    int lastPosition = list.getNumberOfEntries();
    if (pos == lastPosition) {
      list.remove(pos);
      return true;
    }

    list.replace(pos, list.getEntry(lastPosition));
    list.remove(lastPosition);
    siftDown(pos);
    siftUp(pos);
    return true;
  }

  @Override
  public boolean contains(T entry) {
    return positionOf(entry) != -1;
  }

  /**
   * Removes whatever entry currently sits at the given internal heap position (1-based, matches
   * ListInterface). This is NOT "the Nth person in line" - position 2 and 3 are just wherever the
   * tree balancing happened to leave them, not queue order. Mainly useful for testing/debugging the
   * heap directly.
   *
   * @return the removed entry, or null if position is out of range
   */
  public T removeAt(int position) {
    int size = list.getNumberOfEntries();
    if (position < 1 || position > size) return null;

    T removed = list.getEntry(position).entry;

    if (position == size) {
      list.remove(position);
    } else {
      list.replace(position, list.getEntry(size));
      list.remove(size);
      siftDown(position);
      siftUp(position);
    }
    return removed;
  }

  @Override
  public int getNumberOfEntries() {
    return list.getNumberOfEntries();
  }

  @Override
  public int getPriority(T entry) {
    int pos = positionOf(entry);
    if (pos == -1) return -1;
    return list.getEntry(pos).priority;
  }

  @Override
  public boolean changePriority(T entry, int newPriority) {
    int pos = positionOf(entry);
    if (pos == -1) return false;

    int oldPriority = list.getEntry(pos).priority;
    list.getEntry(pos).priority = newPriority;

    if (higherPriority(newPriority, oldPriority)) {
      siftUp(pos);
    } else if (newPriority != oldPriority) {
      siftDown(pos);
    }
    return true;
  }

  @Override
  public boolean isEmpty() {
    return list.isEmpty();
  }

  @Override
  public boolean isFull() {
    return list.isFull();
  }

  @Override
  public void clear() {
    list.clear();
  }

  @Override
  public Iterator<T> getIterator() {
    return new PriorityQueueIterator();
  }

  // ---------- min/max switching ----------

  public boolean isMaxHeap() {
    return isMaxHeap;
  }

  /**
   * Flips between max-heap and min-heap mode. Since a valid max-heap arrangement isn't generally a
   * valid min-heap arrangement, this triggers a full rebuild (Floyd's build-heap, O(n)) rather than
   * just toggling the flag.
   */
  public void setMaxHeap(boolean isMaxHeap) {
    if (this.isMaxHeap == isMaxHeap) return; // no change needed
    this.isMaxHeap = isMaxHeap;
    rebuildHeap();
  }

  // starts from the last parent node and sifts down each one, working
  // backwards to the root - standard O(n) build-heap approach
  private void rebuildHeap() {
    int size = list.getNumberOfEntries();
    for (int i = size / 2; i >= 1; i--) {
      siftDown(i);
    }
  }

  // ---------- internal helpers ----------

  private int positionOf(T entry) {
    for (int i = 1; i <= list.getNumberOfEntries(); i++) {
      if (list.getEntry(i).entry.equals(entry)) return i;
    }
    return -1;
  }

  // "higher priority" means closer to the root - for a max-heap that's the
  // bigger number, for a min-heap that's the smaller number
  private boolean higherPriority(int a, int b) {
    return isMaxHeap ? (a > b) : (a < b);
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
    PriorityEntry<T> temp = list.getEntry(a);
    list.replace(a, list.getEntry(b));
    list.replace(b, temp);
  }

  private void siftUp(int i) {
    while (i > 1 && higherPriority(list.getEntry(i).priority, list.getEntry(parent(i)).priority)) {
      swap(i, parent(i));
      i = parent(i);
    }
  }

  private void siftDown(int i) {
    int size = list.getNumberOfEntries();
    while (true) {
      int l = left(i), r = right(i), best = i;

      if (l <= size && higherPriority(list.getEntry(l).priority, list.getEntry(best).priority))
        best = l;
      if (r <= size && higherPriority(list.getEntry(r).priority, list.getEntry(best).priority))
        best = r;

      if (best == i) break;
      swap(i, best);
      i = best;
    }
  }

  private class PriorityQueueIterator implements Iterator<T> {
    private int position = 1;

    @Override
    public boolean hasNext() {
      return position <= list.getNumberOfEntries();
    }

    @Override
    public T next() {
      if (!hasNext()) throw new NoSuchElementException();
      T data = list.getEntry(position).entry;
      position++;
      return data;
    }
  }
}
