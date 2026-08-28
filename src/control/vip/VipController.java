package control.vip;

import adt.PriorityQueueInterface;
import entity.AllocationEntry;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import entity.VipSystemConfig;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ScheduledExecutorService;
import repo.AllocationRepo;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.RoomRepo;
import repo.VipReservationRepo;
import repo.VipSystemConfigRepo;
import util.ConsoleUtil;
import util.TaskSchedulerUtil;
import view.vip.VipView;

public class VipController {
  private static ScheduledExecutorService boilingScheduler = null;
  private static ScheduledExecutorService expirationScheduler = null;

  private final VipView vipView = new VipView();
  private final AllocationRepo allocationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;
  private final RoomRepo roomRepo;
  private final VipReservationRepo vipReservationRepo;
  private final VipSystemConfigRepo vipSystemConfigRepo;

  public VipController(
      AllocationRepo allocationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      RoomRepo roomRepo,
      VipReservationRepo vipReservationRepo,
      VipSystemConfigRepo vipSystemConfigRepo) {
    this.allocationRepo = allocationRepo;
    this.guestRepo = guestRepo;
    this.memberRepo = memberRepo;
    this.roomRepo = roomRepo;
    this.vipReservationRepo = vipReservationRepo;
    this.vipSystemConfigRepo = vipSystemConfigRepo;
  }

