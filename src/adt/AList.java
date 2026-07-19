package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class AList<T> implements ListInterface<T> {

  private T[] listArray;
  private int numberOfEntries;
  private static final int DEFAULT_CAPACITY = 25;

  public AList() {
    this(DEFAULT_CAPACITY);
  }

  @SuppressWarnings("unchecked")
  public AList(int initialCapacity) {
    // Java generic array instantiation constraint workaround.
    // Making an Object array and casting is the industry bypass.
    listArray = (T[]) new Object[initialCapacity];
    numberOfEntries = 0;
  }

  @Override
  public boolean add(T newEntry) {
    if (isFull()) {
      doubleCapacity();
    }
    listArray[numberOfEntries] = newEntry;
    numberOfEntries++;
    return true;
  }

  @Override
  public boolean add(int newPosition, T newEntry) {
    // Validate 1-based bounds before modifying the array layout
    if (newPosition >= 1 && newPosition <= numberOfEntries + 1) {
      if (isFull()) {
        doubleCapacity();
      }

      // Shove elements forward to carve out an opening for the insert
      makeRoom(newPosition);
      listArray[newPosition - 1] = newEntry; // Convert 1-based index to 0-based array index
      numberOfEntries++;
      return true;
    }
    return false;
  }

  @Override
  public T remove(int givenPosition) {
    if (givenPosition >= 1 && givenPosition <= numberOfEntries) {
      T result = listArray[givenPosition - 1]; // Save old data for return

      // Shift elements backward to cover up the hole just made
      if (givenPosition < numberOfEntries) {
        removeGap(givenPosition);
      }

      listArray[numberOfEntries - 1] = null; // Clean up old slot reference to avoid memory leaks
      numberOfEntries--;
      return result;
    }
    return null;
  }

  @Override
  public void clear() {
    // Explicitly nullify data references so the garbage collector can sweep them
    for (int index = 0; index < numberOfEntries; index++) {
      listArray[index] = null;
    }
    numberOfEntries = 0;
  }

  @Override
  public boolean replace(int givenPosition, T newEntry) {
    if (givenPosition >= 1 && givenPosition <= numberOfEntries) {
      listArray[givenPosition - 1] = newEntry;
      return true;
    }
    return false;
  }

  @Override
  public T getEntry(int givenPosition) {
    if (givenPosition >= 1 && givenPosition <= numberOfEntries) {
      return listArray[givenPosition - 1];
    }
    return null;
  }

  @Override
  public boolean contains(T anEntry) {
    for (int index = 0; index < numberOfEntries; index++) {
      if (anEntry.equals(listArray[index])) {
        return true;
      }
    }
    return false;
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
    // Capacity scales out automatically, so the list is never conceptually full.
    return false;
  }

  @Override
  public Iterator<T> getIterator() {
    return new AListIterator();
  }

  @Override
  public String toString() {
    StringBuilder output = new StringBuilder();
    for (int index = 0; index < numberOfEntries; index++) {
      output.append(listArray[index]).append("\n");
    }
    return output.toString();
  }

  // INTERNAL HELPERS

  @SuppressWarnings("unchecked")
  private void doubleCapacity() {
    int newLength = 2 * listArray.length;
    T[] oldList = listArray;

    listArray = (T[]) new Object[newLength];
    System.arraycopy(oldList, 0, listArray, 0, numberOfEntries);
  }

  private void makeRoom(int newPosition) {
    int newIndex = newPosition - 1;
    int lastIndex = numberOfEntries - 1;

    // Shift elements right starting from the tail to avoid overwriting values
    for (int index = lastIndex; index >= newIndex; index--) {
      listArray[index + 1] = listArray[index];
    }
  }

  private void removeGap(int givenPosition) {
    int removedIndex = givenPosition - 1;
    int lastIndex = numberOfEntries - 1;

    // Shift elements left to overwrite the target spot smoothly
    for (int index = removedIndex; index < lastIndex; index++) {
      listArray[index] = listArray[index + 1];
    }
  }

  // ITERATOR IMPLEMENTATION

  private class AListIterator implements Iterator<T> {
    private int currentIndex = 0;

    @Override
    public boolean hasNext() {
      return currentIndex < numberOfEntries;
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      T entry = listArray[currentIndex];
      currentIndex++;
      return entry;
    }
  }
}
