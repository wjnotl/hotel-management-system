package view.booking;

import adt.ListInterface;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;

public class GuestLookupView {

  private static final int[] SPAN_WIDTH = {106};
  // Column widths must sum to SPAN_WIDTH - 3(n - 1) to frame to the same width as the spanned
  // heading above. This table runs wider than its siblings because an email address is what a
  // clerk reads to tell two similar records apart, and truncating it defeats the point.
  private static final int[] MATCH_WIDTHS = {4, 10, 21, 16, 14, 26};
  private static final int SCREEN_WIDTH = 93;

  public static final int FIELD_GUEST_ID = 1;
  public static final int FIELD_NAME = 2;
  public static final int FIELD_IC = 3;
  public static final int FIELD_PASSPORT = 4;
  public static final int FIELD_PHONE = 5;
  public static final int FIELD_EMAIL = 6;
  public static final int FIELD_BOOKING_CODE = 7;
  public static final int FIELD_ALL = 8;
  public static final int BACK = 0;

  public static class GuestRowDTO {
    private final String guestId;
    private final String name;
    private final String document;
    private final String phoneNumber;
    private final String email;

    public GuestRowDTO(
        String guestId, String name, String document, String phoneNumber, String email) {
      this.guestId = guestId;
      this.name = name;
      this.document = document;
      this.phoneNumber = phoneNumber;
      this.email = email;
    }

    public String getGuestId() {
      return guestId;
    }

    public String getName() {
      return name;
    }

    public String getDocument() {
      return document;
    }

    public String getPhoneNumber() {
      return phoneNumber;
    }

    public String getEmail() {
      return email;
    }
  }

  /**
   * The register itself is the landing screen, so a clerk who can see the guest just picks the row.
   * Searching is a filter over that table rather than a gate in front of it, which is how every
   * other list in this module behaves.
   *
   * @return a row number on the current page, or one of the S / P / N / E commands
   */
  public GetMenuInputResult renderRegisterScreen(
      String title,
      ListInterface<GuestRowDTO> rows,
      String searchField,
      String searchTerm,
      String matchMode,
      int currentPage,
      int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(title, SCREEN_WIDTH);

    System.out.println("SEARCH FIELD : [ " + searchField + " ]");
    System.out.println(
        "SEARCH TERM  : [ "
            + ((searchTerm == null) ? "None, showing everyone" : searchTerm)
            + " ]");
    System.out.println("MATCH MODE   : [ " + matchMode + " ]\n");

    int total = (rows == null) ? 0 : rows.getNumberOfEntries();
    int totalPages = (total == 0) ? 0 : (int) Math.ceil((double) total / pageSize);

    printMatchTable(rows, currentPage, pageSize);

    System.out.printf(
        "%nPage %d / %d (Guests: %d)%n%n", (totalPages == 0) ? 0 : currentPage, totalPages, total);

    System.out.println("[S] Search / Filter   [P] Prev Page   [N] Next Page   [E] Back");
    System.out.println("Pick a row number to use that guest.\n");

    char[] commands = {'S', 'P', 'N', 'E'};

    int rowsOnPage = countRowsOnPage(total, currentPage, pageSize);
    if (rowsOnPage <= 0) {
      return ConsoleUtil.getMenuInput("Enter a command: ", commands);
    }

    String range = (rowsOnPage == 1) ? "1" : "1-" + rowsOnPage;
    return ConsoleUtil.getMenuInput(
        "Pick the guest (" + range + ") or a command: ", 1, rowsOnPage, commands);
  }

  // Returns 1 to change the field, 2 the term, 3 the match mode, 4 to reset, 5 to apply, 6 to
  // discard.
  public int displayFilterMainMenu(String searchField, String searchTerm, String matchMode) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH & REGISTER FILTERS", SCREEN_WIDTH);
    System.out.println("Search Field : [ " + searchField + " ]");
    System.out.println("Search Term  : [ " + ((searchTerm == null) ? "None" : searchTerm) + " ]");
    System.out.println("Match Mode   : [ " + matchMode + " ]\n");

    System.out.println("1. Change Search Field");
    System.out.println("2. Change Search Term");
    System.out.println(
        "3. Switch Match Mode To " + ("EXACT".equals(matchMode) ? "CONTAINS" : "EXACT"));
    System.out.println("4. Reset All Filters");
    System.out.println("5. Apply And Return");
    System.out.println("6. Back (discard these changes)\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
  }

  // Returns one of the FIELD_ constants, or BACK.
  public int displaySearchFieldSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH FIELD", SCREEN_WIDTH);
    System.out.println("Current: [ " + current + " ]\n");
    System.out.println("Searching one field at a time keeps a partial phone number from");
    System.out.println("pulling in every IC that happens to contain the same digits.\n");
    System.out.println("1. Guest ID");
    System.out.println("2. Full Name");
    System.out.println("3. IC Number");
    System.out.println("4. Passport Number");
    System.out.println("5. Phone Number");
    System.out.println("6. Email Address");
    System.out.println("7. Reservation ID or Confirmation Code");
    System.out.println("8. All Of The Above");
    System.out.println("9. Back\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 9).getAsInt();
    return (choice == 9) ? BACK : choice;
  }

  public String promptSearchTerm(String fieldLabel, String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH TERM", SCREEN_WIDTH);
    System.out.println("Field   : [ " + fieldLabel + " ]");
    System.out.println("Current : [ " + ((current == null) ? "None" : current) + " ]\n");
    System.out.println("Type '-' to clear the term and list every guest again.");
    System.out.println("E - Exit and keep the current term\n");

    return ConsoleUtil.getStringInput("Search term: ");
  }

  private void printMatchTable(ListInterface<GuestRowDTO> matches, int page, int pageSize) {
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
    TableUtil.printTableRow(new String[] {"GUEST REGISTER"}, spanSettings);
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
      GuestRowDTO row = matches.getEntry(i);
      if (row == null) continue;

      TableUtil.printTableRow(
          new String[] {
            String.valueOf(i - startIndex + 1),
            row.getGuestId(),
            row.getName(),
            row.getDocument(),
            row.getPhoneNumber(),
            row.getEmail()
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
}
