package util;

import adt.ListInterface;
import entity.Billing;
import entity.Guest;
import entity.HousekeepingStaff;
import entity.Member;
import entity.Reservation;
import entity.Room;
import java.time.LocalDate;
import java.time.LocalDateTime;
import repo.BillingRepo;
import repo.BookingSettingsRepo;
import repo.GuestRepo;
import repo.HousekeepingStaffRepo;
import repo.MemberRepo;
import repo.ReservationRepo;
import repo.RoomRepo;
import repo.StandardReservationRepo;
import repo.VipReservationRepo;
import repo.VipSystemConfigRepo;

public class DatabaseSeeder {

  private static final Reservation.Status WAITING = Reservation.Status.WAITING;

  private static int resCounter = 10001;
  private static int billingCounter = 1001;

  public static void seedIfEmpty() {
    GuestRepo guestRepo = new GuestRepo();
    MemberRepo memberRepo = new MemberRepo();
    RoomRepo roomRepo = new RoomRepo();
    ReservationRepo reservationRepo = new ReservationRepo();
    VipReservationRepo vipRepo = new VipReservationRepo(reservationRepo);
    BillingRepo billingRepo = new BillingRepo();
    HousekeepingStaffRepo staffRepo = new HousekeepingStaffRepo();
    VipSystemConfigRepo vipSystemConfigRepo = new VipSystemConfigRepo();
    BookingSettingsRepo bookingSettingsRepo = new BookingSettingsRepo();
    StandardReservationRepo standardRepo =
        new StandardReservationRepo(reservationRepo, bookingSettingsRepo);

    if (guestRepo.getGuestList().isEmpty()) {
      System.out.println("Seeding expanded mock database...");

      // ==========================================
      // 1. SEED MEMBERS (VIP TIERS ONLY)
      // ==========================================
      Member m1 = new Member("M-1001", Member.LoyaltyTier.DIAMOND, 18500);
      Member m2 = new Member("M-1002", Member.LoyaltyTier.DIAMOND, 15200);
      Member m3 = new Member("M-1003", Member.LoyaltyTier.GOLD, 8900);
      Member m4 = new Member("M-1004", Member.LoyaltyTier.GOLD, 6400);
      Member m5 = new Member("M-1005", Member.LoyaltyTier.SILVER, 3100);
      Member m6 = new Member("M-1006", Member.LoyaltyTier.SILVER, 1800);

      memberRepo.addMember(m1);
      memberRepo.addMember(m2);
      memberRepo.addMember(m3);
      memberRepo.addMember(m4);
      memberRepo.addMember(m5);
      memberRepo.addMember(m6);

      // ==========================================
      // 2. SEED GUESTS (ALL LINKED TO MEMBER ACCOUNTS)
      // ==========================================
      Guest g1 =
          new Guest(
              "G-101",
              "Alex Johnson",
              "920101-14-5543",
              "A12345678",
              "alex@vip.com",
              "012-3456789",
              "M-1001",
              0);
      Guest g2 =
          new Guest(
              "G-102",
              "David Miller",
              "850312-08-3321",
              "B98765432",
              "david@vip.com",
              "019-1122334",
              "M-1002",
              0);
      Guest g3 =
          new Guest(
              "G-103",
              "Sarah Connor",
              "880520-10-6622",
              "C45678912",
              "sarah@vip.com",
              "017-9876543",
              "M-1003",
              1);
      Guest g4 =
          new Guest(
              "G-104",
              "Emma Watson",
              "940708-03-7711",
              "D33221100",
              "emma@vip.com",
              "011-2233445",
              "M-1004",
              0);
      Guest g5 =
          new Guest(
              "G-105",
              "Bruce Wayne",
              "901115-05-1122",
              "E55443322",
              "bruce@vip.com",
              "013-5566778",
              "M-1005",
              2);
      Guest g6 =
          new Guest(
              "G-106",
              "Charles Lim",
              "981205-01-7789",
              "F66778899",
              "charles@vip.com",
              "016-1122334",
              "M-1006",
              0);

      guestRepo.addGuest(g1);
      guestRepo.addGuest(g2);
      guestRepo.addGuest(g3);
      guestRepo.addGuest(g4);
      guestRepo.addGuest(g5);
      guestRepo.addGuest(g6);

      // ==========================================
      // 3. SEED ROOMS (VACANT & CLEAN TARGETS)
      // ==========================================
      Room roomL801 = new Room("L-801", Room.RoomType.LUXURY, Room.Status.VACANT_CLEAN, 850.00);
      Room roomL802 = new Room("L-802", Room.RoomType.LUXURY, Room.Status.VACANT_CLEAN, 850.00);
      Room roomS501 = new Room("S-501", Room.RoomType.SUITE, Room.Status.VACANT_CLEAN, 550.00);
      Room roomS502 = new Room("S-502", Room.RoomType.SUITE, Room.Status.VACANT_CLEAN, 550.00);
      Room roomST101 = new Room("ST-101", Room.RoomType.STANDARD, Room.Status.VACANT_CLEAN, 250.00);
      Room roomST102 = new Room("ST-102", Room.RoomType.STANDARD, Room.Status.VACANT_CLEAN, 250.00);

      roomRepo.addRoom(roomL801);
      roomRepo.addRoom(roomL802);
      roomRepo.addRoom(roomS501);
      roomRepo.addRoom(roomS502);
      roomRepo.addRoom(roomST101);
      roomRepo.addRoom(roomST102);

      // ==========================================
      // 4. SEED VIP RESERVATIONS (3 ROOM QUEUES)
      // ==========================================
      LocalDateTime now = LocalDateTime.now();

      // --- LUXURY QUEUE (5 Reservations) ---
      Reservation res101Luxury =
          addRes(
              vipRepo,
              "G-101",
              Room.RoomType.LUXURY,
              9500,
              true,
              now.minusMinutes(50),
              guestRepo,
              memberRepo,
              vipSystemConfigRepo);
      Reservation res102Luxury =
          addRes(
              vipRepo,
              "G-102",
              Room.RoomType.LUXURY,
              9100,
              false,
              now.minusMinutes(40),
              guestRepo,
              memberRepo,
              vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-101",
          Room.RoomType.LUXURY,
          8800,
          true,
          now.minusMinutes(35),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      Reservation res103Luxury =
          addRes(
              vipRepo,
              "G-103",
              Room.RoomType.LUXURY,
              7600,
              false,
              now.minusMinutes(25),
              guestRepo,
              memberRepo,
              vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-104",
          Room.RoomType.LUXURY,
          7100,
          false,
          now.minusMinutes(15),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);

      // --- SUITE QUEUE (11 Reservations) ---
      Reservation res101Suite =
          addRes(
              vipRepo,
              "G-101",
              Room.RoomType.SUITE,
              8900,
              false,
              now.minusMinutes(45),
              guestRepo,
              memberRepo,
              vipSystemConfigRepo);
      Reservation res102Suite =
          addRes(
              vipRepo,
              "G-102",
              Room.RoomType.SUITE,
              9300,
              true,
              now.minusMinutes(60),
              guestRepo,
              memberRepo,
              vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-103",
          Room.RoomType.SUITE,
          7800,
          true,
          now.minusMinutes(42),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      Reservation res104Suite =
          addRes(
              vipRepo,
              "G-104",
              Room.RoomType.SUITE,
              7400,
              false,
              now.minusMinutes(38),
              guestRepo,
              memberRepo,
              vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-102",
          Room.RoomType.SUITE,
          7200,
          false,
          now.minusMinutes(30),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      Reservation res105Suite =
          addRes(
              vipRepo,
              "G-105",
              Room.RoomType.SUITE,
              5800,
              true,
              now.minusMinutes(28),
              guestRepo,
              memberRepo,
              vipSystemConfigRepo);
      Reservation res106Suite =
          addRes(
              vipRepo,
              "G-106",
              Room.RoomType.SUITE,
              4900,
              false,
              now.minusMinutes(25),
              guestRepo,
              memberRepo,
              vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-103",
          Room.RoomType.SUITE,
          4600,
          false,
          now.minusMinutes(20),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-105",
          Room.RoomType.SUITE,
          4300,
          false,
          now.minusMinutes(18),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-106",
          Room.RoomType.SUITE,
          4100,
          false,
          now.minusMinutes(12),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-104",
          Room.RoomType.SUITE,
          3900,
          false,
          now.minusMinutes(5),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);

      // --- STANDARD QUEUE (19 Reservations) ---
      addRes(
          vipRepo,
          "G-101",
          Room.RoomType.STANDARD,
          9000,
          true,
          now.minusMinutes(70),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      Reservation res102Standard =
          addRes(
              vipRepo,
              "G-102",
              Room.RoomType.STANDARD,
              8700,
              false,
              now.minusMinutes(65),
              guestRepo,
              memberRepo,
              vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-103",
          Room.RoomType.STANDARD,
          7900,
          true,
          now.minusMinutes(58),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-104",
          Room.RoomType.STANDARD,
          7300,
          false,
          now.minusMinutes(55),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-101",
          Room.RoomType.STANDARD,
          7100,
          false,
          now.minusMinutes(50),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      Reservation res105Standard =
          addRes(
              vipRepo,
              "G-105",
              Room.RoomType.STANDARD,
              5900,
              true,
              now.minusMinutes(48),
              guestRepo,
              memberRepo,
              vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-106",
          Room.RoomType.STANDARD,
          5200,
          false,
          now.minusMinutes(44),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-102",
          Room.RoomType.STANDARD,
          5000,
          false,
          now.minusMinutes(40),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-103",
          Room.RoomType.STANDARD,
          4800,
          false,
          now.minusMinutes(36),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-105",
          Room.RoomType.STANDARD,
          4500,
          true,
          now.minusMinutes(32),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-104",
          Room.RoomType.STANDARD,
          4300,
          false,
          now.minusMinutes(28),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-106",
          Room.RoomType.STANDARD,
          4100,
          false,
          now.minusMinutes(25),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-105",
          Room.RoomType.STANDARD,
          3800,
          false,
          now.minusMinutes(22),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-103",
          Room.RoomType.STANDARD,
          3600,
          false,
          now.minusMinutes(18),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-106",
          Room.RoomType.STANDARD,
          3400,
          false,
          now.minusMinutes(15),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-105",
          Room.RoomType.STANDARD,
          3200,
          false,
          now.minusMinutes(12),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-104",
          Room.RoomType.STANDARD,
          3000,
          false,
          now.minusMinutes(9),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-106",
          Room.RoomType.STANDARD,
          2800,
          false,
          now.minusMinutes(6),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);
      addRes(
          vipRepo,
          "G-105",
          Room.RoomType.STANDARD,
          2500,
          false,
          now.minusMinutes(2),
          guestRepo,
          memberRepo,
          vipSystemConfigRepo);

      // ==========================================
      // 5. SEED BILLING / STAY HISTORY
      // ==========================================
      LocalDate today = LocalDate.now();

      addBilling(
          billingRepo,
          "G-101",
          res101Luxury,
          "L-801",
          Room.RoomType.LUXURY,
          today.minusDays(10),
          today.minusDays(7),
          roomL801.getPrice(),
          Billing.Status.PAID,
          now.minusDays(10));
      addBilling(
          billingRepo,
          "G-101",
          res101Suite,
          "S-501",
          Room.RoomType.SUITE,
          today.minusDays(2),
          today.plusDays(1),
          roomS501.getPrice(),
          Billing.Status.UNPAID,
          now.minusDays(2));
      addBilling(
          billingRepo,
          "G-102",
          res102Suite,
          "S-502",
          Room.RoomType.SUITE,
          today.minusDays(2),
          today.plusDays(1),
          roomS502.getPrice(),
          Billing.Status.UNPAID,
          now.minusDays(2));

      addBilling(
          billingRepo,
          "G-102",
          res102Luxury,
          "L-802",
          Room.RoomType.LUXURY,
          today.minusDays(20),
          today.minusDays(18),
          roomL802.getPrice(),
          Billing.Status.PAID,
          now.minusDays(20));
      addBilling(
          billingRepo,
          "G-102",
          res102Standard,
          "ST-101",
          Room.RoomType.STANDARD,
          today.minusDays(5),
          today.minusDays(3),
          roomST101.getPrice(),
          Billing.Status.PAID,
          now.minusDays(5));

      addBilling(
          billingRepo,
          "G-103",
          res103Luxury,
          "S-502",
          Room.RoomType.SUITE,
          today.minusDays(15),
          today.minusDays(12),
          roomS502.getPrice(),
          Billing.Status.PAID,
          now.minusDays(15));

      addBilling(
          billingRepo,
          "G-104",
          res104Suite,
          "ST-102",
          Room.RoomType.STANDARD,
          today.minusDays(6),
          today.minusDays(4),
          roomST102.getPrice(),
          Billing.Status.UNPAID,
          now.minusDays(6));

      addBilling(
          billingRepo,
          "G-105",
          res105Suite,
          "L-801",
          Room.RoomType.LUXURY,
          today.minusDays(30),
          today.minusDays(27),
          roomL801.getPrice(),
          Billing.Status.PAID,
          now.minusDays(30));
      addBilling(
          billingRepo,
          "G-105",
          res105Standard,
          "S-501",
          Room.RoomType.SUITE,
          today.minusDays(1),
          today.plusDays(2),
          roomS501.getPrice(),
          Billing.Status.UNPAID,
          now.minusDays(1));

      addBilling(
          billingRepo,
          "G-106",
          res106Suite,
          "ST-101",
          Room.RoomType.STANDARD,
          today.minusDays(9),
          today.minusDays(7),
          roomST101.getPrice(),
          Billing.Status.PAID,
          now.minusDays(9));
      // ==========================================
      // 5b. MARK ROOMS WITH AN ACTIVE STAY AS OCCUPIED
      // ==========================================
      ListInterface<Billing> seededBillings = billingRepo.getBillingList();
      for (int i = 1; i <= seededBillings.getNumberOfEntries(); i++) {
        Billing b = seededBillings.getEntry(i);
        if (b == null || b.getCheckOutDate() == null) continue;
        if (b.getCheckOutDate().isBefore(today)) continue; // already checked out - historical

        Room activeRoom = roomRepo.findByRoomNumber(b.getRoomNumber());
        if (activeRoom != null && !activeRoom.getIsOccupied()) {
          activeRoom.setIsOccupied(true);
          roomRepo.updateRoom(activeRoom);
        }
      }

      // ==========================================
      // 6. SEED HOUSEKEEPING STAFF
      // ==========================================
      HousekeepingStaff staff1 =
          new HousekeepingStaff(
              "EMP-001",
              "Zhi Kang",
              HousekeepingStaff.Shift.MORNING,
              HousekeepingStaff.Availability.AVAILABLE);

      HousekeepingStaff staff2 =
          new HousekeepingStaff(
              "EMP-002",
              "Di Yao",
              HousekeepingStaff.Shift.MORNING,
              HousekeepingStaff.Availability.AVAILABLE);

      HousekeepingStaff staff3 =
          new HousekeepingStaff(
              "EMP-003",
              "Jian Chin",
              HousekeepingStaff.Shift.AFTERNOON,
              HousekeepingStaff.Availability.AVAILABLE);

      HousekeepingStaff staff4 =
          new HousekeepingStaff(
              "EMP-004",
              "Chu Han",
              HousekeepingStaff.Shift.AFTERNOON,
              HousekeepingStaff.Availability.OFF_DUTY);

      HousekeepingStaff staff5 =
          new HousekeepingStaff(
              "EMP-005",
              "Luo Feng",
              HousekeepingStaff.Shift.NIGHT,
              HousekeepingStaff.Availability.AVAILABLE);

      HousekeepingStaff staff6 =
          new HousekeepingStaff(
              "EMP-006",
              "Cheng Yu",
              HousekeepingStaff.Shift.NIGHT,
              HousekeepingStaff.Availability.AVAILABLE);

      staffRepo.addStaff(staff1);
      staffRepo.addStaff(staff2);
      staffRepo.addStaff(staff3);
      staffRepo.addStaff(staff4);
      staffRepo.addStaff(staff5);
      staffRepo.addStaff(staff6);

      // ==========================================
      // 7. TOP UP ROOM INVENTORY
      // ==========================================
      // Six rooms cannot absorb the seeded demand, which leaves every queue
      // permanently
      // starved. Ten more of each type gives both the VIP heap and the standard lines
      // something to actually allocate.
      for (int i = 3; i <= 12; i++) {
        String suffix = String.format("%02d", i);
        roomRepo.addRoom(
            new Room("L-8" + suffix, Room.RoomType.LUXURY, Room.Status.VACANT_CLEAN, 850.00));
        roomRepo.addRoom(
            new Room("S-5" + suffix, Room.RoomType.SUITE, Room.Status.VACANT_CLEAN, 550.00));
        roomRepo.addRoom(
            new Room("ST-1" + suffix, Room.RoomType.STANDARD, Room.Status.VACANT_CLEAN, 250.00));
      }

      // ==========================================
      // 8. SEED WALK-IN GUESTS (NO LOYALTY CARD)
      // ==========================================
      // Every guest above carries a member card, so the standard queue would
      // otherwise have
      // no NON-MEMBER rows for the booking reports to filter on.
      Guest g7 =
          new Guest(
              "G-107",
              "Nurul Aina",
              "970214-06-5120",
              "G11223344",
              "nurul@mail.com",
              "012-7788990",
              null,
              0);
      Guest g8 =
          new Guest(
              "G-108",
              "Tan Wei Ming",
              "911003-07-4431",
              "H55667788",
              "weiming@mail.com",
              "018-3344556",
              null,
              0);
      Guest g9 =
          new Guest(
              "G-109",
              "Rajesh Kumar",
              "860625-14-8890",
              "J99001122",
              "rajesh@mail.com",
              "014-6677889",
              null,
              1);
      Guest g10 =
          new Guest(
              "G-110",
              "Chloe Fernandez",
              "000118-10-2367",
              "K12459876",
              "chloe@mail.com",
              "010-2244668",
              null,
              2);
      // G-111 was a byte for byte copy of G-110's IC, passport and phone. The registration form
      // refuses all three as duplicates, so the seed was creating a guest the app itself could not
      // have created.
      Guest g11 =
          new Guest(
              "G-111",
              "John Doe",
              "930417-11-5508",
              "L77553311",
              "john@mail.com",
              "017-4455662",
              null,
              0);

      guestRepo.addGuest(g7);
      guestRepo.addGuest(g8);
      guestRepo.addGuest(g9);
      guestRepo.addGuest(g10);
      guestRepo.addGuest(g11);

      // Eight guests fill a line at the default capacity, and a line cannot be shown refusing the
      // ninth without nine distinct people to put in it.
      guestRepo.addGuest(
          walkIn(
              "G-112",
              "Siti Rahman",
              "960822-04-3376",
              "M20114499",
              "siti@mail.com",
              "011-9080706",
              0));
      guestRepo.addGuest(
          walkIn(
              "G-113",
              "Marcus Ooi",
              "890130-12-9042",
              "N65432198",
              "marcus@mail.com",
              "012-3311557",
              0));
      guestRepo.addGuest(
          walkIn(
              "G-114",
              "Priya Nair",
              "010506-08-2213",
              "P30298471",
              "priya@mail.com",
              "016-7742093",
              0));
      guestRepo.addGuest(
          walkIn(
              "G-115",
              "Daniel Yeoh",
              "870919-02-6684",
              "Q48120367",
              "daniel@mail.com",
              "019-2286134",
              1));
      guestRepo.addGuest(
          walkIn(
              "G-116",
              "Aisyah Karim",
              "990311-05-7128",
              "R91038265",
              "aisyah@mail.com",
              "013-6650428",
              0));
      guestRepo.addGuest(
          walkIn(
              "G-117",
              "Lim Kai Sheng",
              "920724-01-4459",
              "S57402913",
              "kaisheng@mail.com",
              "018-1194762",
              0));
      guestRepo.addGuest(
          walkIn(
              "G-118",
              "Grace Anandan",
              "950208-14-8830",
              "T26719048",
              "grace@mail.com",
              "010-5583910",
              0));
      guestRepo.addGuest(
          walkIn(
              "G-119",
              "Haziq Idris",
              "031127-06-1195",
              "U84306172",
              "haziq@mail.com",
              "014-9928375",
              0));
      guestRepo.addGuest(
          walkIn(
              "G-120",
              "Wong Mei Ling",
              "880605-10-3341",
              "V13975860",
              "meiling@mail.com",
              "017-3348820",
              0));

      // Carries nothing but one booking due today, so walking them in exercises the claim path on
      // its own rather than tangled with a queue place or a second booking.
      guestRepo.addGuest(
          walkIn(
              "G-121",
              "Ravi Chandran",
              "910413-07-2264",
              "W60817253",
              "ravi@mail.com",
              "012-4407719",
              0));

      // ==========================================
      // 9. SEED STANDARD RESERVATIONS (WALK-IN & BOOKING TEST BED)
      // ==========================================
      // Every state the module can be in is represented once, so a marker can reach each screen
      // from a fresh seed without typing a setup first. Rooms L-801, L-802, S-501, S-502, ST-101
      // and ST-102 belong to the billing history above, so nothing here touches them.

      // --- STANDARD line: 7 waiting against a default capacity of 8 ---
      // One slot left, so the next walk-in fills the line and the one after it is refused. Both
      // halves of the fixed-capacity rule are reachable without editing anything first.
      addStandardRes(standardRepo, "G-107", Room.RoomType.STANDARD, WAITING, now.minusMinutes(64));
      addStandardRes(standardRepo, "G-108", Room.RoomType.STANDARD, WAITING, now.minusMinutes(55));
      addStandardRes(standardRepo, "G-112", Room.RoomType.STANDARD, WAITING, now.minusMinutes(47));
      addStandardRes(standardRepo, "G-113", Room.RoomType.STANDARD, WAITING, now.minusMinutes(38));
      addStandardRes(standardRepo, "G-114", Room.RoomType.STANDARD, WAITING, now.minusMinutes(29));
      addStandardRes(standardRepo, "G-115", Room.RoomType.STANDARD, WAITING, now.minusMinutes(21));
      addStandardRes(standardRepo, "G-116", Room.RoomType.STANDARD, WAITING, now.minusMinutes(12));

      // --- LUXURY line: 3 waiting, with room to take more ---
      // G-112 is already in the STANDARD line. One guest may hold a place in several lines but
      // only one place per line, so this row is what proves the first half of that rule.
      addStandardRes(standardRepo, "G-109", Room.RoomType.LUXURY, WAITING, now.minusMinutes(41));
      addStandardRes(standardRepo, "G-118", Room.RoomType.LUXURY, WAITING, now.minusMinutes(26));
      addStandardRes(standardRepo, "G-112", Room.RoomType.LUXURY, WAITING, now.minusMinutes(9));

      // --- SUITE line: 2 waiting ---
      addStandardRes(standardRepo, "G-119", Room.RoomType.SUITE, WAITING, now.minusMinutes(33));
      addStandardRes(standardRepo, "G-120", Room.RoomType.SUITE, WAITING, now.minusMinutes(7));

      // --- HOLD past its grace window ---
      // G-110 already carries 2 strikes and the house limit is 3, so sweeping this hold takes them
      // to the limit and closes the booking as a no-show instead of returning them to the line.
      hold(
          standardRepo,
          roomRepo,
          "G-110",
          Room.RoomType.STANDARD,
          "ST-103",
          now.minusMinutes(58),
          now.minusMinutes(23));

      // --- HOLD still inside its grace window ---
      // Reachable from [C] Manage Holds, which offers check-in or a hand marked no-show.
      hold(
          standardRepo,
          roomRepo,
          "G-111",
          Room.RoomType.LUXURY,
          "L-803",
          now.minusMinutes(37),
          now.minusMinutes(3));

      // --- ADVANCE BOOKINGS, one per outcome of the arrival date rule ---
      // A booking may only be checked in on the night it reserves, so only the first of these
      // five can be taken up with [M] today.
      advance(standardRepo, "G-101", Room.RoomType.LUXURY, today.atStartOfDay(), 2);
      advance(standardRepo, "G-102", Room.RoomType.STANDARD, today.plusDays(1).atStartOfDay(), 3);
      advance(standardRepo, "G-103", Room.RoomType.LUXURY, today.plusDays(4).atStartOfDay(), 1);
      advance(standardRepo, "G-104", Room.RoomType.STANDARD, today.plusDays(9).atStartOfDay(), 5);
      advance(standardRepo, "G-105", Room.RoomType.SUITE, today.minusDays(2).atStartOfDay(), 2);
      advance(standardRepo, "G-106", Room.RoomType.STANDARD, today.minusDays(1).atStartOfDay(), 1);

      // Due today as well, and against the STANDARD line, so the queue screen has a room held back
      // for an arrival and the walk-in flow has a booking it may actually claim.
      advance(standardRepo, "G-121", Room.RoomType.STANDARD, today.atStartOfDay(), 2);

      // --- A date with no SUITE left ---
      // Twelve bookings against twelve SUITE rooms commit the type solid for two nights, which is
      // what NO ROOMS ON THAT DATE, findFirstFullDate and findLongestBookableStay are for.
      String[] suiteHolders = {
        "G-107", "G-108", "G-109", "G-110", "G-111", "G-112",
        "G-113", "G-114", "G-115", "G-116", "G-117", "G-118"
      };
      for (int i = 0; i < suiteHolders.length; i++) {
        advance(
            standardRepo,
            suiteHolders[i],
            Room.RoomType.SUITE,
            today.plusDays(5).atStartOfDay(),
            2);
      }

      // --- CLOSED HISTORY ---
      // The two reports aggregate outcomes, so they need finished records spread across enough
      // days that TODAY, LAST 7 DAYS and a custom range each return a different set.
      closed(
          standardRepo,
          "G-113",
          Room.RoomType.STANDARD,
          Reservation.Status.NO_SHOW,
          now.minusHours(5),
          now.minusHours(4));
      closed(
          standardRepo,
          "G-114",
          Room.RoomType.SUITE,
          Reservation.Status.NO_SHOW,
          now.minusHours(9),
          now.minusHours(8));
      closed(
          standardRepo,
          "G-119",
          Room.RoomType.LUXURY,
          Reservation.Status.NO_SHOW,
          now.minusDays(1).minusHours(3),
          now.minusDays(1).minusHours(2));
      closed(
          standardRepo,
          "G-120",
          Room.RoomType.STANDARD,
          Reservation.Status.NO_SHOW,
          now.minusDays(3).minusHours(6),
          now.minusDays(3).minusHours(5));

      checkedOut(
          standardRepo,
          "G-107",
          Room.RoomType.STANDARD,
          "ST-104",
          now.minusDays(2).minusHours(7),
          now.minusDays(2).minusHours(6),
          1);
      checkedOut(
          standardRepo,
          "G-108",
          Room.RoomType.LUXURY,
          "L-804",
          now.minusDays(3).minusHours(4),
          now.minusDays(3).minusHours(4),
          2);
      checkedOut(
          standardRepo,
          "G-115",
          Room.RoomType.SUITE,
          "S-503",
          now.minusDays(5).minusHours(2),
          now.minusDays(5).minusHours(1),
          3);
      checkedOut(
          standardRepo,
          "G-116",
          Room.RoomType.STANDARD,
          "ST-105",
          now.minusDays(6).minusHours(8),
          now.minusDays(6).minusHours(7),
          1);

      // --- STAYS RUNNING RIGHT NOW ---
      // These hold their rooms, which is what makes the queue screen's vacant count move.
      staying(
          standardRepo,
          roomRepo,
          "G-117",
          Room.RoomType.STANDARD,
          "ST-106",
          now.minusHours(6),
          now.minusHours(5),
          2);
      staying(
          standardRepo,
          roomRepo,
          "G-118",
          Room.RoomType.LUXURY,
          "L-805",
          now.minusHours(4),
          now.minusHours(3),
          3);

      System.out.println("Mock database seeded successfully!");
    }
  }

  private static Reservation addRes(
      VipReservationRepo repo,
      String guestId,
      Room.RoomType roomType,
      int priorityScore,
      boolean isBoiling,
      LocalDateTime arrivalTime,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      VipSystemConfigRepo vipSystemConfigRepo) {

    String resId = "RES-" + (resCounter++);

    Reservation r =
        new Reservation(
            resId,
            guestId,
            NumberUtil.generateDigitPin(8),
            roomType,
            Reservation.Status.WAITING,
            isBoiling,
            priorityScore,
            arrivalTime.minusMinutes(15),
            arrivalTime,
            true);

    repo.addReservation(r);
    return r;
  }

  private static Guest walkIn(
      String id, String name, String ic, String passport, String email, String phone, int strikes) {
    return new Guest(id, name, ic, passport, email, phone, null, strikes);
  }

  // Standard bookings carry no priority score: position in the line is earned by arrival order
  // alone. A null arrival time means an advance booking that has not walked in yet.
  private static Reservation addStandardRes(
      StandardReservationRepo repo,
      String guestId,
      Room.RoomType roomType,
      Reservation.Status status,
      LocalDateTime queueArrivalTime) {

    LocalDateTime reservationTime =
        (queueArrivalTime != null) ? queueArrivalTime.minusMinutes(10) : LocalDateTime.now();

    Reservation r =
        new Reservation(
            repo.generateReservationId(),
            guestId,
            repo.generateConfirmationNumber(),
            roomType,
            status,
            false,
            0,
            reservationTime,
            queueArrivalTime,
            false);

    repo.addReservation(r);
    return r;
  }

  // A room called for a guest who has not taken it yet. The grace window is measured from
  // allocatedTime, so how long ago that was decides whether the next screen sweeps it.
  private static void hold(
      StandardReservationRepo repo,
      RoomRepo roomRepo,
      String guestId,
      Room.RoomType roomType,
      String roomNumber,
      LocalDateTime queuedAt,
      LocalDateTime allocatedAt) {

    Reservation r = addStandardRes(repo, guestId, roomType, Reservation.Status.ALLOCATED, queuedAt);
    r.setAllocatedTime(allocatedAt);
    r.setRoomNumber(roomNumber);
    repo.updateReservation(r);

    occupy(roomRepo, roomNumber);
  }

  // An advance booking never enters a line, so it carries no queue arrival time. It does carry the
  // night it was sold for, which is the only night it may be checked in on.
  private static void advance(
      StandardReservationRepo repo,
      String guestId,
      Room.RoomType roomType,
      LocalDateTime arrival,
      int nights) {

    Reservation r = addStandardRes(repo, guestId, roomType, Reservation.Status.RESERVED, null);
    r.setReservationTime(arrival.minusDays(3));
    r.setExpectedArrivalTime(arrival);
    r.setStayDays(nights);
    repo.updateReservation(r);
  }

  // A finished record the reports can count. Both timestamps are kept because the wait between
  // them is exactly what the Queue Performance report measures.
  private static void closed(
      StandardReservationRepo repo,
      String guestId,
      Room.RoomType roomType,
      Reservation.Status status,
      LocalDateTime queuedAt,
      LocalDateTime allocatedAt) {

    Reservation r = addStandardRes(repo, guestId, roomType, status, queuedAt);
    r.setAllocatedTime(allocatedAt);
    repo.updateReservation(r);
  }

  // A stay that has already ended. getOccupancyEndDate collapses to the start date for a
  // CHECKED_OUT record, so it consumes no room today and only shows up in history.
  private static void checkedOut(
      StandardReservationRepo repo,
      String guestId,
      Room.RoomType roomType,
      String roomNumber,
      LocalDateTime queuedAt,
      LocalDateTime allocatedAt,
      int nights) {

    Reservation r =
        addStandardRes(repo, guestId, roomType, Reservation.Status.CHECKED_OUT, queuedAt);
    r.setAllocatedTime(allocatedAt);
    r.setCheckInTime(allocatedAt);
    r.setRoomNumber(roomNumber);
    r.setStayDays(nights);
    repo.updateReservation(r);
  }

  // A stay in progress. The room is marked taken, because a guest in it is what makes the vacant
  // count on the queue screen smaller than the raw room list.
  private static void staying(
      StandardReservationRepo repo,
      RoomRepo roomRepo,
      String guestId,
      Room.RoomType roomType,
      String roomNumber,
      LocalDateTime queuedAt,
      LocalDateTime checkedInAt,
      int nights) {

    Reservation r =
        addStandardRes(repo, guestId, roomType, Reservation.Status.CHECKED_IN, queuedAt);
    r.setAllocatedTime(checkedInAt);
    r.setCheckInTime(checkedInAt);
    r.setRoomNumber(roomNumber);
    r.setStayDays(nights);
    repo.updateReservation(r);

    occupy(roomRepo, roomNumber);
  }

  private static void occupy(RoomRepo roomRepo, String roomNumber) {
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
