package view.frontdesk;

import adt.ArrayList;
import adt.ListInterface;
import entity.Billing;
import entity.Guest;
import entity.Reservation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import util.ConsoleUtil;
import util.TableUtil;

public class GuestInformationView {
  public String promptGuestIdInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("Fetch Guest Information");
    return ConsoleUtil.getStringInput("Enter Guest ID [C to cancel]: ");
  }

  public void displayGuestNotFound(String guestId) {
    ConsoleUtil.printError("No guest found with ID \"" + guestId + "\".");
  }

  public int displayGuestActionSubmenu(Guest guest) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(
        "GUEST ACTIONS: " + guest.getName() + " [" + guest.getGuestId() + "]");
    System.out.println("1. View Guest Details");
    System.out.println("2. View Billing History");
    System.out.println("3. View Assigned Room History");
    System.out.println("4. View Reservation History");
    System.out.println("5. Back to Guest Lookup\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public void displayGuestDetails(
      Guest guest, Reservation latestReservation, Billing latestBilling) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("GUEST DETAILS", 98);

    // 8 columns, one per field: header row on top, values row below
    int[] kvWidths = {10, 14, 12, 10, 10, 12, 12, 10};
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT)
            .setHAlign(2, TableUtil.Align.LEFT)
            .setHAlign(3, TableUtil.Align.LEFT)
            .setHAlign(4, TableUtil.Align.LEFT)
            .setHAlign(5, TableUtil.Align.LEFT)
            .setHAlign(6, TableUtil.Align.LEFT)
            .setHAlign(7, TableUtil.Align.LEFT);

    String bookingId = (latestReservation != null) ? latestReservation.getReservationId() : "N/A";
    String roomNo = (latestBilling != null) ? latestBilling.getRoomNumber() : "N/A";
    String roomType =
        (latestBilling != null && latestBilling.getRoomType() != null)
            ? latestBilling.getRoomType().name()
            : "N/A";
    String checkIn = (latestBilling != null) ? formatDate(latestBilling.getCheckInDate()) : "N/A";
    String checkOut = (latestBilling != null) ? formatDate(latestBilling.getCheckOutDate()) : "N/A";
    String billingStatus = (latestBilling != null) ? latestBilling.getStatus().name() : "N/A";

    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "Guest ID", "Guest Name", "Booking ID", "Room No.",
          "Room Type", "Check-in", "Check-out", "Status"
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          guest.getGuestId(),
          guest.getName(),
          bookingId,
          roomNo,
          roomType,
          checkIn,
          checkOut,
          billingStatus
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    if (latestBilling == null) {
      System.out.println("(No stay records found for this guest yet.)\n");
    }

    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  public ConsoleUtil.GetMenuInputResult displayBillingHistory(
      Guest guest, ListInterface<Billing> historyNewToOld, int currentPage, int pageSize) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(
        "BILLING HISTORY: " + guest.getName() + " [" + guest.getGuestId() + "]", 83);

    ListInterface<Billing> history =
        (historyNewToOld != null) ? historyNewToOld : new ArrayList<>();
    int total = history.getNumberOfEntries();

    int[] colWidths = {5, 12, 10, 12, 12, 10, 14};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(colWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.RIGHT);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "NO.", "BILLING ID", "ROOM NO.", "CHECK-IN", "CHECK-OUT", "STATUS", "TOTAL (RM)"
        },
        settings);

    if (total == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);

      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {77}).setHAlign(0, TableUtil.Align.CENTER);
      TableUtil.printTableRow(new String[] {"*** NO BILLING RECORDS FOUND ***"}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);

      System.out.println("\nPage 0 / 0 (Total Matches: 0)\n");
      return ConsoleUtil.getMenuInput("Press 'C' to return: ", new char[] {'C'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int totalPages = (int) Math.ceil((double) total / pageSize);
    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);

    for (int i = startIndex; i <= endIndex; i++) {
      Billing b = history.getEntry(i);
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(i),
            b.getBillingId(),
            b.getRoomNumber(),
            formatDate(b.getCheckInDate()),
            formatDate(b.getCheckOutDate()),
            b.getStatus().name(),
            String.format("%.2f", b.getTotalAmount())
          },
          settings);
    }
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.printf("Page %d / %d (Total Matches: %d)\n\n", currentPage, totalPages, total);
    System.out.println("Select a Billing record number to view its receipt.");
    System.out.println("[N] Next Page          [P] Previous Page      [C] Cancel\n");

    return ConsoleUtil.getMenuInput(
        "Enter a command or select index (" + startIndex + "-" + endIndex + "): ",
        startIndex,
        endIndex,
        new char[] {'N', 'P', 'C'});
  }

  public void displayReceipt(Guest guest, Billing billing) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("RECEIPT: " + billing.getBillingId(), 64);

    int[] kvWidths = {22, 40};
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"Guest Name", guest.getName()}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(new String[] {"Guest ID", guest.getGuestId()}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          "Room No. / Type", billing.getRoomNumber() + " (" + billing.getRoomType().name() + ")"
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Check-in", formatDate(billing.getCheckInDate())}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Check-out", formatDate(billing.getCheckOutDate())}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Nights Stayed", String.valueOf(billing.getNumberOfNights())}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Rate / Night (RM)", String.format("%.2f", billing.getRatePerNight())},
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"Subtotal (RM)", String.format("%.2f", billing.getSubtotal())}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          "SST (" + (int) (Billing.SST_RATE * 100) + "%) (RM)",
          String.format("%.2f", billing.getSstAmount())
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"TOTAL (RM)", String.format("%.2f", billing.getTotalAmount())}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Billing Status", billing.getStatus().name()}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  public ConsoleUtil.GetMenuInputResult displayAssignedRoomHistory(
      Guest guest, ListInterface<Billing> historyNewToOld, int currentPage, int pageSize) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(
        "ASSIGNED ROOM HISTORY: " + guest.getName() + " [" + guest.getGuestId() + "]", 70);

    ListInterface<Billing> history =
        (historyNewToOld != null) ? historyNewToOld : new ArrayList<>();
    int total = history.getNumberOfEntries();

    int[] colWidths = {5, 12, 12, 12, 12, 10};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(colWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"NO.", "ROOM NO.", "ROOM TYPE", "CHECK-IN", "CHECK-OUT", "STATUS"}, settings);

    if (total == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);

      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {65}).setHAlign(0, TableUtil.Align.CENTER);
      TableUtil.printTableRow(
          new String[] {"*** NO ROOM ASSIGNMENT HISTORY FOUND ***"}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);

      System.out.println("\nPage 0 / 0 (Total Matches: 0)\n");
      return ConsoleUtil.getMenuInput("Press 'C' to return: ", new char[] {'C'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int totalPages = (int) Math.ceil((double) total / pageSize);
    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);

    for (int i = startIndex; i <= endIndex; i++) {
      Billing b = history.getEntry(i);
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(i),
            b.getRoomNumber(),
            b.getRoomType().name(),
            formatDate(b.getCheckInDate()),
            formatDate(b.getCheckOutDate()),
            b.getStatus().name()
          },
          settings);
    }
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.printf("Page %d / %d (Total Matches: %d)\n\n", currentPage, totalPages, total);
    System.out.println("[N] Next Page          [P] Previous Page      [C] Return\n");

    return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'N', 'P', 'C'});
  }

  public ConsoleUtil.GetMenuInputResult displayReservationHistory(
      Guest guest, ListInterface<Reservation> historyNewToOld, int currentPage, int pageSize) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(
        "RESERVATION HISTORY: " + guest.getName() + " [" + guest.getGuestId() + "]", 65);

    ListInterface<Reservation> history =
        (historyNewToOld != null) ? historyNewToOld : new ArrayList<>();
    int total = history.getNumberOfEntries();

    int[] colWidths = {5, 12, 11, 12, 25};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(colWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"NO.", "RES ID", "ROOM TYPE", "STATUS", "RESERVATION TIME"}, settings);

    if (total == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);

      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {60}).setHAlign(0, TableUtil.Align.CENTER);
      TableUtil.printTableRow(new String[] {"*** NO RESERVATION HISTORY FOUND ***"}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);

      System.out.println("\nPage 0 / 0 (Total Matches: 0)\n");
      return ConsoleUtil.getMenuInput("Press 'C' to return: ", new char[] {'C'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int totalPages = (int) Math.ceil((double) total / pageSize);
    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);

    for (int i = startIndex; i <= endIndex; i++) {
      Reservation r = history.getEntry(i);
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(i),
            r.getReservationId(),
            r.getRoomType().name(),
            r.getStatus().name(),
            formatDateTime(r.getReservationTime())
          },
          settings);
    }
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.printf("Page %d / %d (Total Matches: %d)\n\n", currentPage, totalPages, total);
    System.out.println("[N] Next Page          [P] Previous Page      [C] Return\n");

    return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'N', 'P', 'C'});
  }

  private String formatDate(LocalDate date) {
    if (date == null) return "N/A";
    return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  }

  private String formatDateTime(LocalDateTime dateTime) {
    if (dateTime == null) return "N/A";
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
  }
}
