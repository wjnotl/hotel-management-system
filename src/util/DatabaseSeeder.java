package util;

import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import java.time.LocalDateTime;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.RoomRepo;
import repo.VipReservationRepo;

public class DatabaseSeeder {

  private static int resCounter = 10001;

  public static void seedIfEmpty() {
    GuestRepo guestRepo = new GuestRepo();
    MemberRepo memberRepo = new MemberRepo();
    RoomRepo roomRepo = new RoomRepo();
    VipReservationRepo vipRepo = new VipReservationRepo();

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
      roomRepo.addRoom(new Room("L-801", Room.RoomType.LUXURY, Room.Status.VACANT_CLEAN, null));
      roomRepo.addRoom(new Room("L-802", Room.RoomType.LUXURY, Room.Status.VACANT_CLEAN, null));
      roomRepo.addRoom(new Room("S-501", Room.RoomType.SUITE, Room.Status.VACANT_CLEAN, null));
      roomRepo.addRoom(new Room("S-502", Room.RoomType.SUITE, Room.Status.VACANT_CLEAN, null));
      roomRepo.addRoom(new Room("ST-101", Room.RoomType.STANDARD, Room.Status.VACANT_CLEAN, null));
      roomRepo.addRoom(new Room("ST-102", Room.RoomType.STANDARD, Room.Status.VACANT_CLEAN, null));

      // ==========================================
      // 4. SEED VIP RESERVATIONS (3 ROOM QUEUES)
      // ==========================================
      LocalDateTime now = LocalDateTime.now();

      // --- LUXURY QUEUE (5 Reservations) ---
      addRes(
          vipRepo, "G-101", Room.RoomType.LUXURY, 9500, true, now.minusMinutes(50)); // Alex (Res 1)
      addRes(
          vipRepo,
          "G-102",
          Room.RoomType.LUXURY,
          9100,
          false,
          now.minusMinutes(40)); // David (Res 1)
      addRes(
          vipRepo,
          "G-101",
          Room.RoomType.LUXURY,
          8800,
          true,
          now.minusMinutes(35)); // Alex (Res 2 - Multi)
      addRes(vipRepo, "G-103", Room.RoomType.LUXURY, 7600, false, now.minusMinutes(25)); // Sarah
      addRes(vipRepo, "G-104", Room.RoomType.LUXURY, 7100, false, now.minusMinutes(15)); // Emma

      // --- SUITE QUEUE (11 Reservations) ---
      addRes(
          vipRepo,
          "G-102",
          Room.RoomType.SUITE,
          9300,
          true,
          now.minusMinutes(60)); // David (Res 2 - Multi)
      addRes(
          vipRepo,
          "G-101",
          Room.RoomType.SUITE,
          8900,
          false,
          now.minusMinutes(45)); // Alex (Res 3 - Multi)
      addRes(
          vipRepo, "G-103", Room.RoomType.SUITE, 7800, true, now.minusMinutes(42)); // Sarah (Res 2)
      addRes(
          vipRepo, "G-104", Room.RoomType.SUITE, 7400, false, now.minusMinutes(38)); // Emma (Res 2)
      addRes(
          vipRepo,
          "G-102",
          Room.RoomType.SUITE,
          7200,
          false,
          now.minusMinutes(30)); // David (Res 3 - Multi)
      addRes(vipRepo, "G-105", Room.RoomType.SUITE, 5800, true, now.minusMinutes(28)); // Bruce
      addRes(vipRepo, "G-106", Room.RoomType.SUITE, 4900, false, now.minusMinutes(25)); // Charles
      addRes(
          vipRepo,
          "G-103",
          Room.RoomType.SUITE,
          4600,
          false,
          now.minusMinutes(20)); // Sarah (Res 3)
      addRes(
          vipRepo,
          "G-105",
          Room.RoomType.SUITE,
          4300,
          false,
          now.minusMinutes(18)); // Bruce (Res 2)
      addRes(
          vipRepo,
          "G-106",
          Room.RoomType.SUITE,
          4100,
          false,
          now.minusMinutes(12)); // Charles (Res 2)
      addRes(
          vipRepo, "G-104", Room.RoomType.SUITE, 3900, false, now.minusMinutes(5)); // Emma (Res 3)

      // --- STANDARD QUEUE (19 Reservations) ---
      addRes(
          vipRepo,
          "G-101",
          Room.RoomType.STANDARD,
          9000,
          true,
          now.minusMinutes(70)); // Alex (Res 4)
      addRes(
          vipRepo,
          "G-102",
          Room.RoomType.STANDARD,
          8700,
          false,
          now.minusMinutes(65)); // David (Res 4)
      addRes(
          vipRepo,
          "G-103",
          Room.RoomType.STANDARD,
          7900,
          true,
          now.minusMinutes(58)); // Sarah (Res 4)
      addRes(
          vipRepo,
          "G-104",
          Room.RoomType.STANDARD,
          7300,
          false,
          now.minusMinutes(55)); // Emma (Res 4)
      addRes(
          vipRepo,
          "G-101",
          Room.RoomType.STANDARD,
          7100,
          false,
          now.minusMinutes(50)); // Alex (Res 5)
      addRes(
          vipRepo,
          "G-105",
          Room.RoomType.STANDARD,
          5900,
          true,
          now.minusMinutes(48)); // Bruce (Res 3)
      addRes(
          vipRepo,
          "G-106",
          Room.RoomType.STANDARD,
          5200,
          false,
          now.minusMinutes(44)); // Charles (Res 3)
      addRes(
          vipRepo,
          "G-102",
          Room.RoomType.STANDARD,
          5000,
          false,
          now.minusMinutes(40)); // David (Res 5)
      addRes(
          vipRepo,
          "G-103",
          Room.RoomType.STANDARD,
          4800,
          false,
          now.minusMinutes(36)); // Sarah (Res 5)
      addRes(
          vipRepo,
          "G-105",
          Room.RoomType.STANDARD,
          4500,
          true,
          now.minusMinutes(32)); // Bruce (Res 4)
      addRes(
          vipRepo,
          "G-104",
          Room.RoomType.STANDARD,
          4300,
          false,
          now.minusMinutes(28)); // Emma (Res 5)
      addRes(
          vipRepo,
          "G-106",
          Room.RoomType.STANDARD,
          4100,
          false,
          now.minusMinutes(25)); // Charles (Res 4)
      addRes(
          vipRepo,
          "G-105",
          Room.RoomType.STANDARD,
          3800,
          false,
          now.minusMinutes(22)); // Bruce (Res 5)
      addRes(
          vipRepo,
          "G-103",
          Room.RoomType.STANDARD,
          3600,
          false,
          now.minusMinutes(18)); // Sarah (Res 6)
      addRes(
          vipRepo,
          "G-106",
          Room.RoomType.STANDARD,
          3400,
          false,
          now.minusMinutes(15)); // Charles (Res 5)
      addRes(
          vipRepo,
          "G-105",
          Room.RoomType.STANDARD,
          3200,
          false,
          now.minusMinutes(12)); // Bruce (Res 6)
      addRes(
          vipRepo,
          "G-104",
          Room.RoomType.STANDARD,
          3000,
          false,
          now.minusMinutes(9)); // Emma (Res 6)
      addRes(
          vipRepo,
          "G-106",
          Room.RoomType.STANDARD,
          2800,
          false,
          now.minusMinutes(6)); // Charles (Res 6)
      addRes(
          vipRepo,
          "G-105",
          Room.RoomType.STANDARD,
          2500,
          false,
          now.minusMinutes(2)); // Bruce (Res 7)

      System.out.println("Mock database seeded successfully!");
      System.out.println("  -> Luxury Queue   : 5 Reservations");
      System.out.println("  -> Suite Queue    : 11 Reservations");
      System.out.println("  -> Standard Queue : 19 Reservations");
    }
  }

  private static void addRes(
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
  }
}
