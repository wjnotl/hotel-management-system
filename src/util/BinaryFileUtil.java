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
    try {
      ObjectOutputStream ooStream = new ObjectOutputStream(new FileOutputStream(fileName));
      ooStream.writeObject(data);
      ooStream.close();
    } catch (FileNotFoundException ex) {
      ConsoleUtil.printError("File " + fileName + " not found: " + ex.getMessage());
    } catch (IOException ex) {
      ConsoleUtil.printError("Error saving to " + fileName + ": " + ex.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  public T retrieveFromFile() {
    File file = new File(fileName);

    try {
      ObjectInputStream oiStream = new ObjectInputStream(new FileInputStream(file));
      T result = (T) (oiStream.readObject());
      oiStream.close();
      return result;
    } catch (FileNotFoundException ex) {
      ConsoleUtil.printError("File " + fileName + " not found");
    } catch (IOException ex) {
      ConsoleUtil.printError("Error reading from " + fileName + ": " + ex.getMessage());
    } catch (ClassNotFoundException ex) {
      ConsoleUtil.printError("Class not found: " + ex.getMessage());
    }

    return null;
  }
}
