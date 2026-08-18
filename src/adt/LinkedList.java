package adt;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class LinkedList<T> implements ListInterface<T>, Serializable {
  private static final long serialVersionUID = 1L;

  private Node firstNode;
  private int numberOfEntries;

  public LinkedList() {
    clear();
  }

  @Override
  public void clear() {
    firstNode = null;
    numberOfEntries = 0;
  }

  @Override
  public boolean add(T newEntry) {
    Node newNode = new Node(newEntry);

    if (isEmpty()) {
      firstNode = newNode;
    } else {
      Node currentNode = firstNode;
      while (currentNode.next != null) {
        currentNode = currentNode.next;
      }
      currentNode.next = newNode;
    }

    numberOfEntries++;
    return true;
  }

  @Override
  public boolean add(int newPosition, T newEntry) {
    if (newPosition >= 1 && newPosition <= numberOfEntries + 1) {
      Node newNode = new Node(newEntry);

      if (isEmpty() || newPosition == 1) {
        newNode.next = firstNode;
        firstNode = newNode;
      } else {
        Node nodeBefore = firstNode;
        for (int i = 1; i < newPosition - 1; ++i) {
          nodeBefore = nodeBefore.next;
        }
        newNode.next = nodeBefore.next;
        nodeBefore.next = newNode;
      }

      numberOfEntries++;
      return true;
    }
    return false;
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
    if (givenPosition >= 1 && givenPosition <= numberOfEntries) {
      T result = null;

      if (givenPosition == 1) {
        result = firstNode.data;
        firstNode = firstNode.next;
      } else {
        Node nodeBefore = firstNode;
        for (int i = 1; i < givenPosition - 1; ++i) {
          nodeBefore = nodeBefore.next;
        }
        result = nodeBefore.next.data;
        nodeBefore.next = nodeBefore.next.next;
      }

      numberOfEntries--;
      return result;
    }
    return null;
  }

  @Override
  public boolean replace(int givenPosition, T newEntry) {
    if (givenPosition >= 1 && givenPosition <= numberOfEntries) {
      Node currentNode = firstNode;
      for (int i = 0; i < givenPosition - 1; ++i) {
        currentNode = currentNode.next;
      }
      currentNode.data = newEntry;
      return true;
    }
    return false;
  }

  @Override
  public T getEntry(int givenPosition) {
    if (givenPosition >= 1 && givenPosition <= numberOfEntries) {
      Node currentNode = firstNode;
      for (int i = 0; i < givenPosition - 1; ++i) {
        currentNode = currentNode.next;
      }
      return currentNode.data;
    }
    return null;
  }

  @Override
  public boolean contains(T anEntry) {
    return getPosition(anEntry) != -1;
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
    return false;
  }

  @Override
  public void sort(Comparator<T> comparator) {
    if (numberOfEntries <= 1 || comparator == null) {
      return;
    }
    firstNode = mergeSort(firstNode, comparator);
  }

  @Override
  public ListInterface<T> filter(Predicate<T> predicate) {
    ListInterface<T> filtered = new LinkedList<>();
    Node currentNode = firstNode;
    while (currentNode != null) {
      T entry = currentNode.data;
      if (entry != null && predicate.test(entry)) {
        filtered.add(entry);
      }
      currentNode = currentNode.next;
    }
    return filtered;
  }

  @Override
  public T find(Predicate<T> predicate) {
    if (predicate == null) return null;

    Node currentNode = firstNode;
    while (currentNode != null) {
      T entry = currentNode.data;
      if (entry != null && predicate.test(entry)) {
        return entry;
      }
      currentNode = currentNode.next;
    }
    return null;
  }

  @Override
  public <R> ListInterface<R> map(Function<T, R> mapper) {
    ListInterface<R> mappedList = new LinkedList<>();
    if (mapper == null) return mappedList;

    Node currentNode = firstNode;
    while (currentNode != null) {
      T entry = currentNode.data;
      if (entry != null) {
        mappedList.add(mapper.apply(entry));
      }
      currentNode = currentNode.next;
    }
    return mappedList;
  }

  @Override
  public <U> U reduce(U identity, BiFunction<U, T, U> accumulator) {
    U result = identity;
    if (accumulator == null) return result;

    Node currentNode = firstNode;
    while (currentNode != null) {
      T entry = currentNode.data;
      if (entry != null) {
        result = accumulator.apply(result, entry);
      }
      currentNode = currentNode.next;
    }
    return result;
  }

  @Override
  public ListInterface<T> slice(int startPosition, int endPosition) {
    ListInterface<T> slicedList = new LinkedList<>();
    if (startPosition < 1 || startPosition > numberOfEntries || startPosition > endPosition) {
      return slicedList;
    }

    int actualEnd = Math.min(endPosition, numberOfEntries);
    Node currentNode = firstNode;
    int currentPos = 1;

    while (currentNode != null && currentPos <= actualEnd) {
      if (currentPos >= startPosition) {
        slicedList.add(currentNode.data);
      }
      currentNode = currentNode.next;
      currentPos++;
    }

    return slicedList;
  }

  @Override
  public Iterator<T> getIterator() {
    return new LinkedListIterator();
  }

  @Override
  public String toString() {
    StringBuilder output = new StringBuilder();
    Node currentNode = firstNode;
    while (currentNode != null) {
      output.append(currentNode.data).append("\n");
      currentNode = currentNode.next;
    }
    return output.toString();
  }

  private int getPosition(T entry) {
    if (entry == null || isEmpty()) return -1;

    Node currentNode = firstNode;
    for (int i = 1; i <= numberOfEntries; i++) {
      if (currentNode != null && currentNode.data != null && currentNode.data.equals(entry)) {
        return i; // 1-based position indexing
      }
      if (currentNode != null) {
        currentNode = currentNode.next;
      }
    }
    return -1;
  }

  private Node mergeSort(Node head, Comparator<T> comparator) {
    if (head == null || head.next == null) {
      return head;
    }

    Node middle = getMiddle(head);
    Node nextOfMiddle = middle.next;
    middle.next = null;

    Node left = mergeSort(head, comparator);
    Node right = mergeSort(nextOfMiddle, comparator);

    return sortedMerge(left, right, comparator);
  }

  private Node sortedMerge(Node a, Node b, Comparator<T> comparator) {
    if (a == null) return b;
    if (b == null) return a;

    Node result;
    if (comparator.compare(a.data, b.data) <= 0) {
      result = a;
      result.next = sortedMerge(a.next, b, comparator);
    } else {
      result = b;
      result.next = sortedMerge(a, b.next, comparator);
    }
    return result;
  }

  private Node getMiddle(Node head) {
    if (head == null) return head;
    Node slow = head;
    Node fast = head;

    while (fast.next != null && fast.next.next != null) {
      slow = slow.next;
      fast = fast.next.next;
    }
    return slow;
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

  private class LinkedListIterator implements Iterator<T> {
    private Node currentNode = firstNode;

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
