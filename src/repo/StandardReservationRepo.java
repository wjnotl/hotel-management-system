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

public class StandardReservationRepo {
  private final ReservationRepo reservationRepo;
  private final BookingSettingsRepo bookingSettingsRepo;

  // One waiting line per room type. Only WAITING reservations live here.
  // Held as the concrete type because the repository is what chooses the
  // implementation and reports its capacity. Every caller outside this class only
  // ever sees QueueInterface.
  private CircularArrayQueue<Reservation> luxuryQueue;
  private CircularArrayQueue<Reservation> suiteQueue;
  private CircularArrayQueue<Reservation> standardQueue;

  public StandardReservationRepo(
      ReservationRepo reservationRepo, BookingSettingsRepo bookingSettingsRepo) {
    this.reservationRepo = reservationRepo;
    this.bookingSettingsRepo = bookingSettingsRepo;
    load();
  }

  private BookingSettings settings() {
    return bookingSettingsRepo.getSettings();
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

    // A guest sent back to the rear keeps its original slot in masterList but gets
    // a fresh queueArrivalTime, so replaying masterList order would rebuild the line wrong.
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

  // The configured capacity is a starting size, not a licence to drop people already in line.
  // With expansion switched off a smaller configured value would make enqueue refuse the
  // guests being restored, so the array is opened wide enough to hold them first.
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

  // Capacity and the expansion flag are fixed at construction, so a settings change only
  // reaches the live lines by rebuilding them from the master list.
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

  // Capacity belongs to the array implementation, not to the queue contract, so
  // the interface deliberately does not carry it and the repository reports it instead.
  public int getQueueCapacity(Room.RoomType roomType) {
    return queueFor(roomType).getCapacity();
  }

  public boolean canQueueExpand(Room.RoomType roomType) {
    return queueFor(roomType).canExpand();
  }

  // The house rule about how long a line may get, which is separate from how many array slots
  // the queue currently owns. A capped line refuses the guest before any record is created.
  public boolean isLineAtPolicyLimit(Room.RoomType roomType) {
    BookingSettings config = settings();
    if (!config.isQueueLengthCapped(roomType)) return false;
    return queueFor(roomType).getNumberOfEntries() >= config.getMaxQueueLength(roomType);
  }

  public int getMaxQueueLength() {
    return settings().getMaxQueueLength();
  }

  public int getMaxQueueLength(Room.RoomType roomType) {
    return settings().getMaxQueueLength(roomType);
  }

  // Walks the queue front to back without disturbing it, so screens and reports
  // can page through the line while the queue itself stays a queue.
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

  // The queue is the only part that can refuse the entry, so it is asked first. Adding to the
  // master list before that would leave a WAITING record that is in no line at all.
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

  // A loyalty member standing at the desk is served without ever joining the
  // line, so the record jumps straight to ALLOCATED. Routing this through joinQueue then
  // allocateFront would be wrong: allocateFront serves whoever is at the front, not this guest.
  public boolean allocateDirect(Reservation reservation) {
    if (reservation == null || reservation.getRoomType() == null) return false;

    LocalDateTime now = LocalDateTime.now();

    // An advance booking that was already marked as arrived is standing in the
    // line, so it has to leave the queue before it can be held, or the same record would be
    // served twice.
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

  // Serving the front is the O(1) dequeue the FIFO queue exists for, so the ordinary path never
  // degrades into a scan.
  public Reservation allocateFront(Room.RoomType roomType) {
    Reservation front = getQueueByRoomType(roomType).dequeue();
    if (front == null) return null;
    return stampAsHold(front);
  }

  // Serving a row other than the front is a deliberate act the screens make the clerk authorise,
  // so the repository does not police the position. It only guarantees that whoever is served
  // leaves the line and is stamped as a hold, because a queue entry must not survive being
  // allocated. Skipping ahead costs the O(n) removal that reaching into the middle of a queue
  // always costs; the front is still taken by the O(1) dequeue.
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

    // The grace window in force at the moment of allocation is stamped on the record, so a
    // later settings change re-times only the holds created after it rather than silently
    // expiring one that is already running.
    reservation.setAllocatedGraceMins(settings().getHoldGraceMinutes(reservation.getRoomType()));

    updateReservation(reservation);
    return reservation;
  }

  public boolean checkIn(Reservation reservation, int stayDays) {
    if (reservation == null) return false;

    reservation.setStatus(Reservation.Status.CHECKED_IN);
    reservation.setStayDays(stayDays);

    // Stamped so the availability calendar knows which night this stay actually began on. Reading
    // it off allocatedTime instead would date a stay to the moment the room was called, which is
    // the previous day for anybody checked in just after midnight.
    reservation.setCheckInTime(LocalDateTime.now());
    return updateReservation(reservation);
  }

  public boolean cancelReservation(Reservation reservation) {
    if (reservation == null || reservation.getRoomType() == null) return false;

    getQueueByRoomType(reservation.getRoomType()).remove(reservation);
    reservation.setStatus(Reservation.Status.CANCELLED);
    return updateReservation(reservation);
  }

  // End of business cycle: anyone still standing in the line is turned away.
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

  // A guest may hold only one live standard booking at a time. Checking every room type
  // rather than only the line being served is what stops one person occupying the LUXURY and
  // SUITE lines at once, or being queued again while a room is already held for them.
  public Reservation findLiveReservationForGuest(String guestId) {
    if (guestId == null) return null;

    var masterList = reservationRepo.getAllReservations().filter(r -> !r.getIsVip());
    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation r = masterList.getEntry(i);
      if (r == null || !guestId.equalsIgnoreCase(r.getGuestId())) continue;

      if (r.getStatus() == Reservation.Status.WAITING
          || r.getStatus() == Reservation.Status.ALLOCATED) {
        return r;
      }
    }
    return null;
  }

