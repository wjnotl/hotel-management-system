package control.booking;

import adt.ArrayList;
import adt.ListInterface;
import entity.BookingSettings;
import entity.Room;
import repo.BookingSettingsRepo;
import repo.StandardReservationRepo;
import util.ConsoleUtil;
import view.booking.BookingSettingsView;

public class BookingSettingsController {
  private static final String BLANK_INPUT = "Input cannot be empty!";

  private static final int MIN_GRACE_MINUTES = 1;
  private static final int MAX_GRACE_MINUTES = 240;
  private static final int MIN_STRIKES = 1;
  private static final int MAX_STRIKES = 10;

  // Zero is a legal value for both: it switches the rule off rather than setting it to nothing.
  private static final int MIN_QUEUE_CAPACITY = 1;
  private static final int MAX_QUEUE_CAPACITY = 500;
  private static final int MIN_LINE_LENGTH = 0;
  private static final int MAX_LINE_LENGTH = 500;
  private static final int MIN_PAGE_SIZE = 5;
  private static final int MAX_PAGE_SIZE = 50;
  private static final int MIN_STAY_NIGHTS = 1;
  private static final int MAX_STAY_NIGHTS = 365;
  private static final int MIN_LEAD_DAYS = 0;
  private static final int MAX_LEAD_DAYS = 730;
  private static final int MIN_RECORD_LIMIT = 0;
  private static final int MAX_RECORD_LIMIT = 500;

  private final BookingSettingsView settingsView = new BookingSettingsView();
  private final BookingSettingsRepo bookingSettingsRepo;
  private final StandardReservationRepo standardReservationRepo;

  public BookingSettingsController(
      BookingSettingsRepo bookingSettingsRepo, StandardReservationRepo standardReservationRepo) {
    this.bookingSettingsRepo = bookingSettingsRepo;
    this.standardReservationRepo = standardReservationRepo;
  }

