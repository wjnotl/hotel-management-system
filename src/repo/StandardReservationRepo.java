package repo;

import adt.ArrayList;
import adt.CircularArrayQueue;
import adt.ListInterface;
import adt.QueueInterface;
import entity.BookingSettings;
import entity.Guest;
import entity.Reservation;
import entity.Room;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.function.Supplier;

public class StandardReservationRepo {
  private final ReservationRepo reservationRepo;
  // A supplier, not the store itself, so the repo layer never depends on a controller package.
  private final Supplier<BookingSettings> settingsSource;

  // One WAITING line per type. Concrete because the repo owns the implementation and its capacity.
  private CircularArrayQueue<Reservation> luxuryQueue;
  private CircularArrayQueue<Reservation> suiteQueue;
  private CircularArrayQueue<Reservation> standardQueue;

  public StandardReservationRepo(
      ReservationRepo reservationRepo, Supplier<BookingSettings> settingsSource) {
    this.reservationRepo = reservationRepo;
    this.settingsSource = settingsSource;
    load();
  }

  private BookingSettings settings() {
    return settingsSource.get();
  }

  public int getHoldGraceMinutes() {
    return settings().getHoldGraceMinutes();
  }

  public int getHoldGraceMinutes(Room.RoomType roomType) {
    return settings().getHoldGraceMinutes(roomType);
  }

  public int getMaxStrikes() {
    return settings().getMaxStrikes();
  }

  private void load() {
    ListInterface<Reservation> waiting = new ArrayList<>();
    var masterList = reservationRepo.getAllReservations().filter(r -> !r.getIsVip());
    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation r = masterList.getEntry(i);
      if (r != null && r.getRoomType() != null && r.getStatus() == Reservation.Status.WAITING) {
        waiting.add(r);
      }
    }

    // A requeued guest keeps its masterList slot but gets a fresh time, so that order is wrong.
    waiting.sort(
        (a, b) -> {
          LocalDateTime first = a.getQueueArrivalTime();
          LocalDateTime second = b.getQueueArrivalTime();
          if (first == null && second == null) return 0;
          if (first == null) return -1;
          if (second == null) return 1;
          return first.compareTo(second);
        });

    this.luxuryQueue = buildQueue(waiting, Room.RoomType.LUXURY);
    this.suiteQueue = buildQueue(waiting, Room.RoomType.SUITE);
    this.standardQueue = buildQueue(waiting, Room.RoomType.STANDARD);

