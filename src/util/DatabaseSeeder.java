package util;

import control.booking.BookingSettingsStore;
import entity.AllocationEntry;
import entity.Billing;
import entity.Guest;
import entity.HousekeepingStaff;
import entity.HousekeepingTask;
import entity.Member;
import entity.Reservation;
import entity.Room;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import repo.AllocationRepo;
import repo.BillingRepo;
import repo.GuestRepo;
import repo.HousekeepingStaffRepo;
import repo.HousekeepingTaskRepo;
import repo.MemberRepo;
import repo.ReservationRepo;
import repo.RoomRepo;
import repo.StandardReservationRepo;
import repo.VipReservationRepo;
import repo.VipSystemConfigRepo;

public class DatabaseSeeder {

  private static int resCounter = 10001;
  private static int billingCounter = 1001;

  public static void main(String[] args) {
    seedIfEmpty();
  }

  public static void seedIfEmpty() {
    GuestRepo guestRepo = new GuestRepo();
    MemberRepo memberRepo = new MemberRepo();
    RoomRepo roomRepo = new RoomRepo();
    ReservationRepo reservationRepo = new ReservationRepo();
    VipReservationRepo vipRepo = new VipReservationRepo(reservationRepo);
    BillingRepo billingRepo = new BillingRepo();
    HousekeepingStaffRepo staffRepo = new HousekeepingStaffRepo();
    HousekeepingTaskRepo taskRepo = new HousekeepingTaskRepo();
    AllocationRepo allocationRepo = new AllocationRepo();
    VipSystemConfigRepo vipSystemConfigRepo = new VipSystemConfigRepo();
    BookingSettingsStore bookingSettingsStore = new BookingSettingsStore();
    StandardReservationRepo standardRepo =
        new StandardReservationRepo(reservationRepo, bookingSettingsStore::getSettings);

    if (!guestRepo.getGuestList().isEmpty()) {
      return;
    }

    vipSystemConfigRepo.updateConfig(new entity.VipSystemConfig());

    System.out.println("Seeding system-wide master dataset...");
    LocalDateTime now = LocalDateTime.now();
    LocalDate today = LocalDate.now();

    // =========================================================================
    // 1. SEED MEMBERS (40 Members: 10 Diamond, 15 Gold, 15 Silver)
    // =========================================================================
    // Diamond Members (10)
    memberRepo.addMember(new Member("M-1001", Member.LoyaltyTier.DIAMOND, 15500));
    memberRepo.addMember(new Member("M-1002", Member.LoyaltyTier.DIAMOND, 16000));
    memberRepo.addMember(new Member("M-1003", Member.LoyaltyTier.DIAMOND, 16500));
    memberRepo.addMember(new Member("M-1004", Member.LoyaltyTier.DIAMOND, 17000));
    memberRepo.addMember(new Member("M-1005", Member.LoyaltyTier.DIAMOND, 17500));
    memberRepo.addMember(new Member("M-1006", Member.LoyaltyTier.DIAMOND, 18000));
    memberRepo.addMember(new Member("M-1007", Member.LoyaltyTier.DIAMOND, 18500));
    memberRepo.addMember(new Member("M-1008", Member.LoyaltyTier.DIAMOND, 19000));
    memberRepo.addMember(new Member("M-1009", Member.LoyaltyTier.DIAMOND, 19500));
    memberRepo.addMember(new Member("M-1010", Member.LoyaltyTier.DIAMOND, 20000));

    // Gold Members (15)
    memberRepo.addMember(new Member("M-1011", Member.LoyaltyTier.GOLD, 5300));
    memberRepo.addMember(new Member("M-1012", Member.LoyaltyTier.GOLD, 5600));
    memberRepo.addMember(new Member("M-1013", Member.LoyaltyTier.GOLD, 5900));
    memberRepo.addMember(new Member("M-1014", Member.LoyaltyTier.GOLD, 6200));
    memberRepo.addMember(new Member("M-1015", Member.LoyaltyTier.GOLD, 6500));
    memberRepo.addMember(new Member("M-1016", Member.LoyaltyTier.GOLD, 6800));
    memberRepo.addMember(new Member("M-1017", Member.LoyaltyTier.GOLD, 7100));
    memberRepo.addMember(new Member("M-1018", Member.LoyaltyTier.GOLD, 7400));
    memberRepo.addMember(new Member("M-1019", Member.LoyaltyTier.GOLD, 7700));
    memberRepo.addMember(new Member("M-1020", Member.LoyaltyTier.GOLD, 8000));
    memberRepo.addMember(new Member("M-1021", Member.LoyaltyTier.GOLD, 8300));
    memberRepo.addMember(new Member("M-1022", Member.LoyaltyTier.GOLD, 8600));
    memberRepo.addMember(new Member("M-1023", Member.LoyaltyTier.GOLD, 8900));
    memberRepo.addMember(new Member("M-1024", Member.LoyaltyTier.GOLD, 9200));
    memberRepo.addMember(new Member("M-1025", Member.LoyaltyTier.GOLD, 9500));

    // Silver Members (15)
    memberRepo.addMember(new Member("M-1026", Member.LoyaltyTier.SILVER, 1150));
    memberRepo.addMember(new Member("M-1027", Member.LoyaltyTier.SILVER, 1300));
    memberRepo.addMember(new Member("M-1028", Member.LoyaltyTier.SILVER, 1450));
    memberRepo.addMember(new Member("M-1029", Member.LoyaltyTier.SILVER, 1600));
    memberRepo.addMember(new Member("M-1030", Member.LoyaltyTier.SILVER, 1750));
    memberRepo.addMember(new Member("M-1031", Member.LoyaltyTier.SILVER, 1900));
    memberRepo.addMember(new Member("M-1032", Member.LoyaltyTier.SILVER, 2050));
    memberRepo.addMember(new Member("M-1033", Member.LoyaltyTier.SILVER, 2200));
    memberRepo.addMember(new Member("M-1034", Member.LoyaltyTier.SILVER, 2350));
    memberRepo.addMember(new Member("M-1035", Member.LoyaltyTier.SILVER, 2500));
    memberRepo.addMember(new Member("M-1036", Member.LoyaltyTier.SILVER, 2650));
    memberRepo.addMember(new Member("M-1037", Member.LoyaltyTier.SILVER, 2800));
    memberRepo.addMember(new Member("M-1038", Member.LoyaltyTier.SILVER, 2950));
    memberRepo.addMember(new Member("M-1039", Member.LoyaltyTier.SILVER, 3100));
    memberRepo.addMember(new Member("M-1040", Member.LoyaltyTier.SILVER, 3250));

    // =========================================================================
    // 2. SEED GUESTS (80 Guests: 10 Diamond, 15 Gold, 15 Silver, 40 Non-Member)
    // =========================================================================
    // Diamond Guests (G-101..G-110): 8 w/ 0 strikes, 2 w/ 1 strike
    guestRepo.addGuest(makeGuest(1, "Diamond Guest 1", "M-1001", 0));
    guestRepo.addGuest(makeGuest(2, "Diamond Guest 2", "M-1002", 0));
    guestRepo.addGuest(makeGuest(3, "Diamond Guest 3", "M-1003", 0));
    guestRepo.addGuest(makeGuest(4, "Diamond Guest 4", "M-1004", 0));
    guestRepo.addGuest(makeGuest(5, "Diamond Guest 5", "M-1005", 0));
    guestRepo.addGuest(makeGuest(6, "Diamond Guest 6", "M-1006", 0));
    guestRepo.addGuest(makeGuest(7, "Diamond Guest 7", "M-1007", 0));
    guestRepo.addGuest(makeGuest(8, "Diamond Guest 8", "M-1008", 0));
    guestRepo.addGuest(makeGuest(9, "Diamond Guest 9", "M-1009", 1));
    guestRepo.addGuest(makeGuest(10, "Diamond Guest 10", "M-1010", 1));

    // Gold Guests (G-111..G-125): 11 w/ 0 strikes, 2 w/ 1 strike, 2 w/ 2 strikes
    guestRepo.addGuest(makeGuest(11, "Gold Guest 11", "M-1011", 0));
    guestRepo.addGuest(makeGuest(12, "Gold Guest 12", "M-1012", 0));
    guestRepo.addGuest(makeGuest(13, "Gold Guest 13", "M-1013", 0));
    guestRepo.addGuest(makeGuest(14, "Gold Guest 14", "M-1014", 0));
    guestRepo.addGuest(makeGuest(15, "Gold Guest 15", "M-1015", 0));
    guestRepo.addGuest(makeGuest(16, "Gold Guest 16", "M-1016", 0));
    guestRepo.addGuest(makeGuest(17, "Gold Guest 17", "M-1017", 0));
    guestRepo.addGuest(makeGuest(18, "Gold Guest 18", "M-1018", 0));
    guestRepo.addGuest(makeGuest(19, "Gold Guest 19", "M-1019", 0));
    guestRepo.addGuest(makeGuest(20, "Gold Guest 20", "M-1020", 0));
    guestRepo.addGuest(makeGuest(21, "Gold Guest 21", "M-1021", 0));
    guestRepo.addGuest(makeGuest(22, "Gold Guest 22", "M-1022", 1));
    guestRepo.addGuest(makeGuest(23, "Gold Guest 23", "M-1023", 1));
    guestRepo.addGuest(makeGuest(24, "Gold Guest 24", "M-1024", 2));
    guestRepo.addGuest(makeGuest(25, "Gold Guest 25", "M-1025", 2));

    // Silver Guests (G-126..G-140): 10 w/ 0 strikes, 2 w/ 1 strike, 3 w/ 2 strikes
    guestRepo.addGuest(makeGuest(26, "Silver Guest 26", "M-1026", 0));
    guestRepo.addGuest(makeGuest(27, "Silver Guest 27", "M-1027", 0));
    guestRepo.addGuest(makeGuest(28, "Silver Guest 28", "M-1028", 0));
    guestRepo.addGuest(makeGuest(29, "Silver Guest 29", "M-1029", 0));
    guestRepo.addGuest(makeGuest(30, "Silver Guest 30", "M-1030", 0));
    guestRepo.addGuest(makeGuest(31, "Silver Guest 31", "M-1031", 0));
    guestRepo.addGuest(makeGuest(32, "Silver Guest 32", "M-1032", 0));
    guestRepo.addGuest(makeGuest(33, "Silver Guest 33", "M-1033", 0));
    guestRepo.addGuest(makeGuest(34, "Silver Guest 34", "M-1034", 0));
    guestRepo.addGuest(makeGuest(35, "Silver Guest 35", "M-1035", 0));
    guestRepo.addGuest(makeGuest(36, "Silver Guest 36", "M-1036", 1));
    guestRepo.addGuest(makeGuest(37, "Silver Guest 37", "M-1037", 1));
    guestRepo.addGuest(makeGuest(38, "Silver Guest 38", "M-1038", 2));
    guestRepo.addGuest(makeGuest(39, "Silver Guest 39", "M-1039", 2));
    guestRepo.addGuest(makeGuest(40, "Silver Guest 40", "M-1040", 2));

    // Non-Member Guests (G-141..G-180): 35 w/ 0 strikes, 3 w/ 1 strike, 2 w/ 2 strikes
    guestRepo.addGuest(makeGuest(41, "NonMember Guest 41", null, 0));
    guestRepo.addGuest(makeGuest(42, "NonMember Guest 42", null, 0));
    guestRepo.addGuest(makeGuest(43, "NonMember Guest 43", null, 0));
    guestRepo.addGuest(makeGuest(44, "NonMember Guest 44", null, 0));
    guestRepo.addGuest(makeGuest(45, "NonMember Guest 45", null, 0));
    guestRepo.addGuest(makeGuest(46, "NonMember Guest 46", null, 0));
    guestRepo.addGuest(makeGuest(47, "NonMember Guest 47", null, 0));
    guestRepo.addGuest(makeGuest(48, "NonMember Guest 48", null, 0));
    guestRepo.addGuest(makeGuest(49, "NonMember Guest 49", null, 0));
    guestRepo.addGuest(makeGuest(50, "NonMember Guest 50", null, 0));
    guestRepo.addGuest(makeGuest(51, "NonMember Guest 51", null, 0));
    guestRepo.addGuest(makeGuest(52, "NonMember Guest 52", null, 0));
    guestRepo.addGuest(makeGuest(53, "NonMember Guest 53", null, 0));
    guestRepo.addGuest(makeGuest(54, "NonMember Guest 54", null, 0));
    guestRepo.addGuest(makeGuest(55, "NonMember Guest 55", null, 0));
    guestRepo.addGuest(makeGuest(56, "NonMember Guest 56", null, 0));
    guestRepo.addGuest(makeGuest(57, "NonMember Guest 57", null, 0));
    guestRepo.addGuest(makeGuest(58, "NonMember Guest 58", null, 0));
    guestRepo.addGuest(makeGuest(59, "NonMember Guest 59", null, 0));
    guestRepo.addGuest(makeGuest(60, "NonMember Guest 60", null, 0));
    guestRepo.addGuest(makeGuest(61, "NonMember Guest 61", null, 0));
    guestRepo.addGuest(makeGuest(62, "NonMember Guest 62", null, 0));
    guestRepo.addGuest(makeGuest(63, "NonMember Guest 63", null, 0));
    guestRepo.addGuest(makeGuest(64, "NonMember Guest 64", null, 0));
    guestRepo.addGuest(makeGuest(65, "NonMember Guest 65", null, 0));
    guestRepo.addGuest(makeGuest(66, "NonMember Guest 66", null, 0));
    guestRepo.addGuest(makeGuest(67, "NonMember Guest 67", null, 0));
    guestRepo.addGuest(makeGuest(68, "NonMember Guest 68", null, 0));
    guestRepo.addGuest(makeGuest(69, "NonMember Guest 69", null, 0));
    guestRepo.addGuest(makeGuest(70, "NonMember Guest 70", null, 0));
    guestRepo.addGuest(makeGuest(71, "NonMember Guest 71", null, 0));
    guestRepo.addGuest(makeGuest(72, "NonMember Guest 72", null, 0));
    guestRepo.addGuest(makeGuest(73, "NonMember Guest 73", null, 0));
    guestRepo.addGuest(makeGuest(74, "NonMember Guest 74", null, 0));
    guestRepo.addGuest(makeGuest(75, "NonMember Guest 75", null, 0));
    guestRepo.addGuest(makeGuest(76, "NonMember Guest 76", null, 1));
    guestRepo.addGuest(makeGuest(77, "NonMember Guest 77", null, 1));
    guestRepo.addGuest(makeGuest(78, "NonMember Guest 78", null, 1));
    guestRepo.addGuest(makeGuest(79, "NonMember Guest 79", null, 2));
    guestRepo.addGuest(makeGuest(80, "NonMember Guest 80", null, 2));

    // =========================================================================
    // 3. SEED ROOMS (30 Rooms: 10 Luxury, 10 Suite, 10 Standard)
    // =========================================================================
    // Luxury Rooms (L-801..L-810)
    addRoom(roomRepo, "L-801", Room.RoomType.LUXURY, Room.Status.VACANT_CLEAN, 850.00, false);
    addRoom(roomRepo, "L-802", Room.RoomType.LUXURY, Room.Status.VACANT_CLEAN, 850.00, false);
    addRoom(roomRepo, "L-803", Room.RoomType.LUXURY, Room.Status.VACANT_CLEAN, 850.00, false);
    addRoom(roomRepo, "L-804", Room.RoomType.LUXURY, Room.Status.VACANT_CLEAN, 850.00, false);
    addRoom(roomRepo, "L-805", Room.RoomType.LUXURY, Room.Status.VACANT_CLEAN, 850.00, true);
    addRoom(roomRepo, "L-806", Room.RoomType.LUXURY, Room.Status.VACANT_CLEAN, 850.00, true);
    addRoom(roomRepo, "L-807", Room.RoomType.LUXURY, Room.Status.VACANT_CLEAN, 850.00, true);
    addRoom(roomRepo, "L-808", Room.RoomType.LUXURY, Room.Status.VACANT_CLEAN, 850.00, true);
    addRoom(roomRepo, "L-809", Room.RoomType.LUXURY, Room.Status.DIRTY, 850.00, false);
    addRoom(roomRepo, "L-810", Room.RoomType.LUXURY, Room.Status.CLEANING, 850.00, false);

    // Suite Rooms (S-501..S-510)
    addRoom(roomRepo, "S-501", Room.RoomType.SUITE, Room.Status.VACANT_CLEAN, 550.00, false);
    addRoom(roomRepo, "S-502", Room.RoomType.SUITE, Room.Status.VACANT_CLEAN, 550.00, false);
    addRoom(roomRepo, "S-503", Room.RoomType.SUITE, Room.Status.VACANT_CLEAN, 550.00, false);
    addRoom(roomRepo, "S-504", Room.RoomType.SUITE, Room.Status.VACANT_CLEAN, 550.00, false);
    addRoom(roomRepo, "S-505", Room.RoomType.SUITE, Room.Status.VACANT_CLEAN, 550.00, true);
    addRoom(roomRepo, "S-506", Room.RoomType.SUITE, Room.Status.VACANT_CLEAN, 550.00, true);
    addRoom(roomRepo, "S-507", Room.RoomType.SUITE, Room.Status.VACANT_CLEAN, 550.00, true);
    addRoom(roomRepo, "S-508", Room.RoomType.SUITE, Room.Status.VACANT_CLEAN, 550.00, true);
    addRoom(roomRepo, "S-509", Room.RoomType.SUITE, Room.Status.DIRTY, 550.00, false);
    addRoom(roomRepo, "S-510", Room.RoomType.SUITE, Room.Status.CLEANING, 550.00, false);

    // Standard Rooms (ST-101..ST-110)
    addRoom(roomRepo, "ST-101", Room.RoomType.STANDARD, Room.Status.VACANT_CLEAN, 250.00, false);
    addRoom(roomRepo, "ST-102", Room.RoomType.STANDARD, Room.Status.VACANT_CLEAN, 250.00, false);
    addRoom(roomRepo, "ST-103", Room.RoomType.STANDARD, Room.Status.VACANT_CLEAN, 250.00, false);
    addRoom(roomRepo, "ST-104", Room.RoomType.STANDARD, Room.Status.VACANT_CLEAN, 250.00, false);
    addRoom(roomRepo, "ST-105", Room.RoomType.STANDARD, Room.Status.VACANT_CLEAN, 250.00, true);
    addRoom(roomRepo, "ST-106", Room.RoomType.STANDARD, Room.Status.VACANT_CLEAN, 250.00, true);
    addRoom(roomRepo, "ST-107", Room.RoomType.STANDARD, Room.Status.VACANT_CLEAN, 250.00, true);
    addRoom(roomRepo, "ST-108", Room.RoomType.STANDARD, Room.Status.VACANT_CLEAN, 250.00, true);
    addRoom(roomRepo, "ST-109", Room.RoomType.STANDARD, Room.Status.DIRTY, 250.00, false);
    addRoom(roomRepo, "ST-110", Room.RoomType.STANDARD, Room.Status.CLEANING, 250.00, false);

    // =========================================================================
    // 4. SEED RESERVATIONS
    // =========================================================================
    // 4.A Standard Queue Waiting Reservations (10 Non-Members)
    addStdWait(standardRepo, "G-141", Room.RoomType.LUXURY, now.minusMinutes(25));
    addStdWait(standardRepo, "G-142", Room.RoomType.LUXURY, now.minusMinutes(20));
    addStdWait(standardRepo, "G-143", Room.RoomType.LUXURY, now.minusMinutes(15));
    addStdWait(standardRepo, "G-144", Room.RoomType.LUXURY, now.minusMinutes(10));
    addStdWait(standardRepo, "G-145", Room.RoomType.SUITE, now.minusMinutes(22));
    addStdWait(standardRepo, "G-146", Room.RoomType.SUITE, now.minusMinutes(18));
    addStdWait(standardRepo, "G-147", Room.RoomType.SUITE, now.minusMinutes(12));
    addStdWait(standardRepo, "G-148", Room.RoomType.STANDARD, now.minusMinutes(30));
    addStdWait(standardRepo, "G-149", Room.RoomType.STANDARD, now.minusMinutes(22));
    addStdWait(standardRepo, "G-150", Room.RoomType.STANDARD, now.minusMinutes(15));

    // 4.B Advance Reserved Reservations (10 Reservations)
    addStdAdv(standardRepo, "G-101", Room.RoomType.LUXURY, today.atStartOfDay(), 2);
    addStdAdv(standardRepo, "G-111", Room.RoomType.LUXURY, today.plusDays(1).atStartOfDay(), 3);
    addStdAdv(standardRepo, "G-151", Room.RoomType.LUXURY, today.plusDays(2).atStartOfDay(), 1);
    addStdAdv(standardRepo, "G-112", Room.RoomType.SUITE, today.atStartOfDay(), 2);
    addStdAdv(standardRepo, "G-126", Room.RoomType.SUITE, today.plusDays(1).atStartOfDay(), 2);
    addStdAdv(standardRepo, "G-127", Room.RoomType.SUITE, today.plusDays(3).atStartOfDay(), 4);
    addStdAdv(standardRepo, "G-152", Room.RoomType.SUITE, today.plusDays(5).atStartOfDay(), 2);
    addStdAdv(standardRepo, "G-128", Room.RoomType.STANDARD, today.atStartOfDay(), 1);
    addStdAdv(standardRepo, "G-153", Room.RoomType.STANDARD, today.plusDays(2).atStartOfDay(), 2);
    addStdAdv(standardRepo, "G-154", Room.RoomType.STANDARD, today.plusDays(4).atStartOfDay(), 3);

    // 4.C Standard Active Checked-In Stays (5 Non-Members)
    Reservation sc1 =
        addStdCheckedIn(
            standardRepo,
            roomRepo,
            "G-155",
            Room.RoomType.LUXURY,
            "L-805",
            now.minusMinutes(20),
            3);
    Reservation sc2 =
        addStdCheckedIn(
            standardRepo,
            roomRepo,
            "G-156",
            Room.RoomType.LUXURY,
            "L-806",
            now.minusMinutes(15),
            2);
    Reservation sc3 =
        addStdCheckedIn(
            standardRepo, roomRepo, "G-157", Room.RoomType.SUITE, "S-505", now.minusMinutes(25), 4);
    Reservation sc4 =
        addStdCheckedIn(
            standardRepo, roomRepo, "G-158", Room.RoomType.SUITE, "S-506", now.minusMinutes(18), 2);
    Reservation sc5 =
        addStdCheckedIn(
            standardRepo,
            roomRepo,
            "G-159",
            Room.RoomType.STANDARD,
            "ST-105",
            now.minusMinutes(22),
            3);

    addBilling(
        billingRepo,
        "G-155",
        sc1,
        "L-805",
        Room.RoomType.LUXURY,
        today,
        today.plusDays(3),
        850.0,
        Billing.Status.UNPAID,
        now.minusMinutes(20));
    addBilling(
        billingRepo,
        "G-156",
        sc2,
        "L-806",
        Room.RoomType.LUXURY,
        today,
        today.plusDays(2),
        850.0,
        Billing.Status.UNPAID,
        now.minusMinutes(15));
    addBilling(
        billingRepo,
        "G-157",
        sc3,
        "S-505",
        Room.RoomType.SUITE,
        today,
        today.plusDays(4),
        550.0,
        Billing.Status.UNPAID,
        now.minusMinutes(25));
    addBilling(
        billingRepo,
        "G-158",
        sc4,
        "S-506",
        Room.RoomType.SUITE,
        today,
        today.plusDays(2),
        550.0,
        Billing.Status.UNPAID,
        now.minusMinutes(18));
    addBilling(
        billingRepo,
        "G-159",
        sc5,
        "ST-105",
        Room.RoomType.STANDARD,
        today,
        today.plusDays(3),
        250.0,
        Billing.Status.UNPAID,
        now.minusMinutes(22));

    // 4.D Standard Completed Checked-Out Stays (5 Non-Members)
    Reservation co1 =
        addStdCheckedOut(standardRepo, "G-160", Room.RoomType.LUXURY, "L-801", now.minusDays(5), 2);
    Reservation co2 =
        addStdCheckedOut(standardRepo, "G-161", Room.RoomType.LUXURY, "L-802", now.minusDays(3), 3);
    Reservation co3 =
        addStdCheckedOut(standardRepo, "G-162", Room.RoomType.SUITE, "S-501", now.minusDays(6), 1);
    Reservation co4 =
        addStdCheckedOut(standardRepo, "G-163", Room.RoomType.SUITE, "S-502", now.minusDays(2), 2);
    Reservation co5 =
        addStdCheckedOut(
            standardRepo, "G-164", Room.RoomType.STANDARD, "ST-101", now.minusDays(4), 3);

    addBilling(
        billingRepo,
        "G-160",
        co1,
        "L-801",
        Room.RoomType.LUXURY,
        today.minusDays(5),
        today.minusDays(3),
        850.0,
        Billing.Status.PAID,
        now.minusDays(5));
    addBilling(
        billingRepo,
        "G-161",
        co2,
        "L-802",
        Room.RoomType.LUXURY,
        today.minusDays(3),
        today,
        850.0,
        Billing.Status.PAID,
        now.minusDays(3));
    addBilling(
        billingRepo,
        "G-162",
        co3,
        "S-501",
        Room.RoomType.SUITE,
        today.minusDays(6),
        today.minusDays(5),
        550.0,
        Billing.Status.PAID,
        now.minusDays(6));
    addBilling(
        billingRepo,
        "G-163",
        co4,
        "S-502",
        Room.RoomType.SUITE,
        today.minusDays(2),
        today,
        550.0,
        Billing.Status.PAID,
        now.minusDays(2));
    addBilling(
        billingRepo,
        "G-164",
        co5,
        "ST-101",
        Room.RoomType.STANDARD,
        today.minusDays(4),
        today.minusDays(1),
        250.0,
        Billing.Status.PAID,
        now.minusDays(4));

    // 4.E VIP Priority Waiting Reservations (18 Reservations)
    addVipWait(vipRepo, "G-102", Room.RoomType.LUXURY, 9500, false, now.minusMinutes(10));
    addVipWait(vipRepo, "G-103", Room.RoomType.LUXURY, 9200, false, now.minusMinutes(8));
    addVipWait(vipRepo, "G-104", Room.RoomType.LUXURY, 9000, false, now.minusMinutes(5));
    addVipWait(vipRepo, "G-105", Room.RoomType.LUXURY, 12000, true, now.minusMinutes(40));
    addVipWait(vipRepo, "G-113", Room.RoomType.LUXURY, 7500, false, now.minusMinutes(18));
    addVipWait(vipRepo, "G-114", Room.RoomType.LUXURY, 7200, false, now.minusMinutes(15));
    addVipWait(vipRepo, "G-115", Room.RoomType.SUITE, 7800, false, now.minusMinutes(20));
    addVipWait(vipRepo, "G-116", Room.RoomType.SUITE, 7400, false, now.minusMinutes(12));
    addVipWait(vipRepo, "G-117", Room.RoomType.SUITE, 10500, true, now.minusMinutes(45));
    addVipWait(vipRepo, "G-129", Room.RoomType.SUITE, 4800, false, now.minusMinutes(35));
    addVipWait(vipRepo, "G-130", Room.RoomType.SUITE, 4500, false, now.minusMinutes(28));
    addVipWait(vipRepo, "G-131", Room.RoomType.SUITE, 4200, false, now.minusMinutes(22));
    addVipWait(vipRepo, "G-132", Room.RoomType.SUITE, 4000, false, now.minusMinutes(16));
    addVipWait(vipRepo, "G-118", Room.RoomType.STANDARD, 7600, false, now.minusMinutes(20));
    addVipWait(vipRepo, "G-119", Room.RoomType.STANDARD, 10800, true, now.minusMinutes(45));
    addVipWait(vipRepo, "G-133", Room.RoomType.STANDARD, 4900, false, now.minusMinutes(40));
    addVipWait(vipRepo, "G-134", Room.RoomType.STANDARD, 4400, false, now.minusMinutes(30));
    addVipWait(vipRepo, "G-135", Room.RoomType.STANDARD, 4100, false, now.minusMinutes(22));

    // 4.F VIP Room Holding Bay Allocated Reservations (8 Reservations)
    addVipAllocated(
        vipRepo,
        allocationRepo,
        "G-106",
        Room.RoomType.LUXURY,
        "L-803",
        now.minusMinutes(10),
        6,
        10);
    addVipAllocated(
        vipRepo,
        allocationRepo,
        "G-107",
        Room.RoomType.LUXURY,
        "L-804",
        now.minusMinutes(8),
        5,
        10);
    addVipAllocated(
        vipRepo,
        allocationRepo,
        "G-120",
        Room.RoomType.LUXURY,
        "L-807",
        now.minusMinutes(18),
        12,
        15);
    addVipAllocated(
        vipRepo,
        allocationRepo,
        "G-108",
        Room.RoomType.SUITE,
        "S-503",
        now.minusMinutes(12),
        6,
        10);
    addVipAllocated(
        vipRepo,
        allocationRepo,
        "G-121",
        Room.RoomType.SUITE,
        "S-504",
        now.minusMinutes(22),
        11,
        15);
    addVipAllocated(
        vipRepo,
        allocationRepo,
        "G-122",
        Room.RoomType.SUITE,
        "S-507",
        now.minusMinutes(25),
        13,
        15);
    addVipAllocated(
        vipRepo,
        allocationRepo,
        "G-136",
        Room.RoomType.STANDARD,
        "ST-103",
        now.minusMinutes(30),
        15,
        20);
    addVipAllocated(
        vipRepo,
        allocationRepo,
        "G-137",
        Room.RoomType.STANDARD,
        "ST-104",
        now.minusMinutes(36),
        16,
        20);

    // 4.G VIP Active Checked-In Stays (12 Reservations)
    Reservation vc1 =
        addVipCheckedIn(
            vipRepo,
            roomRepo,
            "G-101",
            Room.RoomType.LUXURY,
            "L-805",
            now.minusMinutes(10),
            4,
            10,
            3);
    Reservation vc2 =
        addVipCheckedIn(
            vipRepo,
            roomRepo,
            "G-109",
            Room.RoomType.LUXURY,
            "L-806",
            now.minusMinutes(12),
            5,
            10,
            2);
    Reservation vc3 =
        addVipCheckedIn(
            vipRepo,
            roomRepo,
            "G-110",
            Room.RoomType.LUXURY,
            "L-808",
            now.minusMinutes(14),
            6,
            10,
            4);
    Reservation vc4 =
        addVipCheckedIn(
            vipRepo,
            roomRepo,
            "G-123",
            Room.RoomType.LUXURY,
            "L-807",
            now.minusMinutes(22),
            10,
            15,
            2);
    Reservation vc5 =
        addVipCheckedIn(
            vipRepo,
            roomRepo,
            "G-124",
            Room.RoomType.SUITE,
            "S-505",
            now.minusMinutes(20),
            9,
            15,
            3);
    Reservation vc6 =
        addVipCheckedIn(
            vipRepo,
            roomRepo,
            "G-125",
            Room.RoomType.SUITE,
            "S-506",
            now.minusMinutes(25),
            11,
            15,
            2);
    Reservation vc7 =
        addVipCheckedIn(
            vipRepo,
            roomRepo,
            "G-126",
            Room.RoomType.SUITE,
            "S-507",
            now.minusMinutes(22),
            13,
            20,
            4);
    Reservation vc8 =
        addVipCheckedIn(
            vipRepo,
            roomRepo,
            "G-127",
            Room.RoomType.SUITE,
            "S-508",
            now.minusMinutes(30),
            15,
            20,
            2);
    Reservation vc9 =
        addVipCheckedIn(
            vipRepo,
            roomRepo,
            "G-128",
            Room.RoomType.SUITE,
            "S-505",
            now.minusMinutes(38),
            16,
            20,
            5);
    Reservation vc10 =
        addVipCheckedIn(
            vipRepo,
            roomRepo,
            "G-111",
            Room.RoomType.STANDARD,
            "ST-106",
            now.minusMinutes(22),
            9,
            15,
            2);
    Reservation vc11 =
        addVipCheckedIn(
            vipRepo,
            roomRepo,
            "G-138",
            Room.RoomType.STANDARD,
            "ST-107",
            now.minusMinutes(32),
            15,
            20,
            3);
    Reservation vc12 =
        addVipCheckedIn(
            vipRepo,
            roomRepo,
            "G-139",
            Room.RoomType.STANDARD,
            "ST-108",
            now.minusMinutes(40),
            16,
            20,
            2);

    addBilling(
        billingRepo,
        "G-101",
        vc1,
        "L-805",
        Room.RoomType.LUXURY,
        today,
        today.plusDays(3),
        850.0,
        Billing.Status.UNPAID,
        now.minusMinutes(10));
    addBilling(
        billingRepo,
        "G-109",
        vc2,
        "L-806",
        Room.RoomType.LUXURY,
        today,
        today.plusDays(2),
        850.0,
        Billing.Status.UNPAID,
        now.minusMinutes(12));
    addBilling(
        billingRepo,
        "G-110",
        vc3,
        "L-808",
        Room.RoomType.LUXURY,
        today,
        today.plusDays(4),
        850.0,
        Billing.Status.UNPAID,
        now.minusMinutes(14));
    addBilling(
        billingRepo,
        "G-123",
        vc4,
        "L-807",
        Room.RoomType.LUXURY,
        today,
        today.plusDays(2),
        850.0,
        Billing.Status.UNPAID,
        now.minusMinutes(22));
    addBilling(
        billingRepo,
        "G-124",
        vc5,
        "S-505",
        Room.RoomType.SUITE,
        today,
        today.plusDays(3),
        550.0,
        Billing.Status.UNPAID,
        now.minusMinutes(20));
    addBilling(
        billingRepo,
        "G-125",
        vc6,
        "S-506",
        Room.RoomType.SUITE,
        today,
        today.plusDays(2),
        550.0,
        Billing.Status.UNPAID,
        now.minusMinutes(25));
    addBilling(
        billingRepo,
        "G-126",
        vc7,
        "S-507",
        Room.RoomType.SUITE,
        today,
        today.plusDays(4),
        550.0,
        Billing.Status.UNPAID,
        now.minusMinutes(22));
    addBilling(
        billingRepo,
        "G-127",
        vc8,
        "S-508",
        Room.RoomType.SUITE,
        today,
        today.plusDays(2),
        550.0,
        Billing.Status.UNPAID,
        now.minusMinutes(30));
    addBilling(
        billingRepo,
        "G-128",
        vc9,
        "S-505",
        Room.RoomType.SUITE,
        today,
        today.plusDays(5),
        550.0,
        Billing.Status.UNPAID,
        now.minusMinutes(38));
    addBilling(
        billingRepo,
        "G-111",
        vc10,
        "ST-106",
        Room.RoomType.STANDARD,
        today,
        today.plusDays(2),
        250.0,
        Billing.Status.UNPAID,
        now.minusMinutes(22));
    addBilling(
        billingRepo,
        "G-138",
        vc11,
        "ST-107",
        Room.RoomType.STANDARD,
        today,
        today.plusDays(3),
        250.0,
        Billing.Status.UNPAID,
        now.minusMinutes(32));
    addBilling(
        billingRepo,
        "G-139",
        vc12,
        "ST-108",
        Room.RoomType.STANDARD,
        today,
        today.plusDays(2),
        250.0,
        Billing.Status.UNPAID,
        now.minusMinutes(40));

    // 4.H VIP Completed Checked-Out Stays (6 Reservations)
    Reservation vco1 =
        addVipCheckedOut(
            vipRepo, "G-102", Room.RoomType.LUXURY, "L-801", now.minusDays(4), 5, 10, 2);
    Reservation vco2 =
        addVipCheckedOut(
            vipRepo, "G-103", Room.RoomType.LUXURY, "L-802", now.minusDays(2), 4, 10, 3);
    Reservation vco3 =
        addVipCheckedOut(
            vipRepo, "G-112", Room.RoomType.SUITE, "S-501", now.minusDays(5), 10, 15, 1);
    Reservation vco4 =
        addVipCheckedOut(
            vipRepo, "G-130", Room.RoomType.SUITE, "S-502", now.minusDays(3), 15, 20, 2);
    Reservation vco5 =
        addVipCheckedOut(
            vipRepo, "G-131", Room.RoomType.STANDARD, "ST-101", now.minusDays(6), 14, 20, 2);
    Reservation vco6 =
        addVipCheckedOut(
            vipRepo, "G-132", Room.RoomType.STANDARD, "ST-102", now.minusDays(1), 16, 20, 1);

    addBilling(
        billingRepo,
        "G-102",
        vco1,
        "L-801",
        Room.RoomType.LUXURY,
        today.minusDays(4),
        today.minusDays(2),
        850.0,
        Billing.Status.PAID,
        now.minusDays(4));
    addBilling(
        billingRepo,
        "G-103",
        vco2,
        "L-802",
        Room.RoomType.LUXURY,
        today.minusDays(2),
        today.plusDays(1),
        850.0,
        Billing.Status.PAID,
        now.minusDays(2));
    addBilling(
        billingRepo,
        "G-112",
        vco3,
        "S-501",
        Room.RoomType.SUITE,
        today.minusDays(5),
        today.minusDays(4),
        550.0,
        Billing.Status.PAID,
        now.minusDays(5));
    addBilling(
        billingRepo,
        "G-130",
        vco4,
        "S-502",
        Room.RoomType.SUITE,
        today.minusDays(3),
        today.minusDays(1),
        550.0,
        Billing.Status.PAID,
        now.minusDays(3));
    addBilling(
        billingRepo,
        "G-131",
        vco5,
        "ST-101",
        Room.RoomType.STANDARD,
        today.minusDays(6),
        today.minusDays(4),
        250.0,
        Billing.Status.PAID,
        now.minusDays(6));
    addBilling(
        billingRepo,
        "G-132",
        vco6,
        "ST-102",
        Room.RoomType.STANDARD,
        today.minusDays(1),
        today,
        250.0,
        Billing.Status.PAID,
        now.minusDays(1));

    // 4.I VIP Penalty No-Show Reservations (6 Reservations)
    addVipNoShow(vipRepo, "G-109", Room.RoomType.LUXURY, now.minusHours(4), 10, 10);
    addVipNoShow(vipRepo, "G-110", Room.RoomType.LUXURY, now.minusHours(2), 10, 10);
    addVipNoShow(vipRepo, "G-124", Room.RoomType.SUITE, now.minusHours(5), 15, 15);
    addVipNoShow(vipRepo, "G-125", Room.RoomType.SUITE, now.minusHours(3), 15, 15);
    addVipNoShow(vipRepo, "G-139", Room.RoomType.STANDARD, now.minusHours(6), 20, 20);
    addVipNoShow(vipRepo, "G-140", Room.RoomType.STANDARD, now.minusHours(1), 20, 20);

    // 4.J Standard No-Show Reservations (4 Reservations)
    addStdNoShow(standardRepo, "G-162", Room.RoomType.STANDARD, now.minusHours(3));
    addStdNoShow(standardRepo, "G-163", Room.RoomType.STANDARD, now.minusDays(1).minusHours(2));
    addStdNoShow(standardRepo, "G-164", Room.RoomType.SUITE, now.minusDays(3).minusHours(4));
    addStdNoShow(standardRepo, "G-137", Room.RoomType.LUXURY, now.minusHours(1));

    // =========================================================================
    // 5. SEED HOUSEKEEPING STAFF & TASKS
    // =========================================================================
    HousekeepingStaff s1 =
        new HousekeepingStaff(
            "EMP-001",
            "Zhi Kang",
            HousekeepingStaff.Shift.MORNING,
            HousekeepingStaff.Availability.AVAILABLE);
    HousekeepingStaff s2 =
        new HousekeepingStaff(
            "EMP-002",
            "Di Yao",
            HousekeepingStaff.Shift.MORNING,
            HousekeepingStaff.Availability.ON_TASK);
    HousekeepingStaff s3 =
        new HousekeepingStaff(
            "EMP-003",
            "Jian Chin",
            HousekeepingStaff.Shift.AFTERNOON,
            HousekeepingStaff.Availability.ON_TASK);
    HousekeepingStaff s4 =
        new HousekeepingStaff(
            "EMP-004",
            "Chu Han",
            HousekeepingStaff.Shift.AFTERNOON,
            HousekeepingStaff.Availability.OFF_DUTY);
    HousekeepingStaff s5 =
        new HousekeepingStaff(
            "EMP-005",
            "Luo Feng",
            HousekeepingStaff.Shift.NIGHT,
            HousekeepingStaff.Availability.ON_TASK);
    HousekeepingStaff s6 =
        new HousekeepingStaff(
            "EMP-006",
            "Cheng Yu",
            HousekeepingStaff.Shift.NIGHT,
            HousekeepingStaff.Availability.AVAILABLE);

    s2.getAssignedRoomNumbers().add("L-809");
    s3.getAssignedRoomNumbers().add("S-509");
    s5.getAssignedRoomNumbers().add("L-810");
    s5.getAssignedRoomNumbers().add("ST-110");

    staffRepo.addStaff(s1);
    staffRepo.addStaff(s2);
    staffRepo.addStaff(s3);
    staffRepo.addStaff(s4);
    staffRepo.addStaff(s5);
    staffRepo.addStaff(s6);

    // DIRTY room assignments (2 Tasks: ASSIGNED)
    HousekeepingTask t1 =
        new HousekeepingTask(
            "T-1001",
            "L-809",
            HousekeepingTask.TaskType.STANDARD_CLEAN,
            HousekeepingTask.Status.ASSIGNED,
            "EMP-002",
            false,
            now.minusMinutes(30));
    t1.setAssignedAt(now.minusMinutes(20));
    taskRepo.enqueueTask(t1);

    HousekeepingTask t2 =
        new HousekeepingTask(
            "T-1002",
            "S-509",
            HousekeepingTask.TaskType.DEEP_CLEAN,
            HousekeepingTask.Status.ASSIGNED,
            "EMP-003",
            false,
            now.minusMinutes(45));
    t2.setAssignedAt(now.minusMinutes(30));
    taskRepo.enqueueTask(t2);

    // CLEANING room assignments (2 Tasks: IN_PROGRESS)
    HousekeepingTask t3 =
        new HousekeepingTask(
            "T-1003",
            "L-810",
            HousekeepingTask.TaskType.DEEP_CLEAN,
            HousekeepingTask.Status.IN_PROGRESS,
            "EMP-005",
            true,
            now.minusMinutes(60));
    t3.setAssignedAt(now.minusMinutes(50));
    t3.setStartedAt(now.minusMinutes(30));
    taskRepo.enqueueTask(t3);

    HousekeepingTask t4 =
        new HousekeepingTask(
            "T-1004",
            "ST-110",
            HousekeepingTask.TaskType.STANDARD_CLEAN,
            HousekeepingTask.Status.IN_PROGRESS,
            "EMP-005",
            false,
            now.minusMinutes(40));
    t4.setAssignedAt(now.minusMinutes(35));
    t4.setStartedAt(now.minusMinutes(15));
    taskRepo.enqueueTask(t4);

    // Synchronize VIP queues and calculate priority scores via VipSystemConfig
    vipRepo.applySettingsToQueue(
        vipSystemConfigRepo.getConfig(), guestRepo, memberRepo, false, true);

    System.out.println("Master mock database seeded successfully!");
  }

