package view.booking;

import adt.ListInterface;
import entity.BookingSettings;
import entity.Room;
import util.ConsoleUtil;
import util.TableUtil;
import util.TextUtil;

public class BookingSettingsView {

  private static final int[] SPAN_WIDTH = {93};
  private static final int[] KV_WIDTHS = {34, 56};
  private static final int[] TYPE_WIDTHS = {4, 10, 21, 21, 25};
  private static final int SCREEN_WIDTH = 83;

  public int displayMasterSettingsMenu(BookingSettings config) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("WALK-IN & BOOKING SETTINGS", SCREEN_WIDTH);

    // Each number carries the values it edits. A separate summary block above the menu made the
    // clerk map one list onto the other, and the two lists did not even run in the same order.
    printCard(
        1,
        "HOLD & NO-SHOW RULES",
        "Grace "
            + config.getHoldGraceMinutes()
            + " min   Max strikes "
            + config.getMaxStrikes()
            + "   Lapsed hold "
            + lapsedLabel(config.isRequeueOnLapse()).toLowerCase());

    printCard(
        2,
        "QUEUE & ORDER RULES",
        config.getInitialQueueCapacity()
            + " slots, "
            + (config.isAllowQueueExpansion() ? "may grow" : "fixed size")
            + "   Line limit "
            + queueLimitLabel(config).toLowerCase()
            + "   Auto-assign "
            + yesNo(config.isAutoAssignWhenRoomFree()),
        "VIP bypass "
            + yesNo(config.isEnforceVipBypass())
            + "   Override "
            + yesNo(config.isAllowBypassOverride())
            + "   Out of order "
            + yesNo(config.isAllowNonFrontAllocation())
            + "   One booking per guest "
            + yesNo(config.isBlockDuplicateAcrossLines()));

    printCard(
        3,
        "ADVANCE BOOKING RULES",
        "Sell "
            + leadLabel(config)
            + "   Same-day "
            + yesNo(config.isAllowSameDayAdvanceBooking())
            + "   Block overbooking "
            + yesNo(config.isBlockOverbooking()),
        "Checkout day reusable " + yesNo(config.isCheckoutDayReusable()));

    printCard(
        4,
        "DESK & REPORT DEFAULTS",
        config.getPageSize()
            + " rows per page   Max stay "
            + config.getMaxStayNights()
            + " nights   Report opens "
            + config.getDefaultReportPeriod()
            + ", "
            + (config.getDefaultRecordLimit() == 0
                ? "all rows"
                : "top " + config.getDefaultRecordLimit()),
        "Line sort  " + config.getDefaultQueueSort(),
        "Adv sort   " + config.getDefaultAdvanceSort());

    printCard(
        5,
        "PER-ROOM-TYPE OVERRIDES",
        (config.countOverrides() == 0)
            ? "None in force"
            : config.countOverrides() + " override(s) in force");

