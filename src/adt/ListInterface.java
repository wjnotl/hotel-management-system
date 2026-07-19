package adt;

import java.util.Iterator;

public interface ListInterface<T> {

  // Appends a new item to the tail end of the list.
  public boolean add(T newEntry);

  // Inserts an item into a specific 1-based spot, sliding everything else down.
  public boolean add(int newPosition, T newEntry);

  // Plucks out the entry at the given 1-based position and closes the gap.
  public T remove(int givenPosition);

  // Wipes the list completely clean.
  public void clear();

  // Swaps out the data at a specific 1-based index with a new value.
  public boolean replace(int givenPosition, T newEntry);

  // Peeks at the data sitting at a given 1-based index.
  public T getEntry(int givenPosition);

  // Loops through to see if a matching value lives inside the list.
  public boolean contains(T anEntry);

  // Returns the current element count.
  public int getNumberOfEntries();

  // True if there is absolutely nothing in here.
  public boolean isEmpty();

  // Tells if the backing structure is maxed out.
  public boolean isFull();

  // Gives back a standard iterator to cleanly traverse elements sequentially.
  public Iterator<T> getIterator();
}
