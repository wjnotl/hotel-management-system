package adt;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class LinkedList<T> implements ListInterface<T> {

  private Node firstNode;
  private int numberOfEntries;

  public LinkedList() {
    clear();
  }

  @Override
  public final void clear() {
    firstNode = null;
    numberOfEntries = 0;
  }

  @Override
  public boolean add(T newEntry) {
    Node newNode = new Node(newEntry);

    if (isEmpty()) {
      firstNode = newNode;
    } else {
      // Walk all the way down to the tail end node
      Node currentNode = firstNode;
      while (currentNode.next != null) {
        currentNode = currentNode.next;
      }
      currentNode.next = newNode; // Glue new node to the tail end
    }

    numberOfEntries++;
    return true;
  }

  @Override
  public boolean add(int newPosition, T newEntry) {
    if (newPosition >= 1 && newPosition <= numberOfEntries + 1) {
      Node newNode = new Node(newEntry);

      if (isEmpty() || newPosition == 1) {
        // Swap node references at the very head of the list
        newNode.next = firstNode;
        firstNode = newNode;
      } else {
        // Stop right before the insert position index
        Node nodeBefore = firstNode;
        for (int i = 1; i < newPosition - 1; ++i) {
          nodeBefore = nodeBefore.next;
        }
        // Splice new node seamlessly between nodeBefore and the adjacent neighbor
        newNode.next = nodeBefore.next;
        nodeBefore.next = newNode;
      }

      numberOfEntries++;
      return true;
    }
    return false;
  }

  @Override
  public T remove(int givenPosition) {
    if (givenPosition >= 1 && givenPosition <= numberOfEntries) {
      T result = null;

      if (givenPosition == 1) {
        result = firstNode.data;
        firstNode = firstNode.next; // Snip head element out of the chain
      } else {
        // Stop right before the deletion target node
        Node nodeBefore = firstNode;
        for (int i = 1; i < givenPosition - 1; ++i) {
          nodeBefore = nodeBefore.next;
        }
        result = nodeBefore.next.data;
        // Skip over the target node to disconnect it from the list chain
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
      currentNode.data = newEntry; // Update payload reference directly
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
    Node currentNode = firstNode;
    while (currentNode != null) {
      if (anEntry.equals(currentNode.data)) {
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
    // Dynamic lists allocate references as needed; effectively never capped
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

  // INTERNAL HELPERS

  private Node mergeSort(Node head, Comparator<T> comparator) {
    if (head == null || head.next == null) {
      return head;
    }

    // 1. Split list in half using slow/fast pointers
    Node middle = getMiddle(head);
    Node nextOfMiddle = middle.next;
    middle.next = null;

    // 2. Recursively sort both halves
    Node left = mergeSort(head, comparator);
    Node right = mergeSort(nextOfMiddle, comparator);

    // 3. Merge sorted halves back together
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

  // NESTED STRUCTURAL LAYER

  private class Node {
    private T data;
    private Node next;

    private Node(T data) {
      this.data = data;
      this.next = null;
    }

    private Node(T data, Node next) {
      this.data = data;
      this.next = next;
    }
  }

  // ITERATOR IMPLEMENTATION

  private class LinkedListIterator implements Iterator<T> {
    private Node currentNode = firstNode;

    @Override
    public boolean hasNext() {
      return currentNode != null;
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      T data = currentNode.data;
      currentNode = currentNode.next; // Advance to the next chained node
      return data;
    }
  }
}