  public void start() {
    while (true) {
      try {
        String choice = vipView.displayMenu();

        if ("1".equals(choice)) {
          new VipManageWaitlistController(
                  vipReservationRepo,
                  guestRepo,
                  memberRepo,
                  roomRepo,
                  allocationRepo,
                  vipSystemConfigRepo)
              .startWaitlistManagement();
        } else if ("2".equals(choice)) {
          new VipManageAllocationController(
                  allocationRepo,
                  vipReservationRepo,
                  guestRepo,
                  memberRepo,
                  roomRepo,
                  vipSystemConfigRepo)
              .startAllocationManagement();
        } else if ("3".equals(choice)) {
          new VipReportController(vipReservationRepo, guestRepo, memberRepo, vipSystemConfigRepo)
              .startReportManagement();
        } else if ("4".equals(choice)) {
          new VipSettingsController(
                  vipSystemConfigRepo,
                  vipReservationRepo,
                  guestRepo,
                  memberRepo,
                  allocationRepo,
                  roomRepo)
              .startSettingsManagement();
        } else if ("5".equals(choice)) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  public static void scheduleNextBoilingTask(
      VipReservationRepo vipReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      VipSystemConfigRepo configRepo) {
    if (boilingScheduler != null && !boilingScheduler.isShutdown()) {
      boilingScheduler.shutdownNow();
      boilingScheduler = null;
    }

    if (vipReservationRepo == null || configRepo == null) return;
    VipSystemConfig config = configRepo.getConfig();
    var vipReservationList = vipReservationRepo.getAllReservations();

    if (config == null || vipReservationList == null || vipReservationList.isEmpty()) return;

    long now = System.currentTimeMillis();

    var waitingList =
        vipReservationList.filter(
            r ->
                r != null
                    && r.getStatus() == Reservation.Status.WAITING
                    && !r.getIsBoiling()
                    && r.getQueueArrivalTime() != null);

    if (waitingList.isEmpty()) return;

    // Process overdue boiling targets
    for (Reservation reservation : waitingList) {
      Guest g = (guestRepo != null) ? guestRepo.findById(reservation.getGuestId()) : null;
      Member m =
          (g != null && g.getMemberId() != null && memberRepo != null)
              ? memberRepo.findById(g.getMemberId())
              : null;
      Member.LoyaltyTier tier = (m != null) ? m.getTier() : null;

      int limitMins = config.getBoilingLimitMins(tier);

      long targetMs =
          reservation
                  .getQueueArrivalTime()
                  .atZone(ZoneId.systemDefault())
                  .toInstant()
                  .toEpochMilli()
              + (limitMins * 60 * 1000L);

      if (targetMs <= now) {
        reservation.setBoiling(true);
        int newScore = vipReservationRepo.calculatePriorityScore(reservation, g, m, config);
        reservation.setPriorityScore(newScore);
        vipReservationRepo.updateReservation(reservation);

        PriorityQueueInterface<Reservation> heap =
            vipReservationRepo.getHeapByRoomType(reservation.getRoomType());
        if (heap != null) {
          heap.updatePriority(reservation);
        }
      }
    }

    // Find the earliest FUTURE boiling target
    Reservation earliestFutureRes = null;
    long earliestFutureTargetMs = Long.MAX_VALUE;

    for (Reservation reservation : waitingList) {
      if (reservation.getIsBoiling()) continue; // Skip if it just became boiling above

      Guest g = (guestRepo != null) ? guestRepo.findById(reservation.getGuestId()) : null;
      Member m =
          (g != null && g.getMemberId() != null && memberRepo != null)
              ? memberRepo.findById(g.getMemberId())
              : null;
      Member.LoyaltyTier tier = (m != null) ? m.getTier() : null;

      int limitMins = config.getBoilingLimitMins(tier);

      long targetMs =
          reservation
                  .getQueueArrivalTime()
                  .atZone(ZoneId.systemDefault())
                  .toInstant()
                  .toEpochMilli()
              + (limitMins * 60 * 1000L);

      if (targetMs > now && targetMs < earliestFutureTargetMs) {
        earliestFutureTargetMs = targetMs;
        earliestFutureRes = reservation;
      }
    }

    if (earliestFutureRes == null) return;

    final Reservation targetRes = earliestFutureRes;
    long delayMs = Math.max(1, earliestFutureTargetMs - now);

    boilingScheduler =
        TaskSchedulerUtil.scheduleOnce(
            delayMs,
            () -> {
              try {
                if (targetRes.getStatus() == Reservation.Status.WAITING
                    && !targetRes.getIsBoiling()) {
                  targetRes.setBoiling(true);

                  Guest g = (guestRepo != null) ? guestRepo.findById(targetRes.getGuestId()) : null;
                  Member m =
                      (g != null && g.getMemberId() != null && memberRepo != null)
                          ? memberRepo.findById(g.getMemberId())
                          : null;

                  int newScore = vipReservationRepo.calculatePriorityScore(targetRes, g, m, config);
                  targetRes.setPriorityScore(newScore);
                  vipReservationRepo.updateReservation(targetRes);

                  PriorityQueueInterface<Reservation> heap =
                      vipReservationRepo.getHeapByRoomType(targetRes.getRoomType());
                  if (heap != null) {
                    heap.updatePriority(targetRes);
                  }
                }
              } catch (Exception ignored) {
              } finally {
                scheduleNextBoilingTask(vipReservationRepo, guestRepo, memberRepo, configRepo);
              }
            });
  }

  public static void startMidnightStrikeResetScheduler(
      GuestRepo guestRepo, VipSystemConfigRepo configRepo) {
    if (guestRepo == null || configRepo == null) return;

    // Check if midnight passed while system was offline/shutdown
    LocalDate lastResetDate = configRepo.getConfig().getLastStrikeResetDate();
    LocalDate todayDate = LocalDate.now();

    if (lastResetDate == null || !todayDate.equals(lastResetDate)) {
      guestRepo.resetAllGuestStrikes(configRepo);
    }

    // Schedule recurring timer for the next midnight
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
    long initialDelayMs = Math.max(1, Duration.between(now, nextMidnight).toMillis());
    long twentyFourHoursMs = 24 * 60 * 60 * 1000L;

    TaskSchedulerUtil.scheduleEvery(
        initialDelayMs,
        twentyFourHoursMs,
        () -> {
          try {
            guestRepo.resetAllGuestStrikes(configRepo);
          } catch (Exception e) {
          }
        });
  }

  public static void scheduleNextAutoExpirationTask(
      AllocationRepo allocationRepo,
      RoomRepo roomRepo,
      VipReservationRepo vipReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      VipSystemConfigRepo configRepo) {
    if (expirationScheduler != null && !expirationScheduler.isShutdown()) {
      expirationScheduler.shutdownNow();
      expirationScheduler = null;
    }

    if (allocationRepo == null) return;
    var allocationList = allocationRepo.getAllocationList();
    if (allocationList == null || allocationList.getNumberOfEntries() == 0) return;

    long now = System.currentTimeMillis();

    // Process any overdue expired allocations
    var overdueList = allocationList.filter(e -> e != null && e.getExpirationTimestamp() <= now);
    if (!overdueList.isEmpty()) {
      for (AllocationEntry entry : overdueList) {
        allocationRepo.removeAllocationEntry(entry);

        Room room = roomRepo.findByRoomNumber(entry.getAssignedRoomNumber());
        if (room != null) {
          room.setStatus(Room.Status.VACANT_CLEAN);
          room.setIsOccupied(false);
          roomRepo.updateRoom(room);
        }

        Reservation res = vipReservationRepo.findById(entry.getReservationId());
        if (res != null) {
          Guest guest = guestRepo.findById(res.getGuestId());
          if (guest != null) {
            Member member =
                (guest.getMemberId() != null) ? memberRepo.findById(guest.getMemberId()) : null;

            VipSystemConfig config = configRepo.getConfig();
            Member.LoyaltyTier tier = (member != null) ? member.getTier() : null;
            int maxStrikes = config.getMaxStrikes(tier);

            guest.setStrikeCount(guest.getStrikeCount() + 1);
            guestRepo.updateGuest(guest);

            res.setStatus(Reservation.Status.NO_SHOW);
            res.setStrikeCountSnapshot(guest.getStrikeCount());
            vipReservationRepo.updateReservation(res);

            if (guest.getStrikeCount() <= maxStrikes) {
              String newResId = vipReservationRepo.generateReservationId();
              Reservation newRes =
                  new Reservation(
                      newResId,
                      guest.getGuestId(),
                      null,
                      res.getRoomType(),
                      Reservation.Status.WAITING,
                      false,
                      0,
                      LocalDateTime.now(),
                      LocalDateTime.now(),
                      true);

              int newScore =
                  vipReservationRepo.calculatePriorityScore(newRes, guest, member, config);
              newRes.setPriorityScore(newScore);

              vipReservationRepo.addReservation(newRes);
              scheduleNextBoilingTask(vipReservationRepo, guestRepo, memberRepo, configRepo);
            }
          }
        }
      }
    }

    // Find the earliest FUTURE expiration target
    var futureList = allocationList.filter(e -> e != null && e.getExpirationTimestamp() > now);
    if (futureList.isEmpty()) return;

    AllocationEntry earliestFutureEntry = null;
    long earliestFutureTime = Long.MAX_VALUE;

    for (AllocationEntry current : futureList) {
      if (current.getExpirationTimestamp() < earliestFutureTime) {
        earliestFutureTime = current.getExpirationTimestamp();
        earliestFutureEntry = current;
      }
    }

    if (earliestFutureEntry == null) return;

    final AllocationEntry targetEntry = earliestFutureEntry;
    long remainingMs = Math.max(1, earliestFutureTime - now);

    expirationScheduler =
        TaskSchedulerUtil.scheduleOnce(
            remainingMs,
            () -> {
              try {
                if (allocationList.contains(targetEntry)) {
                  allocationRepo.removeAllocationEntry(targetEntry);

                  Room room = roomRepo.findByRoomNumber(targetEntry.getAssignedRoomNumber());

                  if (room != null) {
                    room.setStatus(Room.Status.VACANT_CLEAN);
                    room.setIsOccupied(false);
                    roomRepo.updateRoom(room);
                  }

                  Reservation res = vipReservationRepo.findById(targetEntry.getReservationId());
                  if (res != null) {
                    Guest guest = guestRepo.findById(res.getGuestId());
                    if (guest != null) {
                      Member member =
                          (guest.getMemberId() != null)
                              ? memberRepo.findById(guest.getMemberId())
                              : null;

                      VipSystemConfig config = configRepo.getConfig();
                      Member.LoyaltyTier tier = (member != null) ? member.getTier() : null;
                      int maxStrikes = config.getMaxStrikes(tier);

                      guest.setStrikeCount(guest.getStrikeCount() + 1);
                      guestRepo.updateGuest(guest);

                      res.setStatus(Reservation.Status.NO_SHOW);
                      res.setStrikeCountSnapshot(guest.getStrikeCount());
                      vipReservationRepo.updateReservation(res);

                      if (guest.getStrikeCount() <= maxStrikes) {
                        String newResId = vipReservationRepo.generateReservationId();
                        Reservation newRes =
                            new Reservation(
                                newResId,
                                guest.getGuestId(),
                                null,
                                res.getRoomType(),
                                Reservation.Status.WAITING,
                                false,
                                0,
                                LocalDateTime.now(),
                                LocalDateTime.now(),
                                true);

                        int newScore =
                            vipReservationRepo.calculatePriorityScore(
                                newRes, guest, member, config);
                        newRes.setPriorityScore(newScore);

                        vipReservationRepo.addReservation(newRes);
                        scheduleNextBoilingTask(
                            vipReservationRepo, guestRepo, memberRepo, configRepo);
                      }
                    }
                  }
                }
              } catch (Exception ignored) {
              } finally {
                scheduleNextAutoExpirationTask(
                    allocationRepo,
                    roomRepo,
                    vipReservationRepo,
                    guestRepo,
                    memberRepo,
                    configRepo);
              }
            });
  }
}