  private static Guest makeGuest(int index, String name, String memberId, int strikes) {
    String gId = String.format("G-%03d", index);
    String ic = String.format("9001%02d-14-%04d", index % 28 + 1, 5000 + index);
    String passport = String.format("P%07d", 1000000 + index);
    String email = String.format("guest%03d@mail.com", index);
    String phone = String.format("012-%07d", 2000000 + index);
    return new Guest(gId, name, ic, passport, email, phone, memberId, strikes);
  }

  private static void addRoom(
      RoomRepo roomRepo,
      String number,
      Room.RoomType type,
      Room.Status status,
      double price,
      boolean occupied) {
    Room r = new Room(number, type, status, price);
    if (occupied) r.setIsOccupied(true);
    roomRepo.addRoom(r);
  }

  private static void addStdWait(
      StandardReservationRepo repo,
      String guestId,
      Room.RoomType roomType,
      LocalDateTime queuedAt) {
    Reservation r =
        new Reservation(
            repo.generateReservationId(),
            guestId,
            repo.generateConfirmationNumber(),
            roomType,
            Reservation.Status.WAITING,
            false,
            0,
            queuedAt.minusMinutes(10),
            queuedAt,
            false);
    repo.addReservation(r);
  }

  private static void addStdAdv(
      StandardReservationRepo repo,
      String guestId,
      Room.RoomType roomType,
      LocalDateTime arrival,
      int nights) {
    Reservation r =
        new Reservation(
            repo.generateReservationId(),
            guestId,
            repo.generateConfirmationNumber(),
            roomType,
            Reservation.Status.RESERVED,
            false,
            0,
            arrival.minusDays(3),
            null,
            false);
    r.setExpectedArrivalTime(arrival);
    r.setStayDays(nights);
    repo.addReservation(r);
  }

