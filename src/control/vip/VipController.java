package control.vip;

import repo.AllocationRepo;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.RoomRepo;
import repo.VipReservationRepo;
import util.ConsoleUtil;
import view.VipView;

public class VipController {
  private final VipView vipView;
  private final VipReservationRepo vipReservationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;
  private final RoomRepo roomRepo;
  private final AllocationRepo allocationRepo;
  private final VipManageWaitlistController waitlistController;

  public VipController() {
    this.vipView = new VipView();
    this.vipReservationRepo = new VipReservationRepo();
    this.guestRepo = new GuestRepo();
    this.memberRepo = new MemberRepo();
    this.roomRepo = new RoomRepo();
    this.allocationRepo = new AllocationRepo();
    this.waitlistController =
        new VipManageWaitlistController(
            vipView, vipReservationRepo, guestRepo, memberRepo, roomRepo, allocationRepo);
  }

  public void start() {
    while (true) {
      try {
        String choice = vipView.displayMenu();

        if ("1".equals(choice)) {
          waitlistController.startWaitlistManagement();
        } else if ("5".equals(choice)) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }
}
