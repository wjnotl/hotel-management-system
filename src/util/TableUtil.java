package util;

public class TableUtil {

  // Allignment Enums
  public enum Align {
    LEFT,
    CENTER,
    RIGHT
  }

  public enum VAlign {
    TOP,
    CENTER,
    BOTTOM
  }

  // Overflow Mode Enums
  private enum OverflowMode {
    WRAP,
    TRUNCATE,
    TRUNCATE_AT
  }

  // Table Settings Configuration
  public static class TableSettings {
    private final int[] colWidths;
    private final Align[] hAligns;
    private final VAlign[] vAligns;
    private final OverflowMode[] overflowModes;
    private final int[] customOverflowLimits;

    public TableSettings(int[] colWidths) {
      this.colWidths = colWidths;
      this.hAligns = new Align[colWidths.length];
      this.vAligns = new VAlign[colWidths.length];
      this.overflowModes = new OverflowMode[colWidths.length];
      this.customOverflowLimits = new int[colWidths.length];

      // Default style = left, top; overflow = wrap; customLimit = colWidth
      for (int i = 0; i < colWidths.length; i++) {
        hAligns[i] = Align.LEFT;
        vAligns[i] = VAlign.TOP;
        overflowModes[i] = OverflowMode.WRAP;
        customOverflowLimits[i] = colWidths[i];
      }
    }

    // Wrap Setter
    public TableSettings setWrap(int colIndex) {
      if (isValidIndex(colIndex)) {
        this.overflowModes[colIndex] = OverflowMode.WRAP;
        this.customOverflowLimits[colIndex] = this.colWidths[colIndex];
      }
      return this;
    }

    // Truncate Setter
    public TableSettings setTruncate(int colIndex) {
      if (isValidIndex(colIndex)) {
        this.overflowModes[colIndex] = OverflowMode.TRUNCATE;
        this.customOverflowLimits[colIndex] = this.colWidths[colIndex];
      }
      return this;
    }

    // TruncateAt Setter - Strictly forces the character limit parameter
    public TableSettings setTruncateAt(int colIndex, int customCharLimit) {
      if (isValidIndex(colIndex)) {
        if (customCharLimit <= 0) {
          throw new IllegalArgumentException(
              "System Error: Custom character limit must be greater than 0!");
        }
        this.overflowModes[colIndex] = OverflowMode.TRUNCATE_AT;
        this.customOverflowLimits[colIndex] = customCharLimit;
      }
      return this;
    }

    public TableSettings setHAlign(int colIndex, Align alignment) {
      if (isValidIndex(colIndex)) this.hAligns[colIndex] = alignment;
      return this;
    }

    public TableSettings setVAlign(int colIndex, VAlign alignment) {
      if (isValidIndex(colIndex)) this.vAligns[colIndex] = alignment;
      return this;
    }

    private boolean isValidIndex(int index) {
      return index >= 0 && index < colWidths.length;
    }
  }

  public enum BorderPosition {
    TOP,
    MIDDLE,
    BOTTOM
  }

  public static void printTableBorder(TableSettings settings, BorderPosition position) {
    String[] borders = new String[3];

    switch (position) {
      case TOP:
        borders = new String[] {"╔", "╦", "╗"};
        break;
      case MIDDLE:
        borders = new String[] {"╠", "╬", "╣"};
        break;
      case BOTTOM:
        borders = new String[] {"╚", "╩", "╝"};
        break;
      default:
        throw new IllegalArgumentException("System Error: Invalid border position specified!");
    }

    System.out.print(borders[0]); // Left border
    for (int i = 0; i < settings.colWidths.length; i++) {
      System.out.print("═".repeat(settings.colWidths[i]));
      if (i < settings.colWidths.length - 1) {
        System.out.print(borders[1]); // Middle border
      } else {
        System.out.print(borders[2]); // Right border
      }
    }
    System.out.println();
  }

  public static void printTableRow(String[] columns, TableSettings settings) {
    String[][] processedCellLines = new String[columns.length][];
    int maxLinesRequired = 0;

    // Pre-process text transformations per column configuration
    for (int i = 0; i < columns.length; i++) {
      // null safety check
      String text = (columns[i] == null) ? "" : columns[i];

      OverflowMode mode = settings.overflowModes[i];
      int width = settings.colWidths[i];
      int customLimit = settings.customOverflowLimits[i];

      String[] lines;
      if (mode == OverflowMode.TRUNCATE) {
        lines = new String[] {TextUtil.truncate(text, width)};
      } else if (mode == OverflowMode.TRUNCATE_AT) {
        lines = new String[] {TextUtil.truncate(text, customLimit)};
      } else {
        lines = TextUtil.wrapText(text, width);
      }

      processedCellLines[i] = lines;

      // find the cell with the most lines
      if (lines.length > maxLinesRequired) {
        maxLinesRequired = lines.length;
      }
    }

    // Render
    for (int lineIndex = 0; lineIndex < maxLinesRequired; lineIndex++) {
      for (int colIndex = 0; colIndex < columns.length; colIndex++) {
        System.out.print("║");

        String[] colLines = processedCellLines[colIndex];
        int totalLinesInCell = colLines.length;

        // Subtract 2 from the width to reserve room for 1 space on each side
        int totalWidth = settings.colWidths[colIndex];
        int printableWidth = totalWidth - 2;

        Align hAlign = settings.hAligns[colIndex];
        VAlign vAlign = settings.vAligns[colIndex];

        String cellText = "";

        // Vertical Align Logic
        int totalBlankLines = maxLinesRequired - totalLinesInCell;
        if (vAlign == VAlign.BOTTOM) {
          if (lineIndex >= totalBlankLines) {
            cellText = colLines[lineIndex - totalBlankLines];
          }
        } else if (vAlign == VAlign.CENTER) {
          int blankLinesBefore = totalBlankLines / 2;
          int blankLinesAfter = totalBlankLines - blankLinesBefore;
          if (lineIndex >= blankLinesBefore && lineIndex < (maxLinesRequired - blankLinesAfter)) {
            cellText = colLines[lineIndex - blankLinesBefore];
          }
        } else {
          if (lineIndex < totalLinesInCell) {
            cellText = colLines[lineIndex];
          }
        }

        // If the text is wider than new printable area, clamp it to fit
        if (cellText.length() > printableWidth) {
          cellText = cellText.substring(0, Math.max(0, printableWidth));
        }

        // Print the left padding space buffer
        System.out.print(" ");

        // Horizontal Align Logic
        if (hAlign == Align.RIGHT) {
          System.out.format("%" + printableWidth + "s", cellText);
        } else if (hAlign == Align.CENTER) {
          int totalPadding = printableWidth - cellText.length();
          int leftPadding = totalPadding / 2;
          int rightPadding = totalPadding - leftPadding;
          System.out.print(" ".repeat(leftPadding) + cellText + " ".repeat(rightPadding));
        } else {
          System.out.format("%-" + printableWidth + "s", cellText);
        }

        // Print the right padding space buffer
        System.out.print(" ");
      }
      System.out.println("║");
    }
  }
}
