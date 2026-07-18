package adt;

public interface PriorityQueueInterface<T> {
  public boolean enqueue(T newEntry, int priority);

  public T dequeue();

  public T peek();

  public boolean remove(T entry);

  public T removeAt(int position);

  public boolean contains(T entry);

  public int getNumberOfEntries();

  public int getPriority(T entry);

  public boolean changePriority(T entry, int newPriority);

  public boolean isEmpty();

  public boolean isFull();

  public void clear();

  public java.util.Iterator<T> getIterator();
}
