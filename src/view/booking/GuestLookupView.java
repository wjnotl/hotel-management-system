package view.booking;

import adt.ListInterface;
import entity.Guest;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;
import util.TextUtil;

// One search screen shared by every booking flow that has to put a name to the person at the
// counter. The field is chosen before the term is typed, so a partial phone number can never come
// back mixed in with every IC that happens to contain the same digits, and the results are paged
// so a one letter search cannot scroll its own header off the terminal.
public class GuestLookupView {

  private static final int[] SPAN_WIDTH = {93};
  private static final int[] KV_WIDTHS = {22, 68};
  // A row frames to 1 border + (width + 2 padding) per column + 1 divider between columns, so a
  // six column set has to sum to 78 to line up under the 93 wide spanned heading above it.
  private static final int[] MATCH_WIDTHS = {4, 10, 20, 15, 13, 16};
  private static final int SCREEN_WIDTH = 83;

  public static final int FIELD_GUEST_ID = 1;
  public static final int FIELD_NAME = 2;
  public static final int FIELD_IC = 3;
  public static final int FIELD_PASSPORT = 4;
  public static final int FIELD_PHONE = 5;
  public static final int FIELD_EMAIL = 6;
  public static final int FIELD_BOOKING_CODE = 7;
  public static final int FIELD_ALL = 8;
  public static final int TOGGLE_MATCH_MODE = 9;
  public static final int REGISTER_NEW_GUEST = 10;
  public static final int BACK = 0;

  // Returns one of the constants above. BACK is what a bare Enter means everywhere in this module.
  public int displayFieldMenu(String title, String matchMode, boolean offerRegistration) {
    String error = null;

    while (true) {
      ConsoleUtil.clearScreen();
      ConsoleUtil.printTitleBox(title, SCREEN_WIDTH);

      System.out.println("Pick the field you are searching on, then type the value.");
      System.out.println("Part of a value is enough while the match mode is CONTAINS.\n");
      System.out.println("MATCH MODE : [ " + matchMode + " ]\n");

      System.out.println(" 1. Guest ID");
      System.out.println(" 2. Full Name");
      System.out.println(" 3. IC Number");
      System.out.println(" 4. Passport Number");
      System.out.println(" 5. Phone Number");
      System.out.println(" 6. Email Address");
      System.out.println(" 7. Reservation ID or Confirmation Code");
      System.out.println(" 8. All Of The Above");
      System.out.println(
          " 9. Switch Match Mode To " + ("EXACT".equals(matchMode) ? "CONTAINS" : "EXACT"));
      if (offerRegistration) {
        System.out.println("10. Register A New Guest");
      }
      System.out.println();
      System.out.println("B - Back");
      System.out.println();

      ConsoleUtil.printFieldError(error);

      try {
        GetMenuInputResult result =
            ConsoleUtil.getNavInput(
                "Choose an option: ", 1, offerRegistration ? 10 : 9, new char[] {'B'});

        if (result.isBlank || "B".equalsIgnoreCase(result.input)) return BACK;
        return result.getAsInt();
      } catch (IllegalArgumentException e) {
        // Redrawn rather than printed under the prompt, so a mistyped key never stacks the screen
        // into a column of alternating prompts and errors.
        error = e.getMessage();
      }
    }
  }

  // Empty string means the clerk wants to go back a step rather than search for nothing.
  public String promptSearchTerm(String fieldLabel, String matchMode) {
    String error = null;

    while (true) {
      ConsoleUtil.clearScreen();
      ConsoleUtil.printTitleBox("SEARCH BY " + fieldLabel.toUpperCase(), SCREEN_WIDTH);
      System.out.println("MATCH MODE : [ " + matchMode + " ]\n");
      System.out.println("B - Back");
      System.out.println();

      ConsoleUtil.printFieldError(error);

      try {
        String input = ConsoleUtil.getStringInput(fieldLabel + ": ");
        if (input == null || input.trim().isEmpty() || "B".equalsIgnoreCase(input.trim())) {
          return "";
        }
        return input.trim();
      } catch (IllegalArgumentException e) {
        error = e.getMessage();
      }
    }
  }

  /**
   * Lists the matches a page at a time and collects the clerk's answer.
   *
   * @return the 1-based row on the current page, or 'P' / 'N' to page, or 'S' to search again, or
   *     null when the clerk backed out
   */
  public GetMenuInputResult displayMatches(
      String fieldLabel,
      String term,
      String matchMode,
      ListInterface<Guest> matches,
      int currentPage,
      int pageSize) {

    String error = null;

    while (true) {
      ConsoleUtil.clearScreen();
      ConsoleUtil.printTitleBox("MATCHING GUEST RECORDS", SCREEN_WIDTH);

      System.out.println("FIELD      : [ " + fieldLabel + " ]");
      System.out.println("TERM       : [ \"" + term + "\" ]");
      System.out.println("MATCH MODE : [ " + matchMode + " ]\n");

      int total = matches.getNumberOfEntries();
      int totalPages = (total == 0) ? 0 : (int) Math.ceil((double) total / pageSize);
      int page = Math.min(Math.max(currentPage, 1), Math.max(totalPages, 1));

      printMatchTable(matches, page, pageSize);

      int rowsOnPage = countRowsOnPage(total, page, pageSize);
      System.out.printf(
          "%nPage %d / %d (Matches: %d)%n%n", (totalPages == 0) ? 0 : page, totalPages, total);

      System.out.println("P - Previous Page      N - Next Page      S - Search Again");
      System.out.println("B - Back");
      System.out.println();

      ConsoleUtil.printFieldError(error);

      try {
        char[] commands = {'P', 'N', 'S', 'B'};

        GetMenuInputResult result;
        if (rowsOnPage <= 0) {
          result = ConsoleUtil.getNavInput("Enter a command: ", commands);
        } else {
          String range = (rowsOnPage == 1) ? "1" : "1-" + rowsOnPage;
          result =
              ConsoleUtil.getNavInput(
                  "Pick the guest (" + range + ") or a command: ", 1, rowsOnPage, commands);
        }

        if (result.isBlank || "B".equalsIgnoreCase(result.input)) return null;
        return result;
      } catch (IllegalArgumentException e) {
        error = e.getMessage();
      }
    }
  }

