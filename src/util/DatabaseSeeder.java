package util;

import control.booking.BookingSettingsStore;
import entity.Billing;
import entity.Guest;
import entity.HousekeepingStaff;
import entity.HousekeepingTask;
import entity.Member;
import entity.Reservation;
import entity.Room;
import entity.VipSystemConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    VipSystemConfigRepo vipSystemConfigRepo = new VipSystemConfigRepo();
    BookingSettingsStore bookingSettingsStore = new BookingSettingsStore();
    StandardReservationRepo standardRepo =
        new StandardReservationRepo(reservationRepo, bookingSettingsStore::getSettings);

    if (!guestRepo.getGuestList().isEmpty()) {
      return;
    }

    VipSystemConfig vipSystemConfig = new VipSystemConfig();
    vipSystemConfig.setLastStrikeResetDate(LocalDate.now());
    vipSystemConfigRepo.updateConfig(vipSystemConfig);

    System.out.println("Seeding system-wide master dataset...");
    LocalDateTime now = LocalDateTime.now();
    LocalDate today = LocalDate.now();

    // Seed members
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

    // Seed guests
    guestRepo.addGuest(
        new Guest(
            "G-101",
            "Tan Wei Meng",
            "880512-14-5211",
            "A5239102",
            "tan.weimeng@gmail.com",
            "012-3849102",
            "M-1001",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-102",
            "Siti Nurhaliza",
            "910324-10-5842",
            "A8492019",
            "siti.nur91@yahoo.com",
            "016-7281940",
            "M-1002",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-103",
            "Rajesh Kumar",
            "851108-08-6123",
            "A3920184",
            "rajesh.kumar@outlook.com",
            "019-4820193",
            "M-1003",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-104",
            "Alexander Wong",
            "930715-14-5029",
            "A9102834",
            "alex.wong@hotmail.com",
            "017-3920184",
            "M-1004",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-105",
            "Fatimah Ahmad",
            "870902-03-5104",
            "A7492018",
            "fatimah.ahmad@gmail.com",
            "013-8291048",
            "M-1005",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-106",
            "Chong Kah Wai",
            "941230-07-5931",
            "A1092837",
            "kwchong@yahoo.com",
            "014-9281039",
            "M-1006",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-107",
            "Priya Sundram",
            "920418-05-5208",
            "A4820193",
            "priya.sundram@gmail.com",
            "011-1029384",
            "M-1007",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-108",
            "Marcus Lee",
            "890822-10-5397",
            "A6029183",
            "marcus.lee@icloud.com",
            "012-9482019",
            "M-1008",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-109",
            "Noraini Zulkifli",
            "900614-14-5612",
            "A2938104",
            "noraini.z@gmail.com",
            "018-3029184",
            "M-1009",
            1));
    guestRepo.addGuest(
        new Guest(
            "G-110",
            "Kevin Teoh",
            "860105-08-5489",
            "A7102938",
            "kevin.teoh@gmail.com",
            "019-2830194",
            "M-1010",
            1));

    // Gold Guests (G-111..G-125): 11 w/ 0 strikes, 2 w/ 1 strike, 2 w/ 2 strikes
    guestRepo.addGuest(
        new Guest(
            "G-111",
            "Ahmad Ridzuan",
            "910819-01-5233",
            "A3820194",
            "ridzuan.ahmad@gmail.com",
            "012-7492018",
            "M-1011",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-112",
            "Grace Lim",
            "950211-14-5820",
            "A9281039",
            "grace.lim95@yahoo.com",
            "016-8302918",
            "M-1012",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-113",
            "Devi Annamalai",
            "880429-08-5192",
            "A4029183",
            "devi.a@outlook.com",
            "017-2930184",
            "M-1013",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-114",
            "Bernard Chen",
            "921005-07-5381",
            "A8192039",
            "bchen@gmail.com",
            "013-9482019",
            "M-1014",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-115",
            "Zainab Ibrahim",
            "870617-10-5028",
            "A1928374",
            "zainab.ibrahim@hotmail.com",
            "014-8392019",
            "M-1015",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-116",
            "Jason Khow",
            "940912-14-5739",
            "A6729104",
            "jason.khow@gmail.com",
            "011-2938401",
            "M-1016",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-117",
            "Kavitha Loganathan",
            "900325-05-5910",
            "A5829103",
            "kavitha.l@yahoo.com",
            "018-9283019",
            "M-1017",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-118",
            "Eric Leong",
            "891114-08-5421",
            "A3928104",
            "eric.leong@gmail.com",
            "019-3829104",
            "M-1018",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-119",
            "Nurul Huda",
            "930130-03-5182",
            "A9028193",
            "nurul.huda93@gmail.com",
            "012-6820194",
            "M-1019",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-120",
            "Dominic Yip",
            "860722-14-5390",
            "A2839104",
            "dom.yip@outlook.com",
            "016-9382019",
            "M-1020",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-121",
            "Subramaniam Naidu",
            "850510-10-6012",
            "A7392018",
            "snaidu@gmail.com",
            "017-8291049",
            "M-1021",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-122",
            "Vanessa Ong",
            "960408-07-5291",
            "A5019283",
            "vong96@gmail.com",
            "013-7281940",
            "M-1022",
            1));
    guestRepo.addGuest(
        new Guest(
            "G-123",
            "Hafiz Razak",
            "911203-01-5847",
            "A1829304",
            "hafiz.razak@yahoo.com",
            "014-3920184",
            "M-1023",
            1));
    guestRepo.addGuest(
        new Guest(
            "G-124",
            "Cynthia Yeoh",
            "880918-14-5632",
            "A8291049",
            "cynthia.yeoh@gmail.com",
            "011-3928104",
            "M-1024",
            3));
    guestRepo.addGuest(
        new Guest(
            "G-125",
            "Murali Krishnan",
            "870228-08-5109",
            "A4920183",
            "murali.k@hotmail.com",
            "018-7291048",
            "M-1025",
            2));

    // Silver Guests (G-126..G-140): 10 w/ 0 strikes, 2 w/ 1 strike, 3 w/ 2 strikes
    guestRepo.addGuest(
        new Guest(
            "G-126",
            "Samantha Goh",
            "970119-14-5402",
            "A3029184",
            "sammy.goh@gmail.com",
            "019-7392018",
            "M-1026",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-127",
            "Farhan Yusof",
            "930804-10-5193",
            "A9182039",
            "farhan.yusof@yahoo.com",
            "012-4829104",
            "M-1027",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-128",
            "Mei Ling Chan",
            "900516-08-5621",
            "A6291048",
            "meiling.chan@outlook.com",
            "016-3920184",
            "M-1028",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-129",
            "Suresh Pillai",
            "861211-05-5382",
            "A2019384",
            "spillai@gmail.com",
            "017-7492018",
            "M-1029",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-130",
            "Chloe Fernandez",
            "951023-14-5910",
            "A8392019",
            "chloe.f@gmail.com",
            "013-8201948",
            "M-1030",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-131",
            "Azman Hashim",
            "890407-03-5241",
            "A4729103",
            "azman.hashim@hotmail.com",
            "014-9382019",
            "M-1031",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-132",
            "Karen Ho",
            "920729-07-5098",
            "A1029384",
            "karen.ho92@gmail.com",
            "011-8291048",
            "M-1032",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-133",
            "Deepak Sharma",
            "880115-10-5731",
            "A7491029",
            "dsharma@yahoo.com",
            "018-3920184",
            "M-1033",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-134",
            "Nadia Osman",
            "940602-14-5129",
            "A5291048",
            "nadia.osman@gmail.com",
            "019-8291048",
            "M-1034",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-135",
            "Timothy Koh",
            "910911-08-5802",
            "A3829104",
            "tim.koh@outlook.com",
            "012-9382019",
            "M-1035",
            0));
    guestRepo.addGuest(
        new Guest(
            "G-136",
            "Amirah Mansor",
            "960318-01-5391",
            "A9012938",
            "amirah.m@gmail.com",
            "016-7392018",
            "M-1036",
            1));
    guestRepo.addGuest(
        new Guest(
            "G-137",
            "Bryan Low",
            "871125-14-5610",
            "A6102938",
            "bryan.low@yahoo.com",
            "017-8392019",
            "M-1037",
            1));
    guestRepo.addGuest(
        new Guest(
            "G-138",
            "Saravanan Vello",
            "850809-10-5283",
            "A2930184",
            "saravanan.v@gmail.com",
            "013-3920184",
            "M-1038",
            3));
    guestRepo.addGuest(
        new Guest(
            "G-139",
            "Rachel Tan",
            "931214-07-5749",
            "A8491029",
            "rachel.tan@hotmail.com",
            "014-7291048",
            "M-1039",
            2));
    guestRepo.addGuest(
        new Guest(
            "G-140",
            "Kamal Mustaffa",
            "900206-03-5102",
            "A4102938",
            "kamal.m@gmail.com",
            "011-9283019",
            "M-1040",
            2));

    // Non-Member Guests (G-141..G-180): 35 w/ 0 strikes, 3 w/ 1 strike, 2 w/ 2 strikes
    guestRepo.addGuest(
        new Guest(
            "G-141",
            "Benjamin Scott",
            "920512-14-6109",
            "B1029384",
            "ben.scott@gmail.com",
            "012-3029184",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-142",
            "Hannah Taylor",
            "940827-14-6382",
            "B9281039",
            "hannah.t@yahoo.com",
            "016-9281039",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-143",
            "Daniel Miller",
            "891014-14-6521",
            "B4820193",
            "dmiller@outlook.com",
            "017-3829104",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-144",
            "Sophia Wilson",
            "960103-14-6810",
            "B7392018",
            "sophia.w@gmail.com",
            "013-9281048",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-145",
            "Lucas Anderson",
            "910319-14-6047",
            "B2938104",
            "lucas.a@hotmail.com",
            "014-8291048",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-146",
            "Olivia Thomas",
            "930722-14-6298",
            "B8192039",
            "olivia.t@gmail.com",
            "011-3029184",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-147",
            "Ethan Jackson",
            "880905-14-6731",
            "B3920184",
            "ethan.j@yahoo.com",
            "018-9281039",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-148",
            "Ava White",
            "951218-14-6182",
            "B6019283",
            "ava.white@outlook.com",
            "019-3820194",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-149",
            "Mason Harris",
            "900411-14-6409",
            "B1829304",
            "mharris@gmail.com",
            "012-8291048",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-150",
            "Isabella Martin",
            "970630-14-6920",
            "B7291048",
            "isabella.m@gmail.com",
            "016-3029184",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-151",
            "James Thompson",
            "870214-14-6310",
            "B4102938",
            "jthompson@yahoo.com",
            "017-9281039",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-152",
            "Mia Garcia",
            "941108-14-6842",
            "B9028193",
            "mia.garcia@hotmail.com",
            "013-3829104",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-153",
            "Alexander Martinez",
            "910825-14-6019",
            "B2839104",
            "alex.m@gmail.com",
            "014-9281048",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-154",
            "Charlotte Robinson",
            "960517-14-6590",
            "B6729104",
            "charlotte.r@outlook.com",
            "011-8291039",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-155",
            "Henry Clark",
            "890129-14-6238",
            "B1092837",
            "henry.clark@gmail.com",
            "018-3029184",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-156",
            "Amelia Rodriguez",
            "931006-14-6701",
            "B8392019",
            "amelia.r@yahoo.com",
            "019-9281048",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-157",
            "Sebastian Lewis",
            "900712-14-6429",
            "B3928104",
            "slewis@gmail.com",
            "012-9281039",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-158",
            "Harper Lee",
            "950403-14-6912",
            "B7491029",
            "harper.lee@hotmail.com",
            "016-3829104",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-159",
            "Jack Walker",
            "881220-14-6150",
            "B2019384",
            "jack.walker@outlook.com",
            "017-8291048",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-160",
            "Evelyn Hall",
            "920915-14-6831",
            "B9102834",
            "evelyn.hall@gmail.com",
            "013-3029184",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-161",
            "Owen Allen",
            "940301-14-6092",
            "B4829103",
            "oallen@yahoo.com",
            "014-9281039",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-162",
            "Abigail Young",
            "910619-14-6620",
            "B8291049",
            "abigail.y@gmail.com",
            "011-3829104",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-163",
            "Wyatt Hernandez",
            "960811-14-6379",
            "B3029184",
            "wyatt.h@hotmail.com",
            "018-8291048",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-164",
            "Emily King",
            "891104-14-6702",
            "B6102938",
            "emily.king@outlook.com",
            "019-3029184",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-165",
            "Luke Wright",
            "930228-14-6241",
            "B1928374",
            "luke.wright@gmail.com",
            "012-9281048",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-166",
            "Ella Lopez",
            "950716-14-6518",
            "B9012938",
            "ella.lopez@yahoo.com",
            "016-3829104",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-167",
            "Oliver Hill",
            "880409-14-6930",
            "B4102938",
            "oliver.hill@gmail.com",
            "017-8291039",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-168",
            "Avery Scott",
            "921201-14-6172",
            "B7392018",
            "ascott@hotmail.com",
            "013-3029184",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-169",
            "Liam Green",
            "901024-14-6640",
            "B2839104",
            "liam.green@outlook.com",
            "014-9281048",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-170",
            "Scarlett Adams",
            "960907-14-6309",
            "B8392019",
            "scarlett.a@gmail.com",
            "011-3829104",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-171",
            "Noah Baker",
            "890615-14-6821",
            "B3928104",
            "noah.baker@yahoo.com",
            "018-8291039",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-172",
            "Grace Gonzalez",
            "930112-14-6098",
            "B6729104",
            "grace.g@gmail.com",
            "019-3029184",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-173",
            "William Nelson",
            "910520-14-6481",
            "B1092837",
            "wnelson@hotmail.com",
            "012-9281048",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-174",
            "Chloe Carter",
            "950803-14-6729",
            "B8291049",
            "chloe.carter@outlook.com",
            "016-3829104",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-175",
            "James Mitchell",
            "880326-14-6201",
            "B3029184",
            "jmitchell@gmail.com",
            "017-8291039",
            null,
            0));
    guestRepo.addGuest(
        new Guest(
            "G-176",
            "Victoria Perez",
            "920218-14-6940",
            "B7491029",
            "v.perez@yahoo.com",
            "013-3029184",
            null,
            1));
    guestRepo.addGuest(
        new Guest(
            "G-177",
            "Benjamin Roberts",
            "941129-14-6138",
            "B2019384",
            "broberts@gmail.com",
            "014-9281048",
            null,
            1));
    guestRepo.addGuest(
        new Guest(
            "G-178",
            "Zoe Turner",
            "900705-14-6602",
            "B9012938",
            "zoe.turner@hotmail.com",
            "011-3829104",
            null,
            1));
    guestRepo.addGuest(
        new Guest(
            "G-179",
            "Samuel Phillips",
            "960414-14-6389",
            "B4829103",
            "sphillips@outlook.com",
            "018-8291039",
            null,
            2));
    guestRepo.addGuest(
        new Guest(
            "G-180",
            "Lily Campbell",
            "890921-14-6750",
            "B8291049",
            "lily.c@gmail.com",
            "019-3029184",
            null,
            2));

    // Seed rooms
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

    // Seed reservations
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

    addVipWait(
        vipRepo, guestRepo, "G-102", Room.RoomType.LUXURY, 9500, false, now.minusMinutes(10));
    addVipWait(vipRepo, guestRepo, "G-103", Room.RoomType.LUXURY, 9200, false, now.minusMinutes(8));
    addVipWait(vipRepo, guestRepo, "G-104", Room.RoomType.LUXURY, 9000, false, now.minusMinutes(5));
    addVipWait(
        vipRepo, guestRepo, "G-105", Room.RoomType.LUXURY, 12000, true, now.minusMinutes(40));
    addVipWait(
        vipRepo, guestRepo, "G-113", Room.RoomType.LUXURY, 7500, false, now.minusMinutes(18));
    addVipWait(
        vipRepo, guestRepo, "G-114", Room.RoomType.LUXURY, 7200, false, now.minusMinutes(15));
    addVipWait(vipRepo, guestRepo, "G-115", Room.RoomType.SUITE, 7800, false, now.minusMinutes(20));
    addVipWait(vipRepo, guestRepo, "G-116", Room.RoomType.SUITE, 7400, false, now.minusMinutes(12));
    addVipWait(vipRepo, guestRepo, "G-117", Room.RoomType.SUITE, 10500, true, now.minusMinutes(45));
    addVipWait(vipRepo, guestRepo, "G-129", Room.RoomType.SUITE, 4800, false, now.minusMinutes(35));
    addVipWait(vipRepo, guestRepo, "G-130", Room.RoomType.SUITE, 4500, false, now.minusMinutes(28));
    addVipWait(vipRepo, guestRepo, "G-131", Room.RoomType.SUITE, 4200, false, now.minusMinutes(22));
    addVipWait(vipRepo, guestRepo, "G-132", Room.RoomType.SUITE, 4000, false, now.minusMinutes(16));
    addVipWait(
        vipRepo, guestRepo, "G-118", Room.RoomType.STANDARD, 7600, false, now.minusMinutes(20));
    addVipWait(
        vipRepo, guestRepo, "G-119", Room.RoomType.STANDARD, 10800, true, now.minusMinutes(45));
    addVipWait(
        vipRepo, guestRepo, "G-133", Room.RoomType.STANDARD, 4900, false, now.minusMinutes(40));
    addVipWait(
        vipRepo, guestRepo, "G-134", Room.RoomType.STANDARD, 4400, false, now.minusMinutes(30));
    addVipWait(
        vipRepo, guestRepo, "G-135", Room.RoomType.STANDARD, 4100, false, now.minusMinutes(22));

    Reservation vc1 =
        addVipCheckedIn(
            vipRepo,
            guestRepo,
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
            guestRepo,
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
            guestRepo,
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
            guestRepo,
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
            guestRepo,
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
            guestRepo,
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
            guestRepo,
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
            guestRepo,
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
            guestRepo,
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
            guestRepo,
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
            guestRepo,
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
            guestRepo,
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

    Reservation vco1 =
        addVipCheckedOut(
            vipRepo, guestRepo, "G-102", Room.RoomType.LUXURY, "L-801", now.minusDays(4), 5, 10, 2);
    Reservation vco2 =
        addVipCheckedOut(
            vipRepo, guestRepo, "G-103", Room.RoomType.LUXURY, "L-802", now.minusDays(2), 4, 10, 3);
    Reservation vco3 =
        addVipCheckedOut(
            vipRepo, guestRepo, "G-112", Room.RoomType.SUITE, "S-501", now.minusDays(5), 10, 15, 1);
    Reservation vco4 =
        addVipCheckedOut(
            vipRepo, guestRepo, "G-130", Room.RoomType.SUITE, "S-502", now.minusDays(3), 15, 20, 2);
    Reservation vco5 =
        addVipCheckedOut(
            vipRepo,
            guestRepo,
            "G-131",
            Room.RoomType.STANDARD,
            "ST-101",
            now.minusDays(6),
            14,
            20,
            2);
    Reservation vco6 =
        addVipCheckedOut(
            vipRepo,
            guestRepo,
            "G-132",
            Room.RoomType.STANDARD,
            "ST-102",
            now.minusDays(1),
            16,
            20,
            1);

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

    addVipNoShow(vipRepo, guestRepo, "G-109", Room.RoomType.LUXURY, now.minusHours(4), 10, 10);
    addVipNoShow(vipRepo, guestRepo, "G-110", Room.RoomType.LUXURY, now.minusHours(2), 10, 10);
    addVipNoShow(vipRepo, guestRepo, "G-124", Room.RoomType.SUITE, now.minusHours(5), 15, 15);
    addVipNoShow(vipRepo, guestRepo, "G-125", Room.RoomType.SUITE, now.minusHours(3), 15, 15);
    addVipNoShow(vipRepo, guestRepo, "G-139", Room.RoomType.STANDARD, now.minusHours(6), 20, 20);
    addVipNoShow(vipRepo, guestRepo, "G-140", Room.RoomType.STANDARD, now.minusHours(1), 20, 20);

    addStdNoShow(standardRepo, "G-162", Room.RoomType.STANDARD, now.minusHours(3));
    addStdNoShow(standardRepo, "G-163", Room.RoomType.STANDARD, now.minusDays(1).minusHours(2));
    addStdNoShow(standardRepo, "G-164", Room.RoomType.SUITE, now.minusDays(3).minusHours(4));
    addStdNoShow(standardRepo, "G-137", Room.RoomType.LUXURY, now.minusHours(1));

    // Seed housekeeping staff and tasks
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

    vipRepo.applySettingsToQueue(
        vipSystemConfigRepo.getConfig(), guestRepo, memberRepo, false, true);

    System.out.println("Master mock database seeded successfully!");
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
            null,
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
            null,
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
            null,
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
      GuestRepo guestRepo,
      String guestId,
      Room.RoomType roomType,
      int score,
      boolean boiling,
      LocalDateTime queuedAt) {
    Guest guest = (guestRepo != null) ? guestRepo.findById(guestId) : null;
    if (guest == null || guest.getMemberId() == null) return;

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

  private static Reservation addVipCheckedIn(
      VipReservationRepo repo,
      GuestRepo guestRepo,
      RoomRepo roomRepo,
      String guestId,
      Room.RoomType roomType,
      String roomNumber,
      LocalDateTime queuedAt,
      int holdingUsedMins,
      int graceMins,
      int nights) {
    Guest guest = (guestRepo != null) ? guestRepo.findById(guestId) : null;
    if (guest == null || guest.getMemberId() == null) return null;

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
      GuestRepo guestRepo,
      String guestId,
      Room.RoomType roomType,
      String roomNumber,
      LocalDateTime queuedAt,
      int holdingUsedMins,
      int graceMins,
      int nights) {
    Guest guest = (guestRepo != null) ? guestRepo.findById(guestId) : null;
    if (guest == null || guest.getMemberId() == null) return null;

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
      GuestRepo guestRepo,
      String guestId,
      Room.RoomType roomType,
      LocalDateTime queuedAt,
      int holdingUsedMins,
      int graceMins) {
    Guest guest = (guestRepo != null) ? guestRepo.findById(guestId) : null;
    if (guest == null || guest.getMemberId() == null) return;

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
