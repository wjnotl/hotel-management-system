package view.booking;

import adt.ListInterface;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import util.ConsoleUtil;
import util.TableUtil;
import util.TextUtil;

public class WalkInRegistrationView {

  private static final int[] SPAN_WIDTH = {93};
  private static final int[] KV_WIDTHS = {22, 68};
  // Column widths must sum to 96 - 3n to frame to the same width as the spanned heading above.
  private static final int[] TYPE_WIDTHS = {4, 10, 7, 9, 9, 8, 28};
  private static final int SCREEN_WIDTH = 83;

  // The clerk knows before touching the keyboard whether this person has stayed here before, so
  // that answer is asked first. A returning guest goes to the search and a first-time arrival goes
  // straight to the form, rather than being made to search for a record that cannot exist.
  public int displayModeMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("REGISTER WALK-IN", SCREEN_WIDTH);

    System.out.println("Has this guest stayed here before?\n");
    System.out.println("1. Existing Guest");
    System.out.println("   Search the register by ID, name, IC, passport, phone or email.\n");
    System.out.println("2. New Guest");
    System.out.println("   Open a guest file now, then place them in a room or the line.\n");
    System.out.println("3. Back to Walk-In & Booking Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public Room.RoomType promptRoomType(
      Guest guest,
      int[] vacantByType,
      int[] arrivingTodayByType,
      int[] vipWaitingByType,
      int[] lineLengthByType,
      boolean enforceVipBypass,
      boolean autoAssignWhenRoomFree) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("REGISTER WALK-IN - ROOM TYPE", SCREEN_WIDTH);
    System.out.println("Guest: " + guest.getName() + " (" + guest.getGuestId() + ")\n");

    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(TYPE_WIDTHS)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER);

    TableUtil.TableSettings headerSettings = centeredHeader(TYPE_WIDTHS);

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"AVAILABILITY RIGHT NOW"}, spanSettings());
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.SPAN_OPEN);
    TableUtil.printTableRow(
        new String[] {"NO.", "ROOM TYPE", "VACANT", "ADV HELD", "VIP WAIT", "IN LINE", "OUTCOME"},
        headerSettings);
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    Room.RoomType[] types = {Room.RoomType.LUXURY, Room.RoomType.SUITE, Room.RoomType.STANDARD};
    for (int i = 0; i < types.length; i++) {
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(i + 1),
            types[i].name(),
            String.valueOf(vacantByType[i]),
            String.valueOf(arrivingTodayByType[i]),
            String.valueOf(vipWaitingByType[i]),
            String.valueOf(lineLengthByType[i]),
            outcomeFor(
                vacantByType[i],
                arrivingTodayByType[i],
                vipWaitingByType[i],
                lineLengthByType[i],
                enforceVipBypass,
                autoAssignWhenRoomFree)
          },
          settings);
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    System.out.println("ADV HELD counts rooms already promised to advance bookings arriving");
    System.out.println("today, so they are not handed to the counter by mistake.");
    System.out.println();
    System.out.println("4. Back To Guest Selection\n");

    int choice = ConsoleUtil.getMenuInput("Choose a room type: ", 1, 4).getAsInt();
    if (choice == 1) return Room.RoomType.LUXURY;
    if (choice == 2) return Room.RoomType.SUITE;
    if (choice == 3) return Room.RoomType.STANDARD;
    return null;
  }

  // Mirrors the decision the controller is about to make, in the same order, so the column never
  // promises an immediate room the placement rule would not actually hand over.
  private String outcomeFor(
      int vacant,
      int arrivingToday,
      int vipWaiting,
      int lineLength,
      boolean enforceVipBypass,
      boolean autoAssignWhenRoomFree) {

    int freeToCounter = vacant - arrivingToday;

    if (vacant == 0) {
      return "No room free, must wait";
    }
    if (freeToCounter <= 0) {
      return "Held for advance bookings";
    }
    if (enforceVipBypass && freeToCounter <= vipWaiting) {
      return "Held for VIP, must wait";
    }
    if (lineLength > 0) {
      return "Joins a line of " + lineLength;
    }
    if (!autoAssignWhenRoomFree) {
      return "Queued, nobody ahead";
    }
    return "Room now, no wait";
  }

  // Members are ranked by priority score in the VIP module, never by FIFO arrival order.
  public void displayMemberBlockedScreen(Guest guest, Member member) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("HANDLED BY THE VIP MODULE", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"STATUS: [X] " + member.getTier().name() + " MEMBER, NOT A WALK-IN"},
        spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Guest", guest.getName() + " (" + guest.getGuestId() + ")", true);
    printKeyValue(
        kvSettings,
        "Loyalty Tier",
        member.getTier().name() + " (" + member.getMemberId() + ")",
        true);
    printKeyValue(kvSettings, "Strike Count", String.valueOf(guest.getStrikeCount()), true);
    printKeyValue(
        kvSettings,
        "Where To Go",
        "Main Menu  >  2. VIP Priority Room Allocation  >  Manage Waitlist",
        true);
    printKeyValue(
        kvSettings,
        "System Notice",
        "This module books non-members only. A tier holder is ranked against the other waiting"
            + " members by priority score rather than by arrival order, so their reservation has"
            + " to be opened in the VIP module. Nothing has been recorded here.",
        false);

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public void displayAlreadyActiveScreen(
      Guest guest, Reservation existing, int position, boolean acrossAllTypes) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("GUEST ALREADY BEING SERVED", SCREEN_WIDTH);

    String where =
        (existing.getStatus() == Reservation.Status.WAITING)
            ? "standing in the "
                + existing.getRoomType().name()
                + " line at position "
                + ((position > 0) ? position : 1)
            : "holding "
                + existing.getRoomType().name()
                + " room "
                + blankToNa(existing.getRoomNumber())
                + " and is due to check in";

    printNoticeBox(
        "STATUS: [X] SECOND BOOKING REFUSED",
        "Target Guest",
        guest.getName() + " (" + guest.getGuestId() + ")",
        "This guest is already "
            + where
            + " under reservation "
            + existing.getReservationId()
            + (acrossAllTypes
                ? ". One person may hold only one live standard booking, across every room type."
                : ". One person may take only one place in a given line, though they may wait in"
                    + " another room type's line at the same time.")
            + " Serve or cancel that entry instead of opening a second one.");
    ConsoleUtil.printContinueMessage();
  }

  public int displayHasAdvanceBookingScreen(Guest guest, Reservation booking, boolean dueToday) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("GUEST BOOKED AHEAD", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();
    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"STATUS: [!] AN ADVANCE BOOKING ALREADY EXISTS"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Guest", guest.getName() + " (" + guest.getGuestId() + ")", true);
    printKeyValue(kvSettings, "Reservation ID", booking.getReservationId(), true);
    printKeyValue(kvSettings, "Confirmation Code", booking.getConfirmationNumber(), true);
    printKeyValue(kvSettings, "Room Type Booked", booking.getRoomType().name(), true);
    printKeyValue(
        kvSettings, "Expected Arrival", formatTime(booking.getExpectedArrivalTime()), true);
    printKeyValue(
        kvSettings,
        "Nights Booked",
        (booking.getStayDays() != null) ? booking.getStayDays() + " night(s)" : "Not stated",
        true);
    printKeyValue(kvSettings, "Booked At", formatTime(booking.getReservationTime()), true);
    printKeyValue(
        kvSettings,
        "System Notice",
        dueToday
            ? "This guest reserved a room in advance and has now arrived. Using that booking keeps"
                + " one record for the stay. Opening a separate walk-in leaves the original"
                + " booking unclaimed."
            : "That booking is for a different night, and a booking may only be taken up on the"
                + " night it reserves. It stays open and untouched. Serving this guest now means"
                + " an ordinary walk-in against today's stock.",
        false);

    System.out.println();

    // Offering a choice that would then be refused is worse than not offering it, so claiming the
    // booking simply is not on the menu on the wrong day. The caller's codes do not move.
    if (!dueToday) {
      System.out.println("1. Register A Walk-In For Tonight");
      System.out.println("2. Back\n");
      return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt() + 1;
    }

    System.out.println("1. Use The Advance Booking (recommended)");
    System.out.println("2. Register A Separate Walk-In Anyway");
    System.out.println("3. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public boolean displayAssignConfirmationScreen(
      Guest guest,
      Room room,
      int graceMinutes,
      int vacantRooms,
      int arrivingToday,
      int vipWaiting) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM IMMEDIATE ROOM ASSIGNMENT", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"ROOM HAND-OVER"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Guest", guest.getName() + " (" + guest.getGuestId() + ")", true);
    printKeyValue(kvSettings, "Room Number", room.getRoomNumber(), true);
    printKeyValue(kvSettings, "Room Type", room.getRoomType().name(), true);
    printKeyValue(kvSettings, "Rate Per Night", String.format("RM %.2f", room.getPrice()), true);
    printKeyValue(kvSettings, "Rooms Left After", String.valueOf(vacantRooms - 1), true);
    printKeyValue(kvSettings, "Held For Arrivals Today", String.valueOf(arrivingToday), true);
    printKeyValue(kvSettings, "VIP Still Waiting", String.valueOf(vipWaiting), true);
    printKeyValue(
        kvSettings,
        "Hold Window",
        graceMinutes + " minutes before the room is released and a strike is recorded",
        false);

    System.out.println();
    System.out.println("A room of this type is free and nobody is entitled to it ahead of this");
    System.out.println("guest, so they are served straight away rather than queuing for nothing.");
    System.out.println();
    System.out.println("1. Hand Room " + room.getRoomNumber() + " Over Now");
    System.out.println("2. Do Not Assign (back to guest selection)\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt() == 1;
  }

  public boolean displayEnqueueConfirmationScreen(
      Guest guest,
      Room.RoomType roomType,
      int projectedPosition,
      int waiting,
      int capacity,
      int vacantRooms,
      int arrivingToday,
      int vipWaiting,
      String reasonForWaiting) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM WALK-IN REGISTRATION", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"GUEST DETAILS"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Guest ID", guest.getGuestId(), true);
    printKeyValue(kvSettings, "Guest Name", guest.getName(), true);
    printKeyValue(
        kvSettings,
        "IC / Passport No",
        (guest.getIcNumber() != null) ? guest.getIcNumber() : blankToNa(guest.getPassportNumber()),
        true);
    printKeyValue(kvSettings, "Phone Number", blankToNa(guest.getPhoneNumber()), true);
    printKeyValue(kvSettings, "Email Address", blankToNa(guest.getEmail()), true);
    printKeyValue(kvSettings, "Strike Count", String.valueOf(guest.getStrikeCount()), false);

    System.out.println();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"QUEUE PLACEMENT"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Target Line", roomType.name(), true);
    printKeyValue(kvSettings, "Vacant Clean Rooms", String.valueOf(vacantRooms), true);
    printKeyValue(kvSettings, "Held For Arrivals Today", String.valueOf(arrivingToday), true);
    printKeyValue(kvSettings, "VIP Already Waiting", String.valueOf(vipWaiting), true);
    printKeyValue(kvSettings, "Currently Waiting", String.valueOf(waiting), true);
    printKeyValue(kvSettings, "Array Capacity", waiting + " / " + capacity + " slots used", true);
    printKeyValue(kvSettings, "Position On Joining", projectedPosition + " (joins the back)", true);
    printKeyValue(kvSettings, "Why They Wait", reasonForWaiting, false);

    System.out.println();
    System.out.println("1. Register This Walk-In Into The " + roomType.name() + " Line");
    System.out.println("2. Do Not Register (back to guest selection)\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt() == 1;
  }

  public void displayLineFullScreen(Room.RoomType roomType, int waiting, int maxQueueLength) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("LINE FULL", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [X] WALK-IN REFUSED",
        "Line Length",
        waiting + " waiting, the limit in force is " + maxQueueLength,
        "The "
            + roomType.name()
            + " line has reached a limit set under Settings & Configuration, either the house"
            + " length rule or a queue array that is full and not allowed to grow, so no further"
            + " walk-in can be taken for this type. Serve the front of the line, raise the limit,"
            + " or offer the guest a different room type.");
    ConsoleUtil.printContinueMessage();
  }

  public void displayAssignSuccessScreen(
      Reservation reservation, Guest guest, Room room, int graceMinutes) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ROOM ASSIGNED", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"STATUS: ALLOCATED WITHOUT QUEUING"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", reservation.getReservationId(), true);
    printKeyValue(kvSettings, "Confirmation Code", reservation.getConfirmationNumber(), true);
    printKeyValue(kvSettings, "Guest Name", guest.getName(), true);
    printKeyValue(kvSettings, "Room Number", room.getRoomNumber(), true);
    printKeyValue(kvSettings, "Room Status", room.getStatus().name(), true);
    printKeyValue(kvSettings, "Hold Expires In", graceMinutes + " minutes", false);

    System.out.println();
    System.out.println("Check the guest in from Walk-In Queue Management, [C] Check In Hold.");
    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public void displayEnqueueSuccessScreen(
      Reservation reservation, Guest guest, int position, int waiting, int capacity) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("WALK-IN REGISTERED", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"STATUS: ENQUEUED"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", reservation.getReservationId(), true);
    printKeyValue(kvSettings, "Confirmation Code", reservation.getConfirmationNumber(), true);
    printKeyValue(kvSettings, "Guest Name", (guest != null) ? guest.getName() : "N/A", true);
    printKeyValue(kvSettings, "Room Type", reservation.getRoomType().name(), true);
    printKeyValue(kvSettings, "Place In Line", String.valueOf(position), true);
    printKeyValue(kvSettings, "Line Length", waiting + " / " + capacity + " slots used", false);

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public void displayMarkArrivalSuccessScreen(
      Reservation reservation, Guest guest, int position, int waiting) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ADVANCE BOOKING JOINED THE LINE", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"STATUS: RESERVED -> WAITING"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", reservation.getReservationId(), true);
    printKeyValue(kvSettings, "Confirmation Code", reservation.getConfirmationNumber(), true);
    printKeyValue(kvSettings, "Guest Name", (guest != null) ? guest.getName() : "N/A", true);
    printKeyValue(kvSettings, "Room Type", reservation.getRoomType().name(), true);
    printKeyValue(kvSettings, "Place In Line", String.valueOf(position), true);
    printKeyValue(kvSettings, "Line Length", String.valueOf(waiting), false);

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  private String blankToNa(String value) {
    return (value == null || value.isEmpty()) ? "N/A" : value;
  }

  private String formatTime(LocalDateTime dateTime) {
    if (dateTime == null) return "N/A";
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
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
