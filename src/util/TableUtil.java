package util;

import adt.LinkedList;
import adt.ListInterface;

public class TableUtil {

  // Alignment Enums
  public static enum Align {
    LEFT,
    CENTER,
    RIGHT
  }

  public static enum VAlign {
    TOP,
    CENTER,
    BOTTOM
  }

  // Overflow Mode Enums
  private static enum OverflowMode {
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

    public TableSettings setWrap(int colIndex) {
      if (isValidIndex(colIndex)) {
        this.overflowModes[colIndex] = OverflowMode.WRAP;
        this.customOverflowLimits[colIndex] = this.colWidths[colIndex];
      }
      return this;
    }

    public TableSettings setTruncate(int colIndex) {
      if (isValidIndex(colIndex)) {
        this.overflowModes[colIndex] = OverflowMode.TRUNCATE;
        this.customOverflowLimits[colIndex] = this.colWidths[colIndex];
      }
      return this;
    }

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

  // Border Position Enums
  public static enum BorderPosition {
    TOP, // ╔ ╦ ╗ - Top row border
    MIDDLE, // ╠ ╬ ╣ - Standard row separator
    BOTTOM, // ╚ ╩ ╝ - Bottom table border with column junctions
    HEADER_CLOSE, // ╠ ╩ ╣ - Caps multi-column headers when transitioning into a merged row
    SPAN_OPEN, // ╠ ╦ ╣ - Opens multi-column rows from a merged section
    PLAIN_ROW, // ╠ ═ ╣ - Clean horizontal divider across a single spanned row
    PLAIN_BOTTOM // ╚ ═ ╝ - Clean bottom frame for a spanned row without column junctions
  }

  public static void printTableBorder(TableSettings settings, BorderPosition position) {
    String[] borders;

    switch (position) {
      case TOP:
        borders =
            ConsoleUtil.isRunningInIDE()
                ? new String[] {"+", "+", "+"}
                : new String[] {"╔", "╦", "╗"};
        break;
      case MIDDLE:
        borders =
            ConsoleUtil.isRunningInIDE()
                ? new String[] {"+", "+", "+"}
                : new String[] {"╠", "╬", "╣"};
        break;
      case BOTTOM:
        borders =
            ConsoleUtil.isRunningInIDE()
                ? new String[] {"+", "+", "+"}
                : new String[] {"╚", "╩", "╝"};
        break;
      case HEADER_CLOSE:
        borders =
            ConsoleUtil.isRunningInIDE()
                ? new String[] {"+", "+", "+"}
                : new String[] {"╠", "╩", "╣"};
        break;
      case SPAN_OPEN:
        borders =
            ConsoleUtil.isRunningInIDE()
                ? new String[] {"+", "+", "+"}
                : new String[] {"╠", "╦", "╣"};
        break;
      case PLAIN_ROW:
        borders =
            ConsoleUtil.isRunningInIDE()
                ? new String[] {"+", "-", "+"}
                : new String[] {"╠", "═", "╣"};
        break;
      case PLAIN_BOTTOM:
        borders =
            ConsoleUtil.isRunningInIDE()
                ? new String[] {"+", "-", "+"}
                : new String[] {"╚", "═", "╝"};
        break;
      default:
        throw new IllegalArgumentException("System Error: Invalid border position specified!");
    }

    System.out.print(borders[0]); // Left border
    for (int i = 0; i < settings.colWidths.length; i++) {
      int cellBorderWidth = settings.colWidths[i] + 2; // Add 2 spaces padding (1 left + 1 right)
      System.out.print((ConsoleUtil.isRunningInIDE() ? "-" : "═").repeat(cellBorderWidth));

      if (i < settings.colWidths.length - 1) {
        System.out.print(borders[1]); // Middle junction
      } else {
        System.out.print(borders[2]); // Right border
      }
    }
    System.out.println();
  }

  @SuppressWarnings("unchecked")
  public static void printTableRow(String[] columns, TableSettings settings) {
    ListInterface<String>[] processedCellLines = new ListInterface[columns.length];
    int maxLinesRequired = 0;

    for (int i = 0; i < columns.length; i++) {
      String text = (columns[i] == null) ? "" : columns[i];

      OverflowMode mode = settings.overflowModes[i];
      int printableWidth = settings.colWidths[i];
      int customLimit = settings.customOverflowLimits[i];

      ListInterface<String> lines;
      if (mode == OverflowMode.TRUNCATE) {
        lines = new LinkedList<>();
        lines.add(TextUtil.truncate(text, printableWidth));
      } else if (mode == OverflowMode.TRUNCATE_AT) {
        lines = new LinkedList<>();
        lines.add(TextUtil.truncate(text, Math.min(customLimit, printableWidth)));
      } else {
        lines = TextUtil.wrapText(text, printableWidth);
      }

      processedCellLines[i] = lines;

      if (lines.getNumberOfEntries() > maxLinesRequired) {
        maxLinesRequired = lines.getNumberOfEntries();
      }
    }

    for (int lineIndex = 0; lineIndex < maxLinesRequired; lineIndex++) {
      for (int colIndex = 0; colIndex < columns.length; colIndex++) {
        System.out.print(ConsoleUtil.isRunningInIDE() ? "|" : "║");

        ListInterface<String> colLines = processedCellLines[colIndex];
        int totalLinesInCell = (colLines != null) ? colLines.getNumberOfEntries() : 0;

        int printableWidth = settings.colWidths[colIndex];

        Align hAlign = settings.hAligns[colIndex];
        VAlign vAlign = settings.vAligns[colIndex];

        String cellText = "";

        int totalBlankLines = maxLinesRequired - totalLinesInCell;
        if (vAlign == VAlign.BOTTOM) {
          if (lineIndex >= totalBlankLines && colLines != null) {
            cellText = colLines.getEntry(lineIndex - totalBlankLines + 1);
          }
        } else if (vAlign == VAlign.CENTER) {
          int blankLinesBefore = totalBlankLines / 2;
          int blankLinesAfter = totalBlankLines - blankLinesBefore;
          if (lineIndex >= blankLinesBefore
              && lineIndex < (maxLinesRequired - blankLinesAfter)
              && colLines != null) {
            cellText = colLines.getEntry(lineIndex - blankLinesBefore + 1);
          }
        } else {
          if (lineIndex < totalLinesInCell && colLines != null) {
            cellText = colLines.getEntry(lineIndex + 1);
          }
        }

        if (cellText == null) {
          cellText = "";
        }

        if (cellText.length() > printableWidth) {
          cellText = cellText.substring(0, Math.max(0, printableWidth));
        }

        System.out.print(" ");

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

        System.out.print(" ");
      }
      System.out.println(ConsoleUtil.isRunningInIDE() ? "|" : "║");
    }
  }
}
