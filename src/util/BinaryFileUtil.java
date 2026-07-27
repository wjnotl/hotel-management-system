package util;

import java.io.*;

public class BinaryFileUtil<T> {
  private final String fileName;

  public BinaryFileUtil(String fileName) {
    this.fileName = fileName;
    ensureFileExists();
  }

  private void ensureFileExists() {
    File file = new File(fileName);
    if (!file.exists()) {
      try {
        file.createNewFile();
      } catch (IOException ex) {
        ConsoleUtil.printError("Error creating file " + fileName + ": " + ex.getMessage());
      }
    }
  }

  public void saveToFile(T data) {
    try (ObjectOutputStream ooStream = new ObjectOutputStream(new FileOutputStream(fileName))) {
      ooStream.writeObject(data);
    } catch (FileNotFoundException ex) {
      ConsoleUtil.printError("File " + fileName + " not found: " + ex.getMessage());
    } catch (IOException ex) {
      ConsoleUtil.printError("Error saving to " + fileName + ": " + ex.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  public T retrieveFromFile() {
    File file = new File(fileName);

    // If file doesn't exist or is empty (0 bytes), return null quietly
    if (!file.exists() || file.length() == 0) {
      return null;
    }

    try (ObjectInputStream oiStream = new ObjectInputStream(new FileInputStream(file))) {
      return (T) oiStream.readObject();
    } catch (FileNotFoundException ex) {
      ConsoleUtil.printError("File " + fileName + " not found");
    } catch (EOFException ex) {
      // Reached end of file cleanly / empty file format
      return null;
    } catch (IOException ex) {
      ConsoleUtil.printError("Error reading from " + fileName + ": " + ex.getMessage());
    } catch (ClassNotFoundException ex) {
      ConsoleUtil.printError("Class not found: " + ex.getMessage());
    }

    return null;
  }
}
