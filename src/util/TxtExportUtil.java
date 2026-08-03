package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Thin, format-agnostic helper for saving a report's text to a .txt file under "exports/". Each
 * module builds its own report content however it wants (aligned table, plain lines, whatever) and
 * just hands the finished string over here to be written to disk.
 *
 * <p>To keep different modules' exports from colliding, prefix fileName with a module folder, e.g.
 * export("frontdesk/checkout_report", content) writes to
 * exports/frontdesk/checkout_report_<timestamp>.txt
 */
public class TxtExportUtil {

  private static final String EXPORT_ROOT_DIR = "exports";
  private static final DateTimeFormatter TIMESTAMP_FMT =
      DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

  /**
   * Writes content to a new timestamped .txt file under exports/.
   *
   * @param fileName file name (no extension needed). May include a subfolder prefix, e.g.
   *     "frontdesk/checkout_report", so each module's exports stay in their own folder.
   * @param content the full text to write, already formatted however the caller wants
   * @return the path of the file that was written
   */
  public static String export(String fileName, String content) {
    String name = (fileName == null || fileName.trim().isEmpty()) ? "report" : fileName.trim();
    name = name.replace('\\', '/');

    int lastSlash = name.lastIndexOf('/');
    String subDir = (lastSlash >= 0) ? name.substring(0, lastSlash) : "";
    String baseName = (lastSlash >= 0) ? name.substring(lastSlash + 1) : name;
    baseName = baseName.replaceAll("[^a-zA-Z0-9_-]", "_");
    if (baseName.isEmpty()) baseName = "report";

    File dir = subDir.isEmpty() ? new File(EXPORT_ROOT_DIR) : new File(EXPORT_ROOT_DIR, subDir);
    if (!dir.exists() && !dir.mkdirs()) {
      throw new RuntimeException("Could not create export directory: " + dir.getAbsolutePath());
    }

    String timestampedName = baseName + "_" + LocalDateTime.now().format(TIMESTAMP_FMT) + ".txt";
    File file = new File(dir, timestampedName);

    try (FileWriter writer = new FileWriter(file)) {
      writer.write(content == null ? "" : content);
    } catch (IOException e) {
      throw new RuntimeException("Failed to export TXT file: " + e.getMessage(), e);
    }

    return file.getPath();
  }
}
