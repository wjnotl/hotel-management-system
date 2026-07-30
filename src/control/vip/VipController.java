package control.vip;

import repo.AllocationRepo;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.RoomRepo;
import repo.VipReservationRepo;
import util.ConsoleUtil;
import view.vip.VipView;

public class VipController {
  private final VipView vipView = new VipView();
  private final AllocationRepo allocationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;
  private final RoomRepo roomRepo;
  private final VipReservationRepo vipReservationRepo;

  public VipController(
      AllocationRepo allocationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      RoomRepo roomRepo,
      VipReservationRepo vipReservationRepo) {
    this.allocationRepo = allocationRepo;
    this.guestRepo = guestRepo;
    this.memberRepo = memberRepo;
    this.roomRepo = roomRepo;
    this.vipReservationRepo = vipReservationRepo;
  }

  public void start() {
    while (true) {
      try {
        String choice = vipView.displayMenu();

        if ("1".equals(choice)) {
          new VipManageWaitlistController(
                  vipReservationRepo, guestRepo, memberRepo, roomRepo, allocationRepo)
              .startWaitlistManagement();
        } else if ("2".equals(choice)) {
          new VipManageAllocationController(
                  allocationRepo, vipReservationRepo, guestRepo, memberRepo, roomRepo)
              .startAllocationManagement();
        } else if ("5".equals(choice)) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }
}
