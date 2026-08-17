package adt;

import java.io.Serializable;
import java.util.Iterator;

public class LinkedDeque<T> implements DequeInterface<T>, Serializable {
  private static final long serialVersionUID = 1L;

  private Node headNode;
  private Node tailNode;
  private int numberOfEntries;

  public LinkedDeque() {
    clear();
  }

  @Override
  public final void clear() {
    headNode = null;
    tailNode = null;
    numberOfEntries = 0;
  }

  @Override
  public boolean addFirst(T newEntry) {
    Node newNode = new Node(newEntry);

    if (isEmpty()) {
      headNode = newNode;
      tailNode = newNode;
    } else {
      newNode.next = headNode;
      headNode.prev = newNode;
      headNode = newNode;
    }

    numberOfEntries++;
    return true;
  }

  @Override
  public boolean addLast(T newEntry) {
    Node newNode = new Node(newEntry);

    if (isEmpty()) {
      headNode = newNode;
      tailNode = newNode;
    } else {
      newNode.prev = tailNode;
      tailNode.next = newNode;
      tailNode = newNode;
    }

    numberOfEntries++;
    return true;
  }

  @Override
  public T removeFirst() {
    if (isEmpty()) return null;

    T result = headNode.data;
    headNode = headNode.next;

    if (headNode == null) {
      tailNode = null; // deque is now empty
    } else {
      headNode.prev = null;
    }

    numberOfEntries--;
    return result;
  }

  @Override
  public T removeLast() {
    if (isEmpty()) return null;

    T result = tailNode.data;
    tailNode = tailNode.prev;

    if (tailNode == null) {
      headNode = null; // deque is now empty
    } else {
      tailNode.next = null;
    }

    numberOfEntries--;
    return result;
  }

  @Override
  public T peekFirst() {
    return isEmpty() ? null : headNode.data;
  }

  @Override
  public T peekLast() {
    return isEmpty() ? null : tailNode.data;
  }

  @Override
  public boolean remove(T entry) {
    if (entry == null || isEmpty()) return false;

    Node currentNode = headNode;
    while (currentNode != null) {
      if (currentNode.data != null && currentNode.data.equals(entry)) {
        unlink(currentNode);
        return true;
      }
      currentNode = currentNode.next;
    }
    return false;
  }

  @Override
  public boolean contains(T entry) {
    if (entry == null || isEmpty()) return false;

    Node currentNode = headNode;
    while (currentNode != null) {
      if (currentNode.data != null && currentNode.data.equals(entry)) {
        return true;
      }
      currentNode = currentNode.next;
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
    return false; // unbounded, grows on the heap as needed
  }

  @Override
  public Iterator<T> getIterator() {
    return new LinkedDequeIterator();
  }

  private void unlink(Node targetNode) {
    Node before = targetNode.prev;
    Node after = targetNode.next;

    if (before == null) {
      headNode = after; // target was the head
    } else {
      before.next = after;
    }

    if (after == null) {
      tailNode = before; // target was the tail
    } else {
      after.prev = before;
    }

    numberOfEntries--;
  }

  private class Node implements Serializable {
    private static final long serialVersionUID = 1L;

    private T data;
    private Node prev;
    private Node next;

    private Node(T data) {
      this.data = data;
      this.prev = null;
      this.next = null;
    }
  }

  private class LinkedDequeIterator implements Iterator<T> {
    private Node currentNode = headNode;

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
