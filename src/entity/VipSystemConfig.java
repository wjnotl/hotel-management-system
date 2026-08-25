package entity;

import java.io.Serializable;
import java.time.LocalDate;

public class VipSystemConfig implements Serializable {
  private static final long serialVersionUID = 1L;

  private String activeStrategyName;
  private String activeFormulaInfix;

  // Tier-Specific Patience Limits / SLA Targets (in minutes)
  private int diamondPatienceLimitMins;
  private int goldPatienceLimitMins;
  private int silverPatienceLimitMins;

  // Tier-Specific Boiling Point Limits / Boost Triggers (in minutes)
  private int diamondBoilingLimitMins;
  private int goldBoilingLimitMins;
  private int silverBoilingLimitMins;

  // Tier-Specific No-Show Grace Windows (in minutes)
  private int diamondGraceWindowMins;
  private int goldGraceWindowMins;
  private int silverGraceWindowMins;

  // Tier-Specific Max Strikes Limits
  private int diamondMaxStrikes;
  private int goldMaxStrikes;
  private int silverMaxStrikes;

  // Tier Base Importance Values (TIER)
  private int diamondBaseValue;
  private int goldBaseValue;
  private int silverBaseValue;

  // Tier Boiling Boosts (W_boiling)
  private double diamondBoilingBoost;
  private double goldBoilingBoost;
  private double silverBoilingBoost;

  // Tier Strike Penalties (W_strike)
  private double diamondStrikePenalty;
  private double goldStrikePenalty;
  private double silverStrikePenalty;

  // Executive Report Alert Targets (%)
  private double diamondSlaTargetPct;
  private double goldSlaTargetPct;
  private double silverSlaTargetPct;

  private double diamondEvictionRateTargetPct;
  private double goldEvictionRateTargetPct;
  private double silverEvictionRateTargetPct;

  private double diamondGraceUtilTargetPct;
  private double goldGraceUtilTargetPct;
  private double silverGraceUtilTargetPct;

  private LocalDate lastStrikeResetDate;

  public VipSystemConfig() {
    resetToDefaults();
  }

  public void resetToDefaults() {
    this.activeStrategyName = "Balanced Lobby Flow";
    this.activeFormulaInfix = "TIER + ( BOILING * W_BOILING ) - ( STRIKES * W_STRIKE )";

    // Patience Limits (SLA Targets)
    this.diamondPatienceLimitMins = 15;
    this.goldPatienceLimitMins = 30;
    this.silverPatienceLimitMins = 45;

    // Boiling Limits (Boost Triggers)
    this.diamondBoilingLimitMins = 10;
    this.goldBoilingLimitMins = 20;
    this.silverBoilingLimitMins = 30;

    this.diamondGraceWindowMins = 10;
    this.goldGraceWindowMins = 15;
    this.silverGraceWindowMins = 20;

    this.diamondMaxStrikes = 3;
    this.goldMaxStrikes = 2;
    this.silverMaxStrikes = 2;

    this.diamondBaseValue = 9000;
    this.goldBaseValue = 7000;
    this.silverBaseValue = 5000;

    this.diamondBoilingBoost = 5000.0;
    this.goldBoilingBoost = 3000.0;
    this.silverBoilingBoost = 1500.0;

    this.diamondStrikePenalty = 1000.0;
    this.goldStrikePenalty = 500.0;
    this.silverStrikePenalty = 200.0;

    this.diamondSlaTargetPct = 90.0;
    this.goldSlaTargetPct = 80.0;
    this.silverSlaTargetPct = 70.0;

    this.diamondEvictionRateTargetPct = 5.0;
    this.goldEvictionRateTargetPct = 10.0;
    this.silverEvictionRateTargetPct = 15.0;

    this.diamondGraceUtilTargetPct = 70.0;
    this.goldGraceUtilTargetPct = 80.0;
    this.silverGraceUtilTargetPct = 85.0;

    this.lastStrikeResetDate = null;
  }

  public String getActiveStrategyName() {
    return activeStrategyName;
  }

  public String getActiveFormulaInfix() {
    return activeFormulaInfix;
  }

  public int getDiamondPatienceLimitMins() {
    return diamondPatienceLimitMins;
  }

  public int getGoldPatienceLimitMins() {
    return goldPatienceLimitMins;
  }

  public int getSilverPatienceLimitMins() {
    return silverPatienceLimitMins;
  }

  public int getDiamondBoilingLimitMins() {
    return diamondBoilingLimitMins;
  }

  public int getGoldBoilingLimitMins() {
    return goldBoilingLimitMins;
  }

  public int getSilverBoilingLimitMins() {
    return silverBoilingLimitMins;
  }

  public int getDiamondGraceWindowMins() {
    return diamondGraceWindowMins;
  }

  public int getGoldGraceWindowMins() {
    return goldGraceWindowMins;
  }

  public int getSilverGraceWindowMins() {
    return silverGraceWindowMins;
  }

