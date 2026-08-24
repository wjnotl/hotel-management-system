package repo;

import adt.ArrayList;
import adt.ListInterface;
import entity.AllocationEntry;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.VipSystemConfig;
import java.time.ZoneId;
import util.BinaryFileUtil;

public class AllocationRepo {
  private final BinaryFileUtil<ListInterface<AllocationEntry>> fileUtil;
  private ListInterface<AllocationEntry> allocationList;

  public AllocationRepo() {
    this.fileUtil = new BinaryFileUtil<>("allocations.dat");
    load();
  }

  private void load() {
    this.allocationList = fileUtil.retrieveFromFile();
    if (this.allocationList == null) {
      this.allocationList = new ArrayList<>();
    }
  }

  public void save() {
    fileUtil.saveToFile(allocationList);
  }

  public void addAllocationEntry(AllocationEntry entry) {
    if (entry == null) return;
    allocationList.add(entry);
    save();
  }

  public boolean removeAllocationEntry(AllocationEntry entry) {
    if (entry == null || allocationList == null) return false;

    boolean removed = false;
    for (int i = 1; i <= allocationList.getNumberOfEntries(); i++) {
      AllocationEntry current = allocationList.getEntry(i);
      if (current != null && current.equals(entry)) {
        allocationList.removeAt(i);
        removed = true;
        break;
      }
    }

    if (removed) {
      save();
    }
    return removed;
  }

  public ListInterface<AllocationEntry> getAllocationList() {
    return allocationList;
  }

  public int recalculateActiveGraceTimers(
      RoomRepo roomRepo,
      VipReservationRepo vipReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      VipSystemConfigRepo configRepo) {
    VipSystemConfig config = configRepo.getConfig();
    if (allocationList == null || allocationList.isEmpty()) return 0;

    int updatedCount = 0;
    long now = System.currentTimeMillis();

    for (int i = 1; i <= allocationList.getNumberOfEntries(); i++) {
      AllocationEntry entry = allocationList.getEntry(i);
      if (entry != null) {
        Reservation r = vipReservationRepo.findById(entry.getReservationId());
        Guest g = (r != null) ? guestRepo.findById(r.getGuestId()) : null;
        Member m =
            (g != null && g.getMemberId() != null) ? memberRepo.findById(g.getMemberId()) : null;

        Member.LoyaltyTier tier = (m != null) ? m.getTier() : null;
        int newGraceMins = config.getGraceWindowMins(tier);

        long newExpiration;
        if (r != null && r.getAllocatedTime() != null) {
          long startMs =
              r.getAllocatedTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
          newExpiration = startMs + (newGraceMins * 60 * 1000L);
          r.setAllocatedGraceMins(newGraceMins);
          vipReservationRepo.updateReservation(r);
        } else {
          newExpiration = now + (newGraceMins * 60 * 1000L);
        }

        entry.setExpirationTimestamp(newExpiration);
        updatedCount++;
      }
    }

    save();
    return updatedCount;
  }
}