  public int displayNotFoundScreen(String fieldLabel, String term, boolean offerRegistration) {
    String error = null;

    while (true) {
      ConsoleUtil.clearScreen();
      ConsoleUtil.printTitleBox("GUEST NOT ON RECORD", SCREEN_WIDTH);

      printNoticeBox(
          "STATUS: [!] NO MATCHING GUEST FILE",
          "Searched " + fieldLabel,
          term,
          "Nothing in the guest register matches this term. A first-time arrival has no file yet,"
              + " so one can be opened now before they are given a room or a place in line.");

      if (offerRegistration) {
        System.out.println("1. Register This Person As A New Guest");
        System.out.println("2. Search Again");
      } else {
        System.out.println("1. Search Again");
      }
      System.out.println();
      System.out.println("B - Back");
      System.out.println();

      ConsoleUtil.printFieldError(error);

      try {
        GetMenuInputResult result =
            ConsoleUtil.getNavInput(
                "Choose an option: ", 1, offerRegistration ? 2 : 1, new char[] {'B'});

        if (result.isBlank || "B".equalsIgnoreCase(result.input)) return BACK;

        int choice = result.getAsInt();
        if (!offerRegistration) return 2;
        return choice;
      } catch (IllegalArgumentException e) {
        error = e.getMessage();
      }
    }
  }

  private void printMatchTable(ListInterface<Guest> matches, int page, int pageSize) {
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(MATCH_WIDTHS)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setTruncateAt(2, MATCH_WIDTHS[2] - 2)
            .setTruncateAt(3, MATCH_WIDTHS[3] - 2)
            .setTruncateAt(4, MATCH_WIDTHS[4] - 2)
            .setTruncateAt(5, MATCH_WIDTHS[5] - 2);

    TableUtil.TableSettings headerSettings = centeredHeader(MATCH_WIDTHS);
    TableUtil.TableSettings spanSettings = spanSettings();

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"GUEST REGISTER MATCHES"}, spanSettings);
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.SPAN_OPEN);
    TableUtil.printTableRow(
        new String[] {"NO.", "GUEST ID", "NAME", "IC / PASSPORT", "PHONE", "EMAIL"},
        headerSettings);

    int total = matches.getNumberOfEntries();
    if (total == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.printTableRow(new String[] {"*** NO GUEST MATCHES THIS SEARCH ***"}, spanSettings);
      TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
      return;
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int startIndex = (page - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);

    for (int i = startIndex; i <= endIndex; i++) {
      Guest g = matches.getEntry(i);
      if (g == null) continue;

      TableUtil.printTableRow(
          new String[] {
            String.valueOf(i - startIndex + 1),
            g.getGuestId(),
            g.getName(),
            (g.getIcNumber() != null) ? g.getIcNumber() : blankToNa(g.getPassportNumber()),
            blankToNa(g.getPhoneNumber()),
            blankToNa(g.getEmail())
          },
          settings);
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
  }

  private int countRowsOnPage(int total, int page, int pageSize) {
    if (total == 0) return 0;
    int startIndex = (page - 1) * pageSize + 1;
    if (startIndex > total) return 0;
    return Math.min(startIndex + pageSize - 1, total) - startIndex + 1;
  }

  private String blankToNa(String value) {
    return (value == null || value.isEmpty()) ? "N/A" : value;
  }

  private TableUtil.TableSettings spanSettings() {
    return new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);
  }

  private TableUtil.TableSettings centeredHeader(int[] widths) {
    TableUtil.TableSettings settings = new TableUtil.TableSettings(widths);
    for (int i = 0; i < widths.length; i++) {
      settings.setHAlign(i, TableUtil.Align.CENTER);
    }
    return settings;
  }

  private void printNoticeBox(String heading, String key, String value, String notice) {
    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {heading}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, key, value, true);
    printKeyValue(kvSettings, "System Notice", notice, false);
    System.out.println();
  }

  // TableUtil wraps a cell at the full column width but only prints width - 2 characters and
  // hard-truncates the rest, so long values are pre-wrapped to the printable width here and
  // emitted one row at a time. Without this every wrapped line loses its last two characters.
  private void printKeyValue(
      TableUtil.TableSettings settings, String key, String value, boolean moreRowsFollow) {
    ListInterface<String> lines = TextUtil.wrapText(value, KV_WIDTHS[1] - 2);

    for (int i = 1; i <= lines.getNumberOfEntries(); i++) {
      TableUtil.printTableRow(new String[] {(i == 1) ? key : "", lines.getEntry(i)}, settings);
    }

    TableUtil.printTableBorder(
        settings,
        moreRowsFollow ? TableUtil.BorderPosition.MIDDLE : TableUtil.BorderPosition.BOTTOM);
  }
}
