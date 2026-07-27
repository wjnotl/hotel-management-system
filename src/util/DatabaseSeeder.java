package util;

import entity.Guest;
import entity.Member;
import entity.Reservation;
import java.time.LocalDateTime;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.VipReservationRepo;

public class DatabaseSeeder {

  public static void seedIfEmpty() {
    GuestRepo guestRepo = new GuestRepo();
    MemberRepo memberRepo = new MemberRepo();
    VipReservationRepo vipRepo = new VipReservationRepo();

    // Only seed if the guest repository is empty
    if (guestRepo.getGuestList().isEmpty()) {
      System.out.println("Seeding initial mock data...");

      // 1. Seed Members
      Member m1 = new Member("M-1001", Member.LoyaltyTier.DIAMOND, 12500);
      Member m2 = new Member("M-1002", Member.LoyaltyTier.GOLD, 4800);
      Member m3 = new Member("M-1003", Member.LoyaltyTier.SILVER, 1200);

      memberRepo.addMember(m1);
      memberRepo.addMember(m2);
      memberRepo.addMember(m3);

      // 2. Seed Guests
      Guest g1 =
          new Guest(
              "G-101",
              "Alex Johnson",
              "010203-10-5543",
              "A12345678",
              "alex@example.com",
              "012-3456789",
              "M-1001",
              0);
      Guest g2 =
          new Guest(
              "G-102",
              "Sarah Connor",
              "850412-14-6621",
              "B98765432",
              "sarah@example.com",
              "019-8765432",
              "M-1002",
              1);
      Guest g3 =
          new Guest(
              "G-103",
              "Charles Lim",
              "920811-08-3312",
              "C45678912",
              "charles@example.com",
              "016-1122334",
              "M-1003",
              2);
      Guest g4 =
          new Guest(
              "G-104",
              "Beatrice Tan",
              "981205-01-7789",
              "D33221100",
              "beatrice@example.com",
              "017-9988776",
              null,
              0);

      guestRepo.addGuest(g1);
      guestRepo.addGuest(g2);
      guestRepo.addGuest(g3);
      guestRepo.addGuest(g4);

      // 3. Seed VIP Reservations
      LocalDateTime now = LocalDateTime.now();

      Reservation r1 =
          new Reservation(
              "G-101",
              NumberUtil.generateDigitPin(8),
              Reservation.Status.WAITING,
              true,
              8180,
              now.minusMinutes(45),
              now.minusMinutes(30));
      Reservation r2 =
          new Reservation(
              "G-102",
              NumberUtil.generateDigitPin(8),
              Reservation.Status.WAITING,
              false,
              6500,
              now.minusMinutes(30),
              now.minusMinutes(20));
      Reservation r3 =
          new Reservation(
              "G-103",
              NumberUtil.generateDigitPin(8),
              Reservation.Status.WAITING,
              true,
              4200,
              now.minusMinutes(20),
              now.minusMinutes(15));
      Reservation r4 =
          new Reservation(
              "G-104",
              NumberUtil.generateDigitPin(8),
              Reservation.Status.WAITING,
              false,
              2100,
              now.minusMinutes(10),
              now.minusMinutes(5));

      vipRepo.addReservation(r1, r1.getPriorityScore());
      vipRepo.addReservation(r2, r2.getPriorityScore());
      vipRepo.addReservation(r3, r3.getPriorityScore());
      vipRepo.addReservation(r4, r4.getPriorityScore());

      System.out.println("Mock data successfully seeded!");
    }
  }
}
