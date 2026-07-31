package repo;

import adt.ArrayList;
import adt.ListInterface;
import entity.Billing;
import util.BinaryFileUtil;

public class BillingRepo {
  private final BinaryFileUtil<ListInterface<Billing>> fileUtil;
  private ListInterface<Billing> billingList;

  public BillingRepo() {
    this.fileUtil = new BinaryFileUtil<>("billings.dat");
    load();
  }

  private void load() {
    this.billingList = fileUtil.retrieveFromFile();
    if (this.billingList == null) {
      this.billingList = new ArrayList<>();
    }
  }

  private void save() {
    fileUtil.saveToFile(billingList);
  }

  public void addBilling(Billing billing) {
    if (billing == null) return;
    billingList.add(billing);
    save();
  }

  public boolean updateBilling(Billing updatedBilling) {
    if (updatedBilling == null || billingList == null) return false;

    for (int i = 1; i <= billingList.getNumberOfEntries(); i++) {
      Billing existing = billingList.getEntry(i);
      if (existing != null && existing.equals(updatedBilling)) {
        billingList.replace(i, updatedBilling);
        save();
        return true;
      }
    }
    return false;
  }

  public void addOrUpdateBilling(Billing billing) {
    if (!updateBilling(billing)) {
      addBilling(billing);
    }
  }

  public Billing findById(String billingId) {
    if (billingId == null || billingList == null) return null;
    for (int i = 1; i <= billingList.getNumberOfEntries(); i++) {
      Billing b = billingList.getEntry(i);
      if (b != null && billingId.equalsIgnoreCase(b.getBillingId())) {
        return b;
      }
    }
    return null;
  }

  public ListInterface<Billing> findByGuestId(String guestId) {
    ListInterface<Billing> matches = new ArrayList<>();
    if (guestId == null || billingList == null) return matches;

    for (int i = 1; i <= billingList.getNumberOfEntries(); i++) {
      Billing b = billingList.getEntry(i);
      if (b != null && guestId.equalsIgnoreCase(b.getGuestId())) {
        matches.add(b);
      }
    }
    return matches;
  }

  public ListInterface<Billing> getBillingList() {
    return billingList;
  }
}
