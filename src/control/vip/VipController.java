package control.vip;

import java.time.Duration;
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
          new VipSettingsController(
                  vipSystemConfigRepo, vipReservationRepo, guestRepo, memberRepo, allocationRepo)
              .startSettingsManagement();
        } else if ("5".equals(choice)) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  public static void startMidnightStrikeResetScheduler(GuestRepo guestRepo) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
    long minutesUntilMidnight = Duration.between(now, nextMidnight).toMinutes();

    TaskSchedulerUtil.scheduleEvery(
        minutesUntilMidnight,
        24 * 60,
        () -> {
          try {
            guestRepo.resetAllGuestStrikes();
          } catch (Exception e) {
          }
        });
  }
}