  private static Reservation addStdCheckedIn(
      StandardReservationRepo repo,
      RoomRepo roomRepo,
      String guestId,
      Room.RoomType roomType,
      String roomNumber,
      LocalDateTime queuedAt,
      int nights) {
    Reservation r =
        new Reservation(
            repo.generateReservationId(),
            guestId,
            repo.generateConfirmationNumber(),
            roomType,
            Reservation.Status.CHECKED_IN,
            false,
            0,
            queuedAt.minusMinutes(10),
            queuedAt,
            false);
    r.setAllocatedTime(queuedAt);
    r.setCheckInTime(queuedAt);
    r.setRoomNumber(roomNumber);
    r.setStayDays(nights);
    repo.addReservation(r);
    occupyRoom(roomRepo, roomNumber);
    return r;
  }

  private static Reservation addStdCheckedOut(
      StandardReservationRepo repo,
      String guestId,
      Room.RoomType roomType,
      String roomNumber,
      LocalDateTime queuedAt,
      int nights) {
    Reservation r =
        new Reservation(
            repo.generateReservationId(),
            guestId,
            repo.generateConfirmationNumber(),
            roomType,
            Reservation.Status.CHECKED_OUT,
            false,
            0,
            queuedAt.minusMinutes(10),
            queuedAt,
            false);
    r.setAllocatedTime(queuedAt);
    r.setCheckInTime(queuedAt);
    r.setRoomNumber(roomNumber);
    r.setStayDays(nights);
    repo.addReservation(r);
    return r;
  }

