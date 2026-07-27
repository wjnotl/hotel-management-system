package adt;

import java.util.Iterator;

public interface SetInterface<T> {

  // Inserts an element into the set. Returns false if the element already exists.
  public boolean add(T element);

  // Evicts the element completely and returns true if it was present, false otherwise.
  public boolean remove(T element);

  // Checks if a specific element exists anywhere in the set.
  public boolean contains(T element);

  // Returns the total number of elements.
  public int size();

  // Checks if the set is empty.
  public boolean isEmpty();

  // Flushes all elements from the set.
  public void clear();

  // Yields an iterator to step through all the elements.
  public Iterator<T> getIterator();
}
