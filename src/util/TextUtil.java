package util;

public class TextUtil {
  // Truncates text with an ellipsis if it exceeds the limit
  public static String truncate(String text, int limit) {
    if (text == null) return "";

    if (text.length() > limit) {
      return (limit > 3) ? text.substring(0, limit - 3) + "..." : text.substring(0, limit);
    }

    return text;
  }

  public static String[] wrapText(String text, int width) {
    // Safe check for null or empty strings
    if (text == null || text.isEmpty()) {
      return new String[] {""};
    }

    // If it is long word with no spaces, slice it directly
    if (!text.contains(" ") && text.length() > width) {
      return chopWordWithNewlines(text, width).split("\n");
    }

    // If it is a normal sentence
    String[] words = text.split(" ");

    String outputString = ""; // will be split (using \n) into an array at the end
    String currentString = ""; // temp string that will be added to the output string

    for (String word : words) {
      int spaceCost = (currentString.length() > 0) ? 1 : 0;

      // If adding this word > column width boundary
      if (currentString.length() + word.length() + spaceCost > width) {

        // Save the completed line to output string
        if (currentString.length() > 0) {
          outputString += currentString + "\n";
          currentString = ""; // Clear for the next line
        }

        // If a single word is wider than the column, chop it up
        if (word.length() > width) {
          outputString += chopWordWithNewlines(word, width) + "\n";
          continue;
        }
      }

      // Put the word onto the active line string
      if (currentString.length() > 0) {
        currentString += " ";
      }
      currentString += word;
    }

    // Grab any trailing text left over
    if (currentString.length() > 0) {
      outputString += currentString;
    }

    // split to return array
    return outputString.split("\n");
  }

  private static String chopWordWithNewlines(String word, int width) {
    String chopped = "";
    while (word.length() > width) {
      chopped += word.substring(0, width) + "\n";
      word = word.substring(width);
    }
    chopped += word; // Add those remaining characters
    return chopped;
  }
}
