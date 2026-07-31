package util;

import entity.Billing;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import java.time.LocalDate;
import java.time.LocalDateTime;
import repo.BillingRepo;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.RoomRepo;
import repo.VipReservationRepo;

public class DatabaseSeeder {

  private static int resCounter = 10001;
  private static int billingCounter = 1001;

  public static void seedIfEmpty() {
    GuestRepo guestRepo = new GuestRepo();
    MemberRepo memberRepo = new MemberRepo();
    RoomRepo roomRepo = new RoomRepo();
    VipReservationRepo vipRepo = new VipReservationRepo();
    BillingRepo billingRepo = new BillingRepo();

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
      Room roomL801 =
          new Room("L-801", Room.RoomType.LUXURY, Room.Status.VACANT_CLEAN, null, 850.00);
      Room roomL802 =
          new Room("L-802", Room.RoomType.LUXURY, Room.Status.VACANT_CLEAN, null, 850.00);
      Room roomS501 =
          new Room("S-501", Room.RoomType.SUITE, Room.Status.VACANT_CLEAN, null, 550.00);
      Room roomS502 =
          new Room("S-502", Room.RoomType.SUITE, Room.Status.VACANT_CLEAN, null, 550.00);
      Room roomST101 =
          new Room("ST-101", Room.RoomType.STANDARD, Room.Status.VACANT_CLEAN, null, 250.00);
      Room roomST102 =
          new Room("ST-102", Room.RoomType.STANDARD, Room.Status.VACANT_CLEAN, null, 250.00);

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
          addRes(vipRepo, "G-101", Room.RoomType.LUXURY, 9500, true, now.minusMinutes(50));
      Reservation res102Luxury =
          addRes(vipRepo, "G-102", Room.RoomType.LUXURY, 9100, false, now.minusMinutes(40));
      addRes(vipRepo, "G-101", Room.RoomType.LUXURY, 8800, true, now.minusMinutes(35));
      Reservation res103Luxury =
          addRes(vipRepo, "G-103", Room.RoomType.LUXURY, 7600, false, now.minusMinutes(25));
      addRes(vipRepo, "G-104", Room.RoomType.LUXURY, 7100, false, now.minusMinutes(15));

      // --- SUITE QUEUE (11 Reservations) ---
      Reservation res101Suite =
          addRes(vipRepo, "G-101", Room.RoomType.SUITE, 8900, false, now.minusMinutes(45));
      Reservation res102Suite =
          addRes(vipRepo, "G-102", Room.RoomType.SUITE, 9300, true, now.minusMinutes(60));
      addRes(vipRepo, "G-103", Room.RoomType.SUITE, 7800, true, now.minusMinutes(42));
      Reservation res104Suite =
          addRes(vipRepo, "G-104", Room.RoomType.SUITE, 7400, false, now.minusMinutes(38));
      addRes(vipRepo, "G-102", Room.RoomType.SUITE, 7200, false, now.minusMinutes(30));
      Reservation res105Suite =
          addRes(vipRepo, "G-105", Room.RoomType.SUITE, 5800, true, now.minusMinutes(28));
      Reservation res106Suite =
          addRes(vipRepo, "G-106", Room.RoomType.SUITE, 4900, false, now.minusMinutes(25));
      addRes(vipRepo, "G-103", Room.RoomType.SUITE, 4600, false, now.minusMinutes(20));
      addRes(vipRepo, "G-105", Room.RoomType.SUITE, 4300, false, now.minusMinutes(18));
      addRes(vipRepo, "G-106", Room.RoomType.SUITE, 4100, false, now.minusMinutes(12));
      addRes(vipRepo, "G-104", Room.RoomType.SUITE, 3900, false, now.minusMinutes(5));

      // --- STANDARD QUEUE (19 Reservations) ---
      addRes(vipRepo, "G-101", Room.RoomType.STANDARD, 9000, true, now.minusMinutes(70));
      Reservation res102Standard =
          addRes(vipRepo, "G-102", Room.RoomType.STANDARD, 8700, false, now.minusMinutes(65));
      addRes(vipRepo, "G-103", Room.RoomType.STANDARD, 7900, true, now.minusMinutes(58));
      addRes(vipRepo, "G-104", Room.RoomType.STANDARD, 7300, false, now.minusMinutes(55));
      addRes(vipRepo, "G-101", Room.RoomType.STANDARD, 7100, false, now.minusMinutes(50));
      Reservation res105Standard =
          addRes(vipRepo, "G-105", Room.RoomType.STANDARD, 5900, true, now.minusMinutes(48));
      addRes(vipRepo, "G-106", Room.RoomType.STANDARD, 5200, false, now.minusMinutes(44));
      addRes(vipRepo, "G-102", Room.RoomType.STANDARD, 5000, false, now.minusMinutes(40));
      addRes(vipRepo, "G-103", Room.RoomType.STANDARD, 4800, false, now.minusMinutes(36));
      addRes(vipRepo, "G-105", Room.RoomType.STANDARD, 4500, true, now.minusMinutes(32));
      addRes(vipRepo, "G-104", Room.RoomType.STANDARD, 4300, false, now.minusMinutes(28));
      addRes(vipRepo, "G-106", Room.RoomType.STANDARD, 4100, false, now.minusMinutes(25));
      addRes(vipRepo, "G-105", Room.RoomType.STANDARD, 3800, false, now.minusMinutes(22));
      addRes(vipRepo, "G-103", Room.RoomType.STANDARD, 3600, false, now.minusMinutes(18));
      addRes(vipRepo, "G-106", Room.RoomType.STANDARD, 3400, false, now.minusMinutes(15));
      addRes(vipRepo, "G-105", Room.RoomType.STANDARD, 3200, false, now.minusMinutes(12));
      addRes(vipRepo, "G-104", Room.RoomType.STANDARD, 3000, false, now.minusMinutes(9));
      addRes(vipRepo, "G-106", Room.RoomType.STANDARD, 2800, false, now.minusMinutes(6));
      addRes(vipRepo, "G-105", Room.RoomType.STANDARD, 2500, false, now.minusMinutes(2));

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

      System.out.println("Mock database seeded successfully!");
    }
  }

  private static Reservation addRes(
      VipReservationRepo repo,
      String guestId,
      Room.RoomType roomType,
      int priorityScore,
      boolean isBoiling,
      LocalDateTime arrivalTime) {

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
            arrivalTime);

    repo.addReservation(r, priorityScore);
    return r;
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
