package adt;

import java.io.Serializable;
import java.util.Iterator;

public class LinkedQueue<T> implements QueueInterface<T>, Serializable {
  private static final long serialVersionUID = 1L;

  private Node headNode;
  private Node tailNode;
  private int numberOfEntries;

  public LinkedQueue() {
    clear();
  }

  @Override
  public final void clear() {
    headNode = null;
    tailNode = null;
    numberOfEntries = 0;
  }

  @Override
  public boolean enqueue(T newEntry) {
    Node newNode = new Node(newEntry);

    if (isEmpty()) {
      headNode = newNode;
      tailNode = newNode;
    } else {
      tailNode.next = newNode;
      tailNode = newNode;
    }

    numberOfEntries++;
    return true;
  }

  @Override
  public T dequeue() {
    if (isEmpty()) return null;

    T result = headNode.data;
    headNode = headNode.next;

    if (headNode == null) {
      tailNode = null; // queue is now empty
    }

    numberOfEntries--;
    return result;
  }

  @Override
  public T peek() {
    return isEmpty() ? null : headNode.data;
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
  public int getPosition(T entry) {
    if (entry == null || isEmpty()) return -1;

    Node currentNode = headNode;
    int position = 1;

    while (currentNode != null) {
      if (currentNode.data != null && currentNode.data.equals(entry)) {
        return position;
      }
      currentNode = currentNode.next;
      position++;
    }
    return -1;
  }

  @Override
  public boolean remove(T entry) {
    if (entry == null || isEmpty()) return false;

    Node previousNode = null;
    Node currentNode = headNode;

    while (currentNode != null) {
      if (currentNode.data != null && currentNode.data.equals(entry)) {
        if (previousNode == null) {
          headNode = currentNode.next;
        } else {
          previousNode.next = currentNode.next;
        }

        if (currentNode == tailNode) {
          tailNode = previousNode;
        }

        currentNode.next = null;
        numberOfEntries--;
        return true;
      }

      previousNode = currentNode;
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
    return new LinkedQueueIterator();
  }

  private class Node implements Serializable {
    private static final long serialVersionUID = 1L;

    private T data;
    private Node next;

    private Node(T data) {
      this.data = data;
      this.next = null;
    }
  }

  private class LinkedQueueIterator implements Iterator<T> {
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
