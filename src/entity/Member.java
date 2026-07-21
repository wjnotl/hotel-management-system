package entity;

public class Member {
  public static enum LoyaltyTier {
    DIAMOND,
    GOLD,
    SILVER,
    NON_MEMBER
  }

  private String memberId;
  private LoyaltyTier tier;
  private int points; // Accumulated points balance
}
