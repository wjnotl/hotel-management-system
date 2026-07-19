package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

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

  public PriorityQueue() {
    this(new AList<>(), true);
  }

  public PriorityQueue(boolean isMaxHeap) {
    this(new AList<>(), isMaxHeap);
  }

  public PriorityQueue(ListInterface<PriorityEntry<T>> list, boolean isMaxHeap) {
    this.list = list;
    this.isMaxHeap = isMaxHeap;
  }

  @Override
  public boolean enqueue(T newEntry, int priority) {
    // Deny duplicates or processing if values are null
    if (newEntry == null || isFull() || positionOf(newEntry) != -1) {
      return false;
    }
    // Append entry at the base of the list array and bubble it up into place
    list.add(new PriorityEntry<>(newEntry, priority));
    siftUp(list.getNumberOfEntries());
    return true;
  }

  @Override
  public T dequeue() {
    if (isEmpty()) return null;

    T top = list.getEntry(1).entry; // Save root element data
    int lastPosition = list.getNumberOfEntries();

    if (lastPosition == 1) {
      list.remove(1);
    } else {
      // Pull the tail end node all the way up to act as temporary root node
      list.replace(1, list.getEntry(lastPosition));
      list.remove(lastPosition);
      siftDown(1); // Bubble it downwards to fix balancing
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

    // Swap structural layout locations with the tail element to delete smoothly
    list.replace(pos, list.getEntry(lastPosition));
    list.remove(lastPosition);

    // Balance in both directions since the exact offset relationship is unknown
    siftDown(pos);
    siftUp(pos);
    return true;
  }

  @Override
  public boolean contains(T entry) {
    return positionOf(entry) != -1;
  }

  @Override
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

    PriorityEntry<T> heapEntry = list.getEntry(pos);
    int oldPriority = heapEntry.priority;

    // Mutate priority score and explicitly overwrite data changes in the AList container layer
    heapEntry.priority = newPriority;
    list.replace(pos, heapEntry);

    // Shift positions up or down based on how priority changed
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

  public boolean isMaxHeap() {
    return isMaxHeap;
  }

  public void setMaxHeap(boolean isMaxHeap) {
    if (this.isMaxHeap == isMaxHeap) return;
    this.isMaxHeap = isMaxHeap;
    rebuildHeap(); // Re-index entire structural collection to honor new sorting strategy
  }

  private void rebuildHeap() {
    int size = list.getNumberOfEntries();
    // Run Floyd's build-heap loop backward from the lowest parent element to the root node
    for (int i = size / 2; i >= 1; i--) {
      siftDown(i);
    }
  }

  // INTERNAL HELPERS

  private int positionOf(T entry) {
    for (int i = 1; i <= list.getNumberOfEntries(); i++) {
      if (list.getEntry(i).entry.equals(entry)) return i;
    }
    return -1;
  }

  private boolean higherPriority(int a, int b) {
    // Determines if entry A stays closer to the root tree position than entry B
    return isMaxHeap ? (a > b) : (a < b);
  }

  // Quick 1-based parent-child tree mapping index algorithms
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
    // Escalate item upward while priority beats parent parameters
    while (i > 1 && higherPriority(list.getEntry(i).priority, list.getEntry(parent(i)).priority)) {
      swap(i, parent(i));
      i = parent(i);
    }
  }

  private void siftDown(int i) {
    int size = list.getNumberOfEntries();
    while (true) {
      int l = left(i), r = right(i), best = i;

      // Identify the most appropriate element candidate among current node and its children
      if (l <= size && higherPriority(list.getEntry(l).priority, list.getEntry(best).priority))
        best = l;
      if (r <= size && higherPriority(list.getEntry(r).priority, list.getEntry(best).priority))
        best = r;

      if (best == i) break; // Balance criteria satisfied completely
      swap(i, best);
      i = best;
    }
  }

  // ITERATOR IMPLEMENTATION

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