  private static void addStdNoShow(
      StandardReservationRepo repo, String guestId, Room.RoomType roomType, LocalDateTime time) {
    Reservation r =
        new Reservation(
            repo.generateReservationId(),
            guestId,
            repo.generateConfirmationNumber(),
            roomType,
            Reservation.Status.NO_SHOW,
            false,
            0,
            time.minusMinutes(30),
            time,
            false);
    r.setAllocatedTime(time);
    repo.addReservation(r);
  }

  private static void addVipWait(
      VipReservationRepo repo,
      String guestId,
      Room.RoomType roomType,
      int score,
      boolean boiling,
      LocalDateTime queuedAt) {
    String resId = "RES-" + (resCounter++);
    Reservation r =
        new Reservation(
            resId,
            guestId,
            NumberUtil.generateDigitPin(8),
            roomType,
            Reservation.Status.WAITING,
            boiling,
            score,
            queuedAt.minusMinutes(15),
            queuedAt,
            true);
    repo.addReservation(r);
  }

  private static void addVipAllocated(
      VipReservationRepo repo,
      AllocationRepo allocRepo,
      String guestId,
      Room.RoomType roomType,
      String roomNumber,
      LocalDateTime queuedAt,
      int holdingUsedMins,
      int graceMins) {
    String resId = "RES-" + (resCounter++);
    LocalDateTime allocatedTime = LocalDateTime.now().minusMinutes(holdingUsedMins);
    Reservation r =
        new Reservation(
            resId,
            guestId,
            NumberUtil.generateDigitPin(8),
            roomType,
            Reservation.Status.ALLOCATED,
            false,
            8000,
            queuedAt.minusMinutes(15),
            queuedAt,
            true);
    r.setAllocatedTime(allocatedTime);
    r.setAllocatedGraceMins(graceMins);
    r.setRoomNumber(roomNumber);
    repo.addReservation(r);

    long expMs =
        allocatedTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            + (graceMins * 60 * 1000L);
    allocRepo.addAllocationEntry(new AllocationEntry(resId, roomNumber, expMs));
  }

