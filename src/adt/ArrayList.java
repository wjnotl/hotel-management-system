package adt;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class ArrayList<T> implements ListInterface<T>, Serializable {
  private static final long serialVersionUID = 1L;

  private T[] array;
  private int numOfEntries;
  private static final int DEFAULT_CAPACITY = 25;
  private boolean canExpand;

  public ArrayList() {
    this(DEFAULT_CAPACITY);
  }

  public ArrayList(int initialCapacity) {
    this(initialCapacity, true);
  }

  @SuppressWarnings("unchecked")
  public ArrayList(int initialCapacity, boolean canExpand) {
    array = (T[]) new Object[initialCapacity];
    numOfEntries = 0;
    this.canExpand = canExpand;
  }

  @Override
  public boolean add(T newEntry) {
    if (isFull()) {
      if (!canExpand) {
        return false;
      }
      doubleCapacity();
    }
    array[numOfEntries] = newEntry;
    numOfEntries++;
    return true;
  }

  @Override
  public boolean add(int newPosition, T newEntry) {
    if (newPosition < 1 || newPosition > numOfEntries + 1) return false;

    if (isFull()) {
      if (!canExpand) {
        return false;
      }
      doubleCapacity();
    }

    makeRoom(newPosition);
    array[newPosition - 1] = newEntry;
    numOfEntries++;

    return true;
  }

  @Override
  public boolean remove(T entry) {
    int pos = getPosition(entry);
    if (pos == -1) return false;

    removeAt(pos);
    return true;
  }

  @Override
  public T removeAt(int givenPosition) {
    if (givenPosition < 1 || givenPosition > numOfEntries) return null;

    T result = array[givenPosition - 1];

    if (givenPosition < numOfEntries) {
      removeGap(givenPosition);
    }

    array[numOfEntries - 1] = null;
    numOfEntries--;
    return result;
  }

  @Override
  public void clear() {
    for (int index = 0; index < numOfEntries; index++) {
      array[index] = null;
    }
    numOfEntries = 0;
  }

  @Override
  public boolean replace(int givenPosition, T newEntry) {
    if (givenPosition < 1 || givenPosition > numOfEntries) return false;

    array[givenPosition - 1] = newEntry;
    return true;
  }

  @Override
  public T getEntry(int givenPosition) {
    if (givenPosition < 1 || givenPosition > numOfEntries) return null;
    return array[givenPosition - 1];
  }

  @Override
  public boolean contains(T anEntry) {
    return getPosition(anEntry) != -1;
  }

  @Override
  public int getNumberOfEntries() {
    return numOfEntries;
  }

  @Override
  public boolean isEmpty() {
    return numOfEntries == 0;
  }

  @Override
  public boolean isFull() {
    return numOfEntries == array.length;
  }

  @Override
  public void sort(Comparator<T> comparator) {
    if (numOfEntries <= 1 || comparator == null) {
      return;
    }

    quickSort(0, numOfEntries - 1, comparator);
  }

  @Override
  public ListInterface<T> filter(Predicate<T> predicate) {
    ListInterface<T> filtered = new ArrayList<>();
    for (int i = 1; i <= numOfEntries; i++) {
      T entry = array[i - 1];
      if (entry != null && predicate.test(entry)) {
        filtered.add(entry);
      }
    }
    return filtered;
  }

  @Override public T find(Predicate<T> predicate) {
    if (predicate == null) return null;

    for (int i = 1; i <= numOfEntries; i++) {
      T entry = array[i - 1];
      if (entry != null && predicate.test(entry)) {
        return entry;
      }
    }
    return null;
  }

  @Override
  public <R> ListInterface<R> map(Function<T, R> mapper) {
    ListInterface<R> mappedList = new ArrayList<>();
    if (mapper == null) return mappedList;

    for (int i = 1; i <= numOfEntries; i++) {
      T entry = array[i - 1];
      if (entry != null) {
        mappedList.add(mapper.apply(entry));
      }
    }
    return mappedList;
  }

  @Override
  public <U> U reduce(U identity, BiFunction<U, T, U> accumulator) {
    U result = identity;
    if (accumulator == null) return result;

    for (int i = 1; i <= numOfEntries; i++) {
      T entry = array[i - 1];
      if (entry != null) {
        result = accumulator.apply(result, entry);
      }
    }
    return result;
  }

  @Override
  public Iterator<T> getIterator() {
    return new ArrayListIterator();
  }

  // INTERNAL HELPERS

  private int getPosition(T entry) {
    if (entry == null || isEmpty()) return -1;

    for (int i = 0; i < numOfEntries; i++) {
      if (array[i] != null && array[i].equals(entry)) {
        return i + 1; // 1-based indexing for 1-based public methods
      }
    }
    return -1;
  }

  @SuppressWarnings("unchecked")
  private void doubleCapacity() {
    int newLength = 2 * array.length;
    T[] oldList = array;

    array = (T[]) new Object[newLength];
    for (int i = 0; i < numOfEntries; i++) {
      array[i] = oldList[i];
    }
  }

  private void makeRoom(int newPosition) {
    for (int index = numOfEntries - 1; index >= newPosition - 1; index--) {
      array[index + 1] = array[index];
    }
  }

  private void removeGap(int givenPosition) {
    for (int index = givenPosition - 1; index < numOfEntries - 1; index++) {
      array[index] = array[index + 1];
    }
  }

  private void quickSort(int low, int high, Comparator<T> comparator) {
    if (low < high) {
      int pivotIndex = partition(low, high, comparator);
      quickSort(low, pivotIndex - 1, comparator);
      quickSort(pivotIndex + 1, high, comparator);
    }
  }

  private int partition(int low, int high, Comparator<T> comparator) {
    T pivot = array[high];
    int i = low - 1;

    for (int j = low; j < high; j++) {
      if (comparator.compare(array[j], pivot) <= 0) {
        i++;
        swap(i, j);
      }
    }
    swap(i + 1, high);
    return i + 1;
  }

  private void swap(int i, int j) {
    if (i == j) return;

    T temp = array[i];
    array[i] = array[j];
    array[j] = temp;
  }

  // ITERATOR IMPLEMENTATION

  private class ArrayListIterator implements Iterator<T> {
    private int currentIndex = 0;

    @Override
    public boolean hasNext() {
      return currentIndex < numOfEntries;
    }

    @Override
    public T next() {
      if (!hasNext()) return null;

      T entry = array[currentIndex];
      currentIndex++;
      return entry;
    }

    @Override
    public void remove() {}
  }
}
