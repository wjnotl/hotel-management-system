package entity;

import java.io.Serializable;

public class Member implements Serializable {
  private static final long serialVersionUID = 1L;

  public static enum LoyaltyTier {
    DIAMOND,
    GOLD,
    SILVER
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

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    Member other = (Member) obj;
    return memberId != null && memberId.equals(other.memberId);
  }

  @Override
  public int hashCode() {
    return memberId != null ? memberId.hashCode() : 0;
  }
}