  public int getDiamondMaxStrikes() {
    return diamondMaxStrikes;
  }

  public int getGoldMaxStrikes() {
    return goldMaxStrikes;
  }

  public int getSilverMaxStrikes() {
    return silverMaxStrikes;
  }

  public int getDiamondBaseValue() {
    return diamondBaseValue;
  }

  public int getGoldBaseValue() {
    return goldBaseValue;
  }

  public int getSilverBaseValue() {
    return silverBaseValue;
  }

  public double getDiamondBoilingBoost() {
    return diamondBoilingBoost;
  }

  public double getGoldBoilingBoost() {
    return goldBoilingBoost;
  }

  public double getSilverBoilingBoost() {
    return silverBoilingBoost;
  }

  public double getDiamondStrikePenalty() {
    return diamondStrikePenalty;
  }

  public double getGoldStrikePenalty() {
    return goldStrikePenalty;
  }

  public double getSilverStrikePenalty() {
    return silverStrikePenalty;
  }

  public double getDiamondSlaTargetPct() {
    return diamondSlaTargetPct;
  }

  public double getGoldSlaTargetPct() {
    return goldSlaTargetPct;
  }

  public double getSilverSlaTargetPct() {
    return silverSlaTargetPct;
  }

  public double getDiamondEvictionRateTargetPct() {
    return diamondEvictionRateTargetPct;
  }

  public double getGoldEvictionRateTargetPct() {
    return goldEvictionRateTargetPct;
  }

  public double getSilverEvictionRateTargetPct() {
    return silverEvictionRateTargetPct;
  }

  public double getDiamondGraceUtilTargetPct() {
    return diamondGraceUtilTargetPct;
  }

  public double getGoldGraceUtilTargetPct() {
    return goldGraceUtilTargetPct;
  }

  public double getSilverGraceUtilTargetPct() {
    return silverGraceUtilTargetPct;
  }

  public LocalDate getLastStrikeResetDate() {
    return lastStrikeResetDate;
  }

  public void setActiveStrategyName(String activeStrategyName) {
    this.activeStrategyName = activeStrategyName;
  }

  public void setActiveFormulaInfix(String activeFormulaInfix) {
    this.activeFormulaInfix = activeFormulaInfix;
  }

  public void setDiamondPatienceLimitMins(int val) {
    this.diamondPatienceLimitMins = val;
  }

  public void setGoldPatienceLimitMins(int val) {
    this.goldPatienceLimitMins = val;
  }

  public void setSilverPatienceLimitMins(int val) {
    this.silverPatienceLimitMins = val;
  }

  public void setDiamondBoilingLimitMins(int val) {
    this.diamondBoilingLimitMins = val;
  }

  public void setGoldBoilingLimitMins(int val) {
    this.goldBoilingLimitMins = val;
  }

  public void setSilverBoilingLimitMins(int val) {
    this.silverBoilingLimitMins = val;
  }

  public void setDiamondGraceWindowMins(int val) {
    this.diamondGraceWindowMins = val;
  }

  public void setGoldGraceWindowMins(int val) {
    this.goldGraceWindowMins = val;
  }

  public void setSilverGraceWindowMins(int val) {
    this.silverGraceWindowMins = val;
  }

  public void setDiamondMaxStrikes(int val) {
    this.diamondMaxStrikes = val;
  }

  public void setGoldMaxStrikes(int val) {
    this.goldMaxStrikes = val;
  }

  public void setSilverMaxStrikes(int val) {
    this.silverMaxStrikes = val;
  }

  public void setDiamondBaseValue(int val) {
    this.diamondBaseValue = val;
  }

  public void setGoldBaseValue(int val) {
    this.goldBaseValue = val;
  }

  public void setSilverBaseValue(int val) {
    this.silverBaseValue = val;
  }

  public void setDiamondBoilingBoost(double val) {
    this.diamondBoilingBoost = val;
  }

  public void setGoldBoilingBoost(double val) {
    this.goldBoilingBoost = val;
  }

  public void setSilverBoilingBoost(double val) {
    this.silverBoilingBoost = val;
  }

  public void setDiamondStrikePenalty(double val) {
    this.diamondStrikePenalty = val;
  }

  public void setGoldStrikePenalty(double val) {
    this.goldStrikePenalty = val;
  }

  public void setSilverStrikePenalty(double val) {
    this.silverStrikePenalty = val;
  }

  public void setDiamondSlaTargetPct(double val) {
    this.diamondSlaTargetPct = val;
  }

  public void setGoldSlaTargetPct(double val) {
    this.goldSlaTargetPct = val;
  }

  public void setSilverSlaTargetPct(double val) {
    this.silverSlaTargetPct = val;
  }

  public void setDiamondEvictionRateTargetPct(double val) {
    this.diamondEvictionRateTargetPct = val;
  }

  public void setGoldEvictionRateTargetPct(double val) {
    this.goldEvictionRateTargetPct = val;
  }

  public void setSilverEvictionRateTargetPct(double val) {
    this.silverEvictionRateTargetPct = val;
  }

