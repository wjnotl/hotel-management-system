package view.booking;

import adt.ListInterface;
import entity.Guest;
import util.ConsoleUtil;
import util.TableUtil;
import util.TextUtil;

public class GuestRegistrationView {

  private static final int[] SPAN_WIDTH = {83};
  private static final int[] KV_WIDTHS = {22, 58};
  private static final int SCREEN_WIDTH = 83;
  private static final int TOTAL_STEPS = 5;

  public String promptName(int step, String current) {
    printFormHeader(step, "Full Name", null);
    System.out.println("Required.");
    printCurrentValue(current, false);
    return ConsoleUtil.getStringInput("Full Name: ");
  }

  public String promptIcNumber(int step, String name, String current) {
    printFormHeader(step, "IC Number", name);
    System.out.println("Format: XXXXXX-XX-XXXX   (example 920101-14-5543)");
    System.out.println("The first six digits are the date of birth as YYMMDD.");
    System.out.println("The 12 digits may be typed without hyphens and will be formatted.");
    System.out.println("Leave blank if the guest is presenting a passport instead.");
    printCurrentValue(current, true);
    return ConsoleUtil.getStringInput("IC Number: ");
  }

  public String promptPassportNumber(int step, String name, String current) {
    printFormHeader(step, "Passport Number", name);
    System.out.println("Leave blank if the IC number was already captured.");
    printCurrentValue(current, true);
    return ConsoleUtil.getStringInput("Passport Number: ");
  }

  public String promptPhoneNumber(int step, String name, String current) {
    printFormHeader(step, "Phone Number", name);
    System.out.println("Required. This is how the desk recalls a guest whose room is ready.");
    printCurrentValue(current, false);
    return ConsoleUtil.getStringInput("Phone Number: ");
  }

  public String promptEmail(int step, String name, String current) {
    printFormHeader(step, "Email Address", name);
    System.out.println("Optional. Leave blank to skip.");
    printCurrentValue(current, true);
    return ConsoleUtil.getStringInput("Email Address: ");
  }

  public void displayFieldErrorScreen(String field, String value, String reason) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("VALIDATION ERROR", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [X] " + field.toUpperCase() + " REJECTED",
        field,
        (value == null || value.isEmpty()) ? "(blank)" : value,
        reason);
    ConsoleUtil.printContinueMessage("Press Enter to try again...");
  }

  public void displayDuplicateGuestScreen(String field, String value, Guest existing) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("DUPLICATE GUEST", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [X] ALREADY ON RECORD",
        field,
        value,
        "This "
            + field.toLowerCase()
            + " already belongs to "
            + existing.getName()
            + " ("
            + existing.getGuestId()
            + "). Opening a second file would split this guest's history across two records."
            + " Cancel and search for "
            + existing.getGuestId()
            + " instead, or capture a different value.");
    ConsoleUtil.printContinueMessage("Press Enter to try again...");
  }

  public boolean displaySameNameWarningScreen(String name, Guest existing) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("NAME ALREADY IN USE", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [!] ANOTHER GUEST SHARES THIS NAME",
        "Existing Record",
        existing.getName() + " (" + existing.getGuestId() + ")",
        "A guest with this exact name is already on record. Searching by name will always"
            + " return the older record, so this new guest will only be reachable by guest ID."
            + " Continue only if these are genuinely two different people.");
    return promptConfirm("Register this as a separate guest anyway? (Y/N): ");
  }

  // Returns 1 to save, 2 to walk back into the form, 3 to discard. The summary is the first
  // point where the clerk sees every field together, so it is also the most likely place for
  // a mistake to be spotted.
  public int displayConfirmationScreen(
      String guestId,
      String name,
      String icNumber,
      String passportNumber,
      String phoneNumber,
      String email) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM NEW GUEST FILE", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"GUEST DETAILS"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Guest ID (assigned)", guestId, true);
    printKeyValue(kvSettings, "Full Name", name, true);
    printKeyValue(kvSettings, "IC Number", blankToNa(icNumber), true);
    printKeyValue(kvSettings, "Passport Number", blankToNa(passportNumber), true);
    printKeyValue(kvSettings, "Phone Number", phoneNumber, true);
    printKeyValue(kvSettings, "Email Address", blankToNa(email), true);
    // Stated rather than asked. A person with no guest file has never stayed here, so there is
    // no card to find and none is issued at the desk.
    printKeyValue(kvSettings, "Loyalty Status", "NON-MEMBER (new guest file)", true);
    printKeyValue(kvSettings, "Strike Count", "0 (new file)", false);

    System.out.println();
    System.out.println("1. Save This Guest To The Register");
    System.out.println("2. Go Back And Edit The Details");
    System.out.println("3. Discard And Cancel\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public void displaySuccessScreen(Guest guest) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("GUEST REGISTERED", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"STATUS: SAVED TO THE GUEST REGISTER"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Guest ID", guest.getGuestId(), true);
    printKeyValue(kvSettings, "Full Name", guest.getName(), true);
    printKeyValue(kvSettings, "Phone Number", blankToNa(guest.getPhoneNumber()), true);
    printKeyValue(kvSettings, "Loyalty Status", "NON-MEMBER", false);

    System.out.println();
    System.out.println("This guest can now be looked up by ID or by name at any booking screen.");
    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  private void printFormHeader(int step, String fieldTitle, String name) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("REGISTER NEW GUEST", SCREEN_WIDTH);
    System.out.println("Step " + step + " of " + TOTAL_STEPS + ": " + fieldTitle);
    if (name != null && !name.isEmpty()) {
      System.out.println("Guest: " + name);
    }
    System.out.println("[Enter 'B' to Go Back a Step, 'C' to Cancel]\n");
  }

  // A field the clerk is walking back to already holds a value, so it is shown rather than
  // silently wiped by an empty line. Optional fields also need a way to be emptied again,
  // which a blank entry cannot express once it means "keep".
  private void printCurrentValue(String current, boolean optional) {
    if (current == null || current.isEmpty()) {
      System.out.println();
      return;
    }

    System.out.println();
    System.out.println("Current value: " + current);
    System.out.println(
        optional ? "Press Enter to keep it, or type '-' to clear it." : "Press Enter to keep it.");
    System.out.println();
  }

  private String blankToNa(String value) {
    return (value == null || value.isEmpty()) ? "N/A" : value;
  }

  private void printNoticeBox(String heading, String key, String value, String notice) {
    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {heading}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, key, value, true);
    printKeyValue(kvSettings, "System Notice", notice, false);
    System.out.println();
  }

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

  private boolean promptConfirm(String prompt) {
    while (true) {
      String choice = ConsoleUtil.getStringInput(prompt);
      if (choice != null) {
        if ("Y".equalsIgnoreCase(choice.trim())) return true;
        if ("N".equalsIgnoreCase(choice.trim())) return false;
      }
      System.out.println("Invalid input. Please enter 'Y' or 'N'.");
    }
  }
}
