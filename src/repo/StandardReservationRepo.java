package repo;

import adt.ArrayList;
import adt.CircularArrayQueue;
import adt.ListInterface;
import adt.QueueInterface;
import entity.Guest;
import entity.Reservation;
import entity.Room;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Iterator;
import util.BinaryFileUtil;
import util.NumberUtil;

public class StandardReservationRepo {
  public static final int GRACE_MINUTES = 15;
  public static final int MAX_STRIKES = 3;

  private final BinaryFileUtil<ListInterface<Reservation>> fileUtil;
  private ListInterface<Reservation> masterList;

  // Small enough that the array actually fills and doubles during a normal shift, which is
  // what makes the circular growth visible on screen instead of theoretical.
  private static final int INITIAL_QUEUE_CAPACITY = 8;

  // One waiting line per room type. Only WAITING reservations live here.
  // Held as the concrete type because the repository is what chooses the implementation and
  // reports its capacity. Every caller outside this class only ever sees QueueInterface.
  private CircularArrayQueue<Reservation> luxuryQueue;
  private CircularArrayQueue<Reservation> suiteQueue;
  private CircularArrayQueue<Reservation> standardQueue;

  public StandardReservationRepo() {
    this.fileUtil = new BinaryFileUtil<>("standard_reservations.dat");
    load();
  }

  private void load() {
    this.masterList = fileUtil.retrieveFromFile();
    if (this.masterList == null) {
      this.masterList = new ArrayList<>();
    }

    this.luxuryQueue = new CircularArrayQueue<>(INITIAL_QUEUE_CAPACITY);
    this.suiteQueue = new CircularArrayQueue<>(INITIAL_QUEUE_CAPACITY);
    this.standardQueue = new CircularArrayQueue<>(INITIAL_QUEUE_CAPACITY);

    ListInterface<Reservation> waiting = new ArrayList<>();
    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation r = masterList.getEntry(i);
      if (r != null && r.getRoomType() != null && r.getStatus() == Reservation.Status.WAITING) {
        waiting.add(r);
      }
    }

    // A guest sent back to the rear keeps its original slot in masterList but gets a fresh
    // queueArrivalTime, so replaying masterList order would rebuild the line wrong.
    waiting.sort(
        (a, b) -> {
          LocalDateTime first = a.getQueueArrivalTime();
          LocalDateTime second = b.getQueueArrivalTime();
          if (first == null && second == null) return 0;
          if (first == null) return -1;
          if (second == null) return 1;
          return first.compareTo(second);
        });

