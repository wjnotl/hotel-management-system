package adt;

public interface PriorityQueueInterface<T extends Comparable<T>> extends QueueInterface<T> {
  // Forces a removal at an exact 1-based heap position index.
  public T removeAt(int position);

  // Re-evaluates an entry's priority score/state on the fly and re-balances the heap tree.
  public boolean updatePriority(T entry);
}
