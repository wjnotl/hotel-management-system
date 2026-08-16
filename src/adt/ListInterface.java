package adt;

import java.util.Comparator;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface ListInterface<T> extends Iterable<T> {

  // Appends a new item to the tail end of the list.
  public boolean add(T newEntry);

  // Inserts an item into a specific 1-based spot, sliding everything else down.
  public boolean add(int newPosition, T newEntry);

  // Plucks out the entry at the given 1-based position and closes the gap.
  public boolean remove(T entry);

  public T removeAt(int givenPosition);

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

  // Sorts the list contents using a custom Comparator.
  public void sort(Comparator<T> comparator);

  // Returns a filtered list of entries that match a given predicate.
  public ListInterface<T> filter(Predicate<T> predicate);

  // Returns the first entry that matches a given predicate.
  public T find(Predicate<T> predicate);

  // Transforms each element from type T to type R using a mapping function.
  public <R> ListInterface<R> map(Function<T, R> mapper);

  // Combines all elements into a single accumulated value.
  public <U> U reduce(U identity, BiFunction<U, T, U> accumulator);

  // Gives back a standard iterator to cleanly traverse elements sequentially.
  public Iterator<T> getIterator();

  @Override
  default Iterator<T> iterator() {
    return getIterator();
  }
}
