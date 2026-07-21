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

  public Member(String memberId, LoyaltyTier tier, int points) {
    this.memberId = memberId;
    this.tier = tier;
    this.points = points;
  }

  public String getMemberId() {
    return memberId;
  }

  public LoyaltyTier getTier() {
    return tier;
  }

  public int getPoints() {
    return points;
  }

  public void setMemberId(String memberId) {
    this.memberId = memberId;
  }

  public void setTier(LoyaltyTier tier) {
    this.tier = tier;
  }

  public void setPoints(int points) {
    this.points = points;
  }
}
