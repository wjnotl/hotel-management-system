package adt;

import java.util.Iterator;

public interface PriorityQueueInterface<T> {
  // Pushes an entry in with an explicitly assigned numeric priority.
  public boolean enqueue(T newEntry, int priority);

  // Pulls out the highest (or lowest) priority item sitting at the root.
  public T dequeue();

  // Peeks at the root element without touching it.
  public T peek();

  // Scans for a value matching this object and completely purges it.
  public boolean remove(T entry);

  // Forces a removal at an exact internal heap position index.
  public T removeAt(int position);

  // Double-checks if this specific data instance is in the queue.
  public boolean contains(T entry);

  // Tells how many entries are currently waiting.
  public int getNumberOfEntries();

  // Grabs the numeric priority score assigned to a given item.
  public int getPriority(T entry);

  // Changes an item's priority score on the fly and re-balances the heap tree.
  public boolean changePriority(T entry, int newPriority);

  // True if the queue is empty.
  public boolean isEmpty();

  // True if the backing list cannot accept more entries.
  public boolean isFull();

  // Clears out the queue completely.
  public void clear();

  // Yields an iterator to step through the elements.
  public Iterator<T> getIterator();
}
