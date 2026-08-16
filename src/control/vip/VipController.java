package control.vip;

import adt.PriorityQueueInterface;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.VipSystemConfig;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    if (vipReservationRepo == null || configRepo == null) return;
    VipSystemConfig config = configRepo.getConfig();
    var vipReservationList = vipReservationRepo.getAllReservations();

    if (config == null || vipReservationList == null || vipReservationList.isEmpty()) return;

    long now = System.currentTimeMillis();

    // 1. Process any overdue boiling targets synchronously (where targetMs <= now)
    for (int i = 1; i <= vipReservationList.getNumberOfEntries(); i++) {
      Reservation reservation = vipReservationList.getEntry(i);
      if (reservation != null
          && reservation.getStatus() == Reservation.Status.WAITING
          && !reservation.getIsBoiling()
          && reservation.getQueueArrivalTime() != null) {
        Guest g = (guestRepo != null) ? guestRepo.findById(reservation.getGuestId()) : null;
        Member m =
            (g != null && g.getMemberId() != null && memberRepo != null)
                ? memberRepo.findById(g.getMemberId())
                : null;
        Member.LoyaltyTier tier = (m != null) ? m.getTier() : null;

        int limitMins =
            (tier == Member.LoyaltyTier.DIAMOND)
                ? config.getDiamondBoilingLimitMins()
                : (tier == Member.LoyaltyTier.GOLD)
                    ? config.getGoldBoilingLimitMins()
                    : config.getSilverBoilingLimitMins();

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
    }

    // 2. Find the earliest FUTURE boiling target (where targetMs > now)
    Reservation earliestFutureRes = null;
    long earliestFutureTargetMs = Long.MAX_VALUE;

    for (int i = 1; i <= vipReservationList.getNumberOfEntries(); i++) {
      Reservation reservation = vipReservationList.getEntry(i);
      if (reservation != null
          && reservation.getStatus() == Reservation.Status.WAITING
          && !reservation.getIsBoiling()
          && reservation.getQueueArrivalTime() != null) {
        Guest g = (guestRepo != null) ? guestRepo.findById(reservation.getGuestId()) : null;
        Member m =
            (g != null && g.getMemberId() != null && memberRepo != null)
                ? memberRepo.findById(g.getMemberId())
                : null;
        Member.LoyaltyTier tier = (m != null) ? m.getTier() : null;

        int limitMins =
            (tier == Member.LoyaltyTier.DIAMOND)
                ? config.getDiamondBoilingLimitMins()
                : (tier == Member.LoyaltyTier.GOLD)
                    ? config.getGoldBoilingLimitMins()
                    : config.getSilverBoilingLimitMins();

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
    }

    if (earliestFutureRes == null) return;

    final Reservation targetRes = earliestFutureRes;
    long delayMs = Math.max(1, earliestFutureTargetMs - now);

    TaskSchedulerUtil.scheduleOnce(
        delayMs,
        () -> {
          try {
            if (targetRes.getStatus() == Reservation.Status.WAITING && !targetRes.getIsBoiling()) {
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

              // Re-arm scheduler for the NEXT earliest non-boiling reservation
              scheduleNextBoilingTask(vipReservationRepo, guestRepo, memberRepo, configRepo);
            }
          } catch (Exception ignored) {
          }
        });
  }

  public static void startMidnightStrikeResetScheduler(
      GuestRepo guestRepo, VipSystemConfigRepo configRepo) {
    if (guestRepo == null || configRepo == null) return;

    // 1. Startup check: Check if midnight passed while system was offline/shutdown
    String lastResetDate = configRepo.getConfig().getLastStrikeResetDate();
    String todayDate = LocalDate.now().toString();

    if (lastResetDate == null || !todayDate.equals(lastResetDate)) {
      guestRepo.resetAllGuestStrikes(configRepo);
    }

    // 2. Schedule recurring timer for the next midnight
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
}
