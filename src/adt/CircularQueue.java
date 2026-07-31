package adt;

import java.io.Serializable;
import java.util.Iterator;

public class CircularQueue<T> implements QueueInterface<T>, Serializable {
  private static final long serialVersionUID = 1L;
  private static final int DEFAULT_CAPACITY = 25;

  private T[] array;
  private int frontIndex;
  private int rearIndex;
  private int numberOfEntries;
  private final boolean canExpand;

  public CircularQueue() {
    this(DEFAULT_CAPACITY, true);
  }

  public CircularQueue(int initialCapacity) {
    this(initialCapacity, true);
  }

  @SuppressWarnings("unchecked")
  public CircularQueue(int initialCapacity, boolean canExpand) {
    if (initialCapacity < 1) {
      initialCapacity = DEFAULT_CAPACITY;
    }

    this.array = (T[]) new Object[initialCapacity];
    this.canExpand = canExpand;
    clear();
  }

  @Override
  public final void clear() {
    for (int i = 0; i < numberOfEntries; i++) {
      array[(frontIndex + i) % array.length] = null;
    }

    frontIndex = 0;
    rearIndex = array.length - 1;
    numberOfEntries = 0;
  }

  @Override
  public boolean enqueue(T newEntry) {
    if (isFull()) {
      if (!canExpand) {
        return false;
      }
      doubleCapacity();
    }

    rearIndex = (rearIndex + 1) % array.length;
    array[rearIndex] = newEntry;
    numberOfEntries++;
    return true;
  }

  @Override
  public T dequeue() {
    if (isEmpty()) return null;

    T result = array[frontIndex];

    array[frontIndex] = null;

    frontIndex = (frontIndex + 1) % array.length;
    numberOfEntries--;
    return result;
  }

  @Override
  public T peek() {
    return isEmpty() ? null : array[frontIndex];
  }

  @Override
  public boolean contains(T entry) {
    return getPosition(entry) != -1;
  }

  @Override
  public int getPosition(T entry) {
    if (entry == null || isEmpty()) return -1;

    for (int i = 0; i < numberOfEntries; i++) {
      T current = array[(frontIndex + i) % array.length];
      if (current != null && current.equals(entry)) {
        return i + 1; // 1-based, so the front of the line reads as position 1
      }
    }
    return -1;
  }

  @Override
  public boolean remove(T entry) {
    int position = getPosition(entry);
    if (position == -1) return false;

    if (position == 1) {
      dequeue();
      return true;
    }

    for (int i = position - 1; i < numberOfEntries - 1; i++) {
      array[(frontIndex + i) % array.length] = array[(frontIndex + i + 1) % array.length];
    }

    array[rearIndex] = null;
    rearIndex = (rearIndex - 1 + array.length) % array.length;
    numberOfEntries--;
    return true;
  }

  @Override
  public int getNumberOfEntries() {
    return numberOfEntries;
  }

  @Override
  public boolean isEmpty() {
    return numberOfEntries == 0;
  }

  @Override
  public boolean isFull() {
    return numberOfEntries == array.length;
  }

  /** Shows how much it currently owns, not the same as how many are in use. */
  public int getCapacity() {
    return array.length;
  }

  public boolean canExpand() {
    return canExpand;
  }

  @Override
  public Iterator<T> getIterator() {
    return new CircularQueueIterator();
  }

  @SuppressWarnings("unchecked")
  private void doubleCapacity() {
    T[] oldArray = array;
    T[] newArray = (T[]) new Object[2 * oldArray.length];

    for (int i = 0; i < numberOfEntries; i++) {
      newArray[i] = oldArray[(frontIndex + i) % oldArray.length];
    }

    array = newArray;
    frontIndex = 0;
    rearIndex = numberOfEntries - 1;
  }

  private class CircularQueueIterator implements Iterator<T> {
    private int stepsTaken = 0;

    @Override
    public boolean hasNext() {
      return stepsTaken < numberOfEntries;
    }

    @Override
    public T next() {
      if (!hasNext()) return null;

      T entry = array[(frontIndex + stepsTaken) % array.length];
      stepsTaken++;
      return entry;
    }
  }
}
