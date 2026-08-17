package util;

import adt.LinkedList;
import adt.ListInterface;

public class TextUtil {

  // Truncates text to ... if exceed limit
  public static String truncate(String text, int limit) {
    if (text == null) return "";

    if (text.length() > limit) {
      return (limit > 3) ? text.substring(0, limit - 3) + "..." : text.substring(0, limit);
    }

    return text;
  }

  public static ListInterface<String> wrapText(String text, int width) {
    ListInterface<String> linesList = new LinkedList<>();

    if (text == null || text.isEmpty()) {
      linesList.add("");
      return linesList;
    }

    // If it is a long word with no spaces, chop it
    if (!text.contains(" ") && text.length() > width) {
      return chopWord(text, width);
    }

    // If it is a normal sentence, process word-by-word
    String[] words = text.split(" ");
    StringBuilder currentLine = new StringBuilder();

    for (String word : words) {
      int spaceCost = (currentLine.length() > 0) ? 1 : 0;

      // If adding this word exceeds column width boundary
      if (currentLine.length() + word.length() + spaceCost > width) {
        if (currentLine.length() > 0) {
          linesList.add(currentLine.toString());
          currentLine.setLength(0); // Reset buffer
        }

        // If a single word is wider than the column, chop it up into linesList
        if (word.length() > width) {
          ListInterface<String> choppedWords = chopWord(word, width);
          for (int i = 1; i <= choppedWords.getNumberOfEntries(); i++) {
            linesList.add(choppedWords.getEntry(i));
          }
          continue;
        }
      }

      // Add word to active line buffer
      if (currentLine.length() > 0) {
        currentLine.append(" ");
      }
      currentLine.append(word);
    }

    // Append remaining text left over
    if (currentLine.length() > 0) {
      linesList.add(currentLine.toString());
    }

    return linesList;
  }

  private static ListInterface<String> chopWord(String word, int width) {
    ListInterface<String> choppedList = new LinkedList<>();
    while (word.length() > width) {
      choppedList.add(word.substring(0, width));
      word = word.substring(width);
    }
    if (!word.isEmpty()) {
      choppedList.add(word); // Remaining characters
    }
    return choppedList;
  }
}