  public void startSettingsManagement() {
    while (true) {
      try {
        int choice = settingsView.displayMasterSettingsMenu(config());

        if (choice == 0) {
          return;
        } else if (choice == 1) {
          manageHoldRules();
        } else if (choice == 2) {
          manageQueueRules();
        } else if (choice == 3) {
          manageAdvanceRules();
        } else if (choice == 4) {
          manageDeskDefaults();
        } else if (choice == 5) {
          managePerTypeOverrides();
        } else if (choice == 6) {
          applyToLiveLines();
        } else if (choice == 7) {
          resetToDefaults();
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private BookingSettings config() {
    return bookingSettingsRepo.getSettings();
  }

  // Blank keeps the current value and redraws the same setting screen, so the swallowed message
  // is the only one ConsoleUtil raises for an empty line.
  private Integer promptIntSetting(
      String title,
      String explanation,
      String currentLabel,
      int min,
      int max,
      String unit,
      String warning) {
    while (true) {
      try {
        return settingsView.promptIntSetting(
            title, explanation, currentLabel, min, max, unit, warning);
      } catch (Exception e) {
        if (!BLANK_INPUT.equals(e.getMessage())) ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private Boolean promptToggleSetting(
      String title,
      String explanation,
      boolean currentValue,
      String onLabel,
      String offLabel,
      String warning) {
    while (true) {
      try {
        return settingsView.promptToggleSetting(
            title, explanation, currentValue, onLabel, offLabel, warning);
      } catch (Exception e) {
        if (!BLANK_INPUT.equals(e.getMessage())) ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String promptSortSetting(String title, String current, ListInterface<String> options) {
    while (true) {
      try {
        return settingsView.promptSortSetting(title, current, options);
      } catch (Exception e) {
        if (!BLANK_INPUT.equals(e.getMessage())) ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // The screens edit the live settings object, so the write is forced here after each change.
  private void persist() {
    bookingSettingsRepo.updateSettings(config());
  }

  private void manageHoldRules() {
    while (true) {
      try {
        int choice = settingsView.displayHoldRulesMenu(config());
        if (choice == 0) return;

        if (choice == 1) {
          Integer value =
              promptIntSetting(
                  "Hold Grace Window",
                  "How long a room stays held for a guest who has been called before it is"
                      + " released and a strike is recorded.",
                  config().getHoldGraceMinutes() + " minutes",
                  MIN_GRACE_MINUTES,
                  MAX_GRACE_MINUTES,
                  "minutes",
                  liveHoldsWarning());
          if (value != null) {
            config().setHoldGraceMinutes(value);
            persist();
            settingsView.displaySavedScreen(
                "Hold Grace Window",
                value + " minutes",
                "Holds already running keep the window they were created under, so this only"
                    + " affects holds opened from now on.");
          }
        } else if (choice == 2) {
          Integer value =
              promptIntSetting(
                  "Max Strikes Before No-Show",
                  "How many lapsed holds a guest may collect before the booking is closed as a"
                      + " no-show instead of being sent back to the line.",
                  String.valueOf(config().getMaxStrikes()),
                  MIN_STRIKES,
                  MAX_STRIKES,
                  "strikes",
                  null);
          if (value != null) {
            config().setMaxStrikes(value);
            persist();
            settingsView.displaySavedScreen(
                "Max Strikes Before No-Show", String.valueOf(value), null);
          }
        } else if (choice == 3) {
          Boolean value =
              promptToggleSetting(
                  "Where A Lapsed Hold Goes",
                  "A guest who misses the grace window can be given another chance at the back"
                      + " of the line, or have the booking closed straight away. The strike is"
                      + " recorded either way.",
                  config().isRequeueOnLapse(),
                  "Back of the line",
                  "Closed as a no-show",
                  null);
          if (value != null) {
            config().setRequeueOnLapse(value);
            persist();
            settingsView.displaySavedScreen(
                "Where A Lapsed Hold Goes",
                value ? "Back of the line" : "Closed as a no-show",
                null);
          }
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void manageQueueRules() {
    while (true) {
      try {
        int choice = settingsView.displayQueueRulesMenu(config());
        if (choice == 0) return;

        if (choice == 1) {
          Integer value =
              promptIntSetting(
                  "Initial Queue Capacity",
                  "How many slots each line owns when it is created. It is a hard stop unless"
                      + " the growth rule below is switched on, in which case the circular array"
                      + " doubles when it fills and this is only a starting size.",
                  config().getInitialQueueCapacity() + " slots",
                  MIN_QUEUE_CAPACITY,
                  MAX_QUEUE_CAPACITY,
                  "slots",
                  capacityWarning());
          if (value != null) {
            config().setInitialQueueCapacity(value);
            persist();
            applyToLiveLines();
          }
        } else if (choice == 2) {
          Boolean value =
              promptToggleSetting(
                  "Allow The Queue Array To Grow",
                  "With growth on, a full line doubles its array and keeps accepting guests."
                      + " With it off, the array is fixed and a full line refuses the next"
                      + " walk-in outright.",
                  config().isAllowQueueExpansion(),
                  "Allow growth",
                  "Fixed size",
                  capacityWarning());
          if (value != null) {
            config().setAllowQueueExpansion(value);
            persist();
            applyToLiveLines();
          }
        } else if (choice == 3) {
          Integer value =
              promptIntSetting(
                  "Maximum Line Length",
                  "The house limit on how many guests may stand in one line, checked before a"
                      + " reservation is created. Enter 0 for no limit.",
                  queueLimitLabel(config().getMaxQueueLength()),
                  MIN_LINE_LENGTH,
                  MAX_LINE_LENGTH,
                  "guests",
                  lineLengthWarning());
          if (value != null) {
            config().setMaxQueueLength(value);
            persist();
            settingsView.displaySavedScreen(
                "Maximum Line Length",
                queueLimitLabel(value),
                "Lowering the limit never removes guests who are already waiting. It only stops"
                    + " the next walk-in from joining a line that is already at or over it.");
          }
        } else if (choice == 4) {
          Boolean value =
              promptToggleSetting(
                  "Auto-Assign When A Room Is Free",
                  "With this on, a walk-in is handed a room at once when one of that type is"
                      + " vacant and clean, not promised to an advance booking arriving today,"
                      + " not owed to a waiting VIP and nobody is already in line. With it off,"
                      + " every walk-in joins the line.",
                  config().isAutoAssignWhenRoomFree(),
                  "Assign immediately",
                  "Always queue",
                  null);
          if (value != null) {
            config().setAutoAssignWhenRoomFree(value);
            persist();
            settingsView.displaySavedScreen(
                "Auto-Assign When A Room Is Free",
                value ? "Assign immediately" : "Always queue",
                null);
          }
        } else if (choice == 5) {
          Boolean value =
              promptToggleSetting(
                  "Enforce VIP Bypass",
                  "With this on, one vacant room is reserved for each high tier member waiting"
                      + " for that type, so the standard line is only served from the surplus."
                      + " With it off, the standard line competes for every free room.",
                  config().isEnforceVipBypass(),
                  "Enforce the bypass",
                  "Ignore the bypass",
                  null);
          if (value != null) {
            config().setEnforceVipBypass(value);
            persist();
            settingsView.displaySavedScreen(
                "Enforce VIP Bypass", value ? "Enforce the bypass" : "Ignore the bypass", null);
          }
        } else if (choice == 6) {
          Boolean value =
              promptToggleSetting(
                  "Allow Supervisor Bypass Override",
                  "Whether a row's Allocate A Room action may take a room the VIP bypass is"
                      + " holding back, on supervisor authority.",
                  config().isAllowBypassOverride(),
                  "Offer the override",
                  "Hide the override",
                  null);
          if (value != null) {
            config().setAllowBypassOverride(value);
            persist();
            settingsView.displaySavedScreen(
                "Allow Supervisor Bypass Override",
                value ? "Offer the override" : "Hide the override",
                null);
          }
        } else if (choice == 7) {
          Boolean value =
              promptToggleSetting(
                  "Allow Serving Out Of FIFO Order",
                  "Whether a row other than the front of the line may be allocated a room. The"
                      + " screen still names every guest who would be skipped and asks for"
                      + " authorisation. With it off, only position 1 can be served.",
                  config().isAllowNonFrontAllocation(),
                  "Allow with authorisation",
                  "Front of the line only",
                  null);
          if (value != null) {
            config().setAllowNonFrontAllocation(value);
            persist();
            settingsView.displaySavedScreen(
                "Allow Serving Out Of FIFO Order",
                value ? "Allow with authorisation" : "Front of the line only",
                null);
          }
        } else if (choice == 8) {
          Boolean value =
              promptToggleSetting(
                  "One Live Booking Per Guest",
                  "With this on, a guest who is already waiting in any line or already holding"
                      + " a room cannot be registered a second time. With it off, the same"
                      + " person may stand in more than one line at once, one place per line."
                      + " Taking two places in the same line is refused either way.",
                  config().isBlockDuplicateAcrossLines(),
                  "One booking only",
                  "Allow several",
                  null);
          if (value != null) {
            config().setBlockDuplicateAcrossLines(value);
            persist();
            settingsView.displaySavedScreen(
                "One Live Booking Per Guest", value ? "One booking only" : "Allow several", null);
          }
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void manageAdvanceRules() {
    while (true) {
      try {
        int choice = settingsView.displayAdvanceRulesMenu(config());
        if (choice == 0) return;

        if (choice == 1) {
          Integer value =
              promptIntSetting(
                  "Booking Lead Time",
                  "How far ahead of today an advance booking may be taken. Enter 0 to remove the"
                      + " limit entirely.",
                  leadLabel(config().getAdvanceBookingLeadDays()),
                  MIN_LEAD_DAYS,
                  MAX_LEAD_DAYS,
                  "days",
                  null);
          if (value != null) {
            config().setAdvanceBookingLeadDays(value);
            persist();
            settingsView.displaySavedScreen(
                "Booking Lead Time",
                leadLabel(value),
                "Bookings already on file beyond the new window are left alone. Only new ones are"
                    + " held to it.");
          }
        } else if (choice == 2) {
          Boolean value =
              promptToggleSetting(
                  "Allow Same Day Advance Bookings",
                  "Whether a booking may be taken for today. With it off, the earliest arrival"
                      + " date offered is tomorrow and someone arriving today is a walk-in.",
                  config().isAllowSameDayAdvanceBooking(),
                  "Allow today",
                  "Tomorrow at the earliest",
                  null);
          if (value != null) {
            config().setAllowSameDayAdvanceBooking(value);
            persist();
            settingsView.displaySavedScreen(
                "Allow Same Day Advance Bookings",
                value ? "Allow today" : "Tomorrow at the earliest",
                null);
          }
        } else if (choice == 3) {
          Boolean value =
              promptToggleSetting(
                  "Checkout Day Is Reusable",
                  "With this on, a stay ending on the 5th does not block another guest arriving"
                      + " on the 5th, because the room is turned over the same day. With it off,"
                      + " every stay reserves an extra night for housekeeping.",
                  config().isCheckoutDayReusable(),
                  "Same day turnover",
                  "Reserve the extra night",
                  turnoverWarning());
          if (value != null) {
            config().setCheckoutDayReusable(value);
            persist();
            settingsView.displaySavedScreen(
                "Checkout Day Is Reusable",
                value ? "Same day turnover" : "Reserve the extra night",
                "Every availability figure in the module is recalculated from this rule, so the"
                    + " advance booking screens will read differently straight away.");
          }
        } else if (choice == 4) {
          Boolean value =
              promptToggleSetting(
                  "Refuse Bookings When Fully Booked",
                  "With this on, an advance booking is refused when every room of that type is"
                      + " already committed on one of the requested nights. With it off, the"
                      + " calendar is shown but never blocks the booking.",
                  config().isBlockOverbooking(),
                  "Refuse the booking",
                  "Warn but allow",
                  null);
          if (value != null) {
            config().setBlockOverbooking(value);
            persist();
            settingsView.displaySavedScreen(
                "Refuse Bookings When Fully Booked",
                value ? "Refuse the booking" : "Warn but allow",
                null);
          }
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void manageDeskDefaults() {
    while (true) {
      try {
        int choice = settingsView.displayDeskDefaultsMenu(config());
        if (choice == 0) return;

        if (choice == 1) {
          Integer value =
              promptIntSetting(
                  "Rows Per Page",
                  "How many rows the walk-in queue and advance booking tables show before"
                      + " paging.",
                  String.valueOf(config().getPageSize()),
                  MIN_PAGE_SIZE,
                  MAX_PAGE_SIZE,
                  "rows",
                  null);
          if (value != null) {
            config().setPageSize(value);
            persist();
            settingsView.displaySavedScreen("Rows Per Page", String.valueOf(value), null);
          }
        } else if (choice == 2) {
          Integer value =
              promptIntSetting(
                  "Maximum Stay In Nights",
                  "The longest stay the check-in and advance booking screens will accept in one"
                      + " booking. The availability calendar may still allow fewer.",
                  config().getMaxStayNights() + " nights",
                  MIN_STAY_NIGHTS,
                  MAX_STAY_NIGHTS,
                  "nights",
                  null);
          if (value != null) {
            config().setMaxStayNights(value);
            persist();
            settingsView.displaySavedScreen("Maximum Stay In Nights", value + " nights", null);
          }
        } else if (choice == 3) {
          String value =
              promptSortSetting(
                  "Default Walk-In Queue Sort", config().getDefaultQueueSort(), queueSortOptions());
          if (value != null) {
            config().setDefaultQueueSort(value);
            persist();
            settingsView.displaySavedScreen("Default Walk-In Queue Sort", value, null);
          }
        } else if (choice == 4) {
          String value =
              promptSortSetting(
                  "Default Advance Booking Sort",
                  config().getDefaultAdvanceSort(),
                  advanceSortOptions());
          if (value != null) {
            config().setDefaultAdvanceSort(value);
            persist();
            settingsView.displaySavedScreen("Default Advance Booking Sort", value, null);
          }
        } else if (choice == 5) {
          String value =
              promptSortSetting(
                  "Default Report Period",
                  config().getDefaultReportPeriod(),
                  reportPeriodOptions());
          if (value != null) {
            config().setDefaultReportPeriod(value);
            persist();
            settingsView.displaySavedScreen("Default Report Period", value, null);
          }
        } else if (choice == 6) {
          Integer value =
              promptIntSetting(
                  "Default Report Record Limit",
                  "How many rows a report writes to the .txt before it is truncated. Enter 0 to"
                      + " export every matching row by default.",
                  (config().getDefaultRecordLimit() == 0)
                      ? "Show All"
                      : "Top " + config().getDefaultRecordLimit(),
                  MIN_RECORD_LIMIT,
                  MAX_RECORD_LIMIT,
                  "rows",
                  null);
          if (value != null) {
            config().setDefaultRecordLimit(value);
            persist();
            settingsView.displaySavedScreen(
                "Default Report Record Limit", (value == 0) ? "Show All" : "Top " + value, null);
          }
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void managePerTypeOverrides() {
    while (true) {
      try {
        int choice = settingsView.displayPerTypeMenu(config());
        if (choice == 0) return;

        if (choice == 4) {
          config().clearAllOverrides();
          persist();
          applyToLiveLines();
          continue;
        }

        Room.RoomType roomType = roomTypeFor(choice);
        manageOneTypeOverride(roomType);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void manageOneTypeOverride(Room.RoomType roomType) {
    while (true) {
      try {
        int choice = settingsView.displayTypeOverrideMenu(config(), roomType);
        if (choice == 0) return;

        if (choice == 1) {
          Integer value =
              promptIntSetting(
                  roomType.name() + " Hold Grace Window",
                  "Overrides the house grace window for this line only. Enter "
                      + BookingSettings.USE_HOUSE_VALUE
                      + " is not accepted here; use option 4 to hand the line back to the house"
                      + " value.",
                  overrideLabel(
                      config().getHoldGraceMinutesOverride(roomType),
                      config().getHoldGraceMinutes(roomType),
                      "minutes"),
                  MIN_GRACE_MINUTES,
                  MAX_GRACE_MINUTES,
                  "minutes",
                  liveHoldsWarning());
          if (value != null) {
            config().setHoldGraceMinutesOverride(roomType, value);
            persist();
            settingsView.displaySavedScreen(
                roomType.name() + " Hold Grace Window", value + " minutes", null);
          }
        } else if (choice == 2) {
          Integer value =
              promptIntSetting(
                  roomType.name() + " Initial Queue Capacity",
                  "Overrides the house starting array size for this line only. The line has to be"
                      + " rebuilt for a new capacity to reach it, which happens automatically"
                      + " after this is saved.",
                  overrideLabel(
                      config().getInitialQueueCapacityOverride(roomType),
                      config().getInitialQueueCapacity(roomType),
                      "slots"),
                  MIN_QUEUE_CAPACITY,
                  MAX_QUEUE_CAPACITY,
                  "slots",
                  capacityWarningFor(roomType));
          if (value != null) {
            config().setInitialQueueCapacityOverride(roomType, value);
            persist();
            applyToLiveLines();
          }
        } else if (choice == 3) {
          Integer value =
              promptIntSetting(
                  roomType.name() + " Maximum Line Length",
                  "Overrides the house line limit for this line only. Enter 0 for no limit on"
                      + " this type.",
                  overrideLabel(
                      config().getMaxQueueLengthOverride(roomType),
                      config().getMaxQueueLength(roomType),
                      "guests"),
                  MIN_LINE_LENGTH,
                  MAX_LINE_LENGTH,
                  "guests",
                  lineLengthWarningFor(roomType));
          if (value != null) {
            config().setMaxQueueLengthOverride(roomType, value);
            persist();
            settingsView.displaySavedScreen(
                roomType.name() + " Maximum Line Length", queueLimitLabel(value), null);
          }
        } else if (choice == 4) {
          config().setHoldGraceMinutesOverride(roomType, BookingSettings.USE_HOUSE_VALUE);
          config().setInitialQueueCapacityOverride(roomType, BookingSettings.USE_HOUSE_VALUE);
          config().setMaxQueueLengthOverride(roomType, BookingSettings.USE_HOUSE_VALUE);
          persist();
          applyToLiveLines();
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // ================= LIVE DATA WARNINGS =================

  private String capacityWarning() {
    int waiting = totalWaiting();
    if (waiting == 0) return null;
    return waiting
        + " guest(s) are standing in the lines right now. Rebuilding replays them by arrival time"
        + " so FIFO order survives, and a line longer than the new capacity is opened wide enough"
        + " to hold everyone rather than dropping the overflow.";
  }

  private String capacityWarningFor(Room.RoomType roomType) {
    int waiting = standardReservationRepo.getQueueByRoomType(roomType).getNumberOfEntries();
    if (waiting == 0) return null;
    return waiting
        + " guest(s) are standing in the "
        + roomType.name()
        + " line. They keep their places, and the array is opened wide enough to hold them even"
        + " if the new capacity is smaller.";
  }

  private String lineLengthWarning() {
    Room.RoomType longest = null;
    int highest = 0;

    for (Room.RoomType roomType : Room.RoomType.values()) {
      int waiting = standardReservationRepo.getQueueByRoomType(roomType).getNumberOfEntries();
      if (waiting > highest) {
        highest = waiting;
        longest = roomType;
      }
    }

    if (longest == null) return null;
    return "The longest line right now is "
        + longest.name()
        + " with "
        + highest
        + " guest(s). A limit below that leaves them where they are and only refuses the next"
        + " arrival.";
  }

  private String lineLengthWarningFor(Room.RoomType roomType) {
    int waiting = standardReservationRepo.getQueueByRoomType(roomType).getNumberOfEntries();
    if (waiting == 0) return null;
    return waiting
        + " guest(s) are already in the "
        + roomType.name()
        + " line. A limit below that leaves them where they are and only refuses the next"
        + " arrival.";
  }

  private String liveHoldsWarning() {
    int holds = 0;
    for (Room.RoomType roomType : Room.RoomType.values()) {
      holds += standardReservationRepo.getHoldsByRoomType(roomType).getNumberOfEntries();
    }

    if (holds == 0) return null;
    return holds
        + " hold(s) are running right now. Each keeps the window it was created under, so none of"
        + " them is re-timed by this change.";
  }

  private String turnoverWarning() {
    return "Switching this off makes every stay occupy one extra night, which can make dates that"
        + " read as free today read as fully booked afterwards. Existing bookings are not"
        + " cancelled, but new ones will be refused on those dates.";
  }

  private int totalWaiting() {
    int waiting = 0;
    for (Room.RoomType roomType : Room.RoomType.values()) {
      waiting += standardReservationRepo.getQueueByRoomType(roomType).getNumberOfEntries();
    }
    return waiting;
  }

  // ================= HELPERS =================

  // These have to match the strings the queue and advance controllers compare against, so they
  // are listed once here rather than being retyped on each screen.
  private ListInterface<String> queueSortOptions() {
    ListInterface<String> options = new ArrayList<>();
    options.add("QUEUE POSITION (FIFO)");
    options.add("WAIT TIME (LONGEST -> SHORTEST)");
    options.add("WAIT TIME (SHORTEST -> LONGEST)");
    options.add("GUEST NAME (A -> Z)");
    options.add("GUEST NAME (Z -> A)");
    options.add("STRIKES (HIGHEST -> LOWEST)");
    return options;
  }

  private ListInterface<String> advanceSortOptions() {
    ListInterface<String> options = new ArrayList<>();
    options.add("ARRIVAL DATE (SOONEST -> LATEST)");
    options.add("ARRIVAL DATE (LATEST -> SOONEST)");
    options.add("BOOKED AT (NEWEST -> OLDEST)");
    options.add("BOOKED AT (OLDEST -> NEWEST)");
    options.add("GUEST NAME (A -> Z)");
    options.add("GUEST NAME (Z -> A)");
    options.add("ROOM TYPE (A -> Z)");
    return options;
  }

  private ListInterface<String> reportPeriodOptions() {
    ListInterface<String> options = new ArrayList<>();
    options.add("TODAY");
    options.add("YESTERDAY");
    options.add("LAST 7 DAYS");
    options.add("LAST 30 DAYS");
    options.add("ALL TIME");
    return options;
  }

  // Capacity and the expansion flag are read once when a CircularArrayQueue is constructed, so a
  // changed value only reaches an existing line by rebuilding it from the master list.
  private void applyToLiveLines() {
    standardReservationRepo.applySettings();

    settingsView.displayRebuildResultScreen(
        standardReservationRepo.getQueueByRoomType(Room.RoomType.LUXURY).getNumberOfEntries(),
        standardReservationRepo.getQueueByRoomType(Room.RoomType.SUITE).getNumberOfEntries(),
        standardReservationRepo.getQueueByRoomType(Room.RoomType.STANDARD).getNumberOfEntries(),
        standardReservationRepo.getQueueCapacity(Room.RoomType.LUXURY),
        standardReservationRepo.getQueueCapacity(Room.RoomType.SUITE),
        standardReservationRepo.getQueueCapacity(Room.RoomType.STANDARD));
  }

  private boolean promptResetConfirmation() {
    while (true) {
      try {
        return settingsView.promptResetConfirmation();
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void resetToDefaults() {
    if (!promptResetConfirmation()) return;

    bookingSettingsRepo.resetToDefaults();
    standardReservationRepo.applySettings();
    settingsView.displayResetSuccessScreen();
  }

  private Room.RoomType roomTypeFor(int choice) {
    if (choice == 1) return Room.RoomType.LUXURY;
    if (choice == 2) return Room.RoomType.SUITE;
    return Room.RoomType.STANDARD;
  }

  private String queueLimitLabel(int value) {
    return (value == BookingSettings.UNLIMITED_QUEUE) ? "Unlimited" : value + " guests";
  }

  private String leadLabel(int value) {
    return (value == BookingSettings.UNLIMITED_LEAD_DAYS)
        ? "No lead time limit"
        : "Up to " + value + " days ahead";
  }

  private String overrideLabel(int override, int effective, String unit) {
    String value =
        (BookingSettings.UNLIMITED_QUEUE == effective && "guests".equals(unit))
            ? "Unlimited"
            : effective + " " + unit;
    return (override == BookingSettings.USE_HOUSE_VALUE)
        ? value + "  (inherited from the house value)"
        : value + "  (this line has its own value)";
  }
}
