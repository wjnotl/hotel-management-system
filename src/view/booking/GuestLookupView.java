package view.booking;

import adt.ListInterface;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;
import util.TextUtil;

public class GuestLookupView {

  private static final int[] SPAN_WIDTH = {93};
  private static final int[] KV_WIDTHS = {22, 68};
  // Column widths must sum to 96 - 3n to frame to the same width as the spanned heading above.
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

  // Returns one of the FIELD_ constants, TOGGLE_MATCH_MODE, or BACK. The fields fill 1 to 9, so
  // back is 0 rather than a tenth entry the eye has to hunt for at the bottom of the list.
  public int displayFieldMenu(String title, String matchMode) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(title, SCREEN_WIDTH);

    System.out.println("Pick the field you are searching on, then type the value.");
    System.out.println("Part of a value is enough while the match mode is CONTAINS.\n");
    System.out.println("MATCH MODE : [ " + matchMode + " ]\n");

    System.out.println("1. Guest ID");
    System.out.println("2. Full Name");
    System.out.println("3. IC Number");
    System.out.println("4. Passport Number");
    System.out.println("5. Phone Number");
    System.out.println("6. Email Address");
    System.out.println("7. Reservation ID or Confirmation Code");
    System.out.println("8. All Of The Above");
    System.out.println(
        "9. Switch Match Mode To " + ("EXACT".equals(matchMode) ? "CONTAINS" : "EXACT"));
    System.out.println("0. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 0, 9).getAsInt();
  }

  // "E" backs out of the prompt. A blank line is rejected rather than silently treated as a
  // command, so an empty search reports itself instead of doing nothing.
  public String promptSearchTerm(String fieldLabel, String matchMode) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH BY " + fieldLabel.toUpperCase(), SCREEN_WIDTH);
    System.out.println("MATCH MODE : [ " + matchMode + " ]\n");
    System.out.println("E - Exit back to the field list\n");

    return requireText(fieldLabel + ": ", "Search term cannot be empty! Type 'E' to go back.");
  }

  public GetMenuInputResult displayMatches(
      String fieldLabel,
      String term,
      String matchMode,
      ListInterface<GuestRowDTO> matches,
      int currentPage,
      int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MATCHING GUEST RECORDS", SCREEN_WIDTH);

    System.out.println("FIELD      : [ " + fieldLabel + " ]");
    System.out.println("TERM       : [ \"" + term + "\" ]");
    System.out.println("MATCH MODE : [ " + matchMode + " ]\n");

    int total = matches.getNumberOfEntries();
    int totalPages = (total == 0) ? 0 : (int) Math.ceil((double) total / pageSize);

    printMatchTable(matches, currentPage, pageSize);

    int rowsOnPage = countRowsOnPage(total, currentPage, pageSize);
    System.out.printf(
        "%nPage %d / %d (Matches: %d)%n%n", (totalPages == 0) ? 0 : currentPage, totalPages, total);

    System.out.println("[P] Prev Page   [N] Next Page   [0] Back\n");

    char[] commands = {'P', 'N'};

    if (rowsOnPage <= 0) {
      return ConsoleUtil.getMenuInput("Enter a command: ", 0, 0, commands);
    }

    String range = (rowsOnPage == 1) ? "1" : "1-" + rowsOnPage;
    return ConsoleUtil.getMenuInput(
        "Pick the guest (" + range + ") or a command: ", 0, rowsOnPage, commands);
  }

  // Returns 1 to search again, or BACK. Opening a guest file is not offered here, because the mode
  // menu at the head of the flow already asked whether this person is new.
  public int displayNotFoundScreen(String fieldLabel, String term) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("GUEST NOT ON RECORD", SCREEN_WIDTH);

    printNoticeBox(
        "STATUS: [!] NO MATCHING GUEST FILE",
        "Searched " + fieldLabel,
        term,
        "Nothing in the guest register matches this term. A first-time arrival has no file yet,"
            + " so back out to the previous screen and choose New Guest to open one.");

    System.out.println("1. Search Again");
    System.out.println("0. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 0, 1).getAsInt();
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

  private void printNoticeBox(String heading, String key, String value, String notice) {
    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {heading}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, key, value, true);
    printKeyValue(kvSettings, "System Notice", notice, false);
    System.out.println();
  }

  // TableUtil prints only width - 2 chars per cell, so long values are pre-wrapped here.
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

  // A blank line is rejected here rather than in the controller, so the error redraws this screen
  // instead of the menu above it.
  private String requireText(String prompt, String emptyMessage) {
    String typed = ConsoleUtil.getStringInput(prompt);
    if (typed == null || typed.trim().isEmpty()) {
      throw new IllegalArgumentException(emptyMessage);
    }
    return typed;
  }
}
