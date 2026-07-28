package adt;

import java.io.Serializable;
import java.util.Iterator;

public class LinkedStack<T> implements StackInterface<T>, Serializable {
  private static final long serialVersionUID = 1L;

  private Node topNode;
  private int numberOfEntries;

  public LinkedStack() {
    clear();
  }

  @Override
  public final void clear() {
    topNode = null;
    numberOfEntries = 0;
  }

  @Override
  public boolean push(T newEntry) {
    Node newNode = new Node(newEntry);
    newNode.next = topNode;
    topNode = newNode;
    numberOfEntries++;
    return true;
  }

  @Override
  public T pop() {
    if (isEmpty()) return null;

    T result = topNode.data;
    topNode = topNode.next;
    numberOfEntries--;
    return result;
  }

  @Override
  public T peek() {
    return isEmpty() ? null : topNode.data;
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
    return false; // unbounded, grows on the heap as needed
  }

  @Override
  public Iterator<T> getIterator() {
    return new LinkedStackIterator();
  }

  // NESTED STRUCTURAL LAYER

  private class Node implements Serializable {
    private static final long serialVersionUID = 1L;

    private T data;
    private Node next;

    private Node(T data) {
      this.data = data;
      this.next = null;
    }
  }

  // ITERATOR IMPLEMENTATION (top -> bottom, most recent first)

  private class LinkedStackIterator implements Iterator<T> {
    private Node currentNode = topNode;

    @Override
    public boolean hasNext() {
      return currentNode != null;
    }

    @Override
    public T next() {
      if (!hasNext()) return null;

      T data = currentNode.data;
      currentNode = currentNode.next;
      return data;
    }
  }
}
