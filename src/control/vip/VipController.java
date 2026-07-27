package control.vip;

import adt.ListInterface;
import adt.MapInterface;
import adt.PriorityQueueInterface;
import entity.Guest;
import entity.Reservation;
import util.BinaryFileUtil;
import util.ConsoleUtil;
import view.VipView;

public class VipController {
  private VipView vipView = new VipView();
  private BinaryFileUtil binaryFileUtil = new BinaryFileUtil<>("vip_reservations.dat");
  private PriorityQueueInterface<Reservation> vipReservationQueue;
  private ListInterface<Reservation> vipReservationList;
  private MapInterface<String, Guest> guestMap;

  public VipController() {
    this.vipReservationList =
        new BinaryFileUtil<ListInterface<Reservation>>().retrieveFromFile("vip_reservations.dat");
  }

  public void start() {
    while (true) {
      try {
        String choice = vipView.displayMenu();

        if (choice == "1") {
          manageWaitlist();
        } else if (choice == "5") {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  public void manageWaitlist() {
    int currentPage = 1;
    int pageSize = 10;

    String searchQuery = null;
    String tierFilter = null;
    String statusFilter = null;
    String sortCriteria = "PRIORITY SCORE (HIGH -> LOW)";

    vipView.manageWaitlist(null, null);
  }
}
