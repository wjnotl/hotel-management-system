package adt;

import java.util.Iterator;

public interface DequeInterface<T> {

  // Pushes a new entry onto the front (head) of the deque.
  public boolean addFirst(T newEntry);

  // Appends a new entry onto the back (tail) of the deque.
  public boolean addLast(T newEntry);

  // Pulls out and returns whatever is sitting at the front of the deque.
  public T removeFirst();

  // Pulls out and returns whatever is sitting at the back of the deque.
  public T removeLast();

  // Peeks at the front entry without removing it.
  public T peekFirst();

  // Peeks at the back entry without removing it.
  public T peekLast();

  // Scans for a matching value anywhere in the deque and removes it in place.
  public boolean remove(T entry);

  // Double-checks if this specific value exists anywhere in the deque.
  public boolean contains(T entry);

  // Returns the current element count.
  public int getNumberOfEntries();

  // True if there is absolutely nothing in here.
  public boolean isEmpty();

  // Tells if the backing structure is maxed out.
  public boolean isFull();

  // Wipes the deque completely clean.
  public void clear();

  // Gives back a standard iterator to traverse front-to-back.
  public Iterator<T> getIterator();
}
