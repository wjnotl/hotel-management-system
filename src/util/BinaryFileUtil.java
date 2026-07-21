package util;

import java.io.*;

public class BinaryFileUtil<T> {

  public void saveToFile(T data, String fileName) {
    try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName))) {
      out.writeObject(data);
    } catch (IOException ex) {
      System.err.println("Error writing to " + fileName + ": " + ex.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  public T retrieveFromFile(String fileName) {
    File file = new File(fileName);
    if (!file.exists()) return null;

    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
      return (T) in.readObject();
    } catch (Exception ex) {
      System.err.println("Error reading from " + fileName + ": " + ex.getMessage());
      return null;
    }
  }
}