  // An advance booking is not a live queue entry, so it does not block a walk-in. It is
  // surfaced instead, because marking that booking as arrived is almost always what the desk
  // meant to do rather than opening a second reservation for the same person.
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

  // A console app has no event loop, so a lapsed hold is resolved the next time a
  // screen asks for the data rather than by a background timer that dies with the process.
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

      // The window this hold was created under wins over the current setting, so editing the
      // grace period never retroactively expires a room a guest is still walking towards.
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
      if (guest != null) {
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

        // A line that refuses the re-entry would otherwise leave a WAITING record standing in
        // no queue at all, so the booking is closed as a no-show instead.
        if (!getQueueByRoomType(r.getRoomType()).enqueue(r)) {
          r.setStatus(Reservation.Status.NO_SHOW);
        }
      } else {
        // allocatedTime is kept so the reports can still measure how long this guest waited
        // before the room was called for them.
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
      roomRepo.updateRoom(room);
    }
  }

  // ===================== DATE AWARE AVAILABILITY =====================
  // "Is a room free" used to mean "is one VACANT & CLEAN this second", which is the right question
  // for a walk-in and the wrong one for a booking three weeks out. These methods answer it per
  // date instead, over one shared calendar: an advance booking, a guest already in a room and a
  // hold that has been called all consume the same physical stock, so all three are counted. VIP
  // reservations are counted too, because a VIP in room 801 makes 801 just as unavailable.

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

  // How many rooms of this type are spoken for on one calendar date.
  public int countCommittedOn(Room.RoomType roomType, LocalDate date) {
    if (roomType == null || date == null) return 0;

    boolean turnover = settings().isCheckoutDayReusable();
    ListInterface<Reservation> all = reservationRepo.getAllReservations();

    int committed = 0;
    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r == null || r.getRoomType() != roomType) continue;

      LocalDate start = r.getOccupancyStartDate();
      LocalDate end = r.getOccupancyEndDate();
      if (start == null || end == null || !start.isBefore(end)) continue;

      // With same day turnover switched off the room is not resold on the day it is vacated, so
      // every stay is padded by the extra night the housekeeping rule reserves for it.
      if (!turnover) {
        end = end.plusDays(1);
      }

      if (!start.isAfter(date) && date.isBefore(end)) {
        committed++;
      }
    }
    return committed;
  }

  public int countAvailableOn(RoomRepo roomRepo, Room.RoomType roomType, LocalDate date) {
    int free = getTotalRoomsOfType(roomRepo, roomType) - countCommittedOn(roomType, date);
    return Math.max(0, free);
  }

  // The tightest night in the requested span decides whether the whole stay can be promised, so
  // the worst date is what comes back rather than the first or the average.
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

  // The first night in the span that has nothing left, so a refusal can name the date that caused
  // it instead of only saying the stay does not fit.
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

  // The longest stay that can still be promised from this arrival date, capped by the house limit.
  public int findLongestBookableStay(
      RoomRepo roomRepo, Room.RoomType roomType, LocalDate arrival, int cap) {
    if (arrival == null || cap <= 0) return 0;

    int nights = 0;
    while (nights < cap && countAvailableOn(roomRepo, roomType, arrival.plusDays(nights)) > 0) {
      nights++;
    }
    return nights;
  }

  // Advance bookings for a date that has not arrived yet still hold stock back from the counter,
  // so the walk-in screens subtract them from what they offer rather than handing out a room that
  // was already promised to somebody driving in this evening.
  public int countReservedArrivingOn(Room.RoomType roomType, LocalDate date) {
    if (roomType == null || date == null) return 0;

    ListInterface<Reservation> all = reservationRepo.getAllReservations();
    int count = 0;
    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r != null
          && r.getRoomType() == roomType
          && r.getStatus() == Reservation.Status.RESERVED
          && r.getExpectedArrivalTime() != null
          && r.getExpectedArrivalTime().toLocalDate().isEqual(date)) {
        count++;
      }
    }
    return count;
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
