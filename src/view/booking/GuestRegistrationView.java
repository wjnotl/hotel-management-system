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

  public static final int STEP_NAME = 1;
  public static final int STEP_IC = 2;
  public static final int STEP_PASSPORT = 3;
  public static final int STEP_PHONE = 4;
  public static final int STEP_EMAIL = 5;

  public static class FormDTO {
    private final String name;
    private final String icNumber;
    private final String passportNumber;
    private final String phoneNumber;
    private final String email;

    public FormDTO(
        String name, String icNumber, String passportNumber, String phoneNumber, String email) {
      this.name = name;
      this.icNumber = icNumber;
      this.passportNumber = passportNumber;
      this.phoneNumber = phoneNumber;
      this.email = email;
    }

    public String getName() {
      return name;
    }

    public String getIcNumber() {
      return icNumber;
    }

    public String getPassportNumber() {
      return passportNumber;
    }

    public String getPhoneNumber() {
      return phoneNumber;
    }

    public String getEmail() {
      return email;
    }
  }

  /**
   * Draws the whole form with everything captured so far and asks for one field.
   *
   * <p>A rejected value is reported here, above the prompt that produced it, rather than on a
   * screen of its own. The clerk retypes the one field that failed and never loses the rest.
   *
   * @param step which field is being asked, one of the STEP_ constants
   * @param form the values captured so far, blank where nothing has been typed yet
   * @param error the reason the last attempt was rejected, or null on a clean pass
   */
  public String promptFormField(int step, FormDTO form, String error) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("REGISTER NEW GUEST", SCREEN_WIDTH);

    System.out.println("Only the field marked > is being asked. The rest are kept as they are.");
    System.out.println("B - Back one field    E - Exit to menu    -  clears an optional field\n");

    printFormLine(step, STEP_NAME, "Full Name", form.getName(), "required");
    printFormLine(step, STEP_IC, "IC Number", form.getIcNumber(), "either IC or passport");
    printFormLine(
        step, STEP_PASSPORT, "Passport Number", form.getPassportNumber(), "either IC or passport");
    printFormLine(step, STEP_PHONE, "Phone Number", form.getPhoneNumber(), "required");
    printFormLine(step, STEP_EMAIL, "Email", form.getEmail(), "optional");

    System.out.println();
    printHint(step);
    printError(error);

    return ConsoleUtil.getStringInput(labelFor(step) + ": ");
  }

  private void printFormLine(
      int currentStep, int thisStep, String label, String value, String note) {

    String shown = (value == null || value.isEmpty()) ? "(" + note + ")" : value;
    System.out.printf("  %s %-16s: %s%n", (currentStep == thisStep) ? ">" : " ", label, shown);
  }

  private void printHint(int step) {
    if (step == STEP_IC) {
      System.out.println("Format XXXXXX-XX-XXXX, for example 920101-14-5543. The first six digits");
      System.out.println("are the date of birth as YYMMDD. The 12 digits may be typed without");
      System.out.println("hyphens and the mask is applied for you. Leave blank to use a passport.");
    } else if (step == STEP_PASSPORT) {
      System.out.println("Leave blank if the IC number was already captured.");
    } else if (step == STEP_PHONE) {
      System.out.println("This is how the desk recalls a guest whose room is ready.");
    } else if (step == STEP_EMAIL) {
      System.out.println("Optional. Leave blank to skip.");
    } else {
      System.out.println("Letters, spaces, apostrophes, hyphens, full stops and slashes.");
    }
    System.out.println();
  }

  private void printError(String error) {
    if (error == null || error.isEmpty()) return;

    ListInterface<String> lines = TextUtil.wrapText(error, SCREEN_WIDTH - 6);
    for (int i = 1; i <= lines.getNumberOfEntries(); i++) {
      System.out.println(((i == 1) ? "[!] " : "    ") + lines.getEntry(i));
    }
    System.out.println();
  }

  private String labelFor(int step) {
    if (step == STEP_IC) return "IC Number";
    if (step == STEP_PASSPORT) return "Passport Number";
    if (step == STEP_PHONE) return "Phone Number";
    if (step == STEP_EMAIL) return "Email";
    return "Full Name";
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

    System.out.println("1. Register this as a separate guest anyway");
    System.out.println("2. Go back and change the name\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt() == 1;
  }

  // Returns 1 to save, 2 to walk back into the form, 3 to discard.
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
    System.out.println("3. Discard And Exit\n");

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
}
