package control.vip;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

  public static void startMidnightStrikeResetScheduler(
      GuestRepo guestRepo, VipSystemConfigRepo configRepo) {
    if (guestRepo == null || configRepo == null)
      return;

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
