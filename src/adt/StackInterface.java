package adt;

import java.util.Iterator;

public interface StackInterface<T> {

  // Pushes a new entry onto the top of the stack.
  public boolean push(T newEntry);

  // Pops the top entry off the stack and returns it.
  public T pop();

  // Peeks at the top entry without removing it.
  public T peek();

  // Returns the current element count.
  public int getNumberOfEntries();

  // True if there is absolutely nothing in here.
  public boolean isEmpty();

  // Tells if the backing structure is maxed out.
  public boolean isFull();

  // Wipes the stack completely clean.
  public void clear();

  // Gives back a standard iterator to traverse top-to-bottom.
  public Iterator<T> getIterator();
}