  private static Reservation addVipCheckedIn(
      VipReservationRepo repo,
      RoomRepo roomRepo,
      String guestId,
      Room.RoomType roomType,
      String roomNumber,
      LocalDateTime queuedAt,
      int holdingUsedMins,
      int graceMins,
      int nights) {
    String resId = "RES-" + (resCounter++);
    LocalDateTime checkedInAt = LocalDateTime.now().minusMinutes(holdingUsedMins);
    Reservation r =
        new Reservation(
            resId,
            guestId,
            NumberUtil.generateDigitPin(8),
            roomType,
            Reservation.Status.CHECKED_IN,
            false,
            8500,
            queuedAt.minusMinutes(15),
            queuedAt,
            true);
    r.setAllocatedTime(checkedInAt);
    r.setCheckInTime(checkedInAt);
    r.setAllocatedGraceMins(graceMins);
    r.setRoomNumber(roomNumber);
    r.setStayDays(nights);
    repo.addReservation(r);
    occupyRoom(roomRepo, roomNumber);
    return r;
  }

  private static Reservation addVipCheckedOut(
      VipReservationRepo repo,
      String guestId,
      Room.RoomType roomType,
      String roomNumber,
      LocalDateTime queuedAt,
      int holdingUsedMins,
      int graceMins,
      int nights) {
    String resId = "RES-" + (resCounter++);
    LocalDateTime checkedInAt = queuedAt.plusMinutes(holdingUsedMins);
    Reservation r =
        new Reservation(
            resId,
            guestId,
            NumberUtil.generateDigitPin(8),
            roomType,
            Reservation.Status.CHECKED_OUT,
            false,
            8500,
            queuedAt.minusMinutes(15),
            queuedAt,
            true);
    r.setAllocatedTime(checkedInAt);
    r.setCheckInTime(checkedInAt);
    r.setAllocatedGraceMins(graceMins);
    r.setRoomNumber(roomNumber);
    r.setStayDays(nights);
    repo.addReservation(r);
    return r;
  }

