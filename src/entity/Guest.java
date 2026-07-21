package entity;

public class Guest {
  private String guestId; // Unique customer ID (e.g., "G-10023")
  private String name; // Full Name
  private String icOrPassport; // IC or Passport Number
  private String memberId; // Links to Member card (null if non-member)
}
