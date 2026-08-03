package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TxtExportUtil {

  private static final String EXPORT_ROOT_DIR = "exports";
  private static final String COLUMN_GAP = "   ";
  private static final DateTimeFormatter TIMESTAMP_FMT =
      DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

  private TxtExportUtil() {}

  /**
   * Writes the given headers + rows to a new .txt file under exports/{moduleName}/, with each
   * column padded to the widest value so the table lines up when opened in Notepad or any plain
   * text viewer.
   *
   * @param moduleName the module this report belongs to, e.g. "frontdesk", "vip", "housekeeping",
   *     "booking" — used as the subfolder name so each module's exports stay separate
   * @param baseFileName file name prefix (no extension), e.g. "checkout_report"
   * @param headers column headers, written as the first line
   * @param rows table body; each element is one row and must match headers.length
   * @return the path of the file that was written
   */
  public static String export(
      String moduleName, String baseFileName, String[] headers, String[][] rows) {
    String safeModuleName =
        (moduleName == null || moduleName.trim().isEmpty())
            ? "general"
            : moduleName.trim().toLowerCase().replaceAll("[^a-zA-Z0-9_-]", "_");

    File dir = new File(EXPORT_ROOT_DIR, safeModuleName);
    if (!dir.exists() && !dir.mkdirs()) {
      throw new RuntimeException("Could not create export directory: " + dir.getAbsolutePath());
    }

    String safeBaseName =
        (baseFileName == null || baseFileName.trim().isEmpty())
            ? "report"
            : baseFileName.trim().replaceAll("[^a-zA-Z0-9_-]", "_");

    String fileName = safeBaseName + "_" + LocalDateTime.now().format(TIMESTAMP_FMT) + ".txt";
    File file = new File(dir, fileName);

    int columnCount = (headers != null) ? headers.length : 0;
    int[] widths = computeColumnWidths(headers, rows, columnCount);

    try (FileWriter writer = new FileWriter(file)) {
      if (headers != null) {
        writer.write(formatRow(headers, widths));
        writer.write(separatorLine(widths));
      }
      if (rows != null) {
        for (String[] row : rows) {
          writer.write(formatRow(row, widths));
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to export TXT file: " + e.getMessage(), e);
    }

    return file.getPath();
  }

  private static int[] computeColumnWidths(String[] headers, String[][] rows, int columnCount) {
    int[] widths = new int[columnCount];

    if (headers != null) {
      for (int c = 0; c < columnCount; c++) {
        widths[c] = lengthOf(headers[c]);
      }
    }

    if (rows != null) {
      for (String[] row : rows) {
        if (row == null) continue;
        for (int c = 0; c < columnCount && c < row.length; c++) {
          widths[c] = Math.max(widths[c], lengthOf(row[c]));
        }
      }
    }

    return widths;
  }

  private static String formatRow(String[] fields, int[] widths) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < widths.length; i++) {
      String value = (fields != null && i < fields.length && fields[i] != null) ? fields[i] : "";
      boolean isLastColumn = (i == widths.length - 1);
      sb.append(isLastColumn ? value : pad(value, widths[i]));
      if (!isLastColumn) sb.append(COLUMN_GAP);
    }
    sb.append(System.lineSeparator());
    return sb.toString();
  }

  private static String separatorLine(int[] widths) {
    int total = 0;
    for (int w : widths) total += w;
    total += COLUMN_GAP.length() * Math.max(0, widths.length - 1);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < total; i++) sb.append('-');
    sb.append(System.lineSeparator());
    return sb.toString();
  }

  private static String pad(String value, int width) {
    StringBuilder sb = new StringBuilder(value);
    while (sb.length() < width) sb.append(' ');
    return sb.toString();
  }

  private static int lengthOf(String value) {
    return (value == null) ? 0 : value.length();
  }
}