  private static void addVipNoShow(
      VipReservationRepo repo,
      String guestId,
      Room.RoomType roomType,
      LocalDateTime queuedAt,
      int holdingUsedMins,
      int graceMins) {
    String resId = "RES-" + (resCounter++);
    LocalDateTime allocatedTime = queuedAt.plusMinutes(10);
    Reservation r =
        new Reservation(
            resId,
            guestId,
            NumberUtil.generateDigitPin(8),
            roomType,
            Reservation.Status.NO_SHOW,
            false,
            8000,
            queuedAt.minusMinutes(15),
            queuedAt,
            true);
    r.setAllocatedTime(allocatedTime);
    r.setAllocatedGraceMins(graceMins);
    repo.addReservation(r);
  }

  private static void occupyRoom(RoomRepo roomRepo, String roomNumber) {
    Room room = roomRepo.findByRoomNumber(roomNumber);
    if (room != null && !room.getIsOccupied()) {
      room.setIsOccupied(true);
      roomRepo.updateRoom(room);
    }
  }

  private static void addBilling(
      BillingRepo repo,
      String guestId,
      Reservation reservation,
      String roomNumber,
      Room.RoomType roomType,
      LocalDate checkInDate,
      LocalDate checkOutDate,
      double ratePerNight,
      Billing.Status status,
      LocalDateTime createdAt) {
    String billingId = "BILL-" + (billingCounter++);
    Billing billing =
        new Billing(
            billingId,
            guestId,
            (reservation != null) ? reservation.getReservationId() : null,
            roomNumber,
            roomType,
            checkInDate,
            checkOutDate,
            ratePerNight,
            status,
            createdAt);
    repo.addBilling(billing);
  }
}