    for (int i = 1; i <= waiting.getNumberOfEntries(); i++) {
      Reservation r = waiting.getEntry(i);
      getQueueByRoomType(r.getRoomType()).enqueue(r);
    }
  }

  // With expansion off a smaller capacity would refuse guests already in line.
  private CircularArrayQueue<Reservation> buildQueue(
      ListInterface<Reservation> waiting, Room.RoomType roomType) {
    int alreadyWaiting = 0;
    for (int i = 1; i <= waiting.getNumberOfEntries(); i++) {
      Reservation r = waiting.getEntry(i);
      if (r != null && r.getRoomType() == roomType) {
        alreadyWaiting++;
      }
    }

    int capacity = Math.max(settings().getInitialQueueCapacity(roomType), alreadyWaiting);
    return new CircularArrayQueue<>(capacity, settings().isAllowQueueExpansion());
  }

  // Capacity and expansion are fixed at construction, so a change needs a rebuild.
  public void applySettings() {
    load();
  }

  private CircularArrayQueue<Reservation> queueFor(Room.RoomType roomType) {
    if (roomType == Room.RoomType.LUXURY) return luxuryQueue;
    if (roomType == Room.RoomType.SUITE) return suiteQueue;
    return standardQueue;
  }

  public QueueInterface<Reservation> getQueueByRoomType(Room.RoomType roomType) {
    return queueFor(roomType);
  }

  // Capacity belongs to the array, not the queue contract, so the repo reports it.
  public int getQueueCapacity(Room.RoomType roomType) {
    return queueFor(roomType).getCapacity();
  }

  public boolean canQueueExpand(Room.RoomType roomType) {
    return queueFor(roomType).canExpand();
  }

  // The house rule on line length, separate from array slots. Checked before any record.
  public boolean isLineAtPolicyLimit(Room.RoomType roomType) {
    BookingSettings config = settings();
    if (!config.isQueueLengthCapped(roomType)) return false;
    return queueFor(roomType).getNumberOfEntries() >= config.getMaxQueueLength(roomType);
  }

  // A line closes two ways: the length rule, or a full array that may not grow.
  public boolean isLineClosed(Room.RoomType roomType) {
    if (isLineAtPolicyLimit(roomType)) return true;

    CircularArrayQueue<Reservation> queue = queueFor(roomType);
    return queue.isFull() && !queue.canExpand();
  }

  // The limit that actually stopped the guest, so the refusal quotes the rule in force.
  public int getClosedLineLimit(Room.RoomType roomType) {
    return isLineAtPolicyLimit(roomType) ? getMaxQueueLength(roomType) : getQueueCapacity(roomType);
  }

  public int getMaxQueueLength() {
    return settings().getMaxQueueLength();
  }

  public int getMaxQueueLength(Room.RoomType roomType) {
    return settings().getMaxQueueLength(roomType);
  }

  // Walks the queue without disturbing it, so screens can page a line that stays a queue.
  public ListInterface<Reservation> snapshotQueue(Room.RoomType roomType) {
    ListInterface<Reservation> snapshot = new ArrayList<>();
    Iterator<Reservation> iterator = getQueueByRoomType(roomType).getIterator();
    while (iterator.hasNext()) {
      Reservation r = iterator.next();
      if (r != null) {
        snapshot.add(r);
      }
    }
    return snapshot;
  }

  public ListInterface<Reservation> getHoldsByRoomType(Room.RoomType roomType) {
    ListInterface<Reservation> holds = new ArrayList<>();
    var masterList = reservationRepo.getAllReservations().filter(r -> !r.getIsVip());
    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation r = masterList.getEntry(i);
      if (r != null
          && r.getRoomType() == roomType
          && r.getStatus() == Reservation.Status.ALLOCATED) {
        holds.add(r);
      }
    }

    holds.sort(
        (a, b) -> {
          LocalDateTime first = a.getAllocatedTime();
          LocalDateTime second = b.getAllocatedTime();
          if (first == null && second == null) return 0;
          if (first == null) return -1;
          if (second == null) return 1;
          return first.compareTo(second);
        });
    return holds;
  }

  // The queue is the only part that can refuse, so it is asked before the master list.
  public boolean addReservation(Reservation reservation) {
    if (reservation == null || reservation.getRoomType() == null) return false;

    if (reservation.getStatus() == Reservation.Status.WAITING) {
      QueueInterface<Reservation> queue = getQueueByRoomType(reservation.getRoomType());
      if (!queue.contains(reservation) && !queue.enqueue(reservation)) {
        return false;
      }
    }

    var masterList = reservationRepo.getAllReservations();
    if (!masterList.contains(reservation)) {
      masterList.add(reservation);
    }

    reservationRepo.save();
    return true;
  }

  public boolean updateReservation(Reservation updatedReservation) {
    return reservationRepo.updateReservation(updatedReservation);
  }

  // An advance booking becomes a body standing in the line.
  public boolean joinQueue(Reservation reservation) {
    if (reservation == null || reservation.getRoomType() == null) return false;

    QueueInterface<Reservation> queue = getQueueByRoomType(reservation.getRoomType());
    if (queue.contains(reservation)) return false;

    Reservation.Status previousStatus = reservation.getStatus();
    LocalDateTime previousArrival = reservation.getQueueArrivalTime();

    reservation.setStatus(Reservation.Status.WAITING);
    reservation.setQueueArrivalTime(LocalDateTime.now());

    if (!queue.enqueue(reservation)) {
      reservation.setStatus(previousStatus);
      reservation.setQueueArrivalTime(previousArrival);
      return false;
    }

    updateReservation(reservation);
    return true;
  }

  // A member skips the line, so the record goes straight to ALLOCATED, not through the front.
  public boolean allocateDirect(Reservation reservation) {
    if (reservation == null || reservation.getRoomType() == null) return false;

    LocalDateTime now = LocalDateTime.now();

    // An arrived booking is standing in the line, so it must leave before it can be held.
    getQueueByRoomType(reservation.getRoomType()).remove(reservation);

    if (reservation.getQueueArrivalTime() == null) {
      reservation.setQueueArrivalTime(now);
    }
    reservation.setStatus(Reservation.Status.ALLOCATED);
    reservation.setAllocatedTime(now);
    reservation.setAllocatedGraceMins(settings().getHoldGraceMinutes(reservation.getRoomType()));

    var masterList = reservationRepo.getAllReservations();
    if (!masterList.contains(reservation)) {
      masterList.add(reservation);
      reservationRepo.save();
      return true;
    }
    return updateReservation(reservation);
  }

  // Serving the front is the O(1) dequeue the FIFO queue exists for.
  public Reservation allocateFront(Room.RoomType roomType) {
    Reservation front = getQueueByRoomType(roomType).dequeue();
    if (front == null) return null;
    return stampAsHold(front);
  }

  // The screens authorise the skip, so the repo only guarantees the entry leaves the line. Reaching
  // into the middle costs O(n); the front stays O(1).
  public Reservation allocateQueued(Reservation reservation) {
    if (reservation == null || reservation.getRoomType() == null) return null;

    QueueInterface<Reservation> queue = getQueueByRoomType(reservation.getRoomType());

    if (reservation.equals(queue.peek())) {
      queue.dequeue();
    } else if (!queue.remove(reservation)) {
      return null;
    }

    return stampAsHold(reservation);
  }

  private Reservation stampAsHold(Reservation reservation) {
    reservation.setStatus(Reservation.Status.ALLOCATED);
    reservation.setAllocatedTime(LocalDateTime.now());

    // The window in force at allocation is stamped, so a later edit cannot expire a live hold.
    reservation.setAllocatedGraceMins(settings().getHoldGraceMinutes(reservation.getRoomType()));

    updateReservation(reservation);
    return reservation;
  }

  public boolean checkIn(Reservation reservation, int stayDays) {
    if (reservation == null) return false;

    String code = reservation.getConfirmationNumber();
    if (code == null || code.trim().isEmpty()) {
      reservation.setConfirmationNumber(generateConfirmationNumber());
    }

    reservation.setStatus(Reservation.Status.CHECKED_IN);
    reservation.setStayDays(stayDays);

    // allocatedTime would date the stay to when the room was called, a day early after midnight.
    reservation.setCheckInTime(LocalDateTime.now());
    return updateReservation(reservation);
  }

  public boolean cancelReservation(Reservation reservation) {
    if (reservation == null || reservation.getRoomType() == null) return false;

    getQueueByRoomType(reservation.getRoomType()).remove(reservation);
    reservation.setStatus(Reservation.Status.CANCELLED);
    return updateReservation(reservation);
  }

  // A hold closed by hand is a decision, not a timeout, so the guest is never requeued.
  public boolean markHoldNoShow(Reservation reservation, RoomRepo roomRepo) {
    if (reservation == null || reservation.getStatus() != Reservation.Status.ALLOCATED) {
      return false;
    }

    releaseHeldRoom(reservation, roomRepo);

    // allocatedTime is kept so the reports can still measure how long the room stood waiting.
    reservation.setStatus(Reservation.Status.NO_SHOW);
    return updateReservation(reservation);
  }

  public int closeQueue(Room.RoomType roomType) {
    QueueInterface<Reservation> queue = getQueueByRoomType(roomType);

    int closed = 0;
    Iterator<Reservation> iterator = queue.getIterator();
    while (iterator.hasNext()) {
      Reservation r = iterator.next();
      if (r != null) {
        r.setStatus(Reservation.Status.CANCELLED);
        closed++;
      }
    }

    queue.clear();
    reservationRepo.save();
    return closed;
  }

  // Only read when the one-booking-per-guest setting is on, which it is not by default.
  public Reservation findLiveReservationForGuest(String guestId) {
    return findLiveReservationForGuest(guestId, null);
  }

  // A null type asks across every line. A named type narrows to that one.
  public Reservation findLiveReservationForGuest(String guestId, Room.RoomType roomType) {
    if (guestId == null) return null;

    var masterList = reservationRepo.getAllReservations().filter(r -> !r.getIsVip());
    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation r = masterList.getEntry(i);
      if (r == null || !guestId.equalsIgnoreCase(r.getGuestId())) continue;
      if (roomType != null && r.getRoomType() != roomType) continue;

      if (r.getStatus() == Reservation.Status.WAITING
          || r.getStatus() == Reservation.Status.ALLOCATED) {
        return r;
      }
    }
    return null;
  }

  public LocalDate bookedArrivalDate(Reservation booking) {
    if (booking == null || booking.getExpectedArrivalTime() == null) return null;
    return booking.getExpectedArrivalTime().toLocalDate();
  }

  // A booking reserves one night, so any other day takes an unheld room. No date is let through.
  public boolean isArrivalDueToday(Reservation booking) {
    LocalDate due = bookedArrivalDate(booking);
    return due == null || due.equals(LocalDate.now());
  }

  // A booking is not a queue entry, so it does not block a walk-in; it is surfaced to be claimed.
  public Reservation findReservedBookingForGuest(String guestId) {
    if (guestId == null) return null;

    var masterList = reservationRepo.getAllReservations().filter(r -> !r.getIsVip());
    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation r = masterList.getEntry(i);
      if (r != null
          && guestId.equalsIgnoreCase(r.getGuestId())
          && r.getStatus() == Reservation.Status.RESERVED) {
        return r;
      }
    }
    return null;
  }

  // No event loop here, so a lapsed hold is resolved the next time a screen asks.
  public int sweepLapsedHolds(RoomRepo roomRepo, GuestRepo guestRepo) {
    LocalDateTime now = LocalDateTime.now();
    BookingSettings config = settings();
    int lapsed = 0;

    var masterList = reservationRepo.getAllReservations().filter(r -> !r.getIsVip());

    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation r = masterList.getEntry(i);
      if (r == null
          || r.getStatus() != Reservation.Status.ALLOCATED
          || r.getAllocatedTime() == null
          || r.getRoomType() == null) {
        continue;
      }

      // The window this hold was created under wins, so an edit never expires it early.
      int graceMinutes =
          (r.getAllocatedGraceMins() != null)
              ? r.getAllocatedGraceMins()
              : config.getHoldGraceMinutes(r.getRoomType());

      if (Duration.between(r.getAllocatedTime(), now).toMinutes() < graceMinutes) {
        continue;
      }

      releaseHeldRoom(r, roomRepo);

      Guest guest = (guestRepo != null) ? guestRepo.findById(r.getGuestId()) : null;
      int strikes = 0;
      if (guestRepo != null && guest != null) {
        strikes = guest.getStrikeCount() + 1;
        guest.setStrikeCount(strikes);
        guestRepo.updateGuest(guest);
      }

      boolean requeue =
          config.isRequeueOnLapse() && guest != null && strikes < config.getMaxStrikes();

      if (requeue) {
        r.setStatus(Reservation.Status.WAITING);
        r.setQueueArrivalTime(now);
        r.setAllocatedTime(null);
        r.setAllocatedGraceMins(null);
        r.setRoomNumber(null);

        // A refused re-entry would leave a WAITING record in no queue, so it is a no-show.
        if (!getQueueByRoomType(r.getRoomType()).enqueue(r)) {
          r.setStatus(Reservation.Status.NO_SHOW);
        }
      } else {
        // allocatedTime is kept so the reports can still measure how long this guest waited.
        r.setStatus(Reservation.Status.NO_SHOW);
      }

      lapsed++;
    }

    if (lapsed > 0) {
      reservationRepo.save();
    }
    return lapsed;
  }

  public Room findHeldRoom(Reservation reservation, RoomRepo roomRepo) {
    if (reservation == null || roomRepo == null || reservation.getRoomNumber() == null) {
      return null;
    }
    return roomRepo.findByRoomNumber(reservation.getRoomNumber());
  }

  private void releaseHeldRoom(Reservation reservation, RoomRepo roomRepo) {
    Room room = findHeldRoom(reservation, roomRepo);
    if (room != null) {
      room.setStatus(Room.Status.VACANT_CLEAN);

      room.setIsOccupied(false);
      roomRepo.updateRoom(room);
    }
  }

  // One counter for every screen. Three private copies drifted, and only one knew isOccupied.
  public int countFreeRooms(RoomRepo roomRepo, Room.RoomType roomType) {
    if (roomRepo == null || roomType == null) return 0;

    ListInterface<Room> rooms = roomRepo.getRoomList();
    int free = 0;
    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      if (room != null
          && room.getRoomType() == roomType
          && room.getStatus() == Room.Status.VACANT_CLEAN
          && !room.getIsOccupied()) {
        free++;
      }
    }
    return free;
  }

  // Counted off the master list, because the VIP module keeps a mirror it rebuilds itself.
  public int countVipWaiting(Room.RoomType roomType) {
    if (roomType == null) return 0;

    ListInterface<Reservation> all = reservationRepo.getAllReservations();
    int waiting = 0;
    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r != null
          && r.getIsVip()
          && r.getRoomType() == roomType
          && r.getStatus() == Reservation.Status.WAITING) {
        waiting++;
      }
    }
    return waiting;
  }

  // ===================== DATE AWARE AVAILABILITY =====================
  // Per date, not free right now. Bookings, occupied rooms, holds and VIP records share one stock.

  // An advance booking holds its room from the night before it arrives, so the room is standing
  // ready rather than being turned around while the guest waits at the counter. One helper, used by
  // both the sweep that counts other people's bookings and the wizard that checks a new one, so the
  // two can never disagree about which night a booking starts eating.
  public static final int ADVANCE_HOLD_LEAD_NIGHTS = 1;

  // A night already past cannot be held, so the eve is dropped rather than the start being dragged
  // forward, which would move an overdue booking's whole footprint into the future.
  public LocalDate getAdvanceHoldStartDate(LocalDate arrival) {
    if (arrival == null) return null;

    LocalDate eve = arrival.minusDays(ADVANCE_HOLD_LEAD_NIGHTS);
    return eve.isBefore(LocalDate.now()) ? arrival : eve;
  }

  // 1 normally, 0 when the eve has already passed. Every widened query adds it to both ends.
  public int getAdvanceHoldLeadNights(LocalDate arrival) {
    LocalDate start = getAdvanceHoldStartDate(arrival);
    if (start == null) return 0;
    return (int) (arrival.toEpochDay() - start.toEpochDay());
  }

  public int getTotalRoomsOfType(RoomRepo roomRepo, Room.RoomType roomType) {
    if (roomRepo == null || roomType == null) return 0;

    ListInterface<Room> rooms = roomRepo.getRoomList();
    int total = 0;
    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      if (room != null && room.getRoomType() == roomType) {
        total++;
      }
    }
    return total;
  }

  public int countCommittedOn(Room.RoomType roomType, LocalDate date) {
    if (roomType == null || date == null) return 0;
    return countCommittedOverWindow(roomType, date, 1)[0];
  }

  // One sweep of the master list for a whole run of nights. Asking night by night re-reads every
  // reservation once per night, so a forecast over d nights costs O(r + d) here rather than
  // O(r * d), and the single date question above is just the window of one.
  public int[] countCommittedOverWindow(Room.RoomType roomType, LocalDate from, int nights) {
    int[] committed = new int[Math.max(0, nights)];
    if (roomType == null || from == null || nights <= 0) return committed;

    boolean turnover = settings().isCheckoutDayReusable();
    LocalDate windowEnd = from.plusDays(nights);
    ListInterface<Reservation> all = reservationRepo.getAllReservations();

    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r == null || r.getRoomType() != roomType) continue;

      LocalDate start = r.getOccupancyStartDate();
      LocalDate end = r.getOccupancyEndDate();
      if (start == null || end == null || !start.isBefore(end)) continue;

      // Padded after the guard above, never before it: a CHECKED_OUT, NO_SHOW or CANCELLED record
      // reports an empty interval, and widening one would turn every closed record into a night.
      if (!r.getIsVip() && r.getStatus() == Reservation.Status.RESERVED) {
        start = getAdvanceHoldStartDate(start);
      }

      // With same day turnover off the room is not resold that day, so stays pad by a night.
      if (!turnover) {
        end = end.plusDays(1);
      }

      if (!start.isBefore(windowEnd) || !from.isBefore(end)) continue;

      int firstNight = (int) Math.max(0, start.toEpochDay() - from.toEpochDay());
      int lastNight = (int) Math.min(nights - 1L, end.toEpochDay() - from.toEpochDay() - 1);
      for (int night = firstNight; night <= lastNight; night++) {
        committed[night]++;
      }
    }
    return committed;
  }

  public int countAvailableOn(RoomRepo roomRepo, Room.RoomType roomType, LocalDate date) {
    int free = getTotalRoomsOfType(roomRepo, roomType) - countCommittedOn(roomType, date);
    return Math.max(0, free);
  }

  // The tightest night decides the whole stay, so the worst date is what comes back.
  public int countAvailableAcross(
      RoomRepo roomRepo, Room.RoomType roomType, LocalDate arrival, int nights) {
    if (arrival == null || nights <= 0) return 0;

    int lowest = getTotalRoomsOfType(roomRepo, roomType);
    for (int night = 0; night < nights; night++) {
      int free = countAvailableOn(roomRepo, roomType, arrival.plusDays(night));
      if (free < lowest) {
        lowest = free;
      }
    }
    return Math.max(0, lowest);
  }

  public boolean canBook(RoomRepo roomRepo, Room.RoomType roomType, LocalDate arrival, int nights) {
    if (!settings().isBlockOverbooking()) return true;
    return countAvailableAcross(roomRepo, roomType, arrival, nights) > 0;
  }

  // The first night with nothing left, so a refusal can name the date that caused it.
  public LocalDate findFirstFullDate(
      RoomRepo roomRepo, Room.RoomType roomType, LocalDate arrival, int nights) {
    if (arrival == null || nights <= 0) return null;

    for (int night = 0; night < nights; night++) {
      LocalDate date = arrival.plusDays(night);
      if (countAvailableOn(roomRepo, roomType, date) <= 0) {
        return date;
      }
    }
    return null;
  }

  public int findLongestBookableStay(
      RoomRepo roomRepo, Room.RoomType roomType, LocalDate arrival, int cap) {
    if (arrival == null || cap <= 0) return 0;

    int nights = 0;
    while (nights < cap && countAvailableOn(roomRepo, roomType, arrival.plusDays(nights)) > 0) {
      nights++;
    }
    return nights;
  }

  // Future bookings still hold stock back, so walk-in screens subtract them.
  public int countReservedArrivingOn(Room.RoomType roomType, LocalDate date) {
    if (roomType == null || date == null) return 0;
    return countArrivalsOverWindow(roomType, date, 1)[0];
  }

  // What a walk-in tonight is really competing with. A booking arriving tomorrow holds its room
  // from tonight, so the counter may not sell it either. One sweep covers both nights.
  public int countReservedHoldingOn(Room.RoomType roomType, LocalDate date) {
    if (roomType == null || date == null) return 0;

    int[] due = countArrivalsOverWindow(roomType, date, 1 + ADVANCE_HOLD_LEAD_NIGHTS);
    int held = 0;
    for (int i = 0; i < due.length; i++) {
      held += due[i];
    }
    return held;
  }

  // The window twin of the count above, for the same reason.
  public int[] countArrivalsOverWindow(Room.RoomType roomType, LocalDate from, int nights) {
    int[] arrivals = new int[Math.max(0, nights)];
    if (roomType == null || from == null || nights <= 0) return arrivals;

    ListInterface<Reservation> all = reservationRepo.getAllReservations();
    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r == null
          || r.getRoomType() != roomType
          || r.getStatus() != Reservation.Status.RESERVED
          || r.getExpectedArrivalTime() == null) {
        continue;
      }

      long night = r.getExpectedArrivalTime().toLocalDate().toEpochDay() - from.toEpochDay();
      if (night >= 0 && night < nights) {
        arrivals[(int) night]++;
      }
    }
    return arrivals;
  }

  public String generateReservationId() {
    return reservationRepo.generateReservationId();
  }

  public String generateConfirmationNumber() {
    return reservationRepo.generateConfirmationNumber();
  }

  public ListInterface<Reservation> getAllReservations() {
    return reservationRepo.getAllReservations().filter(r -> !r.getIsVip());
  }
}