  public void setDiamondGraceUtilTargetPct(double val) {
    this.diamondGraceUtilTargetPct = val;
  }

  public void setGoldGraceUtilTargetPct(double val) {
    this.goldGraceUtilTargetPct = val;
  }

  public void setSilverGraceUtilTargetPct(double val) {
    this.silverGraceUtilTargetPct = val;
  }

  public void setLastStrikeResetDate(LocalDate lastStrikeResetDate) {
    this.lastStrikeResetDate = lastStrikeResetDate;
  }

  // Tier-Based Helper Methods
  public int getPatienceLimitMins(Member.LoyaltyTier tier) {
    if (tier == Member.LoyaltyTier.DIAMOND) return diamondPatienceLimitMins;
    if (tier == Member.LoyaltyTier.GOLD) return goldPatienceLimitMins;
    return silverPatienceLimitMins;
  }

  public int getBoilingLimitMins(Member.LoyaltyTier tier) {
    if (tier == Member.LoyaltyTier.DIAMOND) return diamondBoilingLimitMins;
    if (tier == Member.LoyaltyTier.GOLD) return goldBoilingLimitMins;
    return silverBoilingLimitMins;
  }

  public int getGraceWindowMins(Member.LoyaltyTier tier) {
    if (tier == Member.LoyaltyTier.DIAMOND) return diamondGraceWindowMins;
    if (tier == Member.LoyaltyTier.GOLD) return goldGraceWindowMins;
    return silverGraceWindowMins;
  }

  public int getMaxStrikes(Member.LoyaltyTier tier) {
    if (tier == Member.LoyaltyTier.DIAMOND) return diamondMaxStrikes;
    if (tier == Member.LoyaltyTier.GOLD) return goldMaxStrikes;
    return silverMaxStrikes;
  }

  public int getBaseValue(Member.LoyaltyTier tier) {
    if (tier == Member.LoyaltyTier.DIAMOND) return diamondBaseValue;
    if (tier == Member.LoyaltyTier.GOLD) return goldBaseValue;
    return silverBaseValue;
  }

  public double getBoilingBoost(Member.LoyaltyTier tier) {
    if (tier == Member.LoyaltyTier.DIAMOND) return diamondBoilingBoost;
    if (tier == Member.LoyaltyTier.GOLD) return goldBoilingBoost;
    return silverBoilingBoost;
  }

  public double getStrikePenalty(Member.LoyaltyTier tier) {
    if (tier == Member.LoyaltyTier.DIAMOND) return diamondStrikePenalty;
    if (tier == Member.LoyaltyTier.GOLD) return goldStrikePenalty;
    return silverStrikePenalty;
  }

  public double getSlaTargetPct(Member.LoyaltyTier tier) {
    if (tier == Member.LoyaltyTier.DIAMOND) return diamondSlaTargetPct;
    if (tier == Member.LoyaltyTier.GOLD) return goldSlaTargetPct;
    return silverSlaTargetPct;
  }

  public double getEvictionRateTargetPct(Member.LoyaltyTier tier) {
    if (tier == Member.LoyaltyTier.DIAMOND) return diamondEvictionRateTargetPct;
    if (tier == Member.LoyaltyTier.GOLD) return goldEvictionRateTargetPct;
    return silverEvictionRateTargetPct;
  }

  public double getGraceUtilTargetPct(Member.LoyaltyTier tier) {
    if (tier == Member.LoyaltyTier.DIAMOND) return diamondGraceUtilTargetPct;
    if (tier == Member.LoyaltyTier.GOLD) return goldGraceUtilTargetPct;
    return silverGraceUtilTargetPct;
  }

  public Member.LoyaltyTier getTierFromIndex(int index) {
    if (index == 1) return Member.LoyaltyTier.DIAMOND;
    if (index == 2) return Member.LoyaltyTier.GOLD;
    return Member.LoyaltyTier.SILVER;
  }

  @Override
  public String toString() {
    return "VipSystemConfig{"
        + "activeStrategyName='"
        + activeStrategyName
        + "'"
        + ", activeFormulaInfix='"
        + activeFormulaInfix
        + "'"
        + ", diamondPatienceLimitMins="
        + diamondPatienceLimitMins
        + ", goldPatienceLimitMins="
        + goldPatienceLimitMins
        + ", silverPatienceLimitMins="
        + silverPatienceLimitMins
        + ", diamondBoilingLimitMins="
        + diamondBoilingLimitMins
        + ", goldBoilingLimitMins="
        + goldBoilingLimitMins
        + ", silverBoilingLimitMins="
        + silverBoilingLimitMins
        + ", diamondGraceWindowMins="
        + diamondGraceWindowMins
        + ", goldGraceWindowMins="
        + goldGraceWindowMins
        + ", silverGraceWindowMins="
        + silverGraceWindowMins
        + ", lastStrikeResetDate='"
        + lastStrikeResetDate
        + "'"
        + "}";
  }
}
