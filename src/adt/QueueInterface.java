package adt;

import java.util.Iterator;

public interface QueueInterface<T> {

  // Joins the back of the line.
  public boolean enqueue(T newEntry);

  // Pulls out and returns whoever's at the front of the line.
  public T dequeue();

  // Peeks at the front entry without removing it.
  public T peek();

  // Double-checks if this specific value exists anywhere in the queue.
  public boolean contains(T entry);

  // Reports the 1-based place from the front, or -1 when the entry is not in the queue.
  public int getPosition(T entry);

  // Pulls out the first matching entry wherever it sits, which a plain FIFO queue cannot do.
  public boolean remove(T entry);

  // Returns the current element count.
  public int getNumberOfEntries();

  // True if there is absolutely nothing in here.
  public boolean isEmpty();

  // Tells if the backing structure is maxed out.
  public boolean isFull();

  // Wipes the queue completely clean.
  public void clear();

  // Gives back a standard iterator to traverse front-to-back.
  public Iterator<T> getIterator();
}