    for (int i = 1; i <= waiting.getNumberOfEntries(); i++) {
      Reservation r = waiting.getEntry(i);
      getQueueByRoomType(r.getRoomType()).enqueue(r);
    }
  }

  private void save() {
    fileUtil.saveToFile(masterList);
  }

  private CircularArrayQueue<Reservation> queueFor(Room.RoomType roomType) {
    if (roomType == Room.RoomType.LUXURY) return luxuryQueue;
    if (roomType == Room.RoomType.SUITE) return suiteQueue;
    return standardQueue;
  }

  public QueueInterface<Reservation> getQueueByRoomType(Room.RoomType roomType) {
    return queueFor(roomType);
  }

  // Capacity belongs to the array implementation, not to the queue contract, so the interface
  // deliberately does not carry it and the repository reports it instead.
  public int getQueueCapacity(Room.RoomType roomType) {
    return queueFor(roomType).getCapacity();
  }

  public boolean canQueueExpand(Room.RoomType roomType) {
    return queueFor(roomType).canExpand();
  }

  public ListInterface<Reservation> getAllReservations() {
    return masterList;
  }

  // Walks the queue front to back without disturbing it, so screens and reports can page
  // through the line while the queue itself stays a queue.
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

  public Reservation findById(String reservationId) {
    if (reservationId == null) return null;
    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation r = masterList.getEntry(i);
      if (r != null && reservationId.equalsIgnoreCase(r.getReservationId())) {
        return r;
      }
    }
    return null;
  }

  public Reservation findByConfirmationNumber(String confirmationNumber) {
    if (confirmationNumber == null) return null;
    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation r = masterList.getEntry(i);
      if (r != null && confirmationNumber.equalsIgnoreCase(r.getConfirmationNumber())) {
        return r;
      }
    }
    return null;
  }

  public void addReservation(Reservation reservation) {
    if (reservation == null || reservation.getRoomType() == null) return;

    if (!masterList.contains(reservation)) {
      masterList.add(reservation);
    }

    if (reservation.getStatus() == Reservation.Status.WAITING) {
      QueueInterface<Reservation> queue = getQueueByRoomType(reservation.getRoomType());
      if (!queue.contains(reservation)) {
        queue.enqueue(reservation);
      }
    }

    save();
  }

  public boolean updateReservation(Reservation updatedReservation) {
    if (updatedReservation == null) return false;

    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation existing = masterList.getEntry(i);
      if (existing != null && existing.equals(updatedReservation)) {
        masterList.replace(i, updatedReservation);
        save();
        return true;
      }
    }
    return false;
  }

  // An advance booking becomes a body standing in the line.
  public boolean joinQueue(Reservation reservation) {
    if (reservation == null || reservation.getRoomType() == null) return false;

    QueueInterface<Reservation> queue = getQueueByRoomType(reservation.getRoomType());
    if (queue.contains(reservation)) return false;

    reservation.setStatus(Reservation.Status.WAITING);
    reservation.setQueueArrivalTime(LocalDateTime.now());
    if (!queue.enqueue(reservation)) return false;

    updateReservation(reservation);
    return true;
  }

  // A loyalty member standing at the desk is served without ever joining the line, so the
  // record jumps straight to ALLOCATED. Routing this through joinQueue then allocateFront
  // would be wrong: allocateFront serves whoever is at the front, not this guest.
  public boolean allocateDirect(Reservation reservation) {
    if (reservation == null || reservation.getRoomType() == null) return false;

    LocalDateTime now = LocalDateTime.now();

    // An advance booking that was already marked as arrived is standing in the line, so it
    // has to leave the queue before it can be held, or the same record would be served twice.
    getQueueByRoomType(reservation.getRoomType()).remove(reservation);

    if (reservation.getQueueArrivalTime() == null) {
      reservation.setQueueArrivalTime(now);
    }
    reservation.setStatus(Reservation.Status.ALLOCATED);
    reservation.setAllocatedTime(now);

    if (!masterList.contains(reservation)) {
      masterList.add(reservation);
      save();
      return true;
    }
    return updateReservation(reservation);
  }

  public Reservation allocateFront(Room.RoomType roomType) {
    Reservation front = getQueueByRoomType(roomType).dequeue();
    if (front == null) return null;

    front.setStatus(Reservation.Status.ALLOCATED);
    front.setAllocatedTime(LocalDateTime.now());
    updateReservation(front);
    return front;
  }

  public boolean checkIn(Reservation reservation, int stayDays) {
    if (reservation == null) return false;

    reservation.setStatus(Reservation.Status.CHECKED_IN);
    reservation.setStayDays(stayDays);
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
    save();
    return closed;
  }

  // A console app has no event loop, so a lapsed hold is resolved the next time a screen
  // asks for the data rather than by a background timer that dies with the process.
  @SuppressWarnings("null")
  public int sweepLapsedHolds(RoomRepo roomRepo, GuestRepo guestRepo) {
    LocalDateTime now = LocalDateTime.now();
    int lapsed = 0;

    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation r = masterList.getEntry(i);
      if (r == null
          || r.getStatus() != Reservation.Status.ALLOCATED
          || r.getAllocatedTime() == null
          || r.getRoomType() == null) {
        continue;
      }

      if (Duration.between(r.getAllocatedTime(), now).toMinutes() < GRACE_MINUTES) {
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

      if (guest == null || strikes >= MAX_STRIKES) {
        // allocatedTime is kept so the reports can still measure how long this guest waited
        // before the room was called for them.
        r.setStatus(Reservation.Status.NO_SHOW);
      } else {
        r.setStatus(Reservation.Status.WAITING);
        r.setQueueArrivalTime(now);
        r.setAllocatedTime(null);
        getQueueByRoomType(r.getRoomType()).enqueue(r);
      }

      lapsed++;
    }

    if (lapsed > 0) {
      save();
    }
    return lapsed;
  }

  public Room findHeldRoom(Reservation reservation, RoomRepo roomRepo) {
    if (reservation == null || roomRepo == null || reservation.getConfirmationNumber() == null) {
      return null;
    }

    ListInterface<Room> rooms = roomRepo.getRoomList();
    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      if (room != null
          && reservation.getConfirmationNumber().equals(room.getReservationConfirmationNumber())) {
        return room;
      }
    }
    return null;
  }

  private void releaseHeldRoom(Reservation reservation, RoomRepo roomRepo) {
    Room room = findHeldRoom(reservation, roomRepo);
    if (room == null) return;

    room.setStatus(Room.Status.VACANT_CLEAN);
    room.setReservationConfirmationNumber(null);
    roomRepo.updateRoom(room);
  }

  // The VIP store mints "RES-" ids, so a "STD-" prefix cannot be handed out twice.
  // Reservation.equals compares the id alone and the queue routes contains, getPosition
  // and remove through it, so a duplicate would make the queue act on the wrong record.
  public String generateReservationId() {
    int maxId = 10000;
    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation r = masterList.getEntry(i);
      if (r != null && r.getReservationId() != null && r.getReservationId().startsWith("STD-")) {
        try {
          int num = Integer.parseInt(r.getReservationId().substring(4));
          if (num > maxId) {
            maxId = num;
          }
        } catch (NumberFormatException ignored) {
        }
      }
    }
    return "STD-" + (maxId + 1);
  }

  // Rooms link back to a booking by confirmation number only, so a code shared with the VIP
  // store would make front-desk lookups resolve to the wrong guest.
  public String generateConfirmationNumber(VipReservationRepo vipReservationRepo) {
    while (true) {
      String code = NumberUtil.generateDigitPin(8);
      if (findByConfirmationNumber(code) == null && !existsInVipStore(code, vipReservationRepo)) {
        return code;
      }
    }
  }

  private boolean existsInVipStore(String code, VipReservationRepo vipReservationRepo) {
    if (vipReservationRepo == null) return false;

    ListInterface<Reservation> vipList = vipReservationRepo.getAllReservations();
    if (vipList == null) return false;

    for (int i = 1; i <= vipList.getNumberOfEntries(); i++) {
      Reservation r = vipList.getEntry(i);
      if (r != null && code.equalsIgnoreCase(r.getConfirmationNumber())) {
        return true;
      }
    }
    return false;
  }
}