    printOption(6, "Rebuild live lines from current capacity");
    printOption(7, "Reset to factory defaults");
    printOption(8, "Back to Walk-In & Booking Menu");
    System.out.println();

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 8).getAsInt();
    return (choice == 8) ? 0 : choice;
  }

  public int displayHoldRulesMenu(BookingSettings config) {
    return numberedMenu(
        "HOLD & NO-SHOW RULES",
        new String[] {
          "These decide how long a room is kept for a guest who has been called,",
          "and what happens to the booking when that window runs out."
        },
        new String[] {
          "Hold Grace Window (minutes)        [Current: " + config.getHoldGraceMinutes() + "]",
          "Max Strikes Before No-Show         [Current: " + config.getMaxStrikes() + "]",
          "Where A Lapsed Hold Goes           [Current: "
              + lapsedLabel(config.isRequeueOnLapse())
              + "]"
        });
  }

  private void printCard(int number, String title, String... lines) {
    System.out.println("  " + number + "  " + title);
    for (String line : lines) {
      System.out.println("     " + line);
    }
    System.out.println();
  }

  private void printOption(int number, String label) {
    System.out.println("  " + number + "  " + label);
  }

  public int displayQueueRulesMenu(BookingSettings config) {
    return numberedMenu(
        "QUEUE & ORDER RULES",
        new String[] {
          "These decide who is served at once, who has to wait, how long a line",
          "may get, and who is allowed to jump it."
        },
        new String[] {
          "Initial Queue Capacity             [Current: "
              + config.getInitialQueueCapacity()
              + " slots]",
          "Allow The Queue Array To Grow      [Current: "
              + yesNo(config.isAllowQueueExpansion())
              + "]",
          "Maximum Line Length                [Current: " + queueLimitLabel(config) + "]",
          "Auto-Assign When A Room Is Free    [Current: "
              + yesNo(config.isAutoAssignWhenRoomFree())
              + "]",
          "Enforce VIP Bypass                 [Current: "
              + yesNo(config.isEnforceVipBypass())
              + "]",
          "Allow Supervisor Bypass Override   [Current: "
              + yesNo(config.isAllowBypassOverride())
              + "]",
          "Allow Serving Out Of FIFO Order    [Current: "
              + yesNo(config.isAllowNonFrontAllocation())
              + "]",
          "One Live Booking Per Guest         [Current: "
              + yesNo(config.isBlockDuplicateAcrossLines())
              + "]"
        });
  }

  public int displayAdvanceRulesMenu(BookingSettings config) {
    return numberedMenu(
        "ADVANCE BOOKING RULES",
        new String[] {
          "These decide how far ahead the desk may sell a room, and how the",
          "availability calendar treats the day a guest checks out."
        },
        new String[] {
          "Booking Lead Time                  [Current: " + leadLabel(config) + "]",
          "Allow Same Day Advance Bookings    [Current: "
              + yesNo(config.isAllowSameDayAdvanceBooking())
              + "]",
          "Checkout Day Is Reusable           [Current: "
              + yesNo(config.isCheckoutDayReusable())
              + "]",
          "Refuse Bookings When Fully Booked  [Current: " + yesNo(config.isBlockOverbooking()) + "]"
        });
  }

  public int displayDeskDefaultsMenu(BookingSettings config) {
    return numberedMenu(
        "DESK & REPORT DEFAULTS",
        new String[] {
          "These set how the booking screens open before the clerk changes",
          "anything for the current session."
        },
        new String[] {
          "Rows Per Page                      [Current: " + config.getPageSize() + "]",
          "Maximum Stay In Nights             [Current: " + config.getMaxStayNights() + "]",
          "Default Walk-In Queue Sort         [Current: " + config.getDefaultQueueSort() + "]",
          "Default Advance Booking Sort       [Current: " + config.getDefaultAdvanceSort() + "]",
          "Default Report Period              [Current: " + config.getDefaultReportPeriod() + "]",
          "Default Report Record Limit        [Current: "
              + (config.getDefaultRecordLimit() == 0
                  ? "Show All"
                  : "Top " + config.getDefaultRecordLimit())
              + "]"
        });
  }

  // Shows what each line is actually running on, so an override is never edited blind.
  public int displayPerTypeMenu(BookingSettings config) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("PER-ROOM-TYPE OVERRIDES", SCREEN_WIDTH);
    System.out.println("A line with no override of its own runs on the house wide value.");
    System.out.println("Clearing an override hands that line back to the house value.\n");

    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(TYPE_WIDTHS)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER);

    TableUtil.TableSettings headerSettings = centeredHeader(TYPE_WIDTHS);

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"WHAT EACH LINE IS RUNNING ON"}, spanSettings());
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.SPAN_OPEN);
    TableUtil.printTableRow(
        new String[] {"NO.", "ROOM TYPE", "GRACE (MIN)", "QUEUE SLOTS", "MAX LINE LENGTH"},
        headerSettings);
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    Room.RoomType[] types = {Room.RoomType.LUXURY, Room.RoomType.SUITE, Room.RoomType.STANDARD};
    for (int i = 0; i < types.length; i++) {
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(i + 1),
            types[i].name(),
            overrideCell(
                config.getHoldGraceMinutesOverride(types[i]), config.getHoldGraceMinutes(types[i])),
            overrideCell(
                config.getInitialQueueCapacityOverride(types[i]),
                config.getInitialQueueCapacity(types[i])),
            overrideCell(
                config.getMaxQueueLengthOverride(types[i]),
                config.getMaxQueueLength(types[i]),
                true)
          },
          settings);
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    System.out.println("1. Edit LUXURY");
    System.out.println("2. Edit SUITE");
    System.out.println("3. Edit STANDARD");
    System.out.println("4. Clear Every Override");
    System.out.println("5. Back\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
    return (choice == 5) ? 0 : choice;
  }

  public int displayTypeOverrideMenu(BookingSettings config, Room.RoomType roomType) {
    return numberedMenu(
        roomType.name() + " OVERRIDES",
        new String[] {
          "Grace Window    : "
              + overrideCell(
                  config.getHoldGraceMinutesOverride(roomType),
                  config.getHoldGraceMinutes(roomType)),
          "Queue Slots     : "
              + overrideCell(
                  config.getInitialQueueCapacityOverride(roomType),
                  config.getInitialQueueCapacity(roomType)),
          "Max Line Length : "
              + overrideCell(
                  config.getMaxQueueLengthOverride(roomType),
                  config.getMaxQueueLength(roomType),
                  true)
        },
        new String[] {
          "Set Hold Grace Window For This Type",
          "Set Initial Queue Capacity For This Type",
          "Set Maximum Line Length For This Type",
          "Clear Every Override For This Type"
        });
  }

  /**
   * Collects a number, with the effect of the change stated before it is typed.
   *
   * @param warning a conflict with live data, or null when there is none
   * @return the new value, or null when the clerk backed out
   */
  public Integer promptIntSetting(
      String title,
      String explanation,
      String currentLabel,
      int min,
      int max,
      String unit,
      String warning) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MODIFY " + title.toUpperCase(), SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"SETTING DETAIL"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Setting", title, true);
    printKeyValue(kvSettings, "Current Value", currentLabel, true);
    printKeyValue(kvSettings, "Allowed Range", min + " to " + max + " " + unit, true);
    printKeyValue(kvSettings, "What It Changes", explanation, warning != null);
    if (warning != null) {
      printKeyValue(kvSettings, "[!] Live Data Warning", warning, false);
    }

    System.out.println();
    System.out.println("Type 'C' to keep the current value\n");

    ConsoleUtil.GetMenuInputResult result =
        ConsoleUtil.getMenuInput("New value: ", min, max, new char[] {'C'});
    return result.isNumber ? Integer.valueOf(result.getAsInt()) : null;
  }

  public Boolean promptToggleSetting(
      String title,
      String explanation,
      boolean currentValue,
      String onLabel,
      String offLabel,
      String warning) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MODIFY " + title.toUpperCase(), SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"SETTING DETAIL"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Setting", title, true);
    printKeyValue(kvSettings, "Current Value", currentValue ? onLabel : offLabel, true);
    printKeyValue(kvSettings, "What It Changes", explanation, warning != null);
    if (warning != null) {
      printKeyValue(kvSettings, "[!] Live Data Warning", warning, false);
    }

    System.out.println();
    System.out.println("1. " + onLabel);
    System.out.println("2. " + offLabel);
    System.out.println("3. Back (keep the current value)\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
    if (choice == 3) return null;
    return (choice == 1) ? Boolean.TRUE : Boolean.FALSE;
  }

  public String promptSortSetting(String title, String current, ListInterface<String> options) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MODIFY " + title.toUpperCase(), SCREEN_WIDTH);
    System.out.println("Current default: [ " + current + " ]\n");
    System.out.println("A clerk can still change the order for the session with [O] on the");
    System.out.println("screen itself. This only decides how the screen opens.\n");

    int total = options.getNumberOfEntries();
    for (int i = 1; i <= total; i++) {
      System.out.println(i + ". " + options.getEntry(i));
    }
    System.out.println((total + 1) + ". Back (keep the current default)\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, total + 1).getAsInt();
    if (choice == total + 1) return null;
    return options.getEntry(choice);
  }

  public void displayRebuildResultScreen(
      int luxuryWaiting,
      int suiteWaiting,
      int standardWaiting,
      int luxuryCapacity,
      int suiteCapacity,
      int standardCapacity) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("LIVE LINES REBUILT", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"STATUS: QUEUES RECREATED FROM THE MASTER LIST"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(
        kvSettings, "Luxury Line", luxuryWaiting + " waiting / " + luxuryCapacity + " slots", true);
    printKeyValue(
        kvSettings, "Suite Line", suiteWaiting + " waiting / " + suiteCapacity + " slots", true);
    printKeyValue(
        kvSettings,
        "Standard Line",
        standardWaiting + " waiting / " + standardCapacity + " slots",
        true);
    printKeyValue(
        kvSettings,
        "System Notice",
        "Capacity and the expansion flag are fixed when a queue is created, so a changed value"
            + " only reaches a line by rebuilding it. Waiting guests keep their FIFO order"
            + " because the rebuild replays them by arrival time. A line already longer than a"
            + " newly reduced capacity is opened wide enough to hold everyone rather than"
            + " dropping the overflow.",
        false);

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public boolean promptResetConfirmation() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("RESET BOOKING SETTINGS", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [!] EVERY BOOKING SETTING WILL BE OVERWRITTEN",
        "Scope",
        "Hold rules, queue rules, advance booking rules, desk defaults and every"
            + " per-room-type override",
        "This restores the factory values for this module only. Reservations, guests and rooms"
            + " are not touched, and the live lines are rebuilt afterwards so the restored"
            + " capacity takes effect straight away.");

    System.out.println("1. Reset Every Booking Setting To Its Default");
    System.out.println("2. Leave The Settings Alone\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt() == 1;
  }

  public void displayResetSuccessScreen() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SETTINGS RESET", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: DEFAULTS RESTORED",
        "Result",
        "All booking settings are back to their factory values",
        "The live lines have been rebuilt so the default capacity is already in force.");
    ConsoleUtil.printContinueMessage();
  }

  public void displaySavedScreen(String settingName, String newValue, String effect) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SETTING SAVED", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: WRITTEN TO booking_settings.dat",
        settingName,
        newValue,
        (effect == null || effect.isEmpty())
            ? "The new value is in force immediately and survives a restart."
            : effect);
    ConsoleUtil.printContinueMessage();
  }

  private int numberedMenu(String title, String[] contextLines, String[] options) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(title, SCREEN_WIDTH);

    for (String line : contextLines) {
      System.out.println(line);
    }
    System.out.println();

    for (int i = 0; i < options.length; i++) {
      System.out.println((i + 1) + ". " + options[i]);
    }
    int backOption = options.length + 1;
    System.out.println(backOption + ". Back\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, backOption).getAsInt();
    return (choice == backOption) ? 0 : choice;
  }

  private String overrideCell(int override, int effective) {
    return overrideCell(override, effective, false);
  }

  private String overrideCell(int override, int effective, boolean zeroIsUnlimited) {
    String value =
        (zeroIsUnlimited && effective == BookingSettings.UNLIMITED_QUEUE)
            ? "Unlimited"
            : String.valueOf(effective);
    return (override == BookingSettings.USE_HOUSE_VALUE) ? value + " (house)" : value + " (own)";
  }

  private String yesNo(boolean value) {
    return value ? "YES" : "NO";
  }

  private String lapsedLabel(boolean requeue) {
    return requeue ? "Back of the line" : "Closed as a no-show";
  }

  private String queueLimitLabel(BookingSettings config) {
    return config.isQueueLengthCapped() ? config.getMaxQueueLength() + " guests" : "Unlimited";
  }

  private String leadLabel(BookingSettings config) {
    int lead = config.getAdvanceBookingLeadDays();
    return (lead <= BookingSettings.UNLIMITED_LEAD_DAYS)
        ? "no lead time limit"
        : "up to " + lead + " days ahead";
  }

  private TableUtil.TableSettings kvSettings() {
    return new TableUtil.TableSettings(KV_WIDTHS);
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
    TableUtil.TableSettings kvSettings = kvSettings();

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
}
